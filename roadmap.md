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
hedefli bir graf için maliyeti kazancından büyük. Graf artık dört hedefli (5,
6 ve 8. maddeler), ama hâlâ test edilmiyor; grafın *hangi hedefle açıldığı*
kararı `startDestination` olarak ayrıldığı için o kadarı test ediliyor (madde
6). Ekranlardaki ölü `onClick = {}` çağrılarından "elle ekle" (madde 5) ve
"RAPOR →" (madde 8) bağlandı; ayarlar dişlisi hâlâ ölü, bağlanması 9 numaralı
maddenin işi.

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

**Beyana bilerek yazılmayan bir cümle var.** "Kullanıcı dinlenen banka
listesini daraltabilir" doğru değil: `Prefs.enabledSources` kodda var ve
`NotificationService.kt:52` onu okuyor, ama **hiçbir yer yazmıyor** — ayarlar
ekranı yok (9. madde). Gerekçelendirmeye koysaydık inceleme ekibinin
uygulamada bulamayacağı bir özellik vaat etmiş olurduk. 9. madde bitince
`PLAY_CONSOLE.md`'ye eklenmeli.

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

- [ ] Ayarlar ekranı
- [ ] `HomeScreen.kt:273` `SettingsGearButton` → ekrana bağla (şu an `onClick = {}`)

### 10. Aylık değişim rozeti ("↓ %12 TEMMUZ")

Tasarımda var, kodda hiç render edilmiyor. Önceki ayla karşılaştırma gerekiyor;
`HomeViewModel.kt:74` `HomeUiState` yalnızca tek ayın verisini taşıyor. (Rapor
ekranı — madde 8 — önceki ayları zaten okuyor: `ExpenseRepository.observeMonths`
burada da kullanılabilir.)

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
