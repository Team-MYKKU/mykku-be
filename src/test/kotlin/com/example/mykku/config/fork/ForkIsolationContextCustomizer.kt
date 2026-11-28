package com.example.mykku.config.fork

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory
import org.springframework.test.context.MergedContextConfiguration

class ForkIsolationContextCustomizerFactory : ContextCustomizerFactory {

    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: List<ContextConfigurationAttributes>
    ): ContextCustomizer = ForkIsolationContextCustomizer
}

object ForkIsolationContextCustomizer : ContextCustomizer {

    const val PROPERTY_SOURCE_NAME = "testForkIsolation"
    const val HIKARI_MINIMUM_IDLE = 2
    const val HIKARI_IDLE_TIMEOUT_MILLIS = 10_000L

    override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
        if (!MysqlUrl.isMysql(context.environment.getProperty("spring.datasource.url"))) {
            return
        }
        val fork = TestFork.acquire(context.environment)
        context.environment.propertySources.addFirst(MapPropertySource(PROPERTY_SOURCE_NAME, propertiesOf(fork)))
        context.addBeanFactoryPostProcessor(ForkRedisDatabase(context.environment, fork.slot))
    }

    private fun propertiesOf(fork: ForkDatabase): Map<String, Any> = mapOf(
        "spring.datasource.url" to fork.url,
        "spring.datasource.hikari.minimum-idle" to HIKARI_MINIMUM_IDLE,
        "spring.datasource.hikari.idle-timeout" to HIKARI_IDLE_TIMEOUT_MILLIS,
        "spring.data.redis.database" to fork.slot
    )
}
