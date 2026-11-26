INSERT INTO role (id, name, description, created_at, updated_at)
VALUES (1, '스냅샷 칭호', '계약 스냅샷 칭호 설명', '2001-01-01 00:00:00.123456', '2001-01-01 00:00:00.123456'),
       (2, '첫 만남', NULL, '2001-01-01 00:00:00.120000', '2001-01-01 00:00:00.120000');

INSERT INTO member (id, member_id, nickname, profile_image, provider, social_id, email, password, email_verified,
                    role_id, created_at, updated_at)
VALUES (1, 'snapgoogle', '구글회원', 'https://cdn.example.com/profile/1.png', 'GOOGLE', 'google-snap-1',
        'snap-google@example.com', NULL, 0, 1, '2001-01-02 03:04:05.123456', '2001-01-03 03:04:05.120000'),
       (2, 'snapemail', '이메일회원', '', 'EMAIL', NULL, 'snap-email@example.com',
        '$2a$10$iuDwvN.Y4Ngg3oKm72701uqLfy.ghUmzUi34OJ8AEHAKagxGOxieS', 1, NULL,
        '2001-01-02 03:04:06.120000', '2001-01-02 03:04:06.120000'),
       (4, NULL, NULL, 'https://cdn.example.com/profile/4.png', 'KAKAO', 'kakao-snap-4', 'snap-kakao@example.com',
        NULL, 0, NULL, '2001-01-02 03:04:08.000000', '2001-01-02 03:04:08.000000'),
       (5, 'snapblocked', '차단대상', 'https://cdn.example.com/profile/5.png', 'NAVER', 'naver-snap-5',
        'snap-naver@example.com', NULL, 0, 2, '2001-01-02 03:04:09.123456', '2001-01-02 03:04:09.123456'),
       (6, 'snapleave', '탈퇴예정', '', 'EMAIL', NULL, 'snap-leave@example.com',
        '$2a$10$iuDwvN.Y4Ngg3oKm72701uqLfy.ghUmzUi34OJ8AEHAKagxGOxieS', 1, NULL,
        '2001-01-02 03:04:10.120000', '2001-01-02 03:04:10.120000');

INSERT INTO board (id, title, logo, created_at, updated_at)
VALUES (3, '가람', 'https://cdn.example.com/board/3.png', '2001-01-05 00:00:00.000000', '2001-01-05 00:00:00.000000');
INSERT INTO board (id, title, logo, created_at, updated_at)
VALUES (1, '다솜', 'https://cdn.example.com/board/1.png', '2001-01-03 00:00:00.123456', '2001-01-03 00:00:00.123456');
INSERT INTO board (id, title, logo, created_at, updated_at)
VALUES (2, '나무', 'https://cdn.example.com/board/2.png', '2001-01-04 00:00:00.120000', '2001-01-04 00:00:00.120000');

INSERT INTO feed (id, title, content, like_count, comment_count, board_id, member_id, created_at, updated_at)
VALUES (1, '첫 피드', '스냅샷 본문 하나', 7, 9, 1, 1, '2001-03-01 10:00:00.123456', '2001-03-01 11:00:00.120000'),
       (2, '탈퇴한 작성자 글', '작성자가 탈퇴했다', 0, 0, 1, NULL, '2001-03-02 10:00:00.000000',
        '2001-03-02 10:00:00.000000'),
       (3, '차단 대상 글', '스냅샷 차단 대상 본문', 0, 0, 1, 5, '2001-03-03 10:00:00.120000',
        '2001-03-03 10:00:00.120000'),
       (4, '키워드 스포 포함', '스냅샷 스포 본문', 0, 0, 1, 2, '2001-03-04 10:00:00.123456', '2001-03-04 10:00:00.123456'),
       (5, '프로필 미완료 글', '프로필을 아직 안 정했다', 0, 0, 1, 4, '2001-03-05 10:00:00.000000',
        '2001-03-05 10:00:00.000000'),
       (6, '인기 일등', '인기 피드 본문 하나', 0, 0, 2, 1, '2001-04-01 10:00:00.123456', '2001-04-01 10:00:00.123456'),
       (7, '인기 셋째', '인기 피드 본문 셋', 0, 0, 2, 2, '2001-04-02 10:00:00.000000', '2001-04-02 10:00:00.000000'),
       (8, '인기 둘째', '차단 대상의 인기 피드', 0, 0, 2, 5, '2001-04-03 10:00:00.120000', '2001-04-03 10:00:00.120000'),
       (9, '지난 인기글', '스냅샷 지난 인기 본문', 0, 0, 2, 1, '2001-04-04 10:00:00.000000',
        '2001-04-04 10:00:00.000000'),
       (10, '가람 스냅샷 글', '스냅샷 세번째 게시판', 0, 0, 3, 2, '2001-04-05 10:00:00.120000',
        '2001-04-05 10:00:00.120000');

