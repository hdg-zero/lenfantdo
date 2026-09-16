/*
 * Copyright 2026 L'enfant do Contributors
 *
 * SPDX-License-Identifier: MIT
 */

package com.hdgdev.lenfantdo.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class MigrationTest {

    @Test
    fun migration6to7_executesIndexCreationAndTableCleanup() {
        val executedSql = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_6_7.migrate(dbProxy)

        assertTrue(
            "Expected index on start_date",
            executedSql.any { it.contains("index_sleep_start_date") && it.contains("start_date") }
        )
        assertTrue(
            "Expected index on stop_date",
            executedSql.any { it.contains("index_sleep_stop_date") && it.contains("stop_date") }
        )
        assertTrue(
            "Expected drop of health_connect_deletion table",
            executedSql.any { it.contains("DROP TABLE IF EXISTS") && it.contains("health_connect_deletion") }
        )
    }
}
