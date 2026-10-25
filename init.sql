-- ========================================================
-- SOUNDWAVE DATABASE INITIALIZATION SCRIPT (POSTGRESQL 16)
-- Bao gồm Phân quyền người dùng (RBAC), Chế độ riêng tư (Visibility)
-- & Quy trình kiểm duyệt bài hát (Moderation Workflow)
-- ========================================================

-- 1. XÓA BẢNG CŨ ĐỂ KHỞI TẠO SẠCH SẼ
DROP TABLE IF EXISTS playlist_songs CASCADE;
DROP TABLE IF EXISTS playlists CASCADE;
DROP TABLE IF EXISTS songs CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 2. TẠO CÁC BẢNG DỮ LIỆU

-- Bảng Người dùng (Users): Có cột Role ('ADMIN' hoặc 'USER')
CREATE TABLE users (
    userid SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER' NOT NULL,
    createdat TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Bài hát (Songs):
-- + visibility: 'PUBLIC' (chia sẻ cộng đồng) hoặc 'PRIVATE' (chỉ người upload nghe)
-- + status: 'APPROVED' (được duyệt), 'PENDING' (chờ duyệt), 'REJECTED' (từ chối)
CREATE TABLE songs (
    songid SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    duration INT NOT NULL,
    filepath TEXT NOT NULL,
    coverpath TEXT,
    uploadedby INT REFERENCES users(userid) ON DELETE SET NULL,
    visibility VARCHAR(20) DEFAULT 'PUBLIC' NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    status VARCHAR(20) DEFAULT 'APPROVED' NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejectreason TEXT,
    createdat TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Danh sách phát (Playlists)
CREATE TABLE playlists (
    playlistid SERIAL PRIMARY KEY,
    userid INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('Favourite', 'Waiting')),
    isdefault BOOLEAN DEFAULT FALSE,
    createdat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (userid) REFERENCES users(userid) ON DELETE CASCADE
);

-- Bảng liên kết Danh sách phát - Bài hát (Playlist_Songs)
CREATE TABLE playlist_songs (
    playlistid INT NOT NULL,
    songid INT NOT NULL,
    orderindex INT NOT NULL,
    PRIMARY KEY (playlistid, songid),
    FOREIGN KEY (playlistid) REFERENCES playlists(playlistid) ON DELETE CASCADE,
    FOREIGN KEY (songid) REFERENCES songs(songid) ON DELETE CASCADE
);

-- 3. DỮ LIỆU MẪU (SEED DUMMY DATA)

-- Tài khoản người dùng mẫu (Mật khẩu '123456')
-- Lưu ý: Tài khoản ADMIN được khởi tạo và đồng bộ tự động qua biến môi trường ADMIN_USERNAME và ADMIN_PASSWORD
INSERT INTO users (username, password, role) VALUES
('user_an', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5d5a86aff3ca12020c923adc6ca6', 'USER'),
('user_khoi', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5d5a86aff3ca12020c923adc6ca6', 'USER');

-- Playlists mẫu
INSERT INTO playlists (userid, name, type, isdefault) VALUES
(1, 'Nhạc Trẻ Sôi Động', 'Favourite', FALSE),
(1, 'Danh Sách Chờ Tạm Thời', 'Waiting', FALSE),
(2, 'Chill Cuối Tuần', 'Favourite', FALSE),
(1, 'Liked Songs', 'Favourite', TRUE);

-- Danh sách 32 bài hát mẫu (Tất cả đều là PUBLIC và đã APPROVED)
INSERT INTO songs (title, artist, genre, duration, filepath, coverpath, uploadedby, visibility, status) VALUES
	-- BALLAD (11 bài)
	('Billie Bossa Nova', 'Billie Eilish', 'Ballad', 211, '/assets/Songs/ballad/Billie Bossa Nova.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Dancing With Your Ghost', 'Sasha Sloan', 'Ballad', 214, '/assets/Songs/ballad/Dancing With Your Ghost.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Easy On Me', 'Adele', 'Ballad', 239, '/assets/Songs/ballad/Easy On Me.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('happier', 'Olivia Rodrigo', 'Ballad', 191, '/assets/Songs/ballad/happier.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Happy For You', 'Carly Pearce', 'Ballad', 241, '/assets/Songs/ballad/Happy For You.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('How To Cry', 'Fletcher', 'Ballad', 175, '/assets/Songs/ballad/How To Cry.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('No Promises', 'Cheat Codes ft. Demi Lovato', 'Ballad', 223, '/assets/Songs/ballad/No Promises.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('pov', 'Ariana Grande', 'Ballad', 217, '/assets/Songs/ballad/pov.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Someone Like You', 'Adele', 'Ballad', 285, '/assets/Songs/ballad/Someone Like You.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Stay With Me', 'Sam Smith', 'Ballad', 172, '/assets/Songs/ballad/Stay With Me.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Stuck With U', 'Ariana Grande & Justin Bieber', 'Ballad', 211, '/assets/Songs/ballad/Stuck With U.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	-- BARBIE PLAYLIST (10 bài)
	('Barbie And The Diamond Castle: We''Re Gonna Find It', 'Barbie', 'Barbie Playlist', 196, '/assets/Songs/barbie playlist/Barbie And The Diamond Castle_ We''Re Gonna Find It.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Barbie And The Three Musketeers: All For One', 'Barbie', 'Barbie Playlist', 203, '/assets/Songs/barbie playlist/Barbie And The Three Musketeers_ All For One.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Barbie In A Mermaid Tale 2: Do The Mermaid', 'Barbie', 'Barbie Playlist', 167, '/assets/Songs/barbie playlist/Barbie In A Mermaid Tale 2_ Do The Mermaid.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Be A Friend', 'Barbie', 'Barbie Playlist', 168, '/assets/Songs/barbie playlist/Be A Friend.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Life Is A Fairytale', 'Barbie', 'Barbie Playlist', 170, '/assets/Songs/barbie playlist/Life Is A Fairytale.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Look How High We Can Fly', 'Barbie', 'Barbie Playlist', 198, '/assets/Songs/barbie playlist/Look How High We Can Fly.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('On Top Of The World', 'Barbie', 'Barbie Playlist', 243, '/assets/Songs/barbie playlist/On Top Of The World.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Only A Breath Away', 'Barbie', 'Barbie Playlist', 200, '/assets/Songs/barbie playlist/Only A Breath Away.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('To Be A Princess / To Be A Popstar', 'Barbie', 'Barbie Playlist', 209, '/assets/Songs/barbie playlist/To Be A Princess _ To Be A Popstar.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('You Can Tell She''S A Princess', 'Barbie', 'Barbie Playlist', 179, '/assets/Songs/barbie playlist/You Can Tell She''S A Princess.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	-- POP (11 bài)
	('Baby', 'Justin Bieber', 'Pop', 214, '/assets/Songs/pop/Baby.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Beauty And A Beat', 'Justin Bieber', 'Pop', 227, '/assets/Songs/pop/Beauty And A Beat.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Die With A Smile', 'Lady Gaga & Bruno Mars', 'Pop', 251, '/assets/Songs/pop/Die With A Smile.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Faded', 'Alan Walker', 'Pop', 212, '/assets/Songs/pop/Faded.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Music Town Pop Girl', 'Unknown Artist', 'Pop', 300, '/assets/Songs/pop/Music Town Pop Girl.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('One Of The Girls', 'The Weeknd, Jennie & Lily-Rose Depp', 'Pop', 242, '/assets/Songs/pop/One Of The Girls.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('Shape Of You', 'Ed Sheeran', 'Pop', 234, '/assets/Songs/pop/Shape Of You.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('STAY', 'Justin Bieber & The Kid LAROI', 'Pop', 141, '/assets/Songs/pop/STAY (E).mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('That Girl', 'Olly Murs', 'Pop', 168, '/assets/Songs/pop/That Girl.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('The Fate of Ophelia', 'Taylor Swift', 'Pop', 226, '/assets/Songs/pop/The Fate of Ophelia.mp3', NULL, NULL, 'PUBLIC', 'APPROVED'),
	('We Don''t Talk Anymore', 'Charlie Puth ft. Selena Gomez', 'Pop', 218, '/assets/Songs/pop/We Don''t Talk Anymore.mp3', NULL, NULL, 'PUBLIC', 'APPROVED');
