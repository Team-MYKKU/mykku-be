INSERT INTO board (id, title, logo, created_at, updated_at)
VALUES (9201, 'matrix board', 'matrix-board.png', '2001-02-03 04:05:06.123456', '2001-02-03 04:05:06.123456'),
       (9202, 'matrix board 2', 'matrix-board-2.png', '2001-02-03 04:05:06.123456', '2001-02-03 04:05:06.123456');

INSERT INTO feed (id, title, content, like_count, comment_count, board_id, member_id, created_at, updated_at)
VALUES (9301, 'matrix feed', 'matrix feed content', 0, 0, 9202, 9104,
        '2001-02-03 04:05:06.123456', '2001-02-03 04:05:06.123456');

INSERT INTO daily_message (id, title, content, date, created_at, updated_at)
VALUES (9401, 'matrix daily message', 'matrix daily content', '2001-02-03',
        '2001-02-03 04:05:06.123456', '2001-02-03 04:05:06.123456');
