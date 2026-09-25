package com.example.mykku.image.dto

data class EntityImagesUpdateUploadResult(
    val thumbnailUrl: String?,
    val imageUrls: List<String>
) {
    val allUrls: List<String>
        get() = listOfNotNull(thumbnailUrl) + imageUrls
}
