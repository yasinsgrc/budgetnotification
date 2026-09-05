# Play Console beyanları

Bildirim erişimi (`BIND_NOTIFICATION_LISTENER_SERVICE`) Google Play'de
**kısıtlı bir izindir**. Uygulama, izni çekirdek işlevi için kullandığını ayrı
bir formda gerekçelendirmek zorundadır; gerekçe zayıf ya da gizlilik
politikasıyla çelişkili olursa yayın reddedilir.

Aşağıdaki metinler Play Console'a **kopyalanmak üzere** hazırlandı.
`PRIVACY.md` ile birlikte güncellenmeleri gerekir — ikisi çelişirse inceleme
ekibi çelişkiyi gerekçe sayar.

---

## 1. Bildirim erişimi gerekçelendirmesi

**Türkçe:**

> Bildirim Bütçe, kullanıcının harcamalarını otomatik olarak takip eden bir
> bütçe defteridir. Uygulamanın çekirdek işlevi, bankaların gönderdiği harcama
> bildirimlerini okuyup içindeki tutarı, işyeri adını ve tarihi çıkarmak ve bu
> harcamayı cihazdaki yerel bir veritabanına yazmaktır. Bildirim erişimi
> olmadan uygulamanın otomatik takip işlevi tamamen ortadan kalkar; geriye
> yalnızca her harcamanın elle girildiği bir defter kalır.
>
> Erişim, tüm bildirimlere değil, uygulamayla birlikte gelen `patterns.json`
> dosyasında tanımlı 17 banka uygulamasına sınırlıdır. Tanımlı olmayan bir
> paketten gelen bildirim, metni okunmadan önce reddedilir. Kullanıcı bu listeyi
> uygulama içindeki ayarlar ekranından daha da daraltabilir; kapattığı
> uygulamanın bildirimi servise ulaşsa bile ayrıştırılmadan atılır.
>
> Okunan veriler cihazdan çıkmaz. Uygulama `android.permission.INTERNET`
> iznini istemez; bu, yayınlanan APK'nın manifest dosyasından doğrulanabilir.
> Sunucumuz, analitik aracımız veya reklam ağımız yoktur.

**English:**

> Bildirim Bütçe is an expense tracker. Its core function is to read the
> transaction notifications sent by the user's bank, extract the amount,
> merchant name and date, and store the expense in a local on-device database.
> Without notification access the automatic tracking function disappears
> entirely, leaving only a ledger in which every expense is entered by hand.
>
> Access is limited to the 17 banking apps declared in the bundled
> `patterns.json` file, not to all notifications. A notification from any
> other package is rejected before its text is read. The user can narrow this
> list further from the in-app settings screen; a notification from an app the
> user has switched off is discarded without being parsed.
>
> The data never leaves the device. The app does not request
> `android.permission.INTERNET`, which can be verified from the manifest of
> the published APK. There is no server, analytics SDK or ad network.

### Bu iddiaların kod karşılığı

İnceleme ekibi APK'yı açıp bakarsa aşağıdakileri görür. İddialar buna
dayandığı için **kod değişirse metin de değişmeli**:

| İddia | Nerede doğrulanır |
|---|---|
| İnternet erişimi yok | `AndroidManifest.xml` — `INTERNET` izni yok; birleştirilmiş release manifest'inde de yalnızca AGP'nin eklediği `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` var |
| Yalnızca tanımlı bankalar okunuyor | `NotificationService.kt:51` — `if (!parser.isKnownSource(pkg)) return`, bildirim metni **okunmadan önce** çalışır |
| Kullanıcı listeyi daraltabiliyor | Ayarlar → Kaynaklar (`ui/settings/SourcesScreen.kt`); tercih `Prefs.enabledSources`'a yazılır, `NotificationService.kt:55` metni okumadan önce `SourceSelection.listensTo` ile bakar |
| Veri cihazda kalıyor | Room veritabanı; ağa yazan kod yok |
| Buluta yedeklenmiyor | `AndroidManifest.xml` — `allowBackup="false"` + `@xml/data_extraction_rules` |
| Kullanıcı verisini silebiliyor | Ayarlar → Verilerin nereye gidiyor → "Tüm veriyi sil" (`ExpenseRepository.eraseAll`) |

**Beyandaki iki cümle 9. madde ile doğru oldu.** "Kullanıcı dinlenen banka
listesini daraltabilir" ve "verisini uygulama içinden silebilir" cümleleri, F
bölümü (ayarlar) yazılana kadar bilerek yazılmamıştı: `Prefs.enabledSources`
kodda vardı ama hiçbir yer yazmıyordu, silme yolu ise hiç yoktu. İkisinin de
artık uygulamada karşılığı var; inceleme ekibi ayarlar dişlisinden ikisine de
ulaşabilir.

---

## 2. Veri güvenliği (Data safety) formu

| Soru | Cevap |
|---|---|
| Veri topluyor musunuz? | **Hayır** |
| Veri paylaşıyor musunuz? | **Hayır** |
| Veriler aktarım sırasında şifreleniyor mu? | Uygulanamaz — veri aktarılmıyor (internet izni yok) |
| Kullanıcı verisinin silinmesini isteyebilir mi? | Veriler yalnızca cihazda; uygulama içinden "Tüm veriyi sil" ile ya da uygulamayı kaldırarak silinir |

**Dikkat:** "Veri topluyor musunuz?" sorusunun Play'deki tanımı
"uygulamanızdan **cihaz dışına** veri çıkıyor mu"dur. Bildirimlerin okunup
cihazda saklanması bu tanıma göre "toplama" değildir. Uygulamaya bir gün
INTERNET izni eklenirse (örneğin `PatternProvider.refreshFromRemote()` için)
bu cevapların **hepsi** yeniden değerlendirilmeli.

---

## 3. Gizlilik politikası bağlantısı

Play Console gizlilik politikası için erişilebilir bir URL ister; depodaki
`PRIVACY.md` tek başına yeterli olmayabilir, çünkü ham dosya bağlantısı kalıcı
bir politika sayfası sayılmayabilir. GitHub Pages ya da benzeri bir yerde
yayınlanıp URL'si Console'a girilmeli.

`PRIVACY.md` ile bu dosya çelişmemeli. Şu an ikisi de aynı üç şeyi söylüyor:
internet izni yok, veri cihazda kalıyor, yalnızca tanımlı banka uygulamaları
okunuyor.

---

## 4. Hâlâ insan eli gerektirenler

Bunlar depoya konamaz; Play Console'da ya da tasarım aracında üretilir:

- [ ] 512×512 PNG mağaza ikonu — uygulama içi adaptive icon'dan **ayrı** bir
      varlıktır, `ic_launcher` dosyaları Console'a yüklenemez
- [ ] 1024×500 öne çıkan görsel (feature graphic)
- [ ] En az 2 telefon ekran görüntüsü
- [ ] Kısa (80 karakter) ve uzun (4000 karakter) mağaza açıklaması
- [ ] Gizlilik politikası URL'si (3. madde)
- [ ] İçerik derecelendirme anketi
- [ ] Release imzalama anahtarının üretilmesi ve **yedeklenmesi**
      (`keystore.properties.example`)
