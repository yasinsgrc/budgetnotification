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
hedefli bir graf için maliyeti kazancından büyük. Graf artık beş hedefli (5, 6,
8, 9 ve 11. maddeler), ama hâlâ test edilmiyor; grafın *hangi hedefle açıldığı*
kararı `startDestination` olarak ayrıldığı için o kadarı test ediliyor (madde
6). Ekranlardaki ölü `onClick = {}` çağrılarının **hepsi** bağlandı: "elle ekle"
(madde 5), "RAPOR →" (madde 8), ayarlar dişlisi (madde 9) ve PRO rozeti
(madde 11).

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

Yapıldı. Bu maddeye kadar release yolu (`assembleRelease`) hiç çalıştırılmamıştı;
R8 kuralları "yazıldı ama denenmedi" durumundaydı. Artık release derlemesi
geçiyor ve iddiaların her biri çıktıdan doğrulandı.

- [x] Release imzalama yapılandırması — `signingConfigs` eklendi. Sırlar iki
      kaynaktan okunuyor: önce `BB_KEYSTORE_*` ortam değişkenleri (CI), yoksa
      kökteki `keystore.properties` (yerel, `.gitignore`'da). Şablon:
      `keystore.properties.example`
- [x] `proguard-rules.pro` gözden geçirildi — kural sayısı azaldı, dosya
      büyüdü: kalan iki kuralın **neden** kaldığı ve gerisinin nereden geldiği
      yazılı
- [x] Uygulama ikonu — `ic_launcher_foreground.xml` yeniden çizildi (₺ monogramı
      + bildirim rozeti), ayrı `ic_launcher_monochrome.xml` eklendi
- [x] Play Console metinleri — `PLAY_CONSOLE.md`: bildirim erişimi
      gerekçelendirmesi (TR + EN), veri güvenliği formu cevapları ve her
      iddianın kod karşılığı

**İmzalama sırrı yoksa derleme kırılmıyor**, imzasız APK üretiliyor. Bunun
sebebi CI: `.github/workflows/ci.yml` imzalama sırrına sahip değil, ama R8 /
küçültme yolunun derlenebildiğini görebilmesi gerekiyor. Sessizce imzasız APK
çıkmasın diye release görevi çalışırken uyarı basılıyor; doğrulandı:

```
UYARI: release imzalama yapilandirilmadi (keystore.properties yok ve
BB_KEYSTORE_* ortam degiskenleri bos). Cikan APK/AAB IMZASIZ - ...
```

**R8 kuralları ölçüldü, varsayılmadı.** `NotificationService` için elle yazılmış
`-keep` kuralı **silindi**: AGP zaten manifest'teki her bileşen için kural
üretiyor. Kanıt `aapt_proguard_file/release/.../aapt_rules.txt` içinde:

```
-keep class com.bildirimbutce.app.App { <init>(); }
-keep class com.bildirimbutce.app.MainActivity { <init>(); }
-keep class com.bildirimbutce.app.service.NotificationService { <init>(); }
-keep class com.bildirimbutce.app.widget.BudgetWidget { <init>(); }
```

Room kuralı kaldı (ucuz sigorta); `mapping.txt` içinde `AppDatabase_Impl`
duruyor, yani veritabanı R8'den sağ çıkıyor. `:parser` için kural gerekmedi:
`Class.forName`, `javaClass`, `@Keep`, `Serializable` ve `getIdentifier`
aramaları `:app` ve `:parser` kaynaklarında **sıfır** sonuç veriyor —
ayrıştırıcı JSON'u elle okuyor, reflection yok. `patterns.json` ise kaynak
değil *asset*, `isShrinkResources` ona dokunmuyor.

Sonuç: release APK **1.33 MB** (debug 9.26 MB). `:app` 69 testin 69'u yeşil.

**İkon kâğıt üstünde bırakılmadı, çizilip bakıldı.** İlk üç deneme ₺ yerine
"モ"ye benziyordu; sebep, kolların gövdeden sola fazla taşmasıydı. Son hâlde
taşma ~4 birime indi, kollar dikleşti ve çizgi inceldi. Geometri sayıyla
sınırlandı: adaptive icon'un her maskede görüneceği garanti alan merkezden 33
birim; çizgi uçları en fazla **30.2**, rozet **31.8** yarıçapta kalıyor
(hesap dosyanın başındaki yorumda). 48dp'de de okunuyor.

`monochrome` katmanı artık ayrı dosya. Önceden iki renkli foreground'a
bağlıydı; sistem o katmanı tek renge boyadığı için rozetin marka yeşili
anlamsızdı. `ic_launcher_round.xml` **silindi** ve manifest'ten
`android:roundIcon` kaldırıldı: `minSdk 26` ve adaptive icon her maske şeklini
zaten karşılıyor, dosya birebir kopyaydı.

**Beyana bilerek yazılmayan bir cümle vardı.** "Kullanıcı dinlenen banka
listesini daraltabilir" o gün doğru değildi: `Prefs.enabledSources` kodda vardı
ve `NotificationService` onu okuyordu, ama **hiçbir yer yazmıyordu** — ayarlar
ekranı yoktu. Gerekçelendirmeye koysaydık inceleme ekibinin uygulamada
bulamayacağı bir özellik vaat etmiş olurduk. **9. madde ile beyana eklendi**
(F2, `ui/settings/SourcesScreen.kt`).

**Hâlâ açık — bu madde "yayına hazır" demek değil:** anahtar deposu henüz
üretilmedi (`keytool` komutu örnek dosyada), dolayısıyla **imzalı** bir APK
hiç kurulmadı; imzasız APK da cihazda denenmedi (1. maddedeki açık kutu).
Mağaza varlıkları (512×512 ikon, öne çıkan görsel, ekran görüntüleri,
açıklama metinleri) ve gizlilik politikası URL'si depoya konamaz —
`PLAY_CONSOLE.md`'nin 4. bölümünde liste hâlinde duruyor. Ayrıca 1. maddedeki
`₺` font hatası yayın öncesi düzeltilmeli; ikon vektör olduğu için ondan
etkilenmiyor, ama uygulama içi ekranlar etkileniyor.

---

## P2 — Ürünü tamamlayan ekranlar

### 8. C — Rapor ekranları

Yapıldı — C1. `ui/report/ReportScreen.kt`, `Route.REPORT` hedefi. Bu katmanın
işi yeni veri üretmek değil, zaten kaydedilmiş olana bir soru sormak: "geçen
aya göre ne oldu", "en çok nereye gitti", "hangi gün zayıfım".

- [x] Rapor ekranı — C1: son 6 ay çubukları, üç sayı kutusu (gün ortalaması /
      en yüksek gün / işlem sayısı), haftanın ritmi ve en çok gidilen yerler
- [x] "RAPOR →" → ekrana bağlandı (`HomeScreen.kt:361`, artık ölü değil)

**Ay adreste taşınıyor, ekranda varsayılmıyor.** Rota `report/{year}/{month}`;
"RAPOR →" ana ekranda hangi ay açıksa onu geçiyor. Ekran kendi başına "bu ay"ı
varsaysaydı kullanıcı temmuza bakarken ağustos raporu açılırdı. Ay adreste
durduğu için süreç öldürüldüğünde geri dönüşte de aynı rapor açılıyor.

**Altı ay tek sorguyla okunuyor.** `Ledger.rangeEndingAt` pencereyi hesaplıyor,
`ExpenseRepository.observeMonths` bir kez çekiyor, aylara bölme bellekte
oluyor. Ay başına ayrı sorgu açılsaydı aylar birbirinden farklı anlık
görüntülere düşebilir, çubukların toplamı ekrandaki ay toplamıyla tutmazdı.

**Gün ortalamasının böleni ayın gün sayısı değil, geçen gün.** Ayın 10'unda
3.100,00 ₺ harcamış birine 31'e bölünmüş bir ortalama göstermek "iyi
gidiyorsun" demektir; kullanıcı ortalamasını üçte biri kadar görür. Kapanmış
aylarda bölen ayın tamamı. Hangi sayıya bölündüğü ekranda yazıyor
("₺ / gün · 10 gün"), yoksa iki ayın kutusu karşılaştırılamazdı.

**Haftanın ritmi cümlesi susabiliyor.** "Cuma günleri ortalamanın %64 üstünde
harcıyorsun" ancak tepe gün yedi kovanın ortalamasını **%15'ten fazla** aşarsa
yazılıyor. Tek aylık, yedi kovaya bölünmüş bir örneklemde %5-10 sapma
tesadüfün kendisidir; her ay bir "tespit" uydurmak raporun güvenilirliğini
tüketirdi.

**İşyeri adı olmayan kayıtlar "en çok giden yerler"e girmiyor.** "Bilinmeyen
işyeri" bir yer değil, ayrıştırıcının okuyamadığı bir satır; listenin başına
çıksaydı kullanıcıya gitmediği bir yeri gösterirdik. Aynı listede kategori en
**yeni** kayıttan okunuyor: kullanıcı bir düzeltme yaptıysa öğrenilen kural son
satırda görünür, rapor kullanıcının kendi düzeltmesini yok saymış gibi durmaz.

**Bütün toplamlar işaretli.** İade (REFUND) ay toplamından, gün toplamından,
hafta kovasından ve işyeri satırından düşülüyor — kural tek yerde,
`signedMinor` ana ekranla paylaşılıyor. Neti negatife düşen gün "en yüksek
gün" olamıyor, neti negatif işyeri listeye girmiyor, ay iadeyle negatif
kapandıysa gün ortalaması hiç gösterilmiyor.

**Testler:** `ReportUiStateTest` (23, saf JVM — ay penceresi, bölen seçimi,
hafta indeksi, sıralama ve süzgeçler), `LedgerRangeTest` (6, `:parser` —
pencere sınırları, yıl sınırı, artık yıl şubatı). `:app` toplamı 69 → 92,
`:parser` 18 → 24. Testlerin ısırdığı doğrulandı: pazartesi-ilk kaydırması
(`(dow + 5) % 7`) bir birim kaydırılınca iki test kırmızı yandı, sonra geri
alındı.

**Kapsam dışı bırakıldı — C2 ve C3.** İkisi de bu maddenin iki kutusunu
karşılamıyor ve ayrı kararlar gerektiriyor:

- **C2 (kategori detayı)** — tasarımda ekranın *nereden açıldığı* yok. Tek
  makul giriş ana ekrandaki kategori şeridine tıklamak, o da B1'in davranışını
  değiştirmek demek; bu maddenin istediği iş "RAPOR →"yu bağlamaktı.
- **C3 (ay kapanış özeti, paylaşılabilir)** — "Görseli kaydet" bitmap üretimi,
  `FileProvider` ve paylaşım niyeti istiyor; ayrıca ekranı tetikleyecek bir "ay
  kapandı" olayı uygulamada yok. Dosya paylaşımı, "veri telefondan çıkmaz"
  anlatısına dokunan bir yüzey açıyor (kullanıcının başlattığı paylaşım da
  olsa) — kararı `PLAY_CONSOLE.md`'deki beyanla birlikte verilmeli.

**Hâlâ açık:** Ekranın kendisi (Compose) otomatik test edilmiyor — 4, 5 ve 6.
maddedeki gerekçe burada da geçerli, `compose-ui-test` bağımlılığı hâlâ yok.
Test edilen kısım raporun aritmetiği; kapsam dışı kalan yalnızca çizim. Ekran
gerçek cihazda denenmedi (1. maddedeki açık kutu) ve tasarımdaki `bbBar` çubuk
büyüme animasyonu uygulanmadı; çubuklar animasyonsuz çiziliyor. `₺` font hatası
(1. madde) bu ekranı da etkiliyor.

### 9. F — Ayarlar ekranı

Yapıldı — F1-F4. `ui/settings/` altında dört ekran, `Route.SETTINGS_GRAPH`
altında iç içe bir graf. Bölümün işi tercih toplamak değil, **görünür kılmak**:
bildirimleri okuyan bir uygulamanın neyi okuduğunu, neyi okumadığını ve ne
öğrendiğini gösterebilmesi gerekiyor.

- [x] Ayarlar ekranı — F1 `SettingsScreen.kt` (izin durumu + iki grup), F2
      `SourcesScreen.kt` (dinlenen kaynaklar), F3 `RulesScreen.kt` (öğrenilen
      kurallar), F4 `PrivacyScreen.kt` (izin kanıtı, tüm veriyi sil)
- [x] `SettingsGearButton` → ekrana bağlandı (`HomeScreen.kt`, artık ölü
      değil); o gün geriye yalnızca PRO rozeti kalmıştı, o da 11. maddede
      bağlandı

**`Prefs.enabledSources` artık üç durumlu.** Eskiden `Set<String>` idi ve boş
küme "hepsi" demekti; hiçbir yer yazmadığı için sorun çıkmamıştı. Ekran yazmaya
başlayınca kural tuzağa dönüşüyordu: son anahtarı da kapatan kullanıcı boş küme
yazar, boş küme "hepsi" sayılır ve kullanıcı **tüm** bankaları farkında olmadan
geri açmış olurdu. Şimdi `null` = hiç seçim yapılmadı (hepsi), boş küme = hepsi
kapalı, dolu küme = seçilenler. `null`'ın ikinci faydası: desen setine yarın
eklenecek banka, hiç seçim yapmamış kullanıcı için kendiliğinden dinlenir; tam
liste saklansaydı yeni banka kapalı doğardı.

**Kural iki yerde değil, `data/SourceSelection.kt`'de.** Ekran anahtarları buna
göre çiziyor, `NotificationService` gelen bildirimi buna göre süzüyor. İki kopya
olsaydı biri değişip diğeri kalabilir, kullanıcı kapattığı bankanın
harcamalarını listede görmeye devam ederdi.

**Banka adları `patterns.json`'a girdi** (`sourceLabels`, yeni). Kodda bir tablo
olsaydı desen setine banka eklendiği gün eksik kalırdı — 6. maddedeki "17 banka"
sayısının okunarak yazılmasıyla aynı gerekçe. Karşılığı olmayan paket için ekran
paket adının kendisini gösteriyor; uydurma bir ad, listenin Android'in uygulama
listesiyle karşılaştırılmasını imkânsız kılardı. Satırlarda paket adı da yazıyor,
tam olarak bu karşılaştırma yapılabilsin diye.

**Dört ekran tek ViewModel paylaşıyor.** Sayaçlar F1'de ("12 / 17", "14 kural"),
anahtarlar F2'de. Ekran başına ayrı örnek olsaydı F2'de kapatılan banka F1'e
dönüldüğünde hâlâ açık görünürdü — tercih diske yazılmış olsa bile eski örneğin
akışı bunu duymazdı. Çözüm, ekranları iç içe bir grafa koyup ViewModel'i graf
girişine bağlamak (`AppNavHost.kt`, `settingsViewModel`).

**Kural silmek geçmişi geri almıyor.** Kural öğrenilirken düzeltilen kayıtlar
`userEdited = true` oldu, yani kullanıcının kendi kararı. "✕" yalnızca kuralı
unutturuyor; geçmişe dokunsaydı kullanıcının elle verdiği kararları da silmiş
olurduk.

**"Tüm veriyi sil" veriyi siliyor, ayarı değil.** Kayıtlar ve öğrenilmiş
kurallar gidiyor (kural da kullanıcının harcama geçmişinden türedi; kalsaydı
veri gerçekten silinmemiş olurdu), dinlenen kaynak tercihi ve onboarding bayrağı
kalıyor. Onay diyaloğu var: silme geri alınamıyor ve veri yalnızca cihazda
olduğu için geri getirilebilecek bir kopya yok.

**F4 izin listesini metinden değil `PackageManager`'dan okuyor.** Ekranın tek
iddiası "veri cihazdan çıkmıyor"; iddia cümle olarak yazılsaydı doğrulanamazdı.
Şimdi liste kurulu paketin manifest'inden geliyor, `INTERNET` satırı **yokluğu**
gösterecek şekilde her zaman basılıyor ve koda bir gün o izin eklenirse ekran
başlığı kendiliğinden "Hiçbir yere."den "Bir yere gidebilir."e dönüyor — metni
güncellemeyi unutmak mümkün değil. Buluta yedekleme satırı da
`FLAG_ALLOW_BACKUP` bayrağından okunuyor.

**`PLAY_CONSOLE.md` güncellendi** — 7. maddede "ayarlar ekranı gelince beyana
eklenmeli" diye bırakılan cümle artık doğru olduğu için eklendi.

**Testler:** `SourceSelectionTest` (7, saf JVM — üç durumun ayrımı, açma/kapama
geçişleri), `SettingsDataTest` (7, Robolectric + gerçek SQLite + gerçek
`patterns.json` — kaynak listesinin desen setinden gelmesi, tercihin kalıcılığı,
kural silmenin geçmişe dokunmaması, silmenin ayarları bırakması). `:app` toplamı
92 → 106. Testlerin ısırdığı doğrulandı: `listensTo`'ya eski "boş küme = hepsi"
kuralı geri konunca iki test kırmızı yandı, sonra geri alındı. Release derlemesi
de koştu: imzasız APK 1.40 MB (7. maddede 1.33 MB'tı).

**Kapsam dışı bırakıldı:**

- **F2'deki "Banka ara" alanı** — 17 satır tek ekrana sığıyor; arama kutusu
  aradığı şeyden fazla yer kaplardı. Kaynak sayısı büyürse gerekir.
- **F4'teki "Kaynak kodu aç" düğmesi** — bağlanacak adres yok; depo henüz
  yayınlanmadı ve `README.md` dâhil hiçbir yerde URL geçmiyor. Hiçbir yere
  gitmeyen bir düğme koymak, bu maddede kaldırdığımız ölü tıklamanın aynısı
  olurdu.
- **F4'teki "MANIFEST · SATIR SATIR" bloğu** — tasarımdaki sabit kod dökümü
  yerine `PackageManager`'dan okunan izin listesi ve sayılar konuldu. Ekrana
  elle yazılmış bir manifest, doğruladığını iddia ettiği şeyle bağını ilk
  değişiklikte koparırdı.

**Hâlâ açık:** Ekranların kendisi (Compose) otomatik test edilmiyor — 4, 5, 6 ve
8. maddedeki gerekçe burada da geçerli, `compose-ui-test` bağımlılığı hâlâ yok.
Test edilen kısım kuralların ve veri yollarının davranışı; kapsam dışı kalan
yalnızca çizim. Ekranlar gerçek cihazda denenmedi (1. maddedeki açık kutu) ve
`₺` font hatası (1. madde) bu ekranları da etkiliyor.

### 10. Aylık değişim rozeti ("↓ %12 TEMMUZ")

Yapıldı. Rozetin işi yeni bir sayı üretmek değil, ekrandaki sayıyı
**karşılaştırılabilir kılmak**: "1.821,80 ₺" tek başına iyi mi kötü mü
söylemiyor.

- [x] `HomeUiState`'e önceki ay toplamını ekle — `previousTotalMinor` ve rozetin
      kendisi (`change: MonthChange?`). Ana ekran artık iki aylık pencere
      okuyor (`HOME_MONTH_COUNT = 2`, `ExpenseRepository.observeMonths` —
      madde 8'de rapor için yazılan yol)
- [x] `TotalHeader` altında rozeti render et — `HomeScreen.kt`,
      `MonthChangeBadge`; "bu ay harcadın" satırının yanında

**Bölen ayın tamamı değil, aynı güne kadarı.** Ayın 9'unda kapanmış bir ayın
tamamıyla karşılaştırma yapılsaydı rozet her ayın başında "↓ %70" gösterir,
kullanıcı ay ilerledikçe sayının tersine döndüğünü görürdü. Açık ayda önceki
aydan yalnızca aynı güne kadarki kayıtlar sayılıyor; ay kapandıysa iki ayın
tamamı karşılaştırılıyor. Testlerin ısırdığı doğrulandı: kısıtlama kaldırılınca
`MonthChangeTest` kırmızı yandı, sonra geri alındı.

**Rozet üç durumda susuyor**, üçü de yüzdenin anlamsız olduğu yerler: önceki ay
net sıfır ya da negatifse (bölen yok — "%sonsuz arttın" denemez), açık ay
iadeyle negatife düştüyse (yüzde artık harcamayı anlatmıyor), fark yüzde yarımın
altındaysa (rozet "↓ %0" yazardı). Karşılaştırmanın *yapılmış* olmasıyla
rozetin *çizilmesi* ayrı şeyler: `previousTotalMinor` bu durumlarda da dolu.

**Rozet yalnızca dolu ayda çiziliyor.** Boş ayda karşılaştırılacak harcama yok;
izin kapalıyken (B3) gösterilen sayı ayın toplamı değil, yalnızca elle
girilenlerin toplamı — ikisinde de yüzde yanıltıcı olurdu.

**Kısıtlama rozete sığmıyor, `contentDescription`'a sığıyor.** Ekranda yalnızca
"↓ %12 TEMMUZ" yazıyor; ekran okuyucu "geçen ayın ilk 9 gününe göre %12 daha az
harcadın" diyor. Yüzdenin neye göre hesaplandığını hiç söylememektense bir yerde
söylemek daha doğruydu.

**`MonthCursor.dayOf` ortak.** Rapor ekranındaki `dayOfMonth` kopyası silinip
buraya taşındı; aynı takvim yorumunun iki kopyası olsaydı biri değişip diğeri
kalabilirdi — madde 8'deki `signedMinor` ile aynı gerekçe. Ay adı (`upperLabel`)
Türkçe yerel ayarla büyütülüyor: yerel ayar verilmeseydi "NİSAN" yerine "NISAN"
çıkardı.

**Testler:** `MonthChangeTest` (10, saf JVM — yön, bölen seçimi, susma
koşulları, ay adı ve önceki ayın listeye sızmaması). `HomeUiStateTest`'in 11
çağrısı yeni imzaya (`toUiState(cursor, now)`) taşındı. `:app` toplamı
106 → 116. Release derlemesi de koştu: imzasız APK 1.40 MB (9. maddeyle aynı).

**Hâlâ açık:** Ekran (Compose) otomatik test edilmiyor — 4, 5, 6, 8 ve 9.
maddedeki gerekçe burada da geçerli, `compose-ui-test` bağımlılığı hâlâ yok.
Test edilen kısım rozetin aritmetiği ve ne zaman susacağı; kapsam dışı kalan
yalnızca çizim. Gerçek cihazda denenmedi (1. maddedeki açık kutu). Tasarımda
widget başlığında da bir "↓ %12" var; **12. maddede 4×2 widget'a kondu**, 2×1'de
hâlâ yok (gerekçe orada).

---

## P3 — Pro ve sonrası

### 11. E — Pro / paywall

Yapıldı — E1, bir kutusu bilerek açık. `ui/pro/` altında üç dosya,
`Route.PAYWALL` hedefi. Bu maddenin işi para toplamak değildi; **Pro'nun ne
olduğunu tanımlamak**tı: ücretsiz sürümün nerede bittiği kod olarak
yazılmadan satılacak bir şey yoktu.

- [x] Pro durumu kalıcılığı — `Prefs.isPro` (`pro_entitlement`) ve önündeki
      `data/ProAccess.kt`. Ekranlar bayrağı değil arayüzü görüyor
- [ ] Billing entegrasyonu — **bilerek yapılmadı**, gerekçe aşağıda
- [x] Paywall ekranı — E1 `ui/pro/PaywallScreen.kt`
- [x] `ProChip` → paywall'a bağlandı (`HomeScreen.kt`); ekranlardaki ölü
      `onClick = {}` çağrılarından geriye **hiçbiri kalmadı**

**Billing bağımlılığı eklenmedi ve bu bir eksiklik değil, sıra.** Play
Console'da ne uygulama ne ürün tanımlı; imzalı bir APK hiç üretilmedi (1. ve 7.
madde). Bu koşullarda yazılacak `BillingClient` kodu derlenir ama bir kez bile
çalıştırılamazdı — 7. maddenin R8 kuralları için eleştirdiği "yazıldı ama
denenmedi" durumunun aynısı, üstelik para akışında. Yerine dikiş atıldı:
`ProAccess` arayüzü + `StoredProAccess` uygulaması. Billing geldiğinde eklenecek
dosya bir tane (`PlayBillingProAccess`); ekranlar ve ViewModel'ler kalıcılık
ayrıntısını hiç görmüyor.

**Satın alma düğmesi sessizce hiçbir şey yapmıyor.** `purchase()` her zaman
`PurchaseOutcome.Unavailable` dönüyor ve **sebebini taşıyor**; ekran onu olduğu
gibi basıyor ("Play Console kurulumu tamamlanmadı ve uygulama hiç imzalanıp
yayınlanmadı"). Düğme tıklanamaz yapılsaydı kullanıcı *neden* olmadığını hiç
öğrenemezdi; parlak altın degradeyle çizilseydi çalıştığını sanırdı. İkisinin
arası: soluk ama tıklanabilir.

**Paywall dört özellik değil bir tane satıyor.** Tasarım E1 sınırsız geçmiş,
4×2 widget, CSV dışa aktarma ve kendi desenini yazma sayıyor; **son üçü o gün
kodda yoktu** (4×2 widget 12. madde, diğerleri hiç başlanmadı). Olmayanların da
listelenmesi, 9. maddede kaldırdığımız "hiçbir yere gitmeyen düğme"nin para
karşılığı olurdu. Ekran bunu gizlemiyor, yazıyor: "olmayan bir şeyin parası
istenmiyor." **12. maddeden sonra liste ikiye çıktı**: 4×2 widget yazıldı ve
aynı gün paywall'a eklendi; CSV ve kendi deseni hâlâ yok.

**Fiyat da yazmıyor.** Tasarımdaki 149,00 ₺ bir yer tutucu; gerçek fiyat Play
Console'daki üründen okunur ve ürün yok. Ekrana sabit bir sayı yazmak, ilk fiyat
değişikliğinde sessizce yalan söyleyen bir satır bırakırdı — 6. maddedeki "17
banka" sayısının `patterns.json`'dan okunmasıyla aynı gerekçe.

**Ücretsiz sürümün sınırı: son 3 ay** (`ProLimits.FREE_MONTH_COUNT`, tasarım
E2'deki "3 ay geçmiş görünür" satırı). İçinde bulunulan ay **dahil**; üç "ek" ay
olsaydı kullanıcı dört aylık defter görür ve ekranda yazan sayı yalan olurdu.
Sınır tek yerde duruyor: ay gezinmesini kısıtlayan `previousMonth` ile
kullanıcıya sınırı gösteren ok aynı cümleyi okumak zorunda — 8. maddedeki
`signedMinor` ile aynı gerekçe.

**Sınıra gelen ok susmuyor.** Soluyor ve dokunuş paywall'a gidiyor; ne yaptığını
`contentDescription` söylüyor ("Daha eski aylar Pro sürümde"). Ok sessizce
hiçbir şey yapsaydı 9. maddede kaldırdığımız ölü tıklamalardan birini geri
koymuş olurduk. İleri gitmek kısıtlanmadı: sınır geçmişi satıyor, ileriyi değil.

**Yetki dönüşte yeniden okunuyor.** Satın alma uygulamanın dışında (Play
Store'da) tamamlanır, bu yüzden ana ekran `ON_RESUME`'da `refreshPro()`
çağırıyor — izin durumunun onboarding'de yeniden okunmasıyla aynı gerekçe.

**Hata ayıklama derlemesinde bir anahtar var.** Satın alma yolu olmadığı sürece
üç aylık sınır cihazda başka türlü görülemezdi; `BuildConfig.DEBUG` altında
"PRO'YU AÇ/KAPAT" — ana ekrandaki `TestNotificationSeeder` düğmesiyle aynı
gerekçe. Release APK'da yok.

**`MonthCursor.ordinal` ortak.** İki ayı karşılaştırmanın tek doğru yolu; `minus`
zaten aynı aritmetiği satır içinde yapıyordu, o da buna taşındı.

**Testler:** `ProLimitsTest` (7, saf JVM — pencere sınırları, yıl sınırı, Pro'da
sınırsızlık), `HomeHistoryLimitTest` (7, saf JVM — imlecin sınırda durması, okun
ne zaman solacağı), `ProAccessTest` (6, Robolectric — yetkinin yeni bir örnekte
de okunması, `refresh` olmadan akışın değişmemesi, reddedilen satın almanın
yetkiyi açmaması). `:app` toplamı 116 → 136. Testlerin ısırdığı doğrulandı:
`steppedBack`'teki sınır kontrolü kaldırılıp `earliestMonth` bir ay kaydırılınca
7 test kırmızı yandı, sonra geri alındı. Release derlemesi de koştu: imzasız APK
1.41 MB (10. maddede 1.40 MB'tı).

**Kapsam dışı bırakıldı:**

- **E2 (ücretsiz sürümdeki promo yuvası)** — ana ekrana ikinci bir Pro girişi
  koymak bu maddenin dört kutusunda yoktu; PRO rozeti ve solmuş ok zaten
  paywall'a gidiyor. Kart, ekranın en üstündeki öğelerden birini aşağı itmek
  demekti ve o kararın kendi gerekçesi olmalı.
- **E3 (AdMob karşılaştırma paneli)** — ekran değil, tasarımın kendi karar notu:
  reklam SDK'sı `INTERNET` ve `AD_ID` ister, o satır eklendiği an ürünün tüm
  iddiası (F4, `PLAY_CONSOLE.md`) çöker. Karar zaten Pro'dan yana verildiği için
  uygulanacak bir şey yok.

**Hâlâ açık:** Ekranın kendisi (Compose) otomatik test edilmiyor — 4, 5, 6, 8, 9
ve 10. maddedeki gerekçe burada da geçerli, `compose-ui-test` bağımlılığı hâlâ
yok. Test edilen kısım sınırın aritmetiği ve yetkinin kalıcılığı; kapsam dışı
kalan yalnızca çizim. Gerçek cihazda denenmedi (1. maddedeki açık kutu) ve `₺`
font hatası (1. madde) bu ekranı da etkiliyor. Billing kutusu açık kaldığı
sürece **Pro satılamaz**: ekran görünür ama satın alma yolu yok.
`PLAY_CONSOLE.md`'ye uygulama içi satın alma beyanı da bu yüzden eklenmedi —
7. maddede "olmayan özelliği beyana yazma" diye kurulan kuralın aynısı.

### 12. 4×2 Pro widget

Yapıldı. `widget/` altında üç yeni dosya, iki düzen ve yeni bir provider. Bu
maddenin işi ikinci bir widget çizmek değildi; **11. maddede tanımlanan Pro'ya
satılabilir ikinci bir şey koymak**tı: paywall o güne kadar tek özellik
sayıyordu ve "olmayan bir şeyin parası istenmiyor" diye yazıyordu.

- [x] 2×1 widget'ın yanına 4×2 sürümü — `widget/WideBudgetWidget.kt`,
      `res/layout/widget_budget_wide.xml` (veri) ve
      `res/layout/widget_budget_wide_locked.xml` (ücretsiz sürüm)

**Widget kendi aritmetiğini yapmıyor.** Toplam, kategori kırılımı ve değişim
rozeti ana ekranın kullandığı `toUiState`'ten okunuyor
(`widget/BudgetWidgets.kt`). Widget kendi sorgusunu ve kendi işaret kuralını
yazsaydı iade (REFUND) düşme kuralının iki kopyası olurdu ve kullanıcı ana
ekranda bir rakam, ev ekranında başkasını görürdü — 8. maddedeki `signedMinor`
ile aynı gerekçe. Geriye kalan iş biçimleme; o da `wideSnapshot`'ta duruyor ve
Android bağlamı olmadan test ediliyor.

**Rozetin ne zaman susacağına widget karar vermiyor.** `HomeUiState.change`
zaten null geliyorsa (önceki ay net sıfır/negatif, açık ay iadeyle negatife
düştü, fark yüzde yarımın altında) rozet çizilmiyor. 10. maddede kurulan üç
susma koşulu burada tekrar yazılsaydı ikisi zamanla ayrışırdı.

**Kilit sessiz değil.** Ücretsiz sürümde widget ana ekrandan kaybolmuyor ya da
boş durmuyor: ne olduğunu yazıyor ve dokunuş **doğrudan paywall'a** gidiyor.
Provider'ı `PackageManager` ile kapatmak da düşünüldü — o zaman kullanıcı
widget'ı seçiciden hiç göremez, dolayısıyla Pro'nun ne açtığını da öğrenemezdi.
Kullanıcıyı ana ekrana bırakıp "Pro'da" demek ise 9. maddede kaldırdığımız ölü
tıklamanın uzun yoldan yapılmış hâli olurdu.

**Dokunuş rotayı adreste taşıyor.** `MainActivity.EXTRA_ROUTE` niyetle geliyor,
`deepLinkTarget` (`ui/nav/AppNavHost.kt`) onu iki kez süzüyor: yalnızca
`Route.PAYWALL` açılıyor (uygulama dışından gelen bir dizgi doğrudan
`navigate`'e verilseydi grafta karşılığı olmayan bir adres çalışma anında
patlardı) ve yalnızca onboarding bittiyse (kurulum sihirbazının üstüne satın
alma ekranı itmek, kullanıcıyı izni hiç anlatmadan ödeme sayfasında bırakırdı).
Hedef `startDestination` yerine üstüne itiliyor: geri tuşu kullanıcıyı deftere
bırakmalı, uygulamadan atmamalı.

**Üç satır, dört değil.** 4×2 hücrenin yüksekliği başlık, tutar ve üç satırdan
sonra bitiyor (`WIDE_WIDGET_ROW_COUNT`). Dördüncü satır bazı launcher'larda
sessizce kırpılır, kullanıcı listenin orada bittiğini sanırdı. Satır sayısı
sabit çünkü `RemoteViews` çalışma anında görünüm üretemez; kullanılmayanlar
`GONE` yapılıyor.

**Yenileme tek kapıdan geçiyor.** `BudgetWidgets.refreshAll` iki widget'ı da
çiziyor; üç eski çağrı yeri (`NotificationService`, `SettingsViewModel`,
`TestNotificationSeeder`) buna taşındı ve dördüncüsü eklendi: `ProViewModel`.
Yetki değiştikten sonra yenilenmeseydi Pro'ya geçen kullanıcı bir sonraki
30 dakikalık güncellemeye kadar kilitli kartı görmeye devam ederdi. Çağrı
yerleri tek tek provider'ları bilseydi üçüncü bir widget eklendiği gün biri
unutulurdu.

**2×1'de bir hata düzeldi.** Ay adı `MONTHS[month].uppercase()` ile
büyütülüyordu — yerel ayar verilmediği için "NİSAN"/"EKİM" yerine
"NISAN"/"EKIM" çıkıyordu. Artık `MonthCursor.upperLabel` kullanılıyor; kural
zaten 10. maddede bir kez yazılmıştı, widget'ın kendi kopyası vardı ve o kopya
yanlıştı.

**Paywall listesi büyüdü.** E1 artık iki özellik sayıyor: sınırsız geçmiş ve
4×2 widget. 11. maddede "olmayan bir şeyin parası istenmiyor" diye yazılan
paragraf da güncellendi — CSV dışa aktarma ve kendi desenini yazma hâlâ yok.

**Testler:** `WideWidgetSnapshotTest` (10, saf JVM — başlık, biçimleme,
sıralama, satır kısma, iade düşme, rozet yönü ve susması, önceki ayın listeye
sızmaması), `DeepLinkTest` (4, saf JVM — beyaz liste ve onboarding kısıtı).
`:app` toplamı 136 → 150. Testlerin ısırdığı doğrulandı: `wideSnapshot`'tan
`take(WIDE_WIDGET_ROW_COUNT)` kaldırılınca test kırmızı yandı, sonra geri
alındı. Release derlemesi de koştu: imzasız APK 1.42 MB (11. maddede 1.41
MB'tı) ve R8 kuralı yine elle yazılmadı — AGP üretti:
`-keep class com.bildirimbutce.app.widget.WideBudgetWidget { <init>(); }`.

**Kapsam dışı bırakıldı:**

- **Kilit ekranı widget'ı** — tasarımın E1 listesinde "4×2 kategori kırılımı ve
  kilit ekranı widget'ı" birlikte geçiyor, ama kilit ekranı widget'ları
  Android 5.0'da kaldırıldı. Proje `minSdk 26`; yapılacak bir şey yok. (Yan
  bulgu: 2×1'in `widget_budget_info.xml` dosyasındaki
  `widgetCategory="home_screen|keyguard"` bayrağının `keyguard` kısmı bu yüzden
  etkisiz. Dokunulmadı, ama bir gün temizlenmeli.)
- **2×1'in başlığındaki "↓ %12"** — 10. maddede açık bırakılmıştı. Rozet 4×2'ye
  kondu; 2×1'e sığdırmak tutarın yanına ikinci bir metin alanı sokmak demek ve
  o widget'ın tek işi tek rakamı okunaklı göstermek.

**Hâlâ açık:** `RemoteViews`'in kendisi otomatik test edilmiyor — 4, 5, 6, 8,
9, 10 ve 11. maddedeki gerekçenin aynısı: `compose-ui-test` bağımlılığı yok ve
`RemoteViews` için de bir koşum takımı yok. Test edilen kısım satırların
aritmetiği ve rotanın süzgeci; kapsam dışı kalan yalnızca çizim. Gerçek cihazda
denenmedi (1. maddedeki açık kutu) — özellikle 4×2 düzeninin dar launcher'larda
alta taşıp taşmadığı orada görülür. Kilitli kart, Pro satılamadığı sürece
kullanıcının **tek** göreceği hâl: 11. maddedeki billing kutusu açık kaldıkça
satın alma yolu yok, hata ayıklama derlemesindeki anahtar dışında kilit
açılmıyor.

---

## P4 — Kozmetik ve temizlik

### 13. Bayat dokümantasyon (hızlı iş, ~5 dk)

Yapıldı. Dördü de okuyanı yanlış yönlendiriyordu; her iddia düzeltilmeden önce
dosya sisteminden ve `git ls-files`'tan doğrulandı.

- [x] `design/README.md:17` "font dosyaları eksik" diyor — fontlar
      `app/src/main/res/font/` içinde mevcut (5 ttf)
- [x] `design/README.md:19` "Ekran düzenleri: Aktarılmadı" — B1-B4, D1 ve
      widget aktarıldı
- [x] `README.md:119` "Gradle wrapper jar'ı repoda yok" —
      `gradle/wrapper/gradle-wrapper.jar` (42 KB) repoda var
- [x] `README.md` durum tablosu ve yol haritası bölümü bu dosyaya işaret etsin

**Satır 17'de iki hata vardı, biri listede yoktu.** "Font dosyaları eksik"
cümlesi okuyucuyu `res/font/README.md`'ye yolluyordu — **o dosya da yok**. Bayat
bir cümleyi düzeltip yanındaki kırık bağı bırakmak, maddenin işini yarım yapmak
olurdu.

**"Aktarıldı" yazmakla yetinilmedi.** Madde yazıldığında liste "B1-B4, D1 ve
widget"tı; o gün bugün A, C1, D2, E1, F1-F4 ve 4×2 widget eklendi. Maddedeki
eski listeyi kopyalasaydık bayat bir satırı daha taze bir bayat satırla
değiştirmiş olurduk. Aktarılmayanlar (C2, C3, E2, E3) da yazıldı, yoksa
"Aktarıldı" satırı bu kez fazlasını iddia ederdi.

**Fontlar duruyor ama `₺` bozuk.** Satır 17'yi "fontlar mevcut" diye düzeltip
1. maddedeki U+20BA hatasını yazmamak, tipografi satırını okuyan birine yine
yanlış bilgi vermek demekti; not tabloya eklendi.

**Durum tablosu iki yönden yanlıştı.** `:app` için "cihazda derlenmedi" diyordu
(emülatörde derlendi, 150 test CI'da koşuyor); `:parser` için "167 örnekte %100"
diyordu ama örneklerin **sentetik** olduğunu ve gerçek örnek sayısının **0**
olduğunu söylemiyordu — README'nin kendi "%100 ne anlama gelmiyor" bölümüyle
çelişiyordu. Yayın kararının hangi sayıya baktığı artık tablonun kendisinde
yazıyor.

**Kırmızı testler tabloya yazıldı.** Kurulum adımında okuyucuya
`./gradlew :parser:test` koşturuluyordu; o görev 2c yüzünden kırmızı. Testi
düzeltmek bu maddenin işi değil (2c açık kaldı), ama okuyucunun kırmızı bir
görevi habersiz çalıştırmasına bu madde göz yumamazdı. Komut, CI'ın gerçekten
koştuğu ikisiyle değiştirildi: `:parser:verify` ve `:app:testDebugUnitTest`.

**Kapsam dışı:** 2c'deki üç kırmızı test, 2b'deki "OTOMATIK URETILDI" iddiası ve
`₺` font hatasının kendisi. Üçü de kendi maddesinde duruyor; burada yalnızca
**belgelendiler**.

### 14. Tasarım sapmaları

Yapıldı. İki kutu iki ayrı cinsten: birincisi eksik iş, ikincisi **karar**.
Ayrıntı ve gerekçeler `EKSIKLER.md`'de.

- [x] Boş durum kartındaki kesikli (dashed) kenarlık — şu an düz çizgi, dashed
      için özel `Canvas` çizimi gerekiyor
- [x] Token yuvarlamaları — piksel-birebir değil, token-birebir

**Kesikli kenarlık `Modifier.dashedBorder`'a çıktı** (`ui/theme/Theme.kt`), kart
içine gömülmedi: tasarımın tek `1px dashed` ögesi bu kart, ama kural bir çizim
ayrıntısı değil, token dosyasının işi — `border` modifier'ı yalnızca düz çizgi
çizdiği için Canvas'a düşen bir *primitive* bu.

**Çizgi içeri yaslanıyor.** `Stroke` yolun üzerine ortalanır; kaydırılmasaydı
yarısı dışarıda kalır ve üstteki `clip` onu keserdi — kenarlık yer yer yarıya
inmiş görünürdü. Yarıçap da aynı miktarda küçülüyor, yoksa köşe kaydırılmış
dikdörtgenle eş merkezli olmaz ve çizgi köşede kalınlaşırdı.

**Çizgi/boşluk uzunluğu tasarımdan gelmiyor, gelemezdi.** CSS'te `dashed`
deseninin ölçüsü **tanımsızdır**, tarayıcıya bırakılmıştır; aktarılacak bir sayı
yok. Seçim yapıldı ve gerekçesi yazıldı: çizgi (6dp) boşluktan (4dp) uzun,
çünkü eşit olsalardı 1dp kalınlıkta noktalı kenarlıktan ayırt edilemezdi.

**İkinci kutu "yapılmadı" değil, "böyle kalacak".** Piksel-birebirlik burada
yanlış hedef: `Theme.kt` ölçeğe "tasarım token'ları birebir" diyor ve her
ekranda ham `dp` kullanmak o ölçeği çözerdi — 8. maddedeki `signedMinor` ile
aynı gerekçe, kural tek yerde durmalı. Kutu bu yüzden *karar* olarak
kapatıldı, iş olarak değil.

**Ama "en yakın token" bir kuraldı ve bu kartta tutmuyordu.** Tasarım değerleri
dosyadan okundu (mock 390px genişlikte, yani px ≈ dp) ve dört değer en yakın
token'a değil, daha uzağına yuvarlanmıştı:

| Tasarım | Önce | Sonra |
|---|---|---|
| dikey iç boşluk 28px | `s5` (20dp) | `s6` (26dp) |
| ikon altı boşluk 16px | `s3` (12dp) | `s4` (16dp) — birebir |
| başlık altı boşluk 7px | `s1` (4dp) | `s2` (8dp) |
| paragraf 13px/1.55 | `body` (14.5sp/23) | `bodySmall` (13sp/20) — birebir |

`bodySmall`'un satır yüksekliği (20sp) tasarımın `13×1.55 = 20.15px` değerine
birebir oturuyor; o token zaten bu metin için çizilmiş, yanlış olan seçimdi.

**Dokunulmayanlar da bilinçli.** Yarıçap 20px zaten `AppRadius.lg` (20dp); ikon
kutusunun 16px yarıçapı `md`'ye (13dp) yuvarlandı çünkü en yakını o — `lg` daha
uzak. Başlık tasarımda `600 18px`: hiçbir token bunu karşılamıyor (`titleCard`
boyutta yakın ama 500 ağırlıkta, `bodyLarge` 600 ağırlıkta ama 15.5sp). Boyut
bir kart başlığında ağırlıktan baskın olduğu için `titleCard` kaldı —
`EKSIKLER.md`'nin "600 ağırlık" örneği tam olarak bu.

**Testler:** `DashedBorderTest` (4, saf JVM — yarıçapın çizgi yarısı kadar
küçülmesi ve negatife düşmemesi). `:app` toplamı 150 → 154. Testlerin ısırdığı
doğrulandı: `insetCornerRadius`'taki `/2f` ve `coerceAtLeast` kaldırılınca 4
testin 3'ü kırmızı yandı, sonra geri alındı. Release derlemesi de koştu:
imzasız APK 1.42 MB (12. maddeyle aynı).

**Hâlâ açık:** Çizimin kendisi otomatik test edilmiyor — 4, 5, 6, 8, 9, 10, 11
ve 12. maddedeki gerekçenin aynısı, `compose-ui-test` bağımlılığı yok. Test
edilen kısım köşe aritmetiği; kapsam dışı kalan yalnızca çizim, ve bu maddede
"çizim" işin büyük kısmı — **kesikli kenarlığın ekranda nasıl durduğu gerçek
cihazda görülmeli** (1. maddedeki açık kutu). Token taraması yalnızca bu kartta
yapıldı; diğer ekranlar aynı gözle taranmadı, orada da en yakın token'a
yuvarlanmamış değerler olabilir. İkondaki `bbRing` halka animasyonu uygulanmadı
(`bbRise`, `bbDrop`, `bbBar` ile aynı durumda).

---

## Bilerek dışarıda bırakılanlar

Bunlar "eksik" değil, kapsam kararı: bütçe hedefleri, çoklu para birimi, dışa
aktarma, hesap eşleştirme, bulut senkronizasyonu.