INSERT INTO feed_image (id, url, width, height, feed_id, created_at, updated_at)
VALUES (1, 'https://cdn.example.com/feed/1-a.jpg', 800, 600, 1, '2001-03-01 10:00:00.123456',
        '2001-03-01 10:00:00.123456'),
       (2, 'https://cdn.example.com/feed/1-b.jpg', 1080, 1350, 1, '2001-03-01 10:00:00.123456',
        '2001-03-01 10:00:00.123456'),
       (3, 'https://cdn.example.com/feed/4-a.jpg', 640, 480, 4, '2001-03-04 10:00:00.123456',
        '2001-03-04 10:00:00.123456'),
       (4, 'https://cdn.example.com/feed/10-a.jpg', 300, 300, 10, '2001-04-05 10:00:00.120000',
        '2001-04-05 10:00:00.120000');

INSERT INTO feed_tag (id, feed_id, title, created_at, updated_at)
VALUES (1, 1, '덕질', '2001-03-01 10:00:00.123456', '2001-03-01 10:00:00.123456'),
       (2, 1, '공모태그', '2001-03-01 10:00:00.123456', '2001-03-01 10:00:00.123456'),
       (3, 5, '일상', '2001-03-05 10:00:00.000000', '2001-03-05 10:00:00.000000');

INSERT INTO feed_comment (id, content, like_count, feed_id, parent_comment_id, member_id, created_at, updated_at)
VALUES (1, '첫 댓글', 0, 1, NULL, 5, '2001-03-01 12:00:00.123456', '2001-03-01 12:00:00.123456'),
       (2, '이메일회원 댓글', 5, 1, NULL, 2, '2001-03-01 12:10:00.120000', '2001-03-01 12:15:00.000000'),
       (3, '답글입니다', 0, 1, 2, 1, '2001-03-01 12:20:00.000000', '2001-03-01 12:20:00.000000'),
       (4, '탈퇴한 회원 댓글', 0, 1, NULL, NULL, '2001-03-01 12:30:00.123456', '2001-03-01 12:30:00.123456'),
       (5, '프로필 미완료 댓글', 0, 1, NULL, 4, '2001-03-01 12:40:00.000000', '2001-03-01 12:40:00.000000'),
       (6, '차단 대상 답글', 0, 1, 2, 5, '2001-03-01 12:50:00.120000', '2001-03-01 12:50:00.120000'),
       (7, '탈퇴 회원의 첫 댓글', 0, 5, NULL, NULL, '2001-03-05 12:00:00.000000', '2001-03-05 12:00:00.000000');

INSERT INTO like_feed (id, member_id, feed_id, created_at, updated_at)
VALUES (1, 2, 1, '2001-03-01 13:00:00.123456', '2001-03-01 13:00:00.123456'),
       (2, 4, 1, '2001-03-01 13:10:00.000000', '2001-03-01 13:10:00.000000'),
       (3, 2, 6, '2001-04-01 11:00:00.000000', '2001-04-01 11:00:00.000000'),
       (4, 4, 6, '2001-04-01 11:00:00.000000', '2001-04-01 11:00:00.000000'),
       (5, 5, 6, '2001-04-01 11:00:00.000000', '2001-04-01 11:00:00.000000'),
       (6, 4, 7, '2001-04-02 11:00:00.000000', '2001-04-02 11:00:00.000000'),
       (7, 2, 8, '2001-04-03 11:00:00.000000', '2001-04-03 11:00:00.000000'),
       (8, 4, 8, '2001-04-03 11:00:00.000000', '2001-04-03 11:00:00.000000'),
       (9, 1, 9, '2001-04-04 11:00:00.000000', '2001-04-04 11:00:00.000000'),
       (10, 2, 9, '2001-04-04 11:00:00.000000', '2001-04-04 11:00:00.000000'),
       (11, 4, 9, '2001-04-04 11:00:00.000000', '2001-04-04 11:00:00.000000'),
       (12, 5, 9, '2001-04-04 11:00:00.000000', '2001-04-04 11:00:00.000000'),
       (13, 6, 9, '2001-04-04 11:00:00.000000', '2001-04-04 11:00:00.000000');

