INSERT INTO contest_participation (id, member_id, contest_id, feed_id, created_at, updated_at)
VALUES (5, 1, 1, 4, '2001-01-13 09:00:00.000000', '2001-01-13 09:00:00.000000');

INSERT INTO contest_winner (id, winner_rank, award_title, description, acceptance_speech, contest_id, participation_id,
                            created_at, updated_at)
VALUES (3, 3, '장려상', '', '', 1, 5, '2001-02-01 09:00:00.120000', '2001-02-01 09:00:00.120000');
