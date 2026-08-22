package com.bildirimbutce.app.debug

import android.content.Context

/**
 * Release derlemesindeki bos govde. Gercek test verisi ve ayristirma
 * mantigi yalnizca app/src/debug icinde bulunur; bu dosya sadece
 * HomeScreen.kt (main) icin sembolun her varyantta cozulmesini saglar.
 */
object TestNotificationSeeder {
    suspend fun seed(context: Context): Int = 0
}
