# Tasarım kaynakları

Claude Design ile üretilen ekran tasarımları. `.dc.html` dosyaları tarayıcıda
doğrudan açılır.

| Dosya | İçerik |
|---|---|
| `design-v2-tum-ekranlar.dc.html` | **Güncel.** 20 ekran: onboarding, ana ekran, rapor, düzeltme, Pro, ayarlar, widget |
| `design-v1-uygulama-ekranlari.dc.html` | v1. Token listesi burada duruyor |
| `design-v0-mevcut-ekranlar.dc.html` | Kodun tasarımdan önceki hali |

## Koda aktarım durumu

| Parça | Durum |
|---|---|
| Renk token'ları (açık + koyu + kategori) | `ui/theme/Theme.kt` |
| Tipografi ölçeği | `ui/theme/Type.kt` — font dosyaları eksik, `res/font/README.md`'ye bakın |
| Köşe yarıçapı + aralık ölçeği | `ui/theme/Theme.kt` (`AppRadius`, `AppSpace`) |
| Ekran düzenleri | Aktarılmadı |

## v1 ↔ v2 farkı — dikkat

v1'deki tipografi tablosu **Bricolage Grotesque + Instrument Sans** diyor,
v2 metni ise ikisini bırakıp **Schibsted Grotesk** kullandığını söylüyor.
`Type.kt` v2'yi uyguluyor. v1 tablosunu referans alan olursa yanlış font gelir.
