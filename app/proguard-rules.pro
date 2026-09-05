# R8 kurallari (isMinifyEnabled + isShrinkResources, sadece release).
#
# Bu dosya BILEREK kisa. Asagidakiler zaten baska yerden geliyor, buraya
# yazmak gereksiz tekrar olurdu:
#
#   - Manifest'te tanimli bilesenler (App, MainActivity, NotificationService,
#     BudgetWidget). AGP, birlestirilmis manifest'ten otomatik -keep uretiyor:
#     build/intermediates/aapt_proguard_file/release/.../aapt_rules.txt
#   - Compose, Room, Navigation, Lifecycle: her AAR kendi consumer kurallarini
#     (proguard.txt) tasiyor, R8 onlari otomatik okuyor.
#   - :parser modulu: hicbir yerde reflection kullanmiyor. Class.forName,
#     javaClass, @Keep, Serializable ve getIdentifier aramalari :app ve :parser
#     kaynaklarinda SIFIR sonuc veriyor. Ayristirici saf Kotlin, JSON'u elle
#     okuyor (Json.kt); veri siniflarina isimle erisen kimse yok.
#   - patterns.json: kaynak (resource) degil ASSET. isShrinkResources
#     asset'lere dokunmuyor, kural gerekmiyor.

# Room, veritabani sinifinin uretilmis kardesini isimden buluyor
# (AppDatabase -> AppDatabase_Impl). "extends" dolayli alt siniflari da
# kapsadigi icin tek satir _Impl'i de koruyor. room-runtime'in kendi consumer
# kurali bunu zaten yapiyor; burada ucuz sigorta olarak duruyor - kural
# dusseydi hata derlemede degil, kullanicinin cihazinda calisma aninda cikardi.
-keep class * extends androidx.room.RoomDatabase

# Paging kullanilmiyor; room-runtime yine de ondan bahsettigi icin R8 uyariyor.
-dontwarn androidx.room.paging.**
