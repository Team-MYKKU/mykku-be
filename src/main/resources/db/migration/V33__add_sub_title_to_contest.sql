-- V33: 콘테스트 부제목 추가
ALTER TABLE contest ADD COLUMN sub_title VARCHAR(255) NULL AFTER title;