INSERT INTO like_feed_comment (id, member_id, feed_comment_id, created_at, updated_at)
VALUES (1, 1, 2, '2001-03-01 14:00:00.123456', '2001-03-01 14:00:00.123456'),
       (2, 4, 2, '2001-03-01 14:10:00.000000', '2001-03-01 14:10:00.000000'),
       (3, 2, 4, '2001-03-01 14:20:00.120000', '2001-03-01 14:20:00.120000');

INSERT INTO member_block (id, blocker_id, blocked_id, created_at, updated_at)
VALUES (1, 1, 5, '2001-02-10 09:00:00.120000', '2001-02-10 09:00:00.120000');

INSERT INTO keyword_block (id, member_id, keyword, created_at, updated_at)
VALUES (1, 1, '스포', '2001-02-10 10:00:00.123456', '2001-02-10 10:00:00.123456');

INSERT INTO like_board (id, member_id, board_id, created_at, updated_at)
VALUES (1, 1, 1, '2001-02-11 09:00:00.123456', '2001-02-11 09:00:00.123456'),
       (2, 1, 3, '2001-02-12 09:00:00.000000', '2001-02-12 09:00:00.000000');

INSERT INTO member_role (id, member_id, role_id, created_at, updated_at, checked_at)
VALUES (1, 1, 1, '2001-02-05 10:00:00.123456', '2001-02-05 10:00:00.123456', '2001-02-05 11:00:00.000000'),
       (2, 1, 2, '2001-02-06 10:00:00.000000', '2001-02-06 10:00:00.000000', NULL),
       (3, 5, 2, '2001-02-07 10:00:00.120000', '2001-02-07 10:00:00.120000', '2001-02-07 11:00:00.120000');

INSERT INTO contest (id, title, sub_title, description, expired_at, created_at, updated_at, status, started_at,
                     thumbnail_url)
VALUES (1, '지난 공모전', '지난 공모전 부제', '지난 공모전 설명', '2001-01-31 23:59:59.000000',
        '2000-11-01 09:00:00.123456', '2001-02-01 09:00:00.120000', 'WINNER_SELECTED', '2000-12-01 00:00:00.000000',
        'https://cdn.example.com/contest/1.jpg'),
       (2, '진행 공모전', NULL, NULL, '2099-12-31 23:59:59.000000', '2000-11-02 09:00:00.120000',
        '2000-11-02 09:00:00.120000', 'ACTIVE', '2001-01-01 00:00:00.000000', 'https://cdn.example.com/contest/2.jpg'),
       (3, '예정 공모전', '예정 공모전 부제', '곧 시작하는 공모전', '2098-12-31 23:59:59.000000',
        '2000-11-03 09:00:00.000000', '2000-11-03 09:00:00.000000', 'ACTIVE', '2098-01-01 00:00:00.000000',
        'https://cdn.example.com/contest/3.jpg');

INSERT INTO contest_tag (id, title, contest_id, created_at, updated_at)
VALUES (1, '공모태그', 2, '2000-11-02 09:00:00.120000', '2000-11-02 09:00:00.120000'),
       (2, '지난태그', 1, '2000-11-01 09:00:00.123456', '2000-11-01 09:00:00.123456');

INSERT INTO contest_image (id, url, order_index, contest_id, created_at, updated_at)
VALUES (1, 'https://cdn.example.com/contest/2-b.jpg', 2, 2, '2000-11-02 09:00:00.120000', '2000-11-02 09:00:00.120000'),
       (2, 'https://cdn.example.com/contest/2-a.jpg', 1, 2, '2000-11-02 09:00:00.120000', '2000-11-02 09:00:00.120000');

INSERT INTO contest_participation (id, member_id, contest_id, feed_id, created_at, updated_at)
VALUES (1, 1, 1, 1, '2001-01-10 09:00:00.123456', '2001-01-10 09:00:00.123456'),
       (2, NULL, 1, 2, '2001-01-11 09:00:00.000000', '2001-01-11 09:00:00.000000'),
       (3, 2, 1, 4, '2001-01-12 09:00:00.120000', '2001-01-12 09:00:00.120000'),
       (4, 1, 2, 1, '2001-03-01 10:00:00.123456', '2001-03-01 10:00:00.123456');

