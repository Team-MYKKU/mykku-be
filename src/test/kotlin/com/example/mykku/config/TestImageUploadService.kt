package com.example.mykku.config

import com.example.mykku.image.ImageUploadService
import com.example.mykku.image.dto.EntityImagesUpdateUploadResult
import com.example.mykku.image.dto.EntityImagesUploadResult
import com.example.mykku.image.dto.FanNoteImagesUploadResult
import com.example.mykku.image.dto.ImageUploadResult
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.atomic.AtomicLong

class TestImageUploadService : ImageUploadService {

    companion object {
        private const val BASE_URL = "https://test-bucket.s3.amazonaws.com"
        private const val IMAGE_WIDTH = 800
        private const val IMAGE_HEIGHT = 600
        private val SEQUENCE = AtomicLong()
    }

    override fun uploadImages(images: List<MultipartFile>, pathPrefix: String): List<ImageUploadResult> {
        return images.map { uploadImage(it, pathPrefix) }
    }

    override fun uploadImage(image: MultipartFile, pathPrefix: String): ImageUploadResult {
        return ImageUploadResult(
            url = testUrl("$pathPrefix/test-image"),
            width = IMAGE_WIDTH,
            height = IMAGE_HEIGHT
        )
    }

    override fun uploadFanNoteImages(
        coverImage: MultipartFile?,
        pageImages: List<MultipartFile>?
    ): FanNoteImagesUploadResult {
        return FanNoteImagesUploadResult(
            coverImageUrl = coverImage?.let { testUrl("cover") },
            pageImageUrls = pageImages?.map { testUrl("page") } ?: emptyList()
        )
    }

    override fun uploadEntityImages(
        thumbnailImage: MultipartFile,
        images: List<MultipartFile>?,
        pathPrefix: String
    ): EntityImagesUploadResult {
        return EntityImagesUploadResult(
            thumbnailUrl = testUrl("$pathPrefix/thumbnail"),
            imageUrls = images?.mapIndexed { index, _ -> testUrl("$pathPrefix/image-$index") } ?: emptyList()
        )
    }

    override fun uploadEntityImagesForUpdate(
        thumbnailImage: MultipartFile?,
        images: List<MultipartFile>?,
        pathPrefix: String
    ): EntityImagesUpdateUploadResult {
        return EntityImagesUpdateUploadResult(
            thumbnailUrl = thumbnailImage?.takeIf { !it.isEmpty }?.let { testUrl("$pathPrefix/thumbnail") },
            imageUrls = images.orEmpty().filterNot { it.isEmpty }.mapIndexed { index, _ ->
                testUrl("$pathPrefix/image-$index")
            }
        )
    }

    override fun uploadFanNoteImagesForUpdate(
        coverImage: MultipartFile?,
        pageImages: List<MultipartFile>?
    ): FanNoteImagesUploadResult {
        return FanNoteImagesUploadResult(
            coverImageUrl = coverImage?.takeIf { !it.isEmpty }?.let { testUrl("cover") },
            pageImageUrls = pageImages.orEmpty().filterNot { it.isEmpty }.map { testUrl("page") }
        )
    }

    override fun delete(url: String) {
    }

    private fun testUrl(label: String): String {
        return "$BASE_URL/$label-${SEQUENCE.incrementAndGet()}.jpg"
    }
}
