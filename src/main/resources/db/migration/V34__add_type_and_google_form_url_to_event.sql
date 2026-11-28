-- V34: 이벤트 유형과 구글폼 링크 컬럼 추가 (기존 이벤트는 GENERAL)
ALTER TABLE event
    ADD COLUMN type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN google_form_url VARCHAR(500) NULL;
