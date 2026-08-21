# Bildirim Bütçe

Banka bildirimlerinden harcamalarınızı otomatik çıkaran, **%100 çevrimdışı** Android
bütçe uygulaması.

İnternet izni istemez. Okunan bildirim metinleri cihazı terk etmez.

---

## Durum

| Katman | Durum |
|---|---|
| `:parser` (ayrıştırma, kategori, defter mantığı) | **Kotlin 1.9.24 ile derlendi ve çalıştırıldı.** 167 örnekte %100, uçtan uca akış 8/8 |
| `:app` (Android UI, Room, servis, widget) | Yazıldı, **cihazda derlenmedi** — Android SDK gerektirir |

Doğrulamayı kendiniz çalıştırın:

```bash
gradle :parser:verify
```

Çıktı:

```
Desen seti v1: 5 desen, 7 ignore kurali, 17 kaynak

--- 1) Ayristirma dogrulugu ---
  EXPENSE  115/115  (100.0%)
  IGNORE    32/32   (100.0%)
  NONE       8/8    (100.0%)
  REFUND    12/12   (100.0%)
  TOPLAM   167/167  (100.0%)

--- 2) Uctan uca akis (ayristir -> tekrar koru -> kategorile -> topla) ---
  gelen bildirim      : 8
  kaydedilen islem    : 5
  ay toplami          : 1.821,80 TL
  [GECTI] tekrarlanan bildirim yok sayildi
  [GECTI] OTP islem sayilmadi
  ...
SONUC: TUM DOGRULAMALAR GECTI
```

### %100 ne anlama geliyor, ne anlama gelmiyor

`fixtures.tsv` içindeki 167 örnek **`scripts/generate_corpus.py` tarafından
üretilmiştir** — gerçek banka bildirimlerine benzetilmiş şablonlardır, gerçek
bildirim değildir.

Bu sayı şunu kanıtlar: ayrıştırıcı 20 farklı cümle yapısı, kuruşlu/kuruşsuz
tutarlar, binlik ayraçlar, yabancı para, iadeler, OTP/bakiye/kampanya gürültüsü
karşısında tutarlı davranıyor.

Bu sayı şunu **kanıtlamaz**: gerçek bankaların gerçek bildirimlerinde de aynı
sonucu verecek. Ground truth üreteçten geliyor, gerçeklikten değil.
Yayına çıkmadan önce mutlaka gerçek örnek toplayın (aşağıya bakın).

---

## Neden bir uygulama, neden web sitesi değil

Tarayıcı `NotificationListenerService`'e erişemez. Bir web sitesi bu işi
teknik olarak yapamaz — uygulamanın indirilme sebebi budur. İkinci sebep ana
ekran widget'ıdır.

iOS'ta bu mimari mümkün değildir; proje Android'e özgüdür.

---

## Mimari

```
patterns/patterns.json     ← Tek kaynak: regex desenleri (koda gömülü DEĞİL)
   │
   ├─→ parser/  (saf JVM, SIFIR bağımlılık)  ← iş mantığının tamamı burada
   │     BankNotificationParser, MerchantCleaner, Money, Categorizer, Ledger
   │     Json (kendi minimal okuyucumuz — bağımlılık olmasın diye)
   │     testler: fixtures.tsv üzerinden doğruluk + uçtan uca simülasyon
   │
   └─→ app/     (Android)
         NotificationService  → parse → Room → widget güncelle
```

### Neden desenler ayrı bir JSON dosyasında

Türkiye'de 15+ banka, her birinin birden fazla bildirim şablonu var ve bunlar
habersiz değişiyor. Desenler koda gömülü olsaydı her düzeltme için Play sürüm
incelemesi (1–3 gün) beklemek gerekirdi. JSON olarak tutulduğunda aynı gün
güncelleme yayınlanabilir.

