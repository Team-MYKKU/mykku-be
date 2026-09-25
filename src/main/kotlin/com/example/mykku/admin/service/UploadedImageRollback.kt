package com.example.mykku.admin.service

import com.example.mykku.image.ImageUploadService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UploadedImageRollback(
    private val imageUploadService: ImageUploadService
) {

    fun <T> runOrDelete(uploadedUrls: List<String>, action: () -> T): T {
        return try {
            action()
        } catch (e: Exception) {
            uploadedUrls.forEach { deleteQuietly(it) }
            throw e
        }
    }

    private fun deleteQuietly(url: String) {
        runCatching { imageUploadService.delete(url) }
            .onFailure { log.warn("업로드한 이미지를 되돌리지 못했습니다: {}", url, it) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(UploadedImageRollback::class.java)
    }
}