INSERT INTO contest_winner (id, winner_rank, award_title, description, acceptance_speech, contest_id, participation_id,
                            created_at, updated_at)
VALUES (1, 1, '대상', '축하합니다', '감사합니다', 1, 1, '2001-02-01 09:00:00.123456', '2001-02-01 09:00:00.123456'),
       (2, 2, NULL, '', '', 1, 2, '2001-02-01 09:00:00.000000', '2001-02-01 09:00:00.000000');

INSERT INTO contest_winner_announcement (id, contest_id, title, content, announced_at, created_at, updated_at)
VALUES (1, 1, '지난 공모전 수상자 발표', '수상을 축하합니다', '2001-02-01', '2001-02-01 09:00:00.120000',
        '2001-02-01 09:00:00.120000');

INSERT INTO event (id, title, sub_title, description, expired_at, created_at, updated_at, status, started_at,
                   thumbnail_url)
VALUES (1, '지난 이벤트', '지난 이벤트 부제', '지난 이벤트 설명', '2001-01-31 23:59:59.000000',
        '2000-11-01 10:00:00.123456', '2001-02-01 10:00:00.120000', 'WINNER_SELECTED', '2000-12-01 00:00:00.000000',
        'https://cdn.example.com/event/1.jpg'),
       (2, '진행 이벤트', NULL, NULL, '2099-12-31 23:59:59.000000', '2000-11-02 10:00:00.120000',
        '2000-11-02 10:00:00.120000', 'ACTIVE', '2001-01-01 00:00:00.000000', 'https://cdn.example.com/event/2.jpg'),
       (3, '예정 이벤트', '예정 이벤트 부제', '곧 시작하는 이벤트', '2098-12-31 23:59:59.000000',
        '2000-11-03 10:00:00.000000', '2000-11-03 10:00:00.000000', 'ACTIVE', '2098-01-01 00:00:00.000000',
        'https://cdn.example.com/event/3.jpg');

INSERT INTO event_image (id, url, order_index, event_id, created_at, updated_at)
VALUES (1, 'https://cdn.example.com/event/1-b.jpg', 2, 1, '2000-11-01 10:00:00.123456', '2000-11-01 10:00:00.123456'),
       (2, 'https://cdn.example.com/event/1-a.jpg', 1, 1, '2000-11-01 10:00:00.123456', '2000-11-01 10:00:00.123456');

INSERT INTO event_participation (id, member_id, event_id, created_at, updated_at)
VALUES (1, 1, 1, '2001-01-10 10:00:00.123456', '2001-01-10 10:00:00.123456'),
       (2, 2, 1, '2001-01-11 10:00:00.000000', '2001-01-11 10:00:00.000000'),
       (3, NULL, 1, '2001-01-12 10:00:00.120000', '2001-01-12 10:00:00.120000'),
       (4, 1, 2, '2001-03-01 10:00:00.000000', '2001-03-01 10:00:00.000000');

INSERT INTO event_winner (id, event_id, participation_id, created_at, updated_at)
VALUES (1, 1, 1, '2001-02-02 10:00:00.123456', '2001-02-02 10:00:00.123456'),
       (2, 1, 3, '2001-02-02 10:00:00.000000', '2001-02-02 10:00:00.000000');

INSERT INTO event_winner_announcement (id, event_id, title, content, announced_at, created_at, updated_at)
VALUES (1, 1, '지난 이벤트 당첨자 발표', '당첨을 축하합니다', '2001-02-02', '2001-02-02 10:00:00.120000',
        '2001-02-02 10:00:00.120000');

INSERT INTO daily_message (id, title, content, date, created_at, updated_at)
VALUES (1, '첫 덕담', '오늘도 덕질 화이팅', '2001-01-01', '2000-12-31 09:00:00.123456', '2000-12-31 09:00:00.123456'),
       (2, '두번째 덕담', '내일도 덕질 화이팅', '2001-01-02', '2001-01-01 09:00:00.120000', '2001-01-01 09:00:00.120000'),
       (3, '미래 덕담', '먼 미래의 덕담', '2098-01-01', '2001-01-02 09:00:00.000000', '2001-01-02 09:00:00.000000');

INSERT INTO daily_message_comment (id, content, like_count, daily_message_id, parent_comment_id, member_id,
                                   created_at, updated_at)
