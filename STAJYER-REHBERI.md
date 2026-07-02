# Stajyer Rehberi — etiya-etrade Mikroservis Projesi

Hoş geldin! 🎉 Bu doküman, projeye ilk kez bakan birine **projenin ne olduğunu, nasıl kurgulandığını ve neden böyle tasarlandığını** anlatmak için yazıldı. Kod okumadan önce bunu bir kez baştan sona oku; sonra kodda kaybolmazsın.

Kurulum/çalıştırma adımları için: **[KURULUM-VE-CALISTIRMA.md](KURULUM-VE-CALISTIRMA.md)**.

---

## 1. Bu proje ne yapıyor?

Basit bir **e-ticaret** sisteminin backend'i. Müşteri, ürün, sipariş, sepet ve bildirim gibi işleri **ayrı ayrı küçük servisler** olarak yönetir. Yani tek dev bir uygulama (monolit) yerine, her biri kendi işinden sorumlu **mikroservisler** var.

**Neden mikroservis?**
- Her servis bağımsız geliştirilip dağıtılabilir.
- Bir servis çökse bile diğerleri ayakta kalabilir.
- Her servisin kendi veritabanı vardır (veri izolasyonu).

**Bedeli:** Servisler artık birbirini metot çağırır gibi çağıramaz; ağ üzerinden haberleşmeleri gerekir. Bu projede haberleşme **asenkron** (mesajlaşma) ile yapılır — nedenini birazdan göreceğiz.

---

## 2. Kuşbakışı mimari

```
                       ┌─────────────────┐
   Dış İstekler  ──────►   gateway-server │  (8080)  tek giriş kapısı
   (Postman/UI)        └────────┬─────────┘
                                │ (Eureka'dan servis adresini bulur)
        ┌───────────────┬───────┼─────────────┬───────────────┐
        ▼               ▼       ▼             ▼               ▼
 product-service  order-service customer-service cart-service notification-service
        │               │       │             │               │
        └───── hepsi Eureka'ya kaydolur, config'i Config Server'dan çeker ──────┘

        Servisler arası konuşma:  ┌── Apache Kafka (mesaj kuyruğu) ──┐
                                   olaylar (events) buradan akar
```

Üç tür "altyapı" servisi + beş tür "iş" servisi var:

