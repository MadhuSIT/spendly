package com.madhusit.spendly

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.madhusit.spendly.data.local.FoundationEntity
import com.madhusit.spendly.data.local.MIGRATION_1_2
import com.madhusit.spendly.data.local.MIGRATION_2_3
import com.madhusit.spendly.data.local.MIGRATION_3_4
import com.madhusit.spendly.data.local.SpendlyDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FoundationDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: SpendlyDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SpendlyDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun daoReadsWritesAndUpdatesSingleFoundationState() {
        runBlocking {
            val dao = database.foundationDao()
            assertNull(dao.observe().first())
            dao.save(FoundationEntity(message = "first"))
            assertEquals("first", dao.observe().first()?.message)
            dao.save(FoundationEntity(message = "second"))
            assertEquals("second", dao.observe().first()?.message)
        }
    }

    @Test
    fun dataSurvivesDatabaseReopen() {
        runBlocking {
            val dbName = "foundation-reopen-test.db"
            context.deleteDatabase(dbName)
            val first = Room.databaseBuilder(context, SpendlyDatabase::class.java, dbName).build()
            first.foundationDao().save(FoundationEntity(message = "persisted"))
            first.close()
            val second = Room.databaseBuilder(context, SpendlyDatabase::class.java, dbName)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
            assertEquals("persisted", second.foundationDao().observe().first()?.message)
            second.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun versionOneDatabaseMigratesToCurrentSchema() {
        val dbName = "foundation-migration-test.db"
        context.deleteDatabase(dbName)
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(path, null)
        sqlite.execSQL("CREATE TABLE foundation_state (id INTEGER NOT NULL, message TEXT NOT NULL, PRIMARY KEY(id))")
        sqlite.execSQL("INSERT INTO foundation_state (id, message) VALUES (1, 'legacy')")
        sqlite.execSQL("PRAGMA user_version = 1")
        sqlite.close()

        val migrated = Room.databaseBuilder(context, SpendlyDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
        val entity = runBlocking { migrated.foundationDao().observe().first() }
        assertNotNull(entity)
        assertEquals("legacy", entity?.message)
        assertEquals(0L, entity?.updatedAtEpochMillis)
        assertEquals(4, migrated.openHelper.readableDatabase.version)
        migrated.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun versionTwoDatabaseMigratesToCurrentSchema() {
        val dbName = "ledger-migration-test.db"
        context.deleteDatabase(dbName)
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(path, null)
        sqlite.execSQL(
            "CREATE TABLE foundation_state (id INTEGER NOT NULL, message TEXT NOT NULL, updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))"
        )
        sqlite.execSQL("INSERT INTO foundation_state (id, message, updatedAtEpochMillis) VALUES (1, 'v2', 123)")
        sqlite.execSQL("PRAGMA user_version = 2")
        sqlite.close()

        val migrated = Room.databaseBuilder(context, SpendlyDatabase::class.java, dbName)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()

        assertEquals(4, migrated.openHelper.readableDatabase.version)
        val entity = runBlocking { migrated.foundationDao().observe().first() }
        assertEquals("v2", entity?.message)
        assertEquals(123L, entity?.updatedAtEpochMillis)

        val ledgerTables = listOf(
            "financial_entities",
            "ledger_transactions",
            "transaction_relationships",
            "audit_events",
            "processed_sms_events"
        )
        ledgerTables.forEach { table ->
            migrated.openHelper.readableDatabase.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(table)
            ).use { cursor ->
                assertEquals("Missing migrated table: $table", true, cursor.moveToFirst())
            }
        }

        migrated.close()
        context.deleteDatabase(dbName)
    }

}