INSERT INTO member (id, member_id, nickname, profile_image, provider, social_id, email, password, email_verified,
                    role_id, created_at, updated_at)
VALUES (1, 'tieviewer', '동률조회', '', 'GOOGLE', 'tie-social-1', 'tie-1@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (2, 'tieblocker', '동률차단', '', 'GOOGLE', 'tie-social-2', 'tie-2@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (13, 'tieliker3', '좋아요3', '', 'GOOGLE', 'tie-social-13', 'tie-13@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000');

INSERT INTO member_block (id, blocker_id, blocked_id, created_at, updated_at)
VALUES (101, 2, 13, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000');

INSERT INTO daily_message (id, title, content, date, created_at, updated_at)
VALUES (101, '동률 덕담', '동률 덕담 본문', '2001-04-30', '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000');

INSERT INTO daily_message_comment (id, content, like_count, daily_message_id, parent_comment_id, member_id,
                                   created_at, updated_at)
VALUES (101, '동률 덕담 댓글 101', 0, 101, NULL, 1, '2001-05-01 10:00:00.000000', '2001-05-01 10:00:00.000000'),
       (102, '동률 덕담 댓글 102', 0, 101, NULL, 1, '2001-05-02 10:00:00.123456', '2001-05-02 10:00:00.123456'),
       (103, '동률 덕담 댓글 103', 0, 101, NULL, 1, '2001-05-02 10:00:00.123456', '2001-05-02 10:00:00.123456'),
       (104, '동률 덕담 댓글 104', 0, 101, NULL, 1, '2001-05-02 10:00:00.123456', '2001-05-02 10:00:00.123456'),
       (105, '동률 덕담 댓글 105', 0, 101, NULL, 1, '2001-05-03 10:00:00.120000', '2001-05-03 10:00:00.120000');

INSERT INTO notification (id, type, sender_id, receiver_id, content, is_read, related_resource_id,
                          related_resource_type, created_at, updated_at)
VALUES (101, 'FEED_LIKE', 2, 1, '동률 알림 101', 0, NULL, NULL, '2001-05-01 10:00:00', '2001-05-01 10:00:00'),
       (102, 'FEED_LIKE', 2, 1, '동률 알림 102', 0, NULL, NULL, '2001-05-02 10:00:00', '2001-05-02 10:00:00'),
       (103, 'FEED_LIKE', 2, 1, '동률 알림 103', 0, NULL, NULL, '2001-05-02 10:00:00', '2001-05-02 10:00:00'),
       (104, 'FEED_LIKE', 2, 1, '동률 알림 104', 0, NULL, NULL, '2001-05-02 10:00:00', '2001-05-02 10:00:00'),
       (105, 'FEED_LIKE', 2, 1, '동률 알림 105', 0, NULL, NULL, '2001-05-03 10:00:00', '2001-05-03 10:00:00');

INSERT INTO fan_note (id, title, subtitle, content, production_date, cover_image_url, created_at, updated_at)
VALUES (101, '동률 덕질노트 101', NULL, NULL, '2001-05-01', NULL, '2001-04-30 09:00:00.000000',
        '2001-04-30 09:00:00.000000'),
       (102, '동률 덕질노트 102', NULL, NULL, '2001-05-02', NULL, '2001-04-30 09:00:00.000000',
        '2001-04-30 09:00:00.000000'),
       (103, '동률 덕질노트 103', NULL, NULL, '2001-05-02', NULL, '2001-04-30 09:00:00.000000',
        '2001-04-30 09:00:00.000000'),
       (104, '동률 덕질노트 104', NULL, NULL, '2001-05-02', NULL, '2001-04-30 09:00:00.000000',
        '2001-04-30 09:00:00.000000'),
       (105, '동률 덕질노트 105', NULL, NULL, '2001-05-03', NULL, '2001-04-30 09:00:00.000000',
        '2001-04-30 09:00:00.000000');