### Altyapı servisleri
| Servis | Port | Görevi |
|--------|------|--------|
| **config-server** | 8888 | Tüm servislerin konfigürasyonunu tek yerden (GitHub'daki `configs/` klasörü) dağıtır. |
| **eureka-server** | 8761 | Servis keşfi (service discovery). Her servis "ben buradayım" diye buraya kaydolur; diğerleri "X servisi nerede?" diye buraya sorar. |
| **gateway-server** | 8080 | API Gateway. Dış dünyadan gelen tüm istekler önce buraya gelir, doğru servise yönlendirilir. |

### İş (domain) servisleri
| Servis | Sorumluluğu | Veritabanı (H2) |
|--------|-------------|-----------------|
| **product-service** | Ürün yönetimi (CRUD) | in-memory liste |
| **order-service** | Sipariş yönetimi | `orderdb` |
| **customer-service** | Müşteri yönetimi | `customerdb` |
| **cart-service** | Alışveriş sepeti | `cartdb` |
| **notification-service** | Bildirim gönderme (olayları dinler) | `notificationdb` |

> Her iş servisinin **kendi** veritabanı var. Bir servis başka servisin veritabanına asla doğrudan erişmez.

---

## 3. Altyapı servislerini biraz daha yakından

### 3.1. Config Server — "tek doğru konfigürasyon kaynağı"
Normalde her servisin `application.yml` dosyası kendi içinde olurdu. Bu projede konfigürasyon **dışarı çıkarıldı**: hepsi bu reponun kökündeki `configs/` klasöründe, servis adına göre klasörlenmiş hâlde durur:

```
configs/
  order-service/
    application.yml         ← ortak ayarlar (porttan bağımsız)
    application-local.yml   ← local profil ayarları
    application-test.yml    ← test profil ayarları
    application-prod.yml    ← prod profil ayarları
  customer-service/
    ...
```

config-server bu klasörü GitHub'dan okur ve her servise **kendi** ayarlarını dağıtır. Servisin kendi içindeki `application.yml` ise incecik kaldı — sadece "adım ne, hangi profildeyim, config-server nerede" der:

```yaml
spring:
  application: { name: order-service }
  profiles: { active: local }
  config:
    import: configserver:http://localhost:8888
```

**Profiller (local / test / prod):** Aynı servisin farklı ortamlarda farklı ayarlarla çalışması için. Örneğin:
- `local` → Kafka `localhost:9092`, SQL logları açık, H2 konsolu açık
- `prod` → Kafka `kafka-prod:9092`, loglar kısık (WARN), H2 konsolu kapalı

### 3.2. Eureka — "servis rehberi"
Servislerin portu `0` (yani rastgele). O zaman order-service, product-service'i nasıl bulacak? Adres/port sabit değil ki.

Çözüm: Her servis açılınca Eureka'ya **adıyla** kaydolur (`product-service şu IP:port'ta`). Başka bir servise ulaşmak gerektiğinde Eureka'ya "product-service nerede?" diye sorulur. Gateway de yönlendirmeyi böyle yapar (`lb://product-service`).

### 3.3. Gateway — "tek kapı"
Dışarıdaki istemci (Postman, tarayıcı, mobil) servislerin tek tek portlarını bilmek zorunda kalmasın diye her şey `http://localhost:8080` üzerinden girer. Gateway, yol (path) desenine göre isteği doğru servise iletir:

```
/api/products/**       → product-service
/api/orders/**         → order-service
/api/customers/**      → customer-service
/api/carts/**          → cart-service
/api/notifications/**  → notification-service
```

---

## 4. Servisler nasıl haberleşiyor? (İşin kalbi 💗)

**Kural: Senkron çağrı YOK.** Yani order-service, "bu müşteri var mı?" diye customer-service'i HTTP ile **anlık** çağırmaz. Bunun yerine **olay (event) tabanlı asenkron** haberleşme kullanılır.

**Neden?** Senkron çağrıda A servisi B'yi beklerken, B çökerse A da çöker (zincirleme arıza). Asenkronda A, bir olay yayınlar ve işine devam eder; B olayı hazır olduğunda işler. Servisler birbirinden **gevşek bağlı** olur.

Haberleşme kanalı: **Apache Kafka**. Servisler Kafka'daki "topic"lere mesaj (olay) yazar ve okur. Java tarafında bunu **Spring Cloud Stream** soyutlaması ile yaparız (Kafka'nın detaylarını gizler).

### Olay akışları

```
customer-service ──(CustomerCreated/Updated/Deleted)──► [customer-events] topic
                                                              │
                          ┌───────────────────────────────────┼───────────────────────────┐
                          ▼                                     ▼                           ▼
                   order-service                         cart-service            notification-service
              (müşteriyi lokal kopyaya alır,        (aynı şekilde kopya tutar,   ("hoş geldin" bildirimi)
               customerId doğrulaması için)          sepet için doğrulama)


order-service ──(OrderCreated)──► [order-created] topic
                                        ├──► product-service        (mevcut örnek akış)
                                        └──► notification-service   ("siparişiniz alındı")


cart-service ──(CartCheckedOut)──► [cart-checked-out] topic
                                        └──► notification-service   ("sepetiniz onaylandı")
```

### Peki "müşteri var mı?" kontrolü senkron değilse nasıl yapılıyor?
Çok önemli bir fikir: **lokal replika (yerel kopya).**

