# Yol haritası

Durum tespiti: `tasarim-v2` branch'i, son commit `c63fc9b` (tasarım v2 → B1-B4,
D1, 2×1 widget). Aşağıdaki maddeler öncelik sırasındadır; sıra rastgele değil,
bağımlılığa ve riske göre dizilmiştir.

Kapsam dışı bırakma gerekçeleri ve tasarım sapmaları için `EKSIKLER.md`'ye
bakın. Bu dosya "ne yapılacak", `EKSIKLER.md` "ne neden yapılmadı".

---

## P0 — Önce bunlar, aksi halde gerisi kâğıt üstünde

### 1. `:app` modülünü cihazda derleyip çalıştır

Doğrulama **emülatörde** yapıldı (Medium_Phone_API_36.1). Gerçek cihaz adımı
hâlâ açık; aşağıdaki tek işaretsiz kutu o.

- [x] `./gradlew :app:assembleDebug` yerelde geçsin — temiz derleme, exit 0,
      APK 9.26 MB
- [x] APK'yı kur, uygulamayı aç — emülatörde kuruldu, `MainActivity` crash'siz
      açıldı
- [ ] Aynı akışı **gerçek cihazda** tekrarla (emülatör ≠ cihaz: font fallback,
      launcher widget davranışı ve gerçek banka bildirimleri farklı olabilir)
- [x] Bildirim erişimi iznini ver, `TestNotificationSeeder` ile uçtan uca akışı
      gör (bildirim → parse → Room → ekran → widget) — 3 kayıt işlendi, toplam
      1.400,90 ₺ doğru (REFUND +45,00 toplamdan düşülüyor)
- [x] `sourceKey` tekrar koruması pratikte doğrulandı — seed ikinci kez
      çalıştırıldığında işlem sayısı 3'te kaldı (yine de 3. maddedeki otomatik
      test gerekiyor, manuel gözlem test değildir)
- [x] 2×1 widget'ı ana ekrana ekle, `Bitmap` üzerine çizilen kategori şeridinin
      doğru göründüğünü doğrula (`widget/BudgetWidget.kt`) — şerit Ulaşım/Market
      oranıyla çiziliyor
- [x] CI'daki `app` job'ının bu branch'te de koştuğunu doğrula —
      `.github/workflows/ci.yml` push tetikleyicisine `tasarim-v2` eklendi

**Doğrulamada çıkan hata:** Uygulama içi ekranlarda `₺` (U+20BA) karakteri `£`
olarak render ediliyor. Kaynak doğru (`HomeScreen.kt:246,295,359` hepsi `₺`);
widget sistem fontu kullandığı için `₺`'yi doğru çiziyor. Sorun
`app/src/main/res/font/` altındaki TTF'lerde — U+20BA glifi eksik ya da yanlış
eşlenmiş. Yayın öncesi düzeltilmeli.

### 2. Gerçek fixture topla (150+)

README'nin kendi ifadesiyle "riskin tamamı regex kalitesinde". Depodaki 167
örneğin tamamı `scripts/generate_corpus.py` çıktısı — sentetik. Ground truth
üreteçten geliyor, gerçeklikten değil.

**Ölçüm altyapısı hazır, veri toplama açık.** `fixtures.tsv`'ye 5. kolon
eklendi (`origin`: `REAL`|`SYNTHETIC`, eksikse `SYNTHETIC`); mevcut 167 satır
sentetik sayılıyor. `:parser:verify` artık kökene göre ayrı doğruluk ve yayın
kararı basıyor. Bunlar olmadan karar noktası ölçülemezdi: sentetik örnekler
kendi üreteçlerinden geldikleri için kolaydır ve karma oranı yukarı çeker.

- [x] `fixtures.tsv`'ye köken kolonu, `:parser:verify`'a kökene göre rapor ve
      yayın kapısı ekle (kapı ≥150 gerçek örnekte devreye girer)
- [x] `./scripts/add-fixture.sh` gerçek örnekleri `REAL` damgalasın; aynı metni
      iki kez eklemeyi reddetsin (tekrar, doğruluk oranını şişirir)
- [ ] Kendi telefonundan + 4-5 kişiden gerçek banka bildirimi topla (kart no,
      isim maskelenmiş) — **ajan yapamaz, senin elinde**
