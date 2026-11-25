INSERT INTO member (id, member_id, nickname, profile_image, provider, social_id, email, password, email_verified,
                    role_id, created_at, updated_at)
VALUES (9001, 'contractlogin', '계약로그인', '', 'EMAIL', NULL, 'contract-login@example.com',
        '$2a$10$iuDwvN.Y4Ngg3oKm72701uqLfy.ghUmzUi34OJ8AEHAKagxGOxieS', 1, NULL,
        '2026-01-02 03:04:05.123456', '2026-01-02 03:04:05.123456');

INSERT INTO board (id, title, logo, created_at, updated_at)
VALUES (9001, '계약 픽스처', 'contract-fixture.png', '2026-01-02 03:04:05.120000', '2026-01-02 03:04:05.000000');
