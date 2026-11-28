package com.example.mykku.config.fork

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.core.env.Environment

class ForkRedisDatabase(private val environment: Environment, private val database: Int) : BeanFactoryPostProcessor {

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        val beanNames = beanFactory.getBeanNamesForType(RedissonClient::class.java, true, false)
        if (beanNames.isEmpty()) {
            return
        }
        val definition = beanFactory.getBeanDefinition(beanNames.single()) as AbstractBeanDefinition
        definition.setInstanceSupplier { Redisson.create(config()) }
    }

    private fun config(): Config = Config().apply {
        useSingleServer().setAddress(addressOf(environment)).setDatabase(database)
    }

    companion object {
        fun addressOf(environment: Environment): String {
            val ssl = environment.getRequiredProperty("spring.data.redis.ssl.enabled", Boolean::class.java)
            val host = environment.getRequiredProperty("spring.data.redis.host")
            val port = environment.getRequiredProperty("spring.data.redis.port")
            return "${if (ssl) "rediss" else "redis"}://$host:$port"
        }
    }
}
