package com.example.mykku.image

import com.example.mykku.config.S3Properties
import com.example.mykku.image.exception.ImageErrorCode
import com.example.mykku.image.exception.ImageException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockMultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@DisplayName("S3ImageUploadService 테스트")
class S3ImageUploadServiceTest {

    private val s3Client: S3Client = mock()
    private val s3Properties = S3Properties(
        region = "ap-northeast-2",
        bucketName = "test-bucket",
        cloudfrontDomain = "cdn.test.com"
    )
    private val s3ImageUploadService = S3ImageUploadService(s3Client, s3Properties)

    @Test
    @DisplayName("게시판 로고는 board-images 경로에 업로드된다")
    fun `단일 업로드 - board-images 프리픽스`() {
        val result = s3ImageUploadService.uploadImage(pngFile("logo.png"), "board-images")

        val key = capturedKeys().single()
        assertThat(key).startsWith("board-images/")
        assertThat(key).endsWith(".png")
        assertThat(result.url).isEqualTo("https://cdn.test.com/$key")
    }

    @Test
    @DisplayName("피드 이미지는 feed-images 경로에 업로드된다")
    fun `다중 업로드 - feed-images 프리픽스`() {
        s3ImageUploadService.uploadImages(
            listOf(pngFile("first.png"), pngFile("second.png")),
            "feed-images"
        )

        assertThat(capturedKeys()).hasSize(2).allSatisfy {
            assertThat(it).startsWith("feed-images/")
        }
    }

    @Test
    @DisplayName("덕질노트 생성 업로드는 빈 커버와 빈 페이지를 건너뛰고 남은 페이지에 1부터 번호를 매긴다")
    fun `덕질노트 생성 업로드 - 빈 파트 건너뜀`() {
        val result = s3ImageUploadService.uploadFanNoteImages(
            emptyFile("cover.png"),
            listOf(pngFile("1.png"), emptyFile("2.png"), pngFile("3.png"))
        )

        val keys = capturedKeys()
        assertThat(result.coverImageUrl).isNull()
        assertThat(keys).hasSize(2)
        assertThat(keys[0]).startsWith("fan-note/").endsWith("-1.png")
        assertThat(keys[1]).startsWith("fan-note/").endsWith("-2.png")
        assertThat(result.pageImageUrls).containsExactlyElementsOf(keys.map { "https://cdn.test.com/$it" })
    }

    @Test
    @DisplayName("엔티티 생성 업로드는 빈 이미지를 건너뛰고 남은 이미지에 0부터 번호를 매긴다")
    fun `엔티티 생성 업로드 - 빈 이미지 건너뜀`() {
        val result = s3ImageUploadService.uploadEntityImages(
            pngFile("thumbnail.png"),
            listOf(emptyFile("a.png"), pngFile("b.png")),
            "event-images"
        )

        val keys = capturedKeys()
        assertThat(keys).hasSize(2)
        assertThat(keys[0]).startsWith("event-images/").endsWith("/thumbnail.png")
        assertThat(keys[1]).startsWith("event-images/").endsWith("/0.png")
        assertThat(result.imageUrls).containsExactly("https://cdn.test.com/${keys[1]}")
    }

    @Test
    @DisplayName("엔티티 생성 업로드의 빈 썸네일과 단건 업로드의 빈 파일은 거르지 않고 IM101로 거부한다")
    fun `빈 썸네일과 빈 단건 파일 - IM101`() {
        val thumbnailError = assertThrows<ImageException> {
            s3ImageUploadService.uploadEntityImages(emptyFile("t.png"), listOf(pngFile("a.png")), "contest-images")
        }
        val singleError = assertThrows<ImageException> {
            s3ImageUploadService.uploadImage(emptyFile("logo.png"), "board-images")
        }

        assertThat(thumbnailError.errorCode).isEqualTo(ImageErrorCode.IMAGE_FILE_EMPTY)
        assertThat(singleError.errorCode).isEqualTo(ImageErrorCode.IMAGE_FILE_EMPTY)
        verify(s3Client, never()).putObject(any<PutObjectRequest>(), any<RequestBody>())
    }

    @Test
    @DisplayName("단건 업로드는 ImageIO가 읽지 못한 바이트면 IM104, 읽다가 예외가 나면 IM401로 거부한다")
    fun `단건 업로드 디코딩 실패 - IM104와 IM401`() {
        val unreadable = assertThrows<ImageException> {
            s3ImageUploadService.uploadImage(fileOf("a.png", "fake-image".toByteArray()), "feed-images")
        }
        val broken = assertThrows<ImageException> {
            s3ImageUploadService.uploadImage(fileOf("b.png", truncatedPng()), "feed-images")
        }

        assertThat(unreadable.errorCode).isEqualTo(ImageErrorCode.IMAGE_UNREADABLE)
        assertThat(broken.errorCode).isEqualTo(ImageErrorCode.IMAGE_SIZE_EXTRACTION_FAILED)
        verify(s3Client, never()).putObject(any<PutObjectRequest>(), any<RequestBody>())
    }

    private fun capturedKeys(): List<String> {
        val captor = argumentCaptor<PutObjectRequest>()
        verify(s3Client, atLeastOnce()).putObject(captor.capture(), any<RequestBody>())
        return captor.allValues.map { it.key() }
    }

    private fun pngFile(fileName: String): MockMultipartFile = fileOf(fileName, pngBytes())

    private fun emptyFile(fileName: String): MockMultipartFile = fileOf(fileName, ByteArray(0))

    private fun fileOf(fileName: String, bytes: ByteArray): MockMultipartFile =
        MockMultipartFile("image", fileName, "image/png", bytes)

    private fun truncatedPng(): ByteArray = pngBytes().let { it.copyOf(it.size / 2) }

    private fun pngBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB), "png", output)
        return output.toByteArray()
    }
}
