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

- [ ] Kendi telefonundan + 4-5 kişiden gerçek banka bildirimi topla (kart no,
      isim maskelenmiş)
- [ ] `./scripts/add-fixture.sh` ile test setine ekle
- [ ] `gradle :parser:verify` ile ölç
- [ ] **Karar noktası:** 150-200 gerçek örnekte doğruluk %95'in altında kalıyor
      ve `patterns/patterns.json`'a desen ekleyerek yükselmiyorsa **yayınlama**

### 3. `:app` için test yaz

`testInstrumentationRunner` tanımlı ama `app/src/test` ve `app/src/androidTest`
dizinleri hiç yok. Test edilmemiş kritik yollar:

- [ ] `sourceKey` tekrar koruması — Room unique index'i gerçekten çalışıyor mu
      (saat kovası dahil)
- [ ] `NotificationService` parse → kayıt akışı
- [ ] `ExpenseRepository.correctCategory` ve mağaza→kategori öğrenmesi
- [ ] Room migration testi (`app/schemas/` şemaları bunun için repoda)
- [ ] `HomeViewModel.toUiState` — REFUND'ın toplamdan düşülmesi

---

## P1 — Yayınlanabilirliği belirleyen işler

### 4. Navigasyon altyapısı kur

`MainActivity.kt:16` doğrudan `HomeScreen()` çağırıyor. `NavHost` yok,
`androidx.navigation:navigation-compose` bağımlılığı `gradle/libs.versions.toml`
içinde hiç tanımlı değil. **5, 6, 8, 9 numaralı maddelerin hepsi buna bağlı.**

- [ ] Karar: NavHost mu, tek Activity + state makinesi mi
- [ ] Bağımlılığı `libs.versions.toml`'a ekle
- [ ] Mevcut `HomeScreen` + `EditExpenseSheet`'i yeni yapıya taşı

### 5. D2 — Manuel harcama girişi

İlk yapılacak ekran bu. Sebebi estetik değil: Play Console bildirim erişimini
reddederse elde yayınlanabilir bir bütçe defteri kalması gerekiyor.

- [ ] Manuel harcama giriş ekranı
- [ ] `HomeScreen.kt:379` `FloatingAddButton` → ekrana bağla (şu an `onClick = {}`)
- [ ] B4 boş durumundaki "+ Elle harcama ekle" → aynı ekrana bağla
- [ ] B3 (izin kapalı) ekranına "elle girilenler" listesi ve soluk toplamı ekle
      — D2'ye bağlı olduğu için bilerek atlanmıştı

### 6. A — Onboarding akışı

- [ ] İzin ekranına yönlendirme metni ve akışı
- [ ] İlk açılışta gösterim, `Prefs.kt` üzerinden "görüldü" bayrağı

### 7. Yayın hazırlığı

- [ ] Release imzalama yapılandırması (`app/build.gradle.kts` içinde
      `signingConfigs` yok)
- [ ] `proguard-rules.pro` gözden geçir — 217 byte; Room, Compose ve `:parser`
      reflection kuralları doğrulanmadı, `isMinifyEnabled = true` açık
- [ ] Uygulama ikonu — hâlâ tek `ic_launcher_foreground.xml` vektörü, marka
      ikonu değil
- [ ] Play Console: bildirim erişimi (`BIND_NOTIFICATION_LISTENER_SERVICE`)
      gerekçelendirme metni + gizlilik beyanı; `PRIVACY.md` ile tutarlı olmalı

---

## P2 — Ürünü tamamlayan ekranlar

### 8. C — Rapor ekranları

- [ ] Rapor ekranı/ekranları
- [ ] `HomeScreen.kt:313` "RAPOR →" → ekrana bağla (şu an ölü)

### 9. F — Ayarlar ekranı

- [ ] Ayarlar ekranı
- [ ] `HomeScreen.kt:229` `SettingsGearButton` → ekrana bağla (şu an `onClick = {}`)

### 10. Aylık değişim rozeti ("↓ %12 TEMMUZ")

Tasarımda var, kodda hiç render edilmiyor. Önceki ayla karşılaştırma gerekiyor;
`HomeViewModel.kt:44` `HomeUiState` yalnızca tek ayın verisini taşıyor.

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