Uzaktan güncelleme **varsayılan olarak kapalıdır** (manifest'te INTERNET izni
yok). Açmak isterseniz `PatternProvider.installUpdate()` hazır bekliyor — ama o
zaman `PRIVACY.md` dosyasını da güncellemeniz gerekir.

### Neden parser ayrı ve sıfır bağımlılıklı

Emulator, Android SDK, hatta Maven bağımlılığı olmadan derlenip çalıştırılabilsin
diye. Kategori tahmini, tekrar koruması (`sourceKey`) ve aylık toplama da bu
modülde — Android katmanı yalnızca Room'a çevirip ekrana basan ince bir kabuk.

Pratik sonucu: bu projede **iş mantığının tamamı** `kotlinc` + `java` ile,
internet bağlantısı olmadan doğrulanabilir. JSON okuyucusu bile kendi
içinde (`Json.kt`, ~150 satır) — sadece `patterns.json` okunacağı için tam bir
JSON kütüphanesine gerek yok.

---

## Kurulum

```bash
git clone <repo-url>
cd bildirim-butce

# Gradle wrapper jar'ı repoda yok; bir kez üretin:
gradle wrapper --gradle-version 8.7
# (veya projeyi Android Studio ile açın, otomatik üretilir)

./gradlew :parser:test          # ayrıştırıcı doğruluğu
./gradlew :app:assembleDebug    # APK
```

Gereksinimler: JDK 17, Android SDK 34, Android Studio Koala veya üstü.

---

## İlk yapılması gereken: gerçek fixture toplamak

Bu projede riskin tamamı regex kalitesinde. Yanlış ayrıştırma yapan bir finans
uygulaması bir daha açılmaz.

Depoda hazır gelen 167 örnek sentetiktir; gerçek olanları **eklemelisiniz**
(silmenize gerek yok, ikisi bir arada çalışır).

1. Kendi telefonunuzdan ve 4–5 kişiden gerçek banka bildirimi metinlerini
   toplayın (kart numarası ve isim gibi kısımları maskeleyin).
2. Her birini test setine ekleyin:

```bash
./scripts/add-fixture.sh "Garanti BBVA: ... 245,90 TL harcama yapılmıştır." EXPENSE 24590 "Migros"
./scripts/add-fixture.sh "Tek kullanımlık şifreniz 123456" IGNORE - -
```

3. Ölçün:

```bash
gradle :parser:verify   # ayrıntılı rapor
gradle :parser:test     # CI eşiği (%95 altında build kırılır)
```

Sentetik korpusu yeniden üretmek isterseniz:

```bash
python3 scripts/generate_corpus.py > parser/src/test/resources/fixtures.tsv
```

**Karar noktası:** 150–200 gerçek örnekte doğruluk %95'in altında kalıyorsa ve
`patterns.json`'a desen ekleyerek yükselmiyorsa, projeyi yayınlamayın. Test
%95 eşiğinin altında build'i kırar.

Kolon anlamları (`fixtures.tsv`, TAB ile ayrılır):

| kolon | anlam |
|---|---|
| `text` | bildirim metni |
| `kind` | `EXPENSE` / `REFUND` / `IGNORE` (OTP, bakiye, kampanya) / `NONE` (eşleşmemeli) |
| `amountMinor` | kuruş — `245,90 TL` → `24590` |
| `merchant` | beklenen işyeri adı, yoksa `-` |

---

## Ele alınmış tuzaklar

Bunlar teoride değil, geliştirme sırasında testlerde yakalandı:

- **Tekrarlanan bildirimler.** Android aynı bildirimi güncellendiğinde tekrar
  teslim eder. `sourceKey` üzerindeki unique index olmadan kullanıcı tek
  harcamayı listede 3–4 kez görür. Saat kovası kullanılır, böylece aynı
  mağazadan ertesi saat yapılan gerçek alışveriş kaybolmaz.
