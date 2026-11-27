package com.example.mykku.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

@DisplayName("TestImageUploadService 업로드 가짜")
class TestImageUploadServiceTest {

    private val service = TestImageUploadService()

    @Test
    fun `업로드 URL은 접두사-번호 jpg 형식이고 크기는 800x600이다`() {
        val result = service.uploadImage(file("a.png"), "feed-images")

        assertThat(result.url).matches("https://test-bucket\\.s3\\.amazonaws\\.com/feed-images/test-image-\\d+\\.jpg")
        assertThat(result.width).isEqualTo(800)
        assertThat(result.height).isEqualTo(600)
    }

    @Test
    fun `업로드한 URL과 삭제한 URL을 호출 순서대로 기록하고 reset으로 비운다`() {
        val first = service.uploadImage(file("a.png"), "board-images").url
        val second = service.uploadImages(listOf(file("b.png")), "feed-images").single().url
        service.delete(second)
        service.delete(OTHER_URL)

        assertThat(service.uploadedUrls).containsExactly(first, second)
        assertThat(service.deletedUrls).containsExactly(second, OTHER_URL)
        service.reset()
        assertThat(service.uploadedUrls).isEmpty()
        assertThat(service.deletedUrls).isEmpty()
    }

    @Test
    fun `덕질노트 생성 업로드는 실제처럼 빈 커버와 빈 페이지를 거른다`() {
        val result = service.uploadFanNoteImages(empty(), listOf(file("1.png"), empty(), file("2.png")))

        assertThat(result.coverImageUrl).isNull()
        assertThat(result.pageImageUrls).hasSize(2).allSatisfy { assertThat(it).matches(".*/page-\\d+\\.jpg") }
        assertThat(service.uploadedUrls).containsExactlyElementsOf(result.pageImageUrls)
    }

    @Test
    fun `엔티티 생성 업로드는 실제처럼 빈 이미지만 거르고 남은 이미지 번호를 0부터 매긴다`() {
        val result = service.uploadEntityImages(file("t.png"), listOf(empty(), file("1.png")), "event-images")

        assertThat(result.thumbnailUrl).matches(".*/event-images/thumbnail-\\d+\\.jpg")
        assertThat(result.imageUrls).hasSize(1)
        assertThat(result.imageUrls.single()).matches(".*/event-images/image-0-\\d+\\.jpg")
    }

    @Test
    fun `실제가 IM101을 내는 썸네일과 단건 업로드는 거르기로 바꾸지 않아 빈 파일도 URL을 준다`() {
        val thumbnailUrl = service.uploadEntityImages(empty(), null, "contest-images").thumbnailUrl
        val singleUrl = service.uploadImage(empty(), "profile-images").url

        assertThat(thumbnailUrl).matches(".*/contest-images/thumbnail-\\d+\\.jpg")
        assertThat(singleUrl).matches(".*/profile-images/test-image-\\d+\\.jpg")
    }

    private fun file(name: String) = MockMultipartFile("image", name, "image/png", "fake-image".toByteArray())

    private fun empty() = MockMultipartFile("image", "empty.png", "image/png", ByteArray(0))

    companion object {
        private const val OTHER_URL = "https://example.com/other.jpg"
    }
}
