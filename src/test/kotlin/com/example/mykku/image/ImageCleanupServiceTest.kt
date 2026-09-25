package com.example.mykku.image

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@DisplayName("ImageCleanupService 테스트")
class ImageCleanupServiceTest {

    private val imageUploadService: ImageUploadService = mock()
    private val imageCleanupService = ImageCleanupService(imageUploadService)

    @AfterEach
    fun tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
        TransactionSynchronizationManager.setActualTransactionActive(false)
    }

    @Test
    @DisplayName("트랜잭션이 없으면 바로 지운다")
    fun `트랜잭션 없음 - 즉시 삭제`() {
        imageCleanupService.deleteAfterCommit(listOf("https://a/1.png", "https://a/1.png", " "))

        verify(imageUploadService).delete("https://a/1.png")
    }

    @Test
    @DisplayName("트랜잭션 안에서는 커밋한 뒤에 지운다")
    fun `커밋 - 커밋 후 삭제`() {
        startTransaction()

        imageCleanupService.deleteAfterCommit(listOf("https://a/1.png"))

        verify(imageUploadService, never()).delete(any())
        synchronizations().forEach { it.afterCommit() }
        verify(imageUploadService).delete("https://a/1.png")
    }

    @Test
    @DisplayName("롤백되면 지우지 않는다")
    fun `롤백 - 삭제 안 함`() {
        startTransaction()

        imageCleanupService.deleteAfterCommit(listOf("https://a/1.png"))
        synchronizations().forEach { it.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK) }

        verify(imageUploadService, never()).delete(any())
    }

    @Test
    @DisplayName("하나가 실패해도 나머지를 지운다")
    fun `삭제 실패 - 나머지 계속`() {
        doThrow(IllegalStateException("S3 오류")).whenever(imageUploadService).delete("https://a/1.png")

        imageCleanupService.deleteAfterCommit(listOf("https://a/1.png", "https://a/2.png"))

        verify(imageUploadService).delete("https://a/2.png")
    }

    private fun startTransaction() {
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)
    }

    private fun synchronizations(): List<TransactionSynchronization> {
        return TransactionSynchronizationManager.getSynchronizations()
    }
}
