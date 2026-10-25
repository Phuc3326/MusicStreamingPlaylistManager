package models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lib.DynamicArrayList;
import utils.DBUtils;

public class SongDAO {

    private static final String BASE_SELECT =
        "SELECT s.SongID, s.Title, s.Artist, s.Genre, s.Duration, s.FilePath, s.CoverPath, " +
        "s.UploadedBy, s.Visibility, s.Status, s.RejectReason, s.CreatedAt, u.Username AS UploaderName " +
        "FROM Songs s " +
        "LEFT JOIN Users u ON s.UploadedBy = u.UserID ";

    public Song getById(int songId) throws Exception {
        String sql = BASE_SELECT + "WHERE s.SongID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, songId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapSong(rs) : null;
            }
        }
    }

    /**
     * Lấy toàn bộ bài hát CÔNG KHAI (PUBLIC) và ĐÃ ĐƯỢC DUYỆT (APPROVED)
     * để nạp vào Thư viện chung trang chủ và Bộ máy tìm kiếm toàn cục.
     */
    public DynamicArrayList getAllSongs() throws Exception {
        String sql = BASE_SELECT +
                     "WHERE (s.Visibility = 'PUBLIC' OR s.Visibility IS NULL) " +
                     "AND (s.Status = 'APPROVED' OR s.Status IS NULL) " +
                     "ORDER BY s.Title ASC";
        DynamicArrayList songs = new DynamicArrayList();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                songs.add(mapSong(rs));
            }
        }

        return songs;
    }

    /**
     * Lấy toàn bộ bài hát do người dùng hiện tại tải lên (Bao gồm cả Public và Private, mọi trạng thái)
     */
    public List<Song> getSongsByUploader(int userId) throws Exception {
        String sql = BASE_SELECT + "WHERE s.UploadedBy = ? ORDER BY s.CreatedAt DESC, s.SongID DESC";
        List<Song> songs = new ArrayList<>();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapSong(rs));
                }
            }
        }

        return songs;
    }

    /**
     * Lấy danh sách bài hát CÔNG KHAI đang chờ duyệt (PUBLIC & PENDING) cho Admin
     */
    public List<Song> getPendingSongs() throws Exception {
        String sql = BASE_SELECT +
                     "WHERE (s.Visibility = 'PUBLIC' OR s.Visibility IS NULL) " +
                     "AND s.Status = 'PENDING' ORDER BY s.CreatedAt ASC, s.SongID ASC";
        List<Song> songs = new ArrayList<>();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                songs.add(mapSong(rs));
            }
        }

        return songs;
    }

    /**
     * Đếm số lượng bài hát công khai đang chờ duyệt để hiển thị huy hiệu (Badge) trên menu Admin
     */
    public int countPendingSongs() {
        String sql = "SELECT COUNT(*) FROM Songs WHERE (Visibility = 'PUBLIC' OR Visibility IS NULL) AND Status = 'PENDING'";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
        }
        return 0;
    }

    /**
     * Admin duyệt bài hát
     */
    public boolean approveSong(int songId) throws Exception {
        return updateSongStatus(songId, "APPROVED", null);
    }

    /**
     * Admin từ chối bài hát kèm lý do
     */
    public boolean rejectSong(int songId, String reason) throws Exception {
        return updateSongStatus(songId, "REJECTED", reason);
    }

    public boolean updateSongStatus(int songId, String status, String reason) throws Exception {
        String sql = "UPDATE Songs SET Status = ?, RejectReason = ? WHERE SongID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.toUpperCase());
            ps.setString(2, reason);
            ps.setInt(3, songId);
            return ps.executeUpdate() > 0;
        }
    }

    public DynamicArrayList searchByTitle(String keyword) throws Exception {
        String sql = BASE_SELECT +
                     "WHERE (s.Visibility = 'PUBLIC' OR s.Visibility IS NULL) " +
                     "AND (s.Status = 'APPROVED' OR s.Status IS NULL) " +
                     "AND LOWER(s.Title) LIKE LOWER(?) ORDER BY s.Title ASC";
        DynamicArrayList songs = new DynamicArrayList();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapSong(rs));
                }
            }
        }

        return songs;
    }

    public DynamicArrayList getSongsByGenre(String genre, int limit) throws Exception {
        String sql = BASE_SELECT +
                     "WHERE (s.Visibility = 'PUBLIC' OR s.Visibility IS NULL) " +
                     "AND (s.Status = 'APPROVED' OR s.Status IS NULL) " +
                     "AND LOWER(s.Genre) = LOWER(?) ORDER BY s.SongID ASC LIMIT ?";
        DynamicArrayList songs = new DynamicArrayList();

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, genre);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songs.add(mapSong(rs));
                }
            }
        }

        return songs;
    }

    public boolean songExists(String title, String artist) throws Exception {
        String sql = "SELECT 1 FROM Songs WHERE LOWER(Title) = LOWER(?) AND LOWER(Artist) = LOWER(?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, artist);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertSong(Song song) throws Exception {
        String sql = "INSERT INTO Songs (Title, Artist, Genre, Duration, FilePath, CoverPath, UploadedBy, Visibility, Status, RejectReason) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, song.getTitle());
            ps.setString(2, song.getArtist());
            ps.setString(3, song.getGenre());
            ps.setInt(4, song.getDuration());
            ps.setString(5, song.getFilePath());
            ps.setString(6, song.getCoverPath());

            if (song.getUploadedBy() != null && song.getUploadedBy() > 0) {
                ps.setInt(7, song.getUploadedBy());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }

            String visibility = (song.getVisibility() != null && !song.getVisibility().trim().isEmpty())
                                ? song.getVisibility().toUpperCase() : "PUBLIC";
            ps.setString(8, visibility);

            String status = (song.getStatus() != null && !song.getStatus().trim().isEmpty())
                            ? song.getStatus().toUpperCase() : "APPROVED";
            ps.setString(9, status);

            ps.setString(10, song.getRejectReason());

            int affected = ps.executeUpdate();
            if (affected == 0) return -1;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean updateSong(Song song) throws Exception {
        String sql = "UPDATE Songs SET Title = ?, Artist = ?, Genre = ?, Duration = ?, "
                   + "FilePath = ?, CoverPath = ?, Visibility = ?, Status = ? WHERE SongID = ?";

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, song.getTitle());
            ps.setString(2, song.getArtist());
            ps.setString(3, song.getGenre());
            ps.setInt(4, song.getDuration());
            ps.setString(5, song.getFilePath());
            ps.setString(6, song.getCoverPath());
            ps.setString(7, song.getVisibility());
            ps.setString(8, song.getStatus());
            ps.setInt(9, song.getSongId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteSong(int songId) throws Exception {
        String sql = "DELETE FROM Songs WHERE SongID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, songId);
            return ps.executeUpdate() > 0;
        }
    }

    private Song mapSong(ResultSet rs) throws Exception {
        Integer uploadedBy = null;
        try {
            int uid = rs.getInt("UploadedBy");
            if (!rs.wasNull()) {
                uploadedBy = uid;
            }
        } catch (Exception ignored) {}

        String uploaderName = null;
        try {
            uploaderName = rs.getString("UploaderName");
        } catch (Exception ignored) {}

        String visibility = "PUBLIC";
        try {
            String v = rs.getString("Visibility");
            if (v != null && !v.trim().isEmpty()) {
                visibility = v.toUpperCase();
            }
        } catch (Exception ignored) {}

        String status = "APPROVED";
        try {
            String s = rs.getString("Status");
            if (s != null && !s.trim().isEmpty()) {
                status = s.toUpperCase();
            }
        } catch (Exception ignored) {}

        String rejectReason = null;
        try {
            rejectReason = rs.getString("RejectReason");
        } catch (Exception ignored) {}

        Timestamp createdAt = null;
        try {
            createdAt = rs.getTimestamp("CreatedAt");
        } catch (Exception ignored) {}

        return new Song(
            rs.getInt("SongID"),
            rs.getString("Title"),
            rs.getString("Artist"),
            rs.getString("Genre"),
            rs.getInt("Duration"),
            rs.getString("FilePath"),
            rs.getString("CoverPath"),
            uploadedBy,
            uploaderName,
            visibility,
            status,
            rejectReason,
            createdAt
        );
    }
}