- [ ] `./scripts/add-fixture.sh` ile test setine ekle
- [ ] `gradle :parser:verify` ile ölç — şu an `0/150 gerçek örnek, KARAR
      VERILEMEZ` basıyor
- [ ] **Karar noktası:** 150-200 gerçek örnekte doğruluk %95'in altında kalıyor
      ve `patterns/patterns.json`'a desen ekleyerek yükselmiyorsa **yayınlama**

**Dikkat:** `python3 scripts/generate_corpus.py > .../fixtures.tsv` dosyayı
baştan yazar, elle eklenen `REAL` satırları siler. Üretecin docstring'inde
`REAL` satırları koruyan tarif var; korpusu yeniden üretmeden önce oku.

### 2b. Üreteç korpusu yeniden üretemiyor

`generate_corpus.py` çalıştırıldığında yine 167 satır üretiyor ama **içerik
kümesi depodakiyle aynı değil** — dosya, üreticisinin şu anki hâlinden
türetilemiyor. Korpus, üreteç değiştikten sonra yeniden üretilmemiş olmalı.
Sonuç: `fixtures.tsv` elle bakım gerektiren bir dosya, "üretilmiş" değil.

- [ ] Ya üreteci korpusu yeniden üretecek hâle getir, ya da dosyanın
      başındaki "OTOMATIK URETILDI" iddiasını kaldır

### 2c. `gradle :parser:test` kırmızı, CI bunu görmüyor

CI yalnızca `:parser:verify` koşuyor (`.github/workflows/ci.yml:25`), bu yüzden
üç başarısız JUnit testi fark edilmemiş. Üçü de bu maddeden önce vardı:

- [ ] `ParserAccuracyTest` — `UnknownFormatConversionException: '9'`. Sebebi
      `%95` (satır 75); `%` biçim karakteri, kaçırılmamış. Assertion mesajı
      koşuldan bağımsız değerlendiği için test **her zaman** patlıyor
- [ ] `MerchantCleanerTest` — `Migros Ticaret A.S` yerine `A.s`; Türkçe locale
      `uppercase()`/`lowercase()` farkı
- [ ] `MerchantCleanerTest` — `A101` yerine `null`
- [ ] CI'a `:parser:test` ekle, yoksa bu testler yine görünmez

### 3. `:app` için test yaz

`app/src/test` kuruldu: 36 test, Robolectric üzerinde gerçek SQLite ve gerçek
`patterns.json` ile — emülatör gerekmiyor, CI'da koşuyor.

- [x] `sourceKey` tekrar koruması — Room unique index'i gerçekten çalışıyor mu
      (saat kovası dahil) — `SourceKeyDedupTest`: aynı kova içindeki tekrar
      teslim yok sayılıyor, bir sonraki saatteki gerçek alışveriş kaydediliyor,
      koruma DAO seviyesinde de doğrulandı
- [x] `NotificationService` parse → kayıt akışı — `NotificationFlowTest`
- [x] `ExpenseRepository.correctCategory` ve mağaza→kategori öğrenmesi —
      `CategoryLearningTest`: kural öğrenme, geçmişe dönük düzeltme, kullanıcının
      elle verdiği kararın ezilmemesi
- [x] Room migration testi (`app/schemas/` şemaları bunun için repoda) —
      `AppDatabaseMigrationTest`: v1 şemasıyla kurulmuş dolu bir veritabanı
      güncel kodla açılıyor; v1 parmak izi (`identityHash`) teste sabitlendi,
      entity değişip sürüm artmazsa test kırmızı yanıyor
- [x] `HomeViewModel.toUiState` — REFUND'ın toplamdan düşülmesi —
      `HomeUiStateTest`
- [x] CI'a `:app:testDebugUnitTest` eklendi (`.github/workflows/ci.yml`)

**Testlerin ısırdığı doğrulandı:** `toUiState`'teki REFUND işareti kaldırılınca
`HomeUiStateTest` iki testte kırmızı yandı, sonra geri alındı.

**Hâlâ açık:** `NotificationService.onNotificationPosted`'ın kendisi test
edilmiyor — `StatusBarNotification`'ın kurucusu `@hide`, derleme SDK'sında yok,
yani sınıf JVM testinde kurulamıyor. Servisin yaptığı iş (paket süzgeci →
ayrıştırma → `record` → tekrar teslim) birebir aynı sırayla test ediliyor;
kapsam dışı kalan yalnızca Android bağlantısı. `app/src/androidTest` hâlâ yok.

