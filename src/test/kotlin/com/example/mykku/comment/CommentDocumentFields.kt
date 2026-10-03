package com.example.mykku.comment

import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

object CommentDocumentFields {

    fun authorFields(prefix: String): Array<FieldDescriptor> = arrayOf(
        fieldWithPath("$prefix.author").type(JsonFieldType.OBJECT)
            .description("작성자 정보 (항상 존재. 탈퇴 회원은 memberId·profileImage·role이 null, nickname이 '탈퇴한 회원')"),
        fieldWithPath("$prefix.author.memberId").type(JsonFieldType.STRING)
            .description("회원 아이디 문자열 (DB PK 아님, 탈퇴했거나 아이디를 설정하지 않은 회원이면 null)").optional(),
        fieldWithPath("$prefix.author.nickname").type(JsonFieldType.STRING)
            .description("조회 시점의 현재 닉네임 (탈퇴 회원이면 '탈퇴한 회원', 닉네임 미설정 회원이면 빈 문자열)"),
        fieldWithPath("$prefix.author.profileImage").type(JsonFieldType.STRING)
            .description("프로필 이미지 URL (이미지가 없으면 빈 문자열 \"\", 탈퇴 회원이면 null)").optional()
    ) + roleFields("$prefix.author.role")

    private fun roleFields(prefix: String): Array<FieldDescriptor> = arrayOf(
        fieldWithPath(prefix).type(JsonFieldType.OBJECT)
            .description("대표 칭호 (대표 칭호가 없거나 탈퇴 회원이면 null)").optional(),
        fieldWithPath("$prefix.id").type(JsonFieldType.NUMBER).description("칭호 ID").optional(),
        fieldWithPath("$prefix.name").type(JsonFieldType.STRING).description("칭호 이름").optional(),
        fieldWithPath("$prefix.description").type(JsonFieldType.STRING).description("칭호 설명").optional()
    )

    fun replyFields(prefix: String): Array<FieldDescriptor> = arrayOf(
        fieldWithPath("$prefix.replies[].id").type(JsonFieldType.NUMBER).description("답글 ID").optional(),
        fieldWithPath("$prefix.replies[].content").type(JsonFieldType.STRING).description("답글 내용").optional(),
        fieldWithPath("$prefix.replies[].likeCount").type(JsonFieldType.NUMBER).description("답글 좋아요 수").optional(),
        fieldWithPath("$prefix.replies[].isLiked").type(JsonFieldType.BOOLEAN)
            .description("요청한 회원의 답글 좋아요 여부 (비로그인이거나 토큰이 유효하지 않으면 항상 false)").optional(),
        fieldWithPath("$prefix.replies[].createdAt").type(JsonFieldType.STRING)
            .description("답글 최초 작성 일시 (KST, ISO-8601, 오프셋 없음)").optional(),
        fieldWithPath("$prefix.replies[].updatedAt").type(JsonFieldType.STRING)
            .description("답글 마지막 수정 일시 (KST, ISO-8601, 오프셋 없음)").optional()
    ) + optionalAll(authorFields("$prefix.replies[]"))

    fun commentFields(prefix: String, repliesDescription: String): Array<FieldDescriptor> = arrayOf(
        fieldWithPath("$prefix.id").type(JsonFieldType.NUMBER).description("댓글 ID"),
        fieldWithPath("$prefix.content").type(JsonFieldType.STRING).description("댓글 내용"),
        fieldWithPath("$prefix.likeCount").type(JsonFieldType.NUMBER).description("좋아요 수"),
        fieldWithPath("$prefix.isLiked").type(JsonFieldType.BOOLEAN)
            .description("요청한 회원의 좋아요 여부 (비로그인이거나 토큰이 유효하지 않으면 항상 false)"),
        fieldWithPath("$prefix.replies").type(JsonFieldType.ARRAY).description(repliesDescription),
        fieldWithPath("$prefix.replyCount").type(JsonFieldType.NUMBER).description("답글 수 (replies 배열 길이)"),
        fieldWithPath("$prefix.createdAt").type(JsonFieldType.STRING)
            .description("최초 작성 일시 (수정해도 바뀌지 않음, KST, ISO-8601, 오프셋 없음)"),
        fieldWithPath("$prefix.updatedAt").type(JsonFieldType.STRING)
            .description("마지막 수정 일시 (수정한 적 없으면 작성 일시와 같음, KST, ISO-8601, 오프셋 없음)")
    ) + authorFields(prefix) + replyFields(prefix)

    fun pageFields(): Array<FieldDescriptor> = arrayOf(
        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
        fieldWithPath("data").type(JsonFieldType.OBJECT).description("댓글 목록 응답 데이터"),
        fieldWithPath("data.comments").type(JsonFieldType.ARRAY).description("최상위 댓글 목록 (작성 일시 최신순)"),
        fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 최상위 댓글 수 (답글 제외)"),
        fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("최상위 댓글 기준 전체 페이지 수"),
        fieldWithPath("data.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지 번호 (0부터 시작)"),
        fieldWithPath("data.pageSize").type(JsonFieldType.NUMBER).description("페이지 크기"),
        fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지(최상위 댓글 기준) 존재 여부")
    ) + commentFields("data.comments[]", "답글 목록 (페이지네이션 없이 전부 포함, 오래된 순)")

    fun singleFields(dataDescription: String, repliesDescription: String): Array<FieldDescriptor> = arrayOf(
        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
        fieldWithPath("data").type(JsonFieldType.OBJECT).description(dataDescription)
    ) + commentFields("data", repliesDescription)

    private fun optionalAll(descriptors: Array<FieldDescriptor>): Array<FieldDescriptor> =
        descriptors.map { it.optional() }.toTypedArray()
}
