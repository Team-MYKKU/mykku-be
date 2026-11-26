package com.example.mykku.util

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.junit.jupiter.SpringExtension

class ClassLevelDatabaseCleaner : BeforeAllCallback {

    override fun beforeAll(extensionContext: ExtensionContext) {
        if (extensionContext.requiredTestClass.isAnnotationPresent(Nested::class.java)) return
        val context = SpringExtension.getApplicationContext(extensionContext)
        DatabaseCleaner().cleanup(context)
    }
}
