package com.example.mykku.image

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class ImageCleanupService(
    private val imageUploadService: ImageUploadService
) : ImageCleanupPort {

    override fun deleteAfterCommit(urls: Collection<String>) {
        val targets = urls.filter { it.isNotBlank() }.distinct()
        if (targets.isEmpty()) return
        if (!isInActualTransaction()) {
            deleteQuietly(targets)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                deleteQuietly(targets)
            }
        })
    }

    private fun isInActualTransaction(): Boolean {
        return TransactionSynchronizationManager.isSynchronizationActive() &&
            TransactionSynchronizationManager.isActualTransactionActive()
    }

    private fun deleteQuietly(urls: List<String>) {
        urls.forEach { url ->
            runCatching { imageUploadService.delete(url) }
                .onFailure { log.warn("이미지 삭제에 실패했습니다: {}", url, it) }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ImageCleanupService::class.java)
    }
}
