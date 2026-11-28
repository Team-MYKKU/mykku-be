package com.example.mykku.config

import jakarta.mail.internet.MimeMessage
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

@TestConfiguration
class TestEmailConfig {

    @Bean
    @Primary
    fun javaMailSender(): JavaMailSender {
        val mailSender = Mockito.mock(JavaMailSender::class.java)
        val mimeMessage = Mockito.mock(MimeMessage::class.java)

        // Stub createMimeMessage to return a mock
        Mockito.`when`(mailSender.createMimeMessage()).thenReturn(mimeMessage)

        // Stub the send methods to do nothing
        Mockito.doNothing().`when`(mailSender).send(Mockito.any(SimpleMailMessage::class.java))
        Mockito.doNothing().`when`(mailSender).send(Mockito.any(MimeMessage::class.java))

        return mailSender
    }
}