- **Türkçe locale, noktasız ı.** `"MIGROS".lowercase(tr)` → `"mıgros"`. Marka
  adları için ROOT locale kullanılır; stopword eşleştirmesinde ise TR locale
  doğrudur (`"YAPILMIŞTIR"` → `"yapılmıştır"`).
- **Fiil kayması.** `"Kartınızla 12.500,00 TL harcama yapılmıştır"` cümlesinde
  regex "yapılmıştır" kelimesini mağaza adı sanıyordu. Stopword listesi ve
  zorunlu yer eki (`bünyesinde`, `'de`) ile engellendi.
- **Zincirlenmiş ön ekler.** `"1234 kartınız ile MIGROS"` — tek geçişlik
  temizlik `"ile Migros"` bırakıyordu; sabit noktaya kadar döngü gerekiyor.
- **OTP kodları.** `"Onay kodu: 998877 - 250,00 TL tutarındaki işleminiz"`
  harcama sanılabilirdi; ignore listesi desenlerden önce çalışır.
- **Marka adı büyük/küçük harf.** Önceki kural "4 harften kısa ve tamamı büyük
  harfse dokunma" idi; bu `SOK MARKETLER` → `SOK Marketler`,
  `BURGER KING` → `Burger KING` üretiyordu. Artık yalnızca rakam içeren
  (`A101`, `N11`) veya `patterns.json`'daki `brandTokens` listesindeki kelimeler
  korunur.
- **Dolgu kelimeler.** `"... 3.499,00 TL tutarında kartlı işlem yapıldı"` —
  para birimi ile eylem kelimesi arasına giren kelime deseni tamamen
  kaçırıyordu (bu şablondaki 5 örneğin tamamı `NONE` dönüyordu).
- **Eksik hâl ekleri.** `"GETIR PERAKENDE'den ... ödeme yapıldı"` — `'de/'da`
  vardı ama `'den/'dan` yoktu, işyeri adı kayboluyordu.
- **Bakiye ve limit bildirimleri.** `"bakiyeniz"` ignore listesindeydi ama
  `"bakiyesi"` değildi.
- **Eksik fonksiyon.** `expand()` (makro genişletme) hiç yazılmamıştı — derleyici
  yakaladı. Android SDK'sız derleme imkânı olmasa bu ilk build'e kadar
  görünmezdi.
- **Para birimi ve `Double`.** Tutarlar `Long` kuruş olarak saklanır.

---

## Yol haritası

**v1 kapsamı — bunun dışına çıkmayın:**

- [x] Ayrıştırıcı + veri odaklı test altyapısı
- [x] Room deposu, tekrar koruması
- [x] Ana ekran: aylık toplam, kategori dağılımı, işlem listesi
- [x] Düzeltme sayfası + mağaza→kategori öğrenmesi
- [x] Ana ekran widget'ı
- [x] Sentetik korpus (167 örnek) + uçtan uca simülasyon
- [ ] **Gerçek fixture toplama (150+)** ← sıradaki iş
- [ ] Android katmanını cihazda derleyip çalıştırma
- [ ] Onboarding akışı (izin ekranına yönlendirme metni)
- [ ] Manuel harcama girişi — izin verilmese de uygulama çalışmalı

**Bilerek dışarıda bırakılanlar:** bütçe hedefleri, çoklu para birimi, dışa
aktarma, hesap eşleştirme, bulut senkronizasyonu.

---

## Play Store notu

Bildirim erişimi (`BIND_NOTIFICATION_LISTENER_SERVICE`) kullanan uygulamalar
Play Console'da ek gerekçelendirme ve gizlilik beyanı gerektirir; reddedilme
riski sıfır değildir. Yayına yakın güncel politikayı kontrol edin.

Bu yüzden **manuel harcama girişi** yol haritasında: izin verilmese veya
uygulama reddedilse bile elinizde yayınlanabilir bir bütçe defteri kalır.

---

## Lisans

MIT — `LICENSE` dosyasına bakın.