VALUES (1, '덕담 댓글', 3, 1, NULL, 2, '2001-01-01 10:00:00.123456', '2001-01-01 10:05:00.000000'),
       (2, '덕담 답글', 0, 1, 1, 1, '2001-01-01 10:10:00.000000', '2001-01-01 10:10:00.000000'),
       (3, '탈퇴한 회원 덕담 댓글', 0, 1, NULL, NULL, '2001-01-01 10:20:00.120000', '2001-01-01 10:20:00.120000'),
       (4, '차단 대상 덕담 댓글', 0, 1, NULL, 5, '2001-01-01 10:30:00.123456', '2001-01-01 10:30:00.123456'),
       (5, '프로필 미완료 덕담 댓글', 0, 1, NULL, 4, '2001-01-01 10:40:00.000000', '2001-01-01 10:40:00.000000');

INSERT INTO like_daily_message_comment (id, member_id, daily_message_comment_id, created_at, updated_at)
VALUES (1, 1, 1, '2001-01-01 11:00:00.123456', '2001-01-01 11:00:00.123456'),
       (2, 4, 1, '2001-01-01 11:10:00.000000', '2001-01-01 11:10:00.000000');

INSERT INTO fan_note (id, title, subtitle, content, production_date, cover_image_url, created_at, updated_at)
VALUES (1, '덕질노트 하나', '첫 부제', '덕질노트 본문', '2001-01-10', 'https://cdn.example.com/fannote/1.jpg',
        '2001-01-10 09:00:00.123456', '2001-01-10 09:00:00.123456'),
       (2, '덕질노트 둘', NULL, NULL, '2001-01-20', NULL, '2001-01-20 09:00:00.000000', '2001-01-20 09:00:00.000000');

INSERT INTO fan_note_page (id, page_number, image_url, fan_note_id, created_at, updated_at)
VALUES (1, 2, 'https://cdn.example.com/fannote/1-2.jpg', 1, '2001-01-10 09:00:00.123456', '2001-01-10 09:00:00.123456'),
       (2, 1, 'https://cdn.example.com/fannote/1-1.jpg', 1, '2001-01-10 09:00:00.123456', '2001-01-10 09:00:00.123456');

INSERT INTO notification (id, type, sender_id, receiver_id, content, is_read, related_resource_id,
                          related_resource_type, created_at, updated_at)
VALUES (1, 'FEED_LIKE', 2, 1, '이메일회원님이 회원님의 피드를 좋아합니다', 0, 1, 'FEED', '2001-03-06 10:00:00.123456',
        '2001-03-06 10:00:00.123456'),
       (2, 'FEED_COMMENT', NULL, 1, '탈퇴한 회원님이 댓글을 남겼습니다', 1, 1, 'FEED', '2001-03-06 11:00:00.120000',
        '2001-03-06 11:00:00.120000'),
       (3, 'FEED_COMMENT', 4, 1, '회원님의 피드에 댓글이 달렸습니다', 0, NULL, NULL, '2001-03-06 12:00:00.000000',
        '2001-03-06 12:00:00.000000'),
       (4, 'FEED_LIKE', 1, 2, '구글회원님이 회원님의 피드를 좋아합니다', 0, 4, 'FEED', '2001-03-06 13:00:00.000000',
        '2001-03-06 13:00:00.000000');

INSERT INTO fcm_token (id, member_id, token, device_id, device_type, created_at, updated_at)
VALUES (1, 1, 'snap-fcm-base', 'snap-device-0', 'IOS', '2001-03-07 10:00:00.123456', '2001-03-07 10:00:00.123456');

INSERT INTO notification_setting (id, member_id, notification_type, is_enabled, created_at, updated_at)
VALUES (1, 1, 'FEED_COMMENT', 0, '2001-03-07 11:00:00.120000', '2001-03-07 11:00:00.120000'),
       (2, 1, 'FEED_LIKE', 1, '2001-03-07 11:00:00.000000', '2001-03-07 11:00:00.000000');

INSERT INTO member_genre_preference (id, member_id, genre_type, created_at, updated_at)
VALUES (1, 1, 'KPOP', '2001-03-07 12:00:00.123456', '2001-03-07 12:00:00.123456');

INSERT INTO member_goods_preference (id, member_id, goods_type, created_at, updated_at)
VALUES (1, 1, 'PHOTOCARD_HOLDER', '2001-03-07 12:00:00.120000', '2001-03-07 12:00:00.120000');

INSERT INTO member_mood_preference (id, member_id, mood_type, created_at, updated_at)
VALUES (1, 1, 'COZY', '2001-03-07 12:00:00.000000', '2001-03-07 12:00:00.000000');
