package com.example.mykku.fannote.application.dto

import java.time.LocalDate

data class CreateFanNoteCommand(
    val title: String,
    val subtitle: String?,
    val content: String?,
    val productionDate: LocalDate,
    val coverImageUrl: String?,
    val pageImageUrls: List<String>
)

data class UpdateFanNoteCommand(
    val fanNoteId: Long,
    val title: String,
    val subtitle: String?,
    val content: String?,
    val productionDate: LocalDate,
    val newCoverImageUrl: String?,
    val removeCoverImage: Boolean,
    val keepPageImageUrls: List<String>,
    val newPageImageUrls: List<String>
)
