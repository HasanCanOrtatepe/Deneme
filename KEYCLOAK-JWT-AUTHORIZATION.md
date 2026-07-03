# Keycloak ile JWT Tabanlı Yetkilendirme (order-service & product-service)

Bu doküman, `order-service` ve `product-service`'in Keycloak'tan beslenerek
**JWT (Bearer token) tabanlı authorization** yapması için yapılan değişiklikleri
ve Keycloak tarafında yapılması gereken adımları anlatır.

- Keycloak: `http://localhost:8180`
- Realm: `etiya-project`
- Issuer URI: `http://localhost:8180/realms/etiya-project`

Her iki servis birer **OAuth2 Resource Server** olarak çalışır: gelen her isteğin
`Authorization: Bearer <token>` başlığındaki JWT, Keycloak realm'inin public key'i
(JWK) ile doğrulanır. Token geçersiz/eksikse istek servise ulaşmadan reddedilir.

---

## 1. Mimari akış

```
  Kullanıcı ──(username+password)──▶ Keycloak (etiya-project realm)
       │                                   │
       │◀──────── access_token (JWT) ──────┘
       │
       └──(Authorization: Bearer <JWT>)──▶ order-service / product-service
                                                │
                                                ├─ JWT imza doğrulama (JWK, issuer-uri'den)
                                                ├─ realm_access.roles ─▶ ROLE_* dönüşümü
                                                └─ endpoint yetki kuralları (hasRole ...)
```

Servis, açılışta `issuer-uri` üzerinden Keycloak'ın
`/.well-known/openid-configuration` ucunu okuyarak public key uçlarını keşfeder.
**Bu yüzden servis başlatılırken Keycloak ayakta olmalıdır.**

---

## 2. Kod tarafında yapılan değişiklikler

### 2.1 Bağımlılık (her iki `pom.xml`)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### 2.2 `issuer-uri` konfigürasyonu
Her servisin kendi `src/main/resources/application.yml` dosyasına eklendi
(uygulama jar'ıyla birlikte paketlenir, uzak config repoya push gerektirmez):

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/etiya-project}
```

> `KEYCLOAK_ISSUER_URI` ortam değişkeni ile ortama göre (test/prod) override edilebilir.
> `configs/<service>/application.yml` altına da aynı satırlar eklendi; config'i tamamen
> uzak config-server reposundan yönetmek istenirse "doğru yer" orasıdır.

### 2.3 `KeycloakRealmRoleConverter`
Keycloak, realm rollerini JWT'de `realm_access.roles` claim'inde string listesi
olarak taşır. Spring Security ise `ROLE_` önekli authority bekler. Converter bu
dönüşümü yapar; böylece `hasRole("admin")` doğrudan çalışır.

### 2.4 `SecurityConfig` (yetki kuralları)
Ortak davranış:
- Token tabanlı, **stateless** REST API (`SessionCreationPolicy.STATELESS`, CSRF kapalı).
- `/actuator/**` (ve order-service'te `/h2-console/**`) serbest.
- JWT doğrulama + Keycloak rol dönüşümü aktif.

Endpoint yetkileri (koddaki güncel hâl):

| Servis | Endpoint | Metot | Gerekli yetki |
|---|---|---|---|
| order-service | `/api/orders/**` | GET | **herkese açık** (permitAll) |
| order-service | `/api/orders/**` | POST / PUT / DELETE | `admin` rolü |
| product-service | `/api/products/**` | GET | `user` veya `admin` rolü |
| product-service | `/api/products/**` | POST / PUT / DELETE | `admin` rolü |

> Not: İki serviste okuma (GET) kuralı bilinçli olarak farklıdır. İstenirse
> order-service GET'i de `.hasAnyRole("user", "admin")` yapılarak korumalı hâle getirilebilir.

**Değişen/eklenen dosyalar:**
- `order-service/pom.xml`, `product-service/pom.xml`
- `order-service/src/main/resources/application.yml`, `product-service/src/main/resources/application.yml`
- `order-service/.../security/SecurityConfig.java`, `.../security/KeycloakRealmRoleConverter.java`
- `product-service/.../security/SecurityConfig.java`, `.../security/KeycloakRealmRoleConverter.java`

---

## 3. Keycloak tarafında yapılacaklar (`etiya-project` realm'i)

Konsol: `http://localhost:8180` → sol üstten **etiya-project** realm'ini seç.

### 3.1 Client oluştur
**Clients → Create client**
- Client type: **OpenID Connect**
- Client ID: `etiya-app`
- **Next**
- Client authentication: **OFF** (public client)
- Authentication flow: **Direct access grants** işaretli olsun
- **Save**

### 3.2 Realm rolleri oluştur
**Realm roles → Create role**
- `user`
- `admin`

> İsimler kodla birebir eşleşmeli (küçük harf `user`, `admin`).

### 3.3 Kullanıcı oluştur ve rol ata
**Users → Add user** → Username gir → **Create**
- **Credentials → Set password** → parola, **Temporary: OFF**
- **Role mapping → Assign role** → `admin` (ve/veya `user`) ata

Test için iki kullanıcı önerilir:
- `ahmet` → `admin` (her şeyi yapabilir)
- `mehmet` → `user` (sadece okuma yetkisi olan senaryolar için)

---

## 4. Uçtan uca test

### 4.1 Token al
```bash
curl -X POST http://localhost:8180/realms/etiya-project/protocol/openid-connect/token \
  -d "client_id=etiya-app" \
  -d "grant_type=password" \
  -d "username=ahmet" \
  -d "password=<parola>"
```
Yanıttaki `access_token` alanını kullan.

### 4.2 İstek at
```bash
# Token yok  -> product-service: 401
curl -i http://localhost:8080/api/products

# admin token -> 200
curl http://localhost:8080/api/products \
  -H "Authorization: Bearer <access_token>"

# user rolüyle POST -> 403 (sadece admin yazabilir)
curl -i -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer <user_access_token>" \
  -H "Content-Type: application/json" \
  -d '{ ... }'
```

### 4.3 Beklenen sonuçlar
| Senaryo | Sonuç |
|---|---|
| Token'sız korumalı istek | **401 Unauthorized** |
| Geçerli token, yetersiz rol | **403 Forbidden** |
| `admin` rolü, yazma işlemi | **200 / 201** |
| product-service GET, `user` rolü | **200** |
| order-service GET (token'sız) | **200** (permitAll) |

---

## 5. Sık karşılaşılan hatalar

- **`Method filterChain ... required a bean of type JwtDecoder`**
  → `issuer-uri` uygulamaya ulaşmıyor. `application.yml`'de tanımlı olduğundan ve
  Keycloak'ın erişilebilir olduğundan emin ol.
- **Servis başlamıyor / issuer metadata alınamıyor**
  → Keycloak (`8180`) ayakta değil ya da realm adı yanlış.
- **403 alıyorum ama admin olmalıyım**
  → Kullanıcıya realm rolü **atanmamış** olabilir; ya da rol client rolü olarak
  tanımlanmış (bu kurulum **realm** rollerini bekler, `realm_access.roles`).
- **Token doğrulanmıyor (401)**
  → Token başka bir realm/issuer'dan alınmış olabilir; `issuer-uri` ile token'ın
  `iss` claim'i birebir aynı olmalı.
