/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package fr.lenfantdo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.lenfantdo.data.local.dao.ActiveTrackingDao
import fr.lenfantdo.data.local.dao.SleepSessionDao
import fr.lenfantdo.data.local.entity.ActiveTrackingEntity
import fr.lenfantdo.data.local.entity.SleepSessionEntity

@Database(
    entities = [
        SleepSessionEntity::class,
        ActiveTrackingEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun activeTrackingDao(): ActiveTrackingDao

    companion object {
        const val DATABASE_NAME = "plees-tracker.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep ADD COLUMN wakes INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep ADD COLUMN health_connect_id TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sleep ADD COLUMN health_connect_version INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sleep ADD COLUMN health_connect_synced_version INTEGER NOT NULL DEFAULT -1")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Sleep_health_connect_id ON sleep(health_connect_id)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS health_connect_deletion (" +
                        "health_connect_id TEXT NOT NULL, start_date INTEGER NOT NULL, " +
                        "PRIMARY KEY(health_connect_id))"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `active_tracking` (
                        `slot` INTEGER NOT NULL,
                        `start_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`slot`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun build(context: Context, inMemory: Boolean = false): AppDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            } else {
                Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            }
            return builder
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                .build()
        }
    }
}
