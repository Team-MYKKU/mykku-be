package com.example.mykku.image

import com.example.mykku.config.S3Properties
import com.example.mykku.image.dto.EntityImagesUpdateUploadResult
import com.example.mykku.image.dto.EntityImagesUploadResult
import com.example.mykku.image.dto.FanNoteImagesUploadResult
import com.example.mykku.image.dto.ImageUploadResult
import com.example.mykku.image.exception.ImageException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.imageio.ImageIO

@Service
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true")
class S3ImageUploadService(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties
) : ImageUploadService {
    companion object {
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        private val FILENAME_TS_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val FILENAME_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    override fun uploadImages(images: List<MultipartFile>, pathPrefix: String): List<ImageUploadResult> {
        return images.map { uploadImage(it, pathPrefix) }
    }

    override fun uploadImage(image: MultipartFile, pathPrefix: String): ImageUploadResult {
        validateImage(image)

        val imageBytes = image.bytes
        val dimensions = extractImageDimensions(imageBytes)
        val key = "$pathPrefix/${generateFileName(image.originalFilename)}"

        putObject(key, image, imageBytes)

        return ImageUploadResult(
            url = buildImageUrl(key),
            width = dimensions.first,
            height = dimensions.second
        )
    }

    private fun putObject(key: String, image: MultipartFile, imageBytes: ByteArray) {
        val contentType = resolveContentType(image, getFileExtension(image.originalFilename))
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucketName)
            .key(key)
            .contentType(contentType)
            .contentLength(imageBytes.size.toLong())
            .build()

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes))
    }

    private fun validateImage(image: MultipartFile) {
        if (image.isEmpty) {
            throw ImageException.imageFileEmpty()
        }

        if (image.size > MAX_FILE_SIZE) {
            throw ImageException.imageFileTooLarge()
        }

        val extension = getFileExtension(image.originalFilename)
        if (extension !in ALLOWED_EXTENSIONS) {
            throw ImageException.imageInvalidFormat()
        }
    }

    private fun generateFileName(originalFilename: String?): String {
        val extension = getFileExtension(originalFilename)
        val timestamp = LocalDateTime.now().format(FILENAME_TS_FORMATTER)
        val uuid = UUID.randomUUID().toString().replace("-", "")
        return "${timestamp}_${uuid}.$extension"
    }

    private fun getFileExtension(filename: String?): String {
        return filename?.substringAfterLast(".", "")?.lowercase() ?: ""
    }

    private fun buildImageUrl(key: String): String {
        return if (!s3Properties.cloudfrontDomain.isNullOrBlank()) {
            "https://${s3Properties.cloudfrontDomain}/$key"
        } else {
            s3Client.utilities()
                .getUrl { it.bucket(s3Properties.bucketName).key(key) }
                .toString()
        }
    }

    private fun extractImageDimensions(imageBytes: ByteArray): Pair<Int, Int> {
        return try {
            val bufferedImage: BufferedImage = ImageIO.read(ByteArrayInputStream(imageBytes))
                ?: throw ImageException.imageUnreadable()

            Pair(bufferedImage.width, bufferedImage.height)
        } catch (e: ImageException) {

            throw e
        } catch (e: Exception) {
            throw ImageException.imageSizeExtractionFailed()
        }
    }

    override fun uploadFanNoteImages(
        coverImage: MultipartFile?,
        pageImages: List<MultipartFile>?
    ): FanNoteImagesUploadResult {
        val folderName = createFanNoteFolder()

        val coverUrl = coverImage?.takeIf { !it.isEmpty }?.let {
            uploadFanNoteImageToS3(it, folderName, 0)
        }

        val pageUrls = pageImages?.filterNot { it.isEmpty }?.mapIndexed { index, file ->
            uploadFanNoteImageToS3(file, folderName, index + 1)
        } ?: emptyList()

        return FanNoteImagesUploadResult(coverUrl, pageUrls)
    }

    private fun createFanNoteFolder(): String {
        val date = LocalDate.now().format(DATE_FORMATTER)
        val uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        return "$date-$uuid"
    }

    private fun uploadFanNoteImageToS3(
        image: MultipartFile,
        folderName: String,
        pageNumber: Int
    ): String {
        validateImage(image)

        val fileName = generateFanNoteFileName(image.originalFilename, pageNumber)
        val key = "fan-note/$folderName/$fileName"

        putObject(key, image, image.bytes)

        return buildImageUrl(key)
    }

    private fun generateFanNoteFileName(originalFilename: String?, pageNumber: Int): String {
        val extension = getFileExtension(originalFilename)
        val date = LocalDate.now().format(FILENAME_DATE_FORMATTER)
        return "$date-$pageNumber.$extension"
    }

    override fun uploadEntityImages(
        thumbnailImage: MultipartFile,
        images: List<MultipartFile>?,
        pathPrefix: String
    ): EntityImagesUploadResult {
        val folderName = createEntityFolder()

        val thumbnailUrl = uploadImageToS3(thumbnailImage, pathPrefix, folderName, "thumbnail")

        val imageUrls = images?.filterNot { it.isEmpty }?.mapIndexed { index, file ->
            uploadImageToS3(file, pathPrefix, folderName, "$index")
        } ?: emptyList()

        return EntityImagesUploadResult(thumbnailUrl, imageUrls)
    }

    private fun createEntityFolder(): String {
        val date = LocalDate.now().format(DATE_FORMATTER)
        val uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        return "$date-$uuid"
    }

    private fun uploadImageToS3(
        image: MultipartFile,
        pathPrefix: String,
        folderName: String,
        fileLabel: String
    ): String {
        validateImage(image)

        val extension = getFileExtension(image.originalFilename)
        val key = "$pathPrefix/$folderName/$fileLabel.$extension"

        putObject(key, image, image.bytes)

        return buildImageUrl(key)
    }

    private fun resolveContentType(image: MultipartFile, extension: String): String {
        return image.contentType ?: when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    override fun uploadEntityImagesForUpdate(
        thumbnailImage: MultipartFile?,
        images: List<MultipartFile>?,
        pathPrefix: String
    ): EntityImagesUpdateUploadResult {
        val folderName = createEntityFolder()
        val uploaded = mutableListOf<String>()
        return deleteUploadedOnFailure(uploaded) {
            val thumbnailUrl = thumbnailImage?.takeIf { !it.isEmpty }
                ?.let { uploadImageToS3(it, pathPrefix, folderName, "thumbnail").also(uploaded::add) }
            val imageUrls = images.orEmpty().filterNot { it.isEmpty }.mapIndexed { index, file ->
                uploadImageToS3(file, pathPrefix, folderName, "$index").also(uploaded::add)
            }
            EntityImagesUpdateUploadResult(thumbnailUrl, imageUrls)
        }
    }

    override fun uploadFanNoteImagesForUpdate(
        coverImage: MultipartFile?,
        pageImages: List<MultipartFile>?
    ): FanNoteImagesUploadResult {
        val folderName = createFanNoteFolder()
        val uploaded = mutableListOf<String>()
        return deleteUploadedOnFailure(uploaded) {
            val coverUrl = coverImage?.takeIf { !it.isEmpty }
                ?.let { uploadFanNoteImageToS3(it, folderName, 0).also(uploaded::add) }
            val pageUrls = pageImages.orEmpty().filterNot { it.isEmpty }.mapIndexed { index, file ->
                uploadFanNoteImageToS3(file, folderName, index + 1).also(uploaded::add)
            }
            FanNoteImagesUploadResult(coverUrl, pageUrls)
        }
    }

    private fun <T> deleteUploadedOnFailure(uploaded: List<String>, action: () -> T): T {
        return try {
            action()
        } catch (e: Exception) {
            uploaded.forEach { url -> runCatching { delete(url) } }
            throw e
        }
    }

    override fun delete(url: String) {
        val key = URI(url).path.removePrefix("/")
        if (key.isBlank()) return
        s3Client.deleteObject { it.bucket(s3Properties.bucketName).key(key) }
    }
}
