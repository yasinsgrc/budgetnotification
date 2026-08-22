# Font dosyaları

`Type.kt` bu klasörde şu beş dosyayı bekliyor. Olmadan **derleme başarısız olur**
(`R.font.*` çözülemez):

| Dosya adı | Kaynak |
|---|---|
| `schibsted_grotesk_regular.ttf` | fonts.google.com/specimen/Schibsted+Grotesk (400) |
| `schibsted_grotesk_medium.ttf` | aynı aile (500) |
| `schibsted_grotesk_semibold.ttf` | aynı aile (600) |
| `jetbrains_mono_regular.ttf` | fonts.google.com/specimen/JetBrains+Mono (400) |
| `jetbrains_mono_medium.ttf` | aynı aile (500) |

İkisi de SIL Open Font License — ticari kullanım ve APK içinde dağıtım serbest.
Lisans metnini `OFL.txt` olarak bu klasöre koymanız iyi olur.

Dosya adları **küçük harf ve alt çizgi** olmalı; Android kaynak adı kuralı.
Variable font (`[wght].ttf`) indirirseniz statik ağırlıklara ayırın veya
`Font(..., variationSettings = ...)` kullanın.

Beş TTF yaklaşık 400–600 KB yer kaplar. APK boyutu sorun olursa yalnızca
Latin-Ext alt kümesini gömün.
