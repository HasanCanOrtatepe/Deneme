# Kurulum ve Çalıştırma Rehberi

Bu doküman, **etiya-etrade** mikroservis projesini sıfırdan ayağa kaldırmak için gereken tüm adımları içerir.

---

## 1. Ön Gereksinimler

| Araç | Sürüm | Not |
|------|-------|-----|
| **JDK** | 21+ | Proje Java 21 hedefler (25 ile de derlenir). `java -version` ile doğrula. |
| **Maven** | 3.9+ | `mvn -v`. Projede wrapper (`mvnw`) yok, sistemdeki `mvn` kullanılır. |
| **Podman** veya **Docker** | güncel | Kafka'yı (ve opsiyonel Postgres) container olarak çalıştırmak için. |
| **Git** | güncel | config-server konfigürasyonu GitHub'dan çeker; internet erişimi gerekir. |

> ⚠️ **Önemli:** `config-server`, konfigürasyonu `https://github.com/HasanCanOrtatepe/Deneme.git` reposunun `main` dalındaki `configs/` klasöründen okur. Bu yüzden:
> - Repo **public** olmalı (ya da config-server'a GitHub token eklenmeli — bkz. [Bölüm 7](#7-sık-karşılaşılan-sorunlar)).
> - Konfigürasyonda değişiklik yaptığında **commit + push** etmelisin; aksi halde config-server eski hâli okur.

---

## 2. Altyapıyı Başlat (Kafka)

Servisler arası haberleşme Kafka üzerinden yürür. Önce altyapıyı ayağa kaldır:

```bash
cd infra
podman compose -f podman-compose.yml up -d
# Docker kullanıyorsan:  docker compose -f podman-compose.yml up -d
```

Bu komut şunları başlatır:
- **Kafka** (KRaft modu) → `localhost:9092`
- **PostgreSQL** → `localhost:5433` (şu an servisler H2 kullanıyor, Postgres ileride kullanım için hazır bekler)

Durumu kontrol et:
```bash
podman ps
```

Kafka topic'leri (`order-created`, `customer-events`, `cart-checked-out`) servisler ilk mesajı gönderdiğinde **otomatik** oluşur; elle oluşturmana gerek yok.

Altyapıyı durdurmak için:
```bash
podman compose -f podman-compose.yml down        # verileri korur
podman compose -f podman-compose.yml down -v      # volume'leri de siler
```

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

- **H2 konsolları** (yalnızca local/test profillerinde açık):
  ```
  http://localhost:<servis-portu>/h2-console
  JDBC URL örnekleri: jdbc:h2:mem:orderdb, jdbc:h2:mem:customerdb, jdbc:h2:mem:cartdb, jdbc:h2:mem:notificationdb
  Kullanıcı: sa   Şifre: (boş)
  ```
  (Servis portunu Eureka panelinden görebilirsin, çünkü port `0` = rastgele.)

---

## 6. Uçtan Uca Deneme (Gateway üzerinden)

Tüm istekler gateway'den (`http://localhost:8080`) geçer.

```bash
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

# 4) Sipariş oluştur
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"productId":10,"quantity":2,"unitPrice":49.90,"address":"Istanbul"}'

# 5) Sepeti checkout et
curl -X POST http://localhost:8080/api/carts/1/checkout

# 6) Bildirimleri gör (notification-service tüm olayları dinler)
curl http://localhost:8080/api/notifications/1
```

> ⏱️ **Neden bekleme var?** Outbox poller varsayılan **15 saniyede bir** çalışır (`outbox.poller.fixed-delay`). Müşteri oluşturduktan hemen sonra sipariş açmaya çalışırsan `"Customer not found ... not yet replicated"` hatası alabilirsin; bu **beklenen** async davranıştır. Kısaltmak için ilgili `configs/*/application-local.yml` içinde `fixed-delay`'i düşürebilirsin.

---

## 7. Sık Karşılaşılan Sorunlar

| Belirti | Sebep / Çözüm |
|---------|---------------|
| Servisler başlarken `Could not resolve placeholder` / config hatası | **config-server** kapalı ya da geç kalktı. Önce onu başlat. |
| config-server açılıyor ama boş/yanlış config veriyor | `configs/` değişiklikleri **push edilmemiş**. `git push` yap. config-server remote git'ten okur. |
| config-server `Authentication failed` (private repo) | `config-server/src/main/resources/application.yml` içindeki `git.username` / `git.password` (GitHub PAT) satırlarını doldur. |
| Sipariş/sepette `Customer not found ... not yet replicated` | Müşteri olayı henüz replikaya ulaşmadı. ~15 sn bekle veya poller aralığını düşür. |
| Servisler Eureka'da görünmüyor | **eureka-server** kapalı ya da config-server üzerinden yanlış `defaultZone`. local profilde çalıştığından emin ol. |
| Kafka bağlantı hatası | Altyapı ayakta mı? `podman ps`. Kafka `localhost:9092`'de olmalı. |
| Port 8080/8761/8888 dolu | Başka bir uygulama portu kullanıyor. Kapat ya da ilgili config'te portu değiştir. |

---

## 8. Faydalı Adresler Özeti

| Ne | Adres |
|----|-------|
| Gateway (dış giriş) | http://localhost:8080 |
| Config Server | http://localhost:8888 |
| Eureka paneli | http://localhost:8761 |
| Config örnek | http://localhost:8888/{servis-adı}/{profil} |
| H2 konsol | http://localhost:{servis-portu}/h2-console |

Mimari ve kavramsal anlatım için: **[STAJYER-REHBERI.md](STAJYER-REHBERI.md)**.