**Test edilebilirlik için değişen üretim kodu** (davranış aynı):
`ExpenseRepository`'ye `AppDatabase` alan `internal` kurucu, `toUiState`'in
dosya düzeyine taşınması, bildirim metni kuralının `NotificationText`'e
ayrılması, `AppDatabase.MIGRATIONS` dizisi (üretim ve test aynı diziyi
kullanıyor), `Ledger.HOUR_MILLIS` sabiti.

---

## P1 — Yayınlanabilirliği belirleyen işler

### 4. Navigasyon altyapısı kur

Kuruldu. `MainActivity` artık `AppNavHost()` çağırıyor
(`ui/nav/AppNavHost.kt`). **5, 6, 8, 9 numaralı maddelerin hepsi buna bağlıydı;
artık her biri bir `composable(...)` satırıyla bağlanabilir.**

- [x] Karar: **NavHost**. Tek Activity + state makinesi, sıradaki dört ekranın
      her biri için elle geri yığını yönetmek demekti; `navigation-compose`
      bunu ve `ViewModel`'in hedefe göre kapsamlanmasını hazır veriyor
- [x] Bağımlılığı `libs.versions.toml`'a ekle — `navigation = "2.7.7"`; 2.8.x
      `compileSdk 35` istiyor, proje 34'te
- [x] Mevcut `HomeScreen` + `EditExpenseSheet`'i yeni yapıya taşı —
      `HomeScreen` `Route.HOME` hedefi oldu; `EditExpenseSheet` bilerek rota
      **değil**: modal alt sayfa kendi geri tuşunu yönetiyor ve yalnızca
      listedeki bir kayıttan açılıyor, rotaya çevirmek kaydın id'sini adresten
      taşıyıp veritabanından yeniden okumayı gerektirirdi

**Hâlâ açık:** Grafın kendisi otomatik test edilmiyor — `TestNavHostController`
için `compose-ui-test` + `navigation-testing` bağımlılıkları gerekiyor, tek
hedefli bir graf için maliyeti kazancından büyük. Graf artık üç hedefli (5. ve
6. maddeler), ama hâlâ test edilmiyor; grafın *hangi hedefle açıldığı* kararı
`startDestination` olarak ayrıldığı için o kadarı test ediliyor (madde 6).
Ekranlardaki ölü `onClick = {}` çağrılarından "elle ekle" bağlandı (madde 5);
ayarlar ve rapor hâlâ ölü, bağlanmaları 8 ve 9 numaralı maddelerin işi.

### 5. D2 — Manuel harcama girişi

Yapıldı. Sebebi estetik değildi: Play Console bildirim erişimini reddederse
elde yayınlanabilir bir bütçe defteri kalması gerekiyordu; artık kalıyor.

- [x] Manuel harcama giriş ekranı — `ui/AddExpenseScreen.kt`, `Route.ADD_EXPENSE`
      hedefi. Tek zorunlu alan tutar; tutar çözülemedikçe Kaydet düğmesi soluk
      ve tıklanamaz
- [x] `FloatingAddButton` → ekrana bağlandı; artık izin kapalıyken de görünüyor
      (elle giriş o durumda tek yol)
- [x] B4 boş durumundaki "+ Elle harcama ekle" → aynı ekrana bağlandı
- [x] B3 (izin kapalı) ekranına "elle girilenler" listesi ve soluk toplamı
      eklendi — `HomeUiState.manualExpenses` / `manualTotalMinor`

**Şema değişmedi:** elle girilenler `sourceApp = "manual"` damgasıyla ayrılıyor
(`Ledger.MANUAL_SOURCE`); yeni kolon ve migration gerekmedi.

**Tekrar koruması bilerek devre dışı:** bildirimde `sourceKey` metinden
türetilir ve tekrar teslim yutulur; elle girişte anahtar rastgele üretiliyor.
Aksi halde kullanıcının aynı gün ikinci kez girdiği aynı tutar sessizce yutulur
ve ayın toplamı eksik çıkardı. `ManualEntryTest`'te doğrulandı: anahtar
deterministik yapılınca test kırmızı yandı (2 bekleniyordu, 1 geldi), sonra
geri alındı.

