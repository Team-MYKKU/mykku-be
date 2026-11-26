package com.example.mykku.util

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.support.TransactionTemplate

class DatabaseCleaner : BeforeEachCallback {

    companion object {
        @Volatile
        private var cachedTableNames: List<String>? = null
    }

    override fun beforeEach(extensionContext: ExtensionContext) {
        val context = SpringExtension.getApplicationContext(extensionContext)
        cleanup(context)
    }

    fun cleanup(context: ApplicationContext) {
        val em = context.getBean(EntityManager::class.java)
        val transactionTemplate = context.getBean(TransactionTemplate::class.java)

        transactionTemplate.execute {
            em.clear()
            truncateTables(em)
            null
        }
    }

    private fun truncateTables(em: EntityManager) {
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate()

        val tableNames = findTableNames(em)
        tableNames.forEach { tableName ->
            em.createNativeQuery("TRUNCATE TABLE $tableName").executeUpdate()
        }

        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate()
    }

    private fun findTableNames(em: EntityManager): List<String> {
        cachedTableNames?.let { return it }

        val tableNameSelectQuery = """
            SELECT TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_TYPE = 'BASE TABLE'
            AND TABLE_NAME NOT LIKE 'flyway_%'
        """.trimIndent()

        val results = em.createNativeQuery(tableNameSelectQuery).resultList
        val names = results.map { it.toString() }
        cachedTableNames = names
        return names
    }
}
