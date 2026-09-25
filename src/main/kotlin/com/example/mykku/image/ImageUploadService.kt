package com.example.mykku.image

import com.example.mykku.image.dto.EntityImagesUpdateUploadResult
import com.example.mykku.image.dto.EntityImagesUploadResult
import com.example.mykku.image.dto.FanNoteImagesUploadResult
import com.example.mykku.image.dto.ImageUploadResult
import org.springframework.web.multipart.MultipartFile

interface ImageUploadService {
    fun uploadImages(images: List<MultipartFile>, pathPrefix: String): List<ImageUploadResult>
    fun uploadImage(image: MultipartFile, pathPrefix: String): ImageUploadResult
    fun uploadFanNoteImages(
        coverImage: MultipartFile?,
        pageImages: List<MultipartFile>?
    ): FanNoteImagesUploadResult
    fun uploadEntityImages(
        thumbnailImage: MultipartFile,
        images: List<MultipartFile>?,
        pathPrefix: String
    ): EntityImagesUploadResult

    fun uploadEntityImagesForUpdate(
        thumbnailImage: MultipartFile?,
        images: List<MultipartFile>?,
        pathPrefix: String
    ): EntityImagesUpdateUploadResult

    fun uploadFanNoteImagesForUpdate(
        coverImage: MultipartFile?,
        pageImages: List<MultipartFile>?
    ): FanNoteImagesUploadResult

    fun delete(url: String)
}
