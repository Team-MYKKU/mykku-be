INSERT INTO member (id, member_id, nickname, profile_image, provider, social_id, email, password, email_verified,
                    role_id, created_at, updated_at)
VALUES (1, 'tieviewer', '동률조회', '', 'GOOGLE', 'tie-social-1', 'tie-1@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000');

INSERT INTO event (id, title, sub_title, description, expired_at, created_at, updated_at, status, started_at,
                  thumbnail_url)
VALUES (101, '동률 이벤트 101', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-01 10:00:00.000000',
        '2001-05-01 10:00:00.000000', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/event/101.jpg'),
       (102, '동률 이벤트 102', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-02 10:00:00.123456',
        '2001-05-02 10:00:00.123456', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/event/102.jpg'),
       (103, '동률 이벤트 103', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-02 10:00:00.123456',
        '2001-05-02 10:00:00.123456', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/event/103.jpg'),
       (104, '동률 이벤트 104', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-02 10:00:00.123456',
        '2001-05-02 10:00:00.123456', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/event/104.jpg'),
       (105, '동률 이벤트 105', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-03 10:00:00.120000',
        '2001-05-03 10:00:00.120000', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/event/105.jpg');
