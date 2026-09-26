-- V32: 신고 대상에 하루 덕담 댓글(DAILY_MESSAGE_COMMENT, 21자)이 추가되어 target_type 길이를 늘린다
ALTER TABLE report MODIFY target_type VARCHAR(30) NOT NULL;
