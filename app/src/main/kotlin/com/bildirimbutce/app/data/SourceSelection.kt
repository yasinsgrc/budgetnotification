package com.bildirimbutce.app.data

/**
 * "Hangi kaynaklar dinleniyor" kurali.
 *
 * Kural burada duruyor cunku iki tarafin da ayni cevabi vermesi gerekiyor:
 * ayarlar ekrani anahtarlari buna gore ciziyor, NotificationService gelen
 * bildirimi buna gore suzuyor. Iki kopya olsaydi biri degisip digeri kalabilir,
 * kullanici kapattigi bankanin harcamalarini listede gormeye devam ederdi.
 *
 * Depolanan tercih ([com.bildirimbutce.app.util.Prefs.enabledSources]) uc
 * durumdan birindedir: `null` (hic secim yok - hepsi), bos kume (hicbiri),
 * dolu kume (yalnizca icindekiler).
 */
object SourceSelection {

    /** Bu paketten gelen bildirim okunmali mi? */
    fun listensTo(stored: Set<String>?, packageName: String): Boolean =
        stored == null || packageName in stored

    /** Ekranda acik gorunecek paketler. */
    fun enabled(sources: List<NotificationSource>, stored: Set<String>?): Set<String> =
        stored ?: sources.map { it.packageName }.toSet()

    /**
     * Bir anahtar cevrildikten sonraki tercih. Donen `null` "hepsi" demektir.
     *
     * Hepsi acikken tercih hic yazilmiyor: desen setine yarin eklenecek banka
     * da kendiliginden dinlensin. Tam liste saklansaydi yeni banka kapali
     * dogar ve kullanici hic kapatmadigi bir kaynagi kacirirdi.
     */
    fun toggled(
        sources: List<NotificationSource>,
        stored: Set<String>?,
        packageName: String,
        enable: Boolean
    ): Set<String>? {
        val current = enabled(sources, stored)
        val next = if (enable) current + packageName else current - packageName
        return if (next.containsAll(sources.map { it.packageName })) null else next
    }
}
