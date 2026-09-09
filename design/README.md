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
| Tipografi ölçeği | `ui/theme/Type.kt` — 5 TTF `app/src/main/res/font/` içinde (Schibsted Grotesk ×3, JetBrains Mono ×2) |
| Köşe yarıçapı + aralık ölçeği | `ui/theme/Theme.kt` (`AppRadius`, `AppSpace`) |
| Ekran düzenleri | Aktarıldı — A, B1-B4, C1, D1-D2, E1, F1-F4 ve iki widget (2×1, 4×2) |

**Fontlar duruyor ama `₺` (U+20BA) glifi sorunlu:** uygulama içi ekranlarda `£`
olarak render ediliyor, kaynak metinler doğru. Widget sistem fontu kullandığı
için etkilenmiyor. Yayın öncesi düzeltilmeli — `roadmap.md` 1. madde.

Aktarılmayan tasarım ekranları (C2, C3, E2, E3) ve gerekçeleri kökteki
`roadmap.md` ile `EKSIKLER.md`'de.

## v1 ↔ v2 farkı — dikkat

v1'deki tipografi tablosu **Bricolage Grotesque + Instrument Sans** diyor,
v2 metni ise ikisini bırakıp **Schibsted Grotesk** kullandığını söylüyor.
`Type.kt` v2'yi uyguluyor. v1 tablosunu referans alan olursa yanlış font gelir.
