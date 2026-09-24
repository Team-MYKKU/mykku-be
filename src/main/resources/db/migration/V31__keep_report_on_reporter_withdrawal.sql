-- V31: 신고자가 탈퇴해도 신고를 보존한다(reporter_id는 NULL로 남긴다)
ALTER TABLE report DROP FOREIGN KEY fk_report_reporter;

ALTER TABLE report MODIFY reporter_id BIGINT NULL;

ALTER TABLE report
    ADD CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES member (id) ON DELETE SET NULL;
