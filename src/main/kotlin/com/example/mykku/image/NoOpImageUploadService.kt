package com.example.mykku.image

import com.example.mykku.image.dto.EntityImagesUpdateUploadResult
import com.example.mykku.image.dto.EntityImagesUploadResult
import com.example.mykku.image.dto.FanNoteImagesUploadResult
import com.example.mykku.image.dto.ImageUploadResult
import com.example.mykku.image.exception.ImageException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpImageUploadService : ImageUploadService {

    override fun uploadImages(images: List<MultipartFile>, pathPrefix: String): List<ImageUploadResult> {
        throw ImageException.imageUploadServiceUnavailable()
    }

    override fun uploadImage(image: MultipartFile, pathPrefix: String): ImageUploadResult {
        throw ImageException.imageUploadServiceUnavailable()
    }

    override fun uploadFanNoteImages(
        coverImage: MultipartFile?,
        pageImages: List<MultipartFile>?
    ): FanNoteImagesUploadResult {
        throw ImageException.imageUploadServiceUnavailable()
    }

    override fun uploadEntityImages(
        thumbnailImage: MultipartFile,
        images: List<MultipartFile>?,
        pathPrefix: String
    ): EntityImagesUploadResult {
        throw ImageException.imageUploadServiceUnavailable()
    }

    override fun uploadEntityImagesForUpdate(
        thumbnailImage: MultipartFile?,
        images: List<MultipartFile>?,
        pathPrefix: String
    ): EntityImagesUpdateUploadResult {
        requireNoFiles(listOfNotNull(thumbnailImage) + images.orEmpty())
        return EntityImagesUpdateUploadResult(null, emptyList())
    }

    override fun uploadFanNoteImagesForUpdate(
        coverImage: MultipartFile?,
        pageImages: List<MultipartFile>?
    ): FanNoteImagesUploadResult {
        requireNoFiles(listOfNotNull(coverImage) + pageImages.orEmpty())
        return FanNoteImagesUploadResult(null, emptyList())
    }

    override fun delete(url: String) {
    }

    private fun requireNoFiles(files: List<MultipartFile>) {
        if (files.any { !it.isEmpty }) {
            throw ImageException.imageUploadServiceUnavailable()
        }
    }
}
