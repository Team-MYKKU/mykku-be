INSERT INTO member (id, member_id, nickname, profile_image, provider, social_id, email, password, email_verified,
                    role_id, created_at, updated_at)
VALUES (1, 'tieviewer', '동률조회', '', 'GOOGLE', 'tie-social-1', 'tie-1@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000');

INSERT INTO contest (id, title, sub_title, description, expired_at, created_at, updated_at, status, started_at,
                    thumbnail_url)
VALUES (101, '동률 공모전 101', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-01 10:00:00.000000',
        '2001-05-01 10:00:00.000000', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/contest/101.jpg'),
       (102, '동률 공모전 102', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-02 10:00:00.123456',
        '2001-05-02 10:00:00.123456', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/contest/102.jpg'),
       (103, '동률 공모전 103', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-02 10:00:00.123456',
        '2001-05-02 10:00:00.123456', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/contest/103.jpg'),
       (104, '동률 공모전 104', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-02 10:00:00.123456',
        '2001-05-02 10:00:00.123456', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/contest/104.jpg'),
       (105, '동률 공모전 105', NULL, NULL, '2099-12-31 23:59:59.000000', '2001-05-03 10:00:00.120000',
        '2001-05-03 10:00:00.120000', 'ACTIVE', '2001-01-01 00:00:00.000000',
        'https://cdn.example.com/contest/105.jpg');
