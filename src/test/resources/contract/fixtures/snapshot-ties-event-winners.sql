INSERT INTO member (id, member_id, nickname, profile_image, provider, social_id, email, password, email_verified,
                    role_id, created_at, updated_at)
VALUES (1, 'tieviewer', '동률조회', '', 'GOOGLE', 'tie-social-1', 'tie-1@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (101, 'tiewinner101', '당첨101', '', 'GOOGLE', 'tie-social-101', 'tie-101@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (102, 'tiewinner102', '당첨102', '', 'GOOGLE', 'tie-social-102', 'tie-102@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (103, 'tiewinner103', '당첨103', '', 'GOOGLE', 'tie-social-103', 'tie-103@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (104, 'tiewinner104', '당첨104', '', 'GOOGLE', 'tie-social-104', 'tie-104@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (105, 'tiewinner105', '당첨105', '', 'GOOGLE', 'tie-social-105', 'tie-105@example.com', NULL, 0, NULL,
        '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000');

INSERT INTO event (id, title, sub_title, description, expired_at, created_at, updated_at, status, started_at,
                   thumbnail_url)
VALUES (101, '동률 이벤트', NULL, NULL, '2001-01-31 23:59:59.000000', '2001-04-30 09:00:00.000000',
        '2001-04-30 09:00:00.000000', 'WINNER_SELECTED', '2000-12-01 00:00:00.000000',
        'https://cdn.example.com/event/101.jpg');

INSERT INTO event_participation (id, member_id, event_id, created_at, updated_at)
VALUES (101, 101, 101, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (102, 102, 101, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (103, 103, 101, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (104, 104, 101, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (105, 105, 101, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000');

INSERT INTO event_winner (id, event_id, participation_id, created_at, updated_at)
VALUES (101, 101, 101, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (102, 101, 102, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (103, 101, 103, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (104, 101, 104, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000'),
       (105, 101, 105, '2001-04-30 09:00:00.000000', '2001-04-30 09:00:00.000000');