- customer-service, her müşteri değişikliğinde bir olay yayınlar.
- order-service ve cart-service bu olayları dinler ve **kendi veritabanlarında** küçük bir "müşteri kopyası" tablosu (`customer_replica`) tutar.
- Sipariş/sepet oluştururken, `customerId` bu **yerel kopyadan** kontrol edilir — customer-service'e hiç gidilmez.

Buna **eventual consistency (nihai tutarlılık)** denir: kopya birkaç saniye gecikmeyle güncel hâle gelir. Bu yüzden müşteri oluşturduktan hemen sonra sipariş açmak isterse "müşteri henüz replike olmadı" hatası normaldir.

---

## 5. İki kritik desen: Outbox ve Inbox

Asenkron mesajlaşmada iki klasik problem vardır. Bu proje bunları iki desenle çözer.

### 5.1. Transactional Outbox — "mesajı kaybetme"
**Problem:** Bir servis hem veritabanına yazıp hem Kafka'ya mesaj göndermek ister. Ya veritabanı yazıldı ama tam o an Kafka'ya gönderilemedi? Mesaj kaybolur, sistem tutarsız kalır.

**Çözüm:** Mesajı Kafka'ya **doğrudan** göndermek yerine, aynı veritabanı transaction'ı içinde bir **`outbox_events`** tablosuna yaz. Böylece "veriyi kaydet" ve "mesajı kuyruğa al" ya birlikte olur ya da hiç olmaz (atomik).