**Kategori:** kullanıcı seçmezse işyeri adından çözülüyor (önce öğrenilmiş
kural, sonra anahtar kelime) ve kayıt `userEdited = false` kalıyor — geçmişe
dönük düzeltme bunu da yakalayabilsin. Kullanıcı çipe dokunduysa karar onun:
`userEdited = true`, bir daha ezilmiyor.

**Kapsam dışı bırakıldı:** tarih seçici yok, kayıt "şu an"a yazılıyor ve ekran
hangi tarihe yazdığını açıkça gösteriyor. İade (REFUND) girişi de yok; elle
giriş her zaman harcama. İkisi de bu maddenin istediği iş değildi.

**Testler:** `LedgerManualEntryTest` (7, `:parser`), `ManualEntryTest` (9,
Robolectric + gerçek SQLite), `ManualEntryDraftTest` (13, saf JVM) ve
`HomeUiStateTest`'e elle giriş süzgeci için 4 test. `:app` toplamı 36 → 62.

**Hâlâ açık:** Ekranın kendisi (Compose) otomatik test edilmiyor — 4. maddedeki
gerekçe burada da geçerli, `compose-ui-test` bağımlılığı hâlâ yok. Test edilen
kısım formun kuralları ve kayıt yolu; kapsam dışı kalan yalnızca çizim. Ekran
gerçek cihazda da denenmedi (1. maddedeki açık kutu).

### 6. A — Onboarding akışı

Yapıldı. `ui/onboarding/OnboardingScreen.kt`, `Route.ONBOARDING` hedefi. Akışın
işi izin *istemek* değil, izni **anlaşılır kılmak**: Android'in "tüm
bildirimleri okuyabilecek" uyarısı, ne yaptığını bilmeyen kullanıcıyı ilk
karşılaşmada kaçırıyor.

- [x] İzin ekranına yönlendirme metni ve akışı — A1 (ne işe yarar) → A2 (izin
      öncesi hazırlık) → A3 (dinlemeye başladım). A2, Android'in soracağı
      diyaloğun bir örneğini önden gösteriyor ve yanına üç somut söz koyuyor:
      internet izni yok, veri telefondan çıkmıyor, yalnızca tanımlı banka
      uygulamaları okunuyor
- [x] İlk açılışta gösterim, `Prefs.kt` üzerinden "görüldü" bayrağı —
      `startDestination(onboardingDone)` (`ui/nav/AppNavHost.kt`) ilk açılışta
      `Route.ONBOARDING`, sonrasında `Route.HOME` döndürüyor

**Sayfalar rota değil, tek rotanın iç durumu.** Üçü birlikte tek bir kurulum
sihirbazı; ayrı rota olsalardı her çıkışın kendi `popUpTo` zinciri olurdu ve
grafta "izin verilmeden ulaşılabilen A3" gibi anlamsız adresler doğardı.

**Bayrak her çıkışta yazılıyor** — izin verildiğinde de "şimdilik elle girerim"
dendiğinde de. Aksi halde izni reddeden kullanıcı aynı üç sayfayı her açılışta
görürdü; hatırlatmayı zaten ana ekrandaki izin kartı (B3) sürdürüyor.

**İzin durumu dönüşte kendiliğinden okunuyor:** izin sistem ayarında veriliyor,
`ON_RESUME`'da tekrar bakılıyor ve verilmişse A3'e geçiliyor. Ana ekranda
kullanıcıya "izni verdim" dedirten bir düğme var; burada akışın sıradaki adımı
izne bağlı olduğu için fazladan bir dokunuş istemenin anlamı yoktu.

**Ekrandaki sayılar sabit değil:** "17 banka" ve "5 desen" `patterns.json`'dan
okunuyor (`PatternProvider.sourceCount`, yeni). Elle yazılsalardı desen setine
banka eklendiği gün sessizce yalan söylerlerdi. "0 sunucu" sabit — manifest'te
INTERNET izni yok.

**Testler:** `OnboardingFlowTest` (4, saf JVM — sayfa sırası ve başlangıç
hedefi), `OnboardingSeenFlagTest` (3, Robolectric — bayrağın yeni bir `Prefs`
örneğinde de okunması). `:app` toplamı 62 → 69. Testlerin ısırdığı doğrulandı:
`startDestination` her zaman `Route.HOME` döndürecek şekilde bozulunca iki test
kırmızı yandı, sonra geri alındı.

