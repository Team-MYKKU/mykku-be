-- V35: 이벤트 출석 기록 테이블 (이벤트·회원·날짜당 1행, 날짜는 앱이 KST로 계산)
CREATE TABLE event_attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    member_id BIGINT NULL,
    attendance_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_event_attendance_event_member_date UNIQUE (event_id, member_id, attendance_date),
    INDEX idx_event_attendance_member_id (member_id),
    CONSTRAINT fk_event_attendance_event FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_attendance_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE SET NULL
);