Sonra arka planda çalışan bir **poller** (`OutboxMessageRelay`, her 15 sn'de bir) bu tabloyu tarar, `PENDING` kayıtları Kafka'ya gönderir ve `PUBLISHED` olarak işaretler. Gönderilemezse tekrar dener (`FAILED` olana kadar).

Kod: her servisin `outbox/` paketi (`OutboxEvent`, `OutboxService`, `OutboxMessageRelay`).

### 5.2. Inbox / Idempotency — "aynı mesajı iki kez işleme"
**Problem:** Kafka "en az bir kez" (at-least-once) teslim eder. Yani aynı mesaj bazen iki kez gelebilir. Aynı bildirimi iki kez göndermek ya da kopyayı iki kez uygulamak istemeyiz.

**Çözüm:** Her olayın benzersiz bir `eventId`'si vardır. Tüketici (consumer), olayı işlemeden önce `inbox_messages` tablosuna bakar: "bu `eventId`'yi daha önce işledim mi?" Evetse **atlar**. Hayırsa işler ve `eventId`'yi kaydeder — hepsi tek transaction'da.

Kod: `order-service`, `cart-service`, `notification-service` içindeki `inbox/` paketi ve `messaging/*EventHandler` sınıfları (`@Transactional`).

> **Özet:** Outbox = "gönderirken kaybetme", Inbox = "alırken tekrar işleme". İkisi birlikte **güvenilir asenkron teslimat** sağlar.

---

## 6. Kod nasıl düzenlenmiş? (Katmanlı yapı)

Her iş servisi aynı klasör düzenini takip eder. Örnek: `order-service`

```
com.etiya.orderservice
├── controllers/          REST uç noktaları (@RestController). Sadece HTTP işi.
│   └── GlobalExceptionHandler   Hataları düzgün JSON'a çevirir.
├── services/
│   ├── abstracts/        Arayüz (interface) — sözleşme (örn. OrderService)
│   ├── concretes/        Uygulama (örn. OrderManager) — iş kuralları burada
│   ├── dtos/
│   │   ├── requests/     Dışarıdan gelen veri (CreateOrderRequest)
│   │   └── responses/    Dışarıya dönen veri (CreatedOrderResponse)
│   └── exceptions/       BusinessException (iş kuralı ihlali)
├── entities/             Veritabanı/domain nesneleri (Order)
├── repositories/         Veri erişimi
├── events/               Kafka olay tanımları (record'lar)
├── outbox/               Transactional Outbox altyapısı
├── inbox/                Idempotency (Inbox) altyapısı
├── customers/            Lokal müşteri replikası (customerId doğrulaması için)
└── messaging/            Kafka consumer'ları (olay dinleyiciler)
```

**Akış (bir POST isteğinde):**
```
Controller → Service arayüzü → Manager (iş kuralı) → Repository (DB)
                                     └→ OutboxService (olay kuyruğa)
```

**Neden arayüz + Manager?** Controller, somut sınıfa değil **arayüze** bağımlıdır. Bu, kodu test edilebilir ve değiştirilebilir kılar (bağımlılıkların tersine çevrilmesi / dependency inversion).

---

## 7. Küçük sözlük

| Terim | Anlamı |
|-------|--------|
| **Mikroservis** | Tek bir işten sorumlu, bağımsız çalışan küçük servis. |
| **Service Discovery (Eureka)** | Servislerin birbirini adıyla bulmasını sağlayan rehber. |
| **API Gateway** | Dış isteklerin geçtiği tek kapı; yönlendirme yapar. |
| **Config Server** | Konfigürasyonu merkezî olarak dağıtan servis. |
| **Profil (local/test/prod)** | Aynı kodun farklı ortam ayarlarıyla çalışması. |
| **Event (olay)** | "Bir şey oldu" bilgisini taşıyan mesaj (örn. OrderCreated). |
| **Topic** | Kafka'da olayların yazıldığı/okunduğu kanal. |
| **Producer / Consumer** | Olay yazan / olay okuyan taraf. |
| **Outbox Pattern** | Olayı DB ile aynı transaction'da kaydedip sonra güvenle yayınlama. |
| **Inbox / Idempotency** | Aynı olayı tekrar işlememek için tekilleştirme. |
| **Eventual Consistency** | Verinin kopyalar arasında kısa gecikmeyle tutarlı hâle gelmesi. |
| **DTO** | Katmanlar/servisler arası veri taşıyan sade nesne. |
| **Replika** | Başka servisin verisinin, doğrulama için tutulan yerel kopyası. |

---

## 8. Nereden okumaya başlamalı?

Önerilen sıra (kolaydan zora):

1. **product-service** — en basit CRUD servisi. Controller → Manager → Repository akışını gör.
2. **customer-service** — CRUD + **Outbox** (olay üretimi). `CustomerManager.recordEvent()` ve `outbox/` paketine bak.
3. **order-service** — **Inbox** + **replika** + `customerId` doğrulaması. `messaging/CustomerEventHandler` ve `services/concretes/OrderManager` önemli.
4. **cart-service** — sepet aggregate'i (Cart + CartItem) + hem tüketici hem üretici.
5. **notification-service** — saf tüketici; üç farklı topic'i idempotent şekilde dinler.
6. **gateway-server / eureka-server / config-server** — altyapı; konfigürasyon dosyalarına bakman yeterli.

---

## 9. Kendini dene (alıştırmalar)

- [ ] Projeyi ayağa kaldır ve Eureka panelinde (http://localhost:8761) tüm servisleri gör.
- [ ] Gateway üzerinden bir müşteri oluştur, sonra o `customerId` ile sipariş aç. "Henüz replike olmadı" hatası alırsan neden olduğunu açıkla.
- [ ] H2 konsolundan `order-service`'in `customer_replica` ve `inbox_messages` tablolarına bak. İçlerinde ne var?
- [ ] Aynı müşteri olayı iki kez gelseydi Inbox tablosu ne yapardı? Kodda `CustomerEventHandler`'da bunu bul.
- [ ] `configs/order-service/application-local.yml` içinde `outbox.poller.fixed-delay`'i düşür, yeniden başlat, replikasyonun hızlandığını gözlemle.
- [ ] Yeni bir servis eklesen (örn. `payment-service`) hangi adımları izlerdin? (Modül + config klasörü + gateway route + gerekiyorsa Outbox/Inbox.)

Takıldığın yerde ekibe sormaktan çekinme. İyi çalışmalar! 🚀