**Hâlâ açık:** Ekranların kendisi (Compose) otomatik test edilmiyor — 4. ve 5.
maddedeki gerekçe burada da geçerli, `compose-ui-test` bağımlılığı hâlâ yok.
Test edilen kısım akışın kuralları; kapsam dışı kalan yalnızca çizim. Akış
gerçek cihazda da denenmedi (1. maddedeki açık kutu). Tasarımdaki `bbRise` /
`bbDrop` giriş animasyonları uygulanmadı; sayfalar animasyonsuz geçiyor.

### 7. Yayın hazırlığı

- [ ] Release imzalama yapılandırması (`app/build.gradle.kts` içinde
      `signingConfigs` yok)
- [ ] `proguard-rules.pro` gözden geçir — 217 byte; Room, Compose ve `:parser`
      reflection kuralları doğrulanmadı, `isMinifyEnabled = true` açık
- [ ] Uygulama ikonu — hâlâ tek `ic_launcher_foreground.xml` vektörü, marka
      ikonu değil
- [ ] Play Console: bildirim erişimi (`BIND_NOTIFICATION_LISTENER_SERVICE`)
      gerekçelendirme metni + gizlilik beyanı; `PRIVACY.md` ile tutarlı olmalı

---

## P2 — Ürünü tamamlayan ekranlar

### 8. C — Rapor ekranları

- [ ] Rapor ekranı/ekranları
- [ ] `HomeScreen.kt:313` "RAPOR →" → ekrana bağla (şu an ölü)

### 9. F — Ayarlar ekranı

- [ ] Ayarlar ekranı
- [ ] `HomeScreen.kt:229` `SettingsGearButton` → ekrana bağla (şu an `onClick = {}`)

### 10. Aylık değişim rozeti ("↓ %12 TEMMUZ")

Tasarımda var, kodda hiç render edilmiyor. Önceki ayla karşılaştırma gerekiyor;
`HomeViewModel.kt:44` `HomeUiState` yalnızca tek ayın verisini taşıyor.

- [ ] `HomeUiState`'e önceki ay toplamını ekle
- [ ] `TotalHeader` altında rozeti render et

---

## P3 — Pro ve sonrası

### 11. E — Pro / paywall

`HomeScreen.kt:209` `ProChip` şu an yalnızca görsel rozet. Altyapının hiçbiri
yok: billing bağımlılığı yok, Pro durumunu tutacak yer yok (`Prefs.kt` 26 satır).

- [ ] Pro durumu kalıcılığı
- [ ] Billing entegrasyonu
- [ ] Paywall ekranı
- [ ] `ProChip` → paywall'a bağla

### 12. 4×2 Pro widget

- [ ] 2×1 widget'ın yanına 4×2 sürümü

---

## P4 — Kozmetik ve temizlik

### 13. Bayat dokümantasyon (hızlı iş, ~5 dk)

Bunlar okuyanı yanlış yönlendiriyor:

- [ ] `design/README.md:17` "font dosyaları eksik" diyor — fontlar
      `app/src/main/res/font/` içinde mevcut (5 ttf)
- [ ] `design/README.md:19` "Ekran düzenleri: Aktarılmadı" — B1-B4, D1 ve
      widget aktarıldı
- [ ] `README.md:119` "Gradle wrapper jar'ı repoda yok" —
      `gradle/wrapper/gradle-wrapper.jar` (42 KB) repoda var
- [ ] `README.md` durum tablosu ve yol haritası bölümü bu dosyaya işaret etsin

### 14. Tasarım sapmaları

Ayrıntı ve gerekçeler `EKSIKLER.md`'de:

- [ ] Boş durum kartındaki kesikli (dashed) kenarlık — şu an düz çizgi, dashed
      için özel `Canvas` çizimi gerekiyor
- [ ] Token yuvarlamaları — piksel-birebir değil, token-birebir

---

## Bilerek dışarıda bırakılanlar

Bunlar "eksik" değil, kapsam kararı: bütçe hedefleri, çoklu para birimi, dışa
aktarma, hesap eşleştirme, bulut senkronizasyonu.
