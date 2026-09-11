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
| Tipografi ölçeği | `ui/theme/Type.kt` — 6 TTF `app/src/main/res/font/` içinde (Schibsted Grotesk ×4, JetBrains Mono ×2). Ölçeğin tasarımla aynı kaldığı `TypeScaleTest` ile sabitli |
| Köşe yarıçapı + aralık ölçeği | `ui/theme/Theme.kt` (`AppRadius`, `AppSpace`) |
| Ekran düzenleri | Aktarıldı — A, B1-B4, C1, D1-D2, E1, F1-F4 ve iki widget (2×1, 4×2) |

**`₺` (U+20BA) simgesi bu fontlardan çizilmiyor:** Schibsted Grotesk 1.100'ün
U+20BA glifi yanlış çizilmiş (çift çizgili pound) ve JetBrains Mono 2.211'de
glif hiç yok. Simge sistem fontuna düşürülüyor, rakamlar Grotesk'te kalıyor —
kural `ui/theme/Lira.kt`'de, gerekçesiyle. `roadmap.md` 15. madde.

Aktarılmayan tasarım ekranları (C2, C3, E2, E3) ve gerekçeleri kökteki
`roadmap.md` ile `EKSIKLER.md`'de.

## Tasarımda 700 var, Mono'da yok

Tasarımın bütün display katmanı (66/46/34/31 px tutarlar) **700**. Uygulamada
uzun süre 700 kesimi yoktu ve bu sayılar 600'de çiziliyordu; `roadmap.md` 17.
maddede `schibsted_grotesk_bold.ttf` eklendi. **JetBrains Mono'nun 700'ü
bilerek yok** — tasarımda Mono-700 yalnızca on adet tek karakterlik rozette
geçiyor ve 109 KB'lik bir TTF onun için pahalı. Mono'ya Bold istenirse Compose
sentetik kalınlık üretir; istenmemeli.

## v1 ↔ v2 farkı — dikkat

v1'deki tipografi tablosu **Bricolage Grotesque + Instrument Sans** diyor,
v2 metni ise ikisini bırakıp **Schibsted Grotesk** kullandığını söylüyor.
`Type.kt` v2'yi uyguluyor. v1 tablosunu referans alan olursa yanlış font gelir.
