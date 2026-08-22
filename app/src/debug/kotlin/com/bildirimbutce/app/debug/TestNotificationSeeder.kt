package com.bildirimbutce.app.debug

import android.content.Context
import com.bildirimbutce.app.data.ExpenseRepository
import com.bildirimbutce.app.data.PatternProvider
import com.bildirimbutce.app.widget.BudgetWidget
import com.bildirimbutce.parser.ParseResult

/**
 * Sadece debug derlemesinde derlenir (app/src/debug). Gercek bir banka
 * bildirimi beklemeden NotificationService'in izledigi ayni yolu
 * (parse -> ExpenseRepository.record -> widget refresh) tetikler.
 */
object TestNotificationSeeder {

    private const val SOURCE_APP = "debug.test.seed"

    val SAMPLE_TEXTS = listOf(
        "Garanti BBVA: 1234 kartiniz ile MIGROS isyerinde 245,90 TL tutarinda harcama yapilmistir.",
        "Garanti BBVA: 1234 kartiniz ile SHELL PETROL isyerinde 1.200,00 TL tutarinda harcama yapilmistir.",
        "MIGROS isyerinden 45,00 TL iade yapılmıştır."
    )

    suspend fun seed(context: Context): Int {
        val parser = PatternProvider.parser(context)
        val repository = ExpenseRepository(context)
        var added = 0
        SAMPLE_TEXTS.forEach { text ->
            val postedAt = System.currentTimeMillis()
            when (val result = parser.parse(text)) {
                is ParseResult.Match -> {
                    if (repository.record(result.transaction, SOURCE_APP, postedAt)) {
                        added++
                        BudgetWidget.refresh(context)
                    }
                }
                is ParseResult.Ignored, ParseResult.NoMatch -> Unit
            }
        }
        return added
    }
}
