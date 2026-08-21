# Gizlilik Politikası

**Bildirim Bütçe**, harcamalarınızı takip etmek için telefonunuza gelen banka
bildirimlerini okur.

## Toplanan veriler

Hiçbir veri toplanmaz. Uygulama, okuduğu bildirim metinlerini, tutarları ve
işyeri adlarını **yalnızca cihazınızın kendi deposunda** saklar.

## Bu iddianın teknik doğrulaması

Bu uygulama `android.permission.INTERNET` iznini **istemez**. İzin
`AndroidManifest.xml` içinde tanımlı olmadığı için uygulama teknik olarak
ağa bağlanamaz. Kaynak kodu açıktır ve manifest dosyasından kendiniz
doğrulayabilirsiniz.

Ayrıca `data_extraction_rules.xml` ile veritabanı bulut yedeklemesinden
hariç tutulmuştur; harcama geçmişiniz Google hesabınıza da yüklenmez.

## Bildirim erişimi izni

Android, bildirim okuma iznini ayrı bir sistem ekranından ister. Uygulama
yalnızca `patterns.json` dosyasında tanımlı banka ve mesajlaşma
uygulamalarından gelen bildirimleri işler; diğer bildirimler okunmaz.

## Üçüncü taraflar

Reklam ağı, analitik veya çökme raporlama servisi kullanılmaz.

## Verilerin silinmesi

Uygulamayı kaldırmak tüm verileri kalıcı olarak siler.

## İletişim

Sorular için GitHub üzerinden issue açabilirsiniz.
