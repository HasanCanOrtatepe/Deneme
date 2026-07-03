# Kurulum ve Çalıştırma Rehberi

Bu doküman, **etiya-etrade** mikroservis projesini sıfırdan ayağa kaldırmak için gereken tüm adımları içerir.

---

## 1. Ön Gereksinimler

| Araç | Sürüm | Not |
|------|-------|-----|
| **JDK** | 21+ | Proje Java 21 hedefler (25 ile de derlenir). `java -version` ile doğrula. |
| **Maven** | 3.9+ | `mvn -v`. Projede wrapper (`mvnw`) yok, sistemdeki `mvn` kullanılır. |
| **Podman** veya **Docker** | güncel | Kafka, PostgreSQL, MySQL, Redis ve Keycloak'ı container olarak çalıştırmak için. |
| **Git** | güncel | config-server konfigürasyonu GitHub'dan çeker; internet erişimi gerekir. |

> ⚠️ **Önemli:** `config-server`, konfigürasyonu `https://github.com/HasanCanOrtatepe/Deneme.git` reposunun `main` dalındaki `configs/` klasöründen okur. Bu yüzden:
> - Repo **public** olmalı (ya da config-server'a GitHub token eklenmeli — bkz. [Bölüm 7](#7-sık-karşılaşılan-sorunlar)).
> - Konfigürasyonda değişiklik yaptığında **commit + push** etmelisin; aksi halde config-server eski hâli okur.

---

## 2. Altyapıyı Başlat (Kafka + Veritabanları + Redis + Keycloak)

Servisler arası haberleşme Kafka üzerinden, veri kalıcılığı PostgreSQL/MySQL üzerinden,
cache Redis üzerinden yürür. Önce altyapıyı ayağa kaldır:

```bash
cd infra
podman compose -f podman-compose.yml up -d
# Docker kullanıyorsan:  docker compose -f podman-compose.yml up -d
```

Bu komut şunları başlatır:

| Container | Adres | Ne için? |
|-----------|-------|----------|
| **Kafka** (KRaft modu) | `localhost:9092` | Servisler arası async mesajlaşma |
| **PostgreSQL** | `localhost:5433` | `productdb` (product), `notificationdb` (notification), `customerdb` (customer), `keycloakdb` (Keycloak). Kullanıcı: `postgres` / `postgres` |
| **MySQL** | `localhost:3307` | `orderdb` (order), `cartdb` (cart). Kullanıcı: `etiya` / `etiya` (root: `root`) |
| **Redis** | `localhost:6379` | Tüm servislerin ortak cache katmanı (TTL 10 dk) |
| **Keycloak** | `localhost:8180` | Kimlik yönetimi (JWT). Admin: `admin` / `admin` |

> ℹ️ `*-db-init` adlı container'lar (keycloak/notification/customer/mysql) tek seferlik çalışıp
> ilgili veritabanını oluşturur ve kapanır; `podman ps -a`'da **Exited** görünmeleri normaldir.
> Tablolar ise servisler ilk açıldığında Hibernate (`ddl-auto: update`) tarafından otomatik oluşturulur.

Durumu kontrol et:
```bash
podman ps
```

Kafka topic'leri (`order-created`, `customer-events`, `cart-checked-out`) servisler ilk mesajı gönderdiğinde **otomatik** oluşur; elle oluşturmana gerek yok.

Altyapıyı durdurmak için:
```bash
podman compose -f podman-compose.yml down        # verileri korur
podman compose -f podman-compose.yml down -v      # volume'leri de siler (DB verileri dahil!)
```

> ⚠️ Servis verileri artık gerçek veritabanlarında (PostgreSQL/MySQL) tutulduğundan
> `down -v` tüm sipariş/müşteri/ürün verilerini kalıcı olarak siler.

---

## 3. Projeyi Derle

Repo kökünde:

```bash
mvn clean install -DskipTests
```

Tüm 9 modül derlenir: `config-server`, `eureka-server`, `product-service`, `order-service`, `customer-service`, `cart-service`, `notification-service`, `gateway-server` (+ parent).

---

## 4. Servisleri Başlat (Sıra Önemli!)

Servisler birbirine bağımlı olduğundan **şu sırayla** başlatılmalı. Her biri için **ayrı bir terminal** aç.

| Sıra | Servis | Port | Neden önce? |
|------|--------|------|-------------|
| 1 | **config-server** | 8888 | Diğer herkes konfigürasyonunu bundan çeker. |
| 2 | **eureka-server** | 8761 | Servis keşfi; diğerleri buraya kaydolur. |
| 3 | product / order / customer / cart / notification | rastgele (0) | İş servisleri. |
| 4 | **gateway-server** | 8080 | Dış dünyaya tek giriş kapısı. |

> ℹ️ **Not:** Gateway **8080** portunda (eskiden 8888'di; 8888 config-server'a verildi). Dışarıdan tüm istekler `http://localhost:8080` üzerinden gider.

### Yöntem A — Maven ile (geliştirme için pratik)

```bash
# 1. Config Server
mvn -pl config-server spring-boot:run

# 2. Eureka
mvn -pl eureka-server spring-boot:run

# 3. İş servisleri (her biri ayrı terminalde)
mvn -pl product-service   spring-boot:run
mvn -pl order-service     spring-boot:run
mvn -pl customer-service  spring-boot:run
mvn -pl cart-service      spring-boot:run
mvn -pl notification-service spring-boot:run

# 4. Gateway
mvn -pl gateway-server spring-boot:run
```

### Yöntem B — JAR ile

```bash
mvn clean package -DskipTests
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar
java -jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar
java -jar product-service/target/product-service-0.0.1-SNAPSHOT.jar
# ... diğerleri aynı şekilde
```

### Profil seçme (local / test / prod)

Varsayılan profil **local**'dir. Farklı profille başlatmak için:

```bash
# Maven ile
mvn -pl order-service spring-boot:run -Dspring-boot.run.profiles=test

# JAR ile
java -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Ortam değişkeni ile (tüm servisler için)
export SPRING_PROFILES_ACTIVE=test   # Windows PowerShell: $env:SPRING_PROFILES_ACTIVE="test"
```

---

## 5. Ayağa Kalktığını Doğrula

- **Config Server** bir servisin konfigürasyonunu döndürüyor mu?
  ```
  http://localhost:8888/order-service/local
  http://localhost:8888/product-service/prod
  ```
  (JSON olarak profil ayarlarını görmelisin.)

- **Eureka paneli** — kayıtlı servisleri listeler:
  ```
  http://localhost:8761
  ```

- **Veritabanları** — servisler tablolarını ilk açılışta oluşturur; container içinden kontrol edebilirsin:
  ```bash
  # PostgreSQL (product / notification / customer)
  podman exec -it etiya-postgres psql -U postgres -d productdb -c "\dt"
  podman exec -it etiya-postgres psql -U postgres -d customerdb -c "SELECT * FROM customers;"

  # MySQL (order / cart)
  podman exec -it etiya-mysql mysql -uetiya -petiya orderdb -e "SHOW TABLES;"
  podman exec -it etiya-mysql mysql -uetiya -petiya cartdb -e "SELECT * FROM carts;"
  ```
  Host'tan bir DB istemcisiyle bağlanmak istersen: PostgreSQL → `localhost:5433`, MySQL → `localhost:3307`.

- **Redis cache** — bir GET isteği attıktan sonra cache anahtarlarını gör:
  ```bash
  podman exec -it etiya-redis redis-cli KEYS "*"
  # örn. "products::1", "ordersList::all", "activeCarts::1" gibi anahtarlar görünür
  podman exec -it etiya-redis redis-cli GET "products::1"
  ```

- **Keycloak admin konsolu** (JWT ayarları için):
  ```
  http://localhost:8180   (admin / admin)
  ```

---

## 6. Uçtan Uca Deneme (Gateway üzerinden)

Tüm istekler gateway'den (`http://localhost:8080`) geçer.

> 🔐 **JWT gerekli:** `order-service` ve `product-service`'in **yazma** uçları (POST/PUT/DELETE)
> Keycloak'tan alınmış, `admin` rolüne sahip bir Bearer token ister (GET uçları serbesttir).
> Realm/kullanıcı kurulumu ve ayrıntılar için: **[KEYCLOAK-JWT-AUTHORIZATION.md](KEYCLOAK-JWT-AUTHORIZATION.md)**.

```bash
# 0) Admin token al (etiya-project realm'inde admin rollü bir kullanıcıyla)
TOKEN=$(curl -s -X POST http://localhost:8180/realms/etiya-project/protocol/openid-connect/token \
  -d "client_id=etiya-app" \
  -d "grant_type=password" \
  -d "username=<admin-kullanici>" \
  -d "password=<sifre>" | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

# 1) Müşteri oluştur
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","phone":"555-0100"}'
# -> id: 1 döner. Bu olay customer-events topic'ine yayınlanır.

# 2) ~15 sn bekle (outbox poller aralığı) — order/cart bu müşteriyi lokal replikasına alsın.

# 3) Sepete ürün ekle
curl -X POST http://localhost:8080/api/carts/items \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"productId":10,"quantity":2,"unitPrice":49.90}'

# 4) Sipariş oluştur (admin token gerekli)
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"productId":10,"quantity":2,"unitPrice":49.90,"address":"Istanbul"}'

# 5) Sepeti checkout et
curl -X POST http://localhost:8080/api/carts/1/checkout

# 6) Bildirimleri gör (notification-service tüm olayları dinler)
curl http://localhost:8080/api/notifications/1

# 7) Aynı GET'i iki kez at — ikincisi Redis cache'inden döner (DB'ye SQL gitmez)
curl http://localhost:8080/api/orders/1
curl http://localhost:8080/api/orders/1
```

> ⏱️ **Neden bekleme var?** Outbox poller varsayılan **15 saniyede bir** çalışır (`outbox.poller.fixed-delay`). Müşteri oluşturduktan hemen sonra sipariş açmaya çalışırsan `"Customer not found ... not yet replicated"` hatası alabilirsin; bu **beklenen** async davranıştır. Kısaltmak için ilgili `configs/*/application-local.yml` içinde `fixed-delay`'i düşürebilirsin.

---

## 7. Sık Karşılaşılan Sorunlar

| Belirti | Sebep / Çözüm |
|---------|---------------|
| Servisler başlarken `Could not resolve placeholder` / config hatası | **config-server** kapalı ya da geç kalktı. Önce onu başlat. |
| config-server açılıyor ama boş/yanlış config veriyor | `configs/` değişiklikleri **push edilmemiş**. `git push` yap. config-server remote git'ten okur. |
| config-server `Authentication failed` (private repo) | `config-server/src/main/resources/application.yml` içindeki `git.username` / `git.password` (GitHub PAT) satırlarını doldur. |
| Açılışta `Connection refused` → PostgreSQL (5433) / MySQL (3307) | Altyapı ayakta mı? `podman ps` ile `etiya-postgres` / `etiya-mysql` container'larını kontrol et. |
| `Unknown database 'cartdb'` / `database "customerdb" does not exist` | İlgili `*-db-init` container'ı çalışmamış. `podman compose up -d` ile tekrar tetikle, `podman logs etiya-mysql-db-init` ile kontrol et. |
| Açılışta veya ilk GET'te Redis `Connection refused` (6379) | `etiya-redis` container'ı ayakta mı? `podman exec -it etiya-redis redis-cli ping` → `PONG` dönmeli. |
| order/product POST'ta **401 Unauthorized** | `Authorization: Bearer <token>` başlığı yok ya da token süresi dolmuş (varsayılan ~5 dk). Yeni token al. |
| order/product POST'ta **403 Forbidden** | Token geçerli ama kullanıcıda `admin` realm rolü yok. Bkz. [KEYCLOAK-JWT-AUTHORIZATION.md](KEYCLOAK-JWT-AUTHORIZATION.md). |
| GET eski/bayat veri dönüyor | Redis cache'i (TTL 10 dk). DB'yi elle değiştirdiysen cache'i temizle: `podman exec -it etiya-redis redis-cli FLUSHALL`. |
| Sipariş/sepette `Customer not found ... not yet replicated` | Müşteri olayı henüz replikaya ulaşmadı. ~15 sn bekle veya poller aralığını düşür. |
| Servisler Eureka'da görünmüyor | **eureka-server** kapalı ya da config-server üzerinden yanlış `defaultZone`. local profilde çalıştığından emin ol. |
| Kafka bağlantı hatası | Altyapı ayakta mı? `podman ps`. Kafka `localhost:9092`'de olmalı. |
| Port 8080/8761/8888/8180 dolu | Başka bir uygulama portu kullanıyor. Kapat ya da ilgili config'te portu değiştir. |

---

## 8. Faydalı Adresler Özeti

| Ne | Adres |
|----|-------|
| Gateway (dış giriş) | http://localhost:8080 |
| Config Server | http://localhost:8888 |
| Eureka paneli | http://localhost:8761 |
| Keycloak admin konsolu | http://localhost:8180 (admin / admin) |
| Config örnek | http://localhost:8888/{servis-adı}/{profil} |
| PostgreSQL | localhost:5433 (postgres / postgres) — productdb, notificationdb, customerdb |
| MySQL | localhost:3307 (etiya / etiya) — orderdb, cartdb |
| Redis | localhost:6379 — `podman exec -it etiya-redis redis-cli` |

Mimari ve kavramsal anlatım için: **[STAJYER-REHBERI.md](STAJYER-REHBERI.md)**.
JWT/Keycloak kurulumu ve yetkilendirme kuralları için: **[KEYCLOAK-JWT-AUTHORIZATION.md](KEYCLOAK-JWT-AUTHORIZATION.md)**.
