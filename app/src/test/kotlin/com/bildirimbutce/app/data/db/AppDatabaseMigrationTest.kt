package com.bildirimbutce.app.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.bildirimbutce.app.inMemoryDb
import com.bildirimbutce.app.testContext
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Surum yukseltme guvenligi.
 *
 * Yayindaki kullanicilarin telefonunda v1 semasi var. Entity'ler degisip
 * surum artmazsa Room acilista "identity hash mismatch" atar; uygulama her
 * acilista coker ve kullanicinin tum harcama gecmisi erisilemez olur.
 * Bu testler o hatayi cihaz yerine CI'da yakalar.
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    private val dbFile: File = testContext().getDatabasePath("migration_v1_test.db")

    @Before
    fun setUp() = removeDbFiles()

    @After
    fun tearDown() = removeDbFiles()

    /**
     * v1'in parmak izi sabit. Bu test kirmizi yaniyorsa entity'lerden biri
     * degismis demektir: @Database surumunu artir, MIGRATIONS'a gecisi ekle ve
     * yeni semayi (2.json) commit et. Sabiti guncelleyip gecmek, yuklu
     * kullanicilarin veritabanini bozar.
     */
    @Test
    fun `v1 semasinin parmak izi degismedi`() {
        assertEquals(V1_IDENTITY_HASH, schema(1).getString("identityHash"))
    }

    /**
     * Surum artirilip semasi commit edilmezse migration testi yazilamaz hale
     * gelir. Surum @Database'den okunamiyor (annotation'in retention'i CLASS),
     * bu yuzden kurulmus veritabanindan aliniyor.
     */
    @Test
    fun `bildirilen surum icin sema dosyasi commit edilmis`() {
        val db = inMemoryDb()
        val declared = try {
            db.openHelper.readableDatabase.version
        } finally {
            db.close()
        }

        assertTrue(
            "surum $declared bildirilmis ama schemas/ altinda $declared.json yok",
            schemaFile(declared).exists()
        )
    }

    /**
     * Asil senaryo: diskte v1 semasiyla kurulmus, icinde veri olan bir
     * veritabani var. Guncel kod onu acabilmeli ve veri yerinde kalmali.
     */
    @Test
    fun `v1 veritabani guncel kodla acilabiliyor ve verisi duruyor`() {
        createV1DatabaseWithData()

        val db = Room.databaseBuilder(testContext(), AppDatabase::class.java, dbFile.name)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()

        try {
            val row = runBlocking { db.expenseDao().getBetween(0L, Long.MAX_VALUE) }.single()

            assertEquals(24_590L, row.amountMinor)
            assertEquals("Migros", row.merchant)
            assertEquals("MARKET", row.category)
            assertEquals("EXPENSE", row.kind)
            assertEquals("v1-kullanicisi", row.sourceKey)

            val rule = runBlocking { db.merchantRuleDao().find("migros") }
            assertEquals("ogrenilen kurallar da tasinmali", "MARKET", rule?.category)
        } finally {
            db.close()
        }
    }

    /** v1 kullanicisinin telefonundaki veritabanini semadan birebir kurar. */
    private fun createV1DatabaseWithData() {
        val database = schema(1)
        dbFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { raw ->
            val entities = database.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                raw.execSQL(entity.getString("createSql").withTable(table))

                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) {
                    raw.execSQL(indices.getJSONObject(j).getString("createSql").withTable(table))
                }
            }

            // room_master_table + identity hash; Room acilista bunu dogruluyor.
            val setup = database.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) raw.execSQL(setup.getString(i))

            raw.execSQL(
                """
                INSERT INTO expenses
                (amountMinor, currency, merchant, category, kind, occurredAt, sourceApp,
                 patternId, confidence, rawText, sourceKey, userEdited)
                VALUES (24590, 'TL', 'Migros', 'MARKET', 'EXPENSE', 1700000000000,
                        'com.garanti.cepsubesi', 'v1', 0.9,
                        'MIGROS isyerinde 245,90 TL harcama', 'v1-kullanicisi', 0)
                """.trimIndent()
            )
            raw.execSQL(
                "INSERT INTO merchant_rules (merchantKey, category) VALUES ('migros', 'MARKET')"
            )

            raw.version = database.getInt("version")
        }
    }

    private fun String.withTable(table: String): String = replace("\${TABLE_NAME}", table)

    private fun schema(version: Int): JSONObject =
        JSONObject(schemaFile(version).readText()).getJSONObject("database")

    /**
     * Semalar kaynak agacinda duruyor; testin calisma dizini modul ya da kok
     * olabilir, ikisi de deneniyor.
     */
    private fun schemaFile(version: Int): File {
        val relative = "schemas/${AppDatabase::class.java.canonicalName}/$version.json"
        val candidates = listOf(File(relative), File("app/$relative"))
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }

    private fun removeDbFiles() {
        listOf("", "-wal", "-shm").forEach { suffix ->
            File(dbFile.parentFile, dbFile.name + suffix).delete()
        }
    }

    private companion object {
        const val V1_IDENTITY_HASH = "8ec90394fc465150e6916919870309a7"
    }
}
