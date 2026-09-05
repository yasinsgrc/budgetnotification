# Eksikler (Görev 2 — tasarım v2 uygulaması)

Görev 2 kapsamı yalnızca B1-B4, D1 ve widget'ın 2×1 sürümüydü. Tasarımda
görünen ama bu kapsamda **görsel olarak konup işlevsiz bırakılan** öğeler:

- ~~**"RAPOR →"** (HomeScreen, B1/B2 işlemler başlığı)~~ — **bağlandı.** C1
  yapıldı (`ui/report/ReportScreen.kt`); açık olan ayın raporunu `Route.REPORT`
  üzerinden açıyor. C2 (kategori detayı) ve C3 (paylaşılabilir ay kapanışı)
  hâlâ yok; gerekçeleri `roadmap.md` madde 8'de.
- ~~**"+ Elle ekle"** (HomeScreen, kayan buton) ve **"+ Elle harcama ekle"**
  (B4 boş durum kartı)~~ — **bağlandı.** D2 yapıldı (`ui/AddExpenseScreen.kt`);
  ikisi de `Route.ADD_EXPENSE` hedefini açıyor. Ayrıntı: `roadmap.md` madde 5.
- **PRO rozeti** (MonthTopBar) — E bölümü (Pro/paywall) bu aşamada yok.
  Sadece görsel; hiçbir tıklama davranışı yok.
- **Ayarlar dişlisi** (MonthTopBar, ⚙) — F bölümü (ayarlar ekranı) bu
  aşamada yok. Tıklanınca hiçbir şey yapmıyor.

## Bilinçli kapsam dışı bırakılanlar (talimatta belirtildiği gibi)

- ~~B3 (izin kapalı) ekranında "elle girilenler" listesi ve altındaki soluk
  toplam yapılmadı — talimat açıkça bunu manuel girişe bağlı olduğu için
  hariç tuttu.~~ — **yapıldı** (D2 ile birlikte, `roadmap.md` madde 5). B3
  artık: izin uyarısı kartı + elle girilenler listesi ve soluk toplamı; elle
  giriş yoksa boş durum + "+ Elle harcama ekle" düğmesi.

## Veriye bağlı olduğu için bu pasta eklenmeyen tasarım öğesi

- **"↓ %12 TEMMUZ" değişim rozeti** (B1/B2 toplam başlığı altında) —
  önceki ayla karşılaştırma gerektiriyor. `HomeViewModel`'in state akışını
  değiştirmeden hesaplanamaz ve talimat state akışına dokunmamamı
  söylüyor ("davranış değişikliği gerekiyorsa önce sor"). Bu pas atlandı;
  rozet hiç render edilmiyor (yanıltıcı sabit bir değer koymak yerine).

## Widget

- Yalnızca 2×1 yapıldı (talimat gereği). 4×2 Pro widget'ı bu aşamada yok.
- Kategori şeridi RemoteViews'ta gerçek oranlarla çizilemediği için
  (`setViewLayoutWeight` API 31+ istiyor, minSdk 26) bir `Bitmap` üzerine
  `Canvas` ile çiziliyor. Bkz. `BudgetWidget.kt` içindeki yorum.

## Bilinen küçük sapmalar

- Tasarımdaki bazı px/font-weight değerleri (ör. 600 ağırlık) doğrudan
  karşılığı olmayan yerlerde en yakın `AppText`/`AppSpace`/`AppRadius`
  token'ına yuvarlandı (örn. buton metinleri `labelChip`, ikon kutuları
  `AppSpace.s8`). Piksel-birebir değil, token-birebir.
- Boş durum kartındaki kesikli (dashed) kenarlık, Compose'ta standart
  `border` modifier ile düz çizgiye indirgendi (dashed için özel `Canvas`
  çizimi gerekirdi, kapsam dışı bırakıldı).
