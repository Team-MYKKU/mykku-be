package com.example.mykku

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class MykkuApplicationTests {

    @Autowired
    private lateinit var applicationContext: ConfigurableApplicationContext

    @Test
    fun contextLoads() {
    }

    @Test
    @DisplayName("test 프로필도 dev처럼 빈 정의 덮어쓰기를 허용하지 않는다")
    fun disallowsBeanDefinitionOverriding() {
        val beanFactory = applicationContext.beanFactory as DefaultListableBeanFactory
        assertThat(beanFactory.isAllowBeanDefinitionOverriding).isFalse()
    }

}
