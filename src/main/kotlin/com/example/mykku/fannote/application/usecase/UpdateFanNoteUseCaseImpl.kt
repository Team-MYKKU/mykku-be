package com.example.mykku.fannote.application.usecase

import com.example.mykku.fannote.application.dto.FanNoteDetailResult
import com.example.mykku.fannote.application.dto.UpdateFanNoteCommand
import com.example.mykku.fannote.application.port.input.UpdateFanNoteUseCase
import com.example.mykku.fannote.application.port.output.FanNotePageRepository
import com.example.mykku.fannote.application.port.output.FanNoteRepository
import com.example.mykku.fannote.domain.entity.FanNote
import com.example.mykku.fannote.domain.entity.FanNotePage
import com.example.mykku.fannote.domain.vo.FanNoteId
import com.example.mykku.fannote.exception.FanNoteException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UpdateFanNoteUseCaseImpl(
    private val fanNoteRepository: FanNoteRepository,
    private val fanNotePageRepository: FanNotePageRepository
) : UpdateFanNoteUseCase {

    override fun execute(command: UpdateFanNoteCommand): FanNoteDetailResult {
        val fanNoteId = FanNoteId.of(command.fanNoteId)
        val fanNote = fanNoteRepository.findById(fanNoteId)
            ?: throw FanNoteException.fanNoteNotFound()
        if (command.newCoverImageUrl != null && command.removeCoverImage) {
            throw FanNoteException.coverImageConflict()
        }
        val existingUrls = fanNotePageRepository.findByFanNoteIdOrderByPageNumber(fanNoteId).map { it.imageUrl }
        validateKeepUrls(existingUrls, command.keepPageImageUrls)
        val saved = fanNoteRepository.save(updateInfo(fanNote, command))
        val pages = replacePages(saved, command.keepPageImageUrls + command.newPageImageUrls)
        logReplacedImages(fanNote, saved, existingUrls - command.keepPageImageUrls.toSet())
        return FanNoteDetailResult.from(saved, pages)
    }

    private fun validateKeepUrls(existingUrls: List<String>, keepUrls: List<String>) {
        if (keepUrls.size != keepUrls.toSet().size || !existingUrls.containsAll(keepUrls)) {
            throw FanNoteException.invalidKeepImageUrls()
        }
    }

    private fun updateInfo(fanNote: FanNote, command: UpdateFanNoteCommand): FanNote {
        val coverImageUrl = when {
            command.newCoverImageUrl != null -> command.newCoverImageUrl
            command.removeCoverImage -> null
            else -> fanNote.coverImageUrl
        }
        return fanNote.updateInfo(
            title = command.title,
            subtitle = command.subtitle,
            content = command.content,
            productionDate = command.productionDate,
            coverImageUrl = coverImageUrl
        )
    }

    private fun replacePages(fanNote: FanNote, urls: List<String>): List<FanNotePage> {
        fanNotePageRepository.deleteByFanNoteId(fanNote.id)
        val pages = urls.mapIndexed { index, url ->
            FanNotePage.create(fanNoteId = fanNote.id.value, pageNumber = index + 1, imageUrl = url)
        }
        return if (pages.isEmpty()) emptyList() else fanNotePageRepository.saveAll(pages)
    }

    private fun logReplacedImages(before: FanNote, after: FanNote, removedUrls: List<String>) {
        val replacedCover = before.coverImageUrl?.takeIf { it != after.coverImageUrl }
        if (replacedCover == null && removedUrls.isEmpty()) return
        log.info(
            "fan note images replaced id={}, previousCover={}, removedPages={}",
            before.id.value,
            replacedCover,
            removedUrls
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(UpdateFanNoteUseCaseImpl::class.java)
    }
}
