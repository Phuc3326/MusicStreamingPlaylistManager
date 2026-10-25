package models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import utils.DBUtils;
import utils.PasswordUtils;

public class UserDAO {

    public User login(String username, String password) throws Exception {
        User user = getByUsername(username);
        if (user == null) {
            return null;
        }
        if (!PasswordUtils.verify(password, user.getPassword())) {
            return null;
        }
        return user;
    }

    public boolean usernameExists(String username) throws Exception {
        String sql = "SELECT 1 FROM Users WHERE LOWER(Username) = LOWER(?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int register(String username, String password) throws Exception {
        return register(username, password, "USER");
    }

    public int register(String username, String password, String role) throws Exception {
        if (usernameExists(username)) {
            return -1;
        }

        String assignedRole = (role != null && !role.trim().isEmpty()) ? role.toUpperCase() : "USER";
        String sql = "INSERT INTO Users (Username, Password, Role) VALUES (?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, PasswordUtils.hash(password));
            ps.setString(3, assignedRole);

            int affected = ps.executeUpdate();
            if (affected == 0) return -1;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean updateUsername(int userId, String newUsername) throws Exception {
        String sql = "UPDATE Users SET Username = ? WHERE UserID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newUsername);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(int userId, String newPassword) throws Exception {
        String sql = "UPDATE Users SET Password = ? WHERE UserID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, PasswordUtils.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateRole(int userId, String role) throws Exception {
        String sql = "UPDATE Users SET Role = ? WHERE UserID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role != null ? role.toUpperCase() : "USER");
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public User getById(int userId) throws Exception {
        String sql = "SELECT * FROM Users WHERE UserID = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public User getByUsername(String username) throws Exception {
        String sql = "SELECT * FROM Users WHERE LOWER(Username) = LOWER(?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public List<User> getAllUsers() throws Exception {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM Users ORDER BY UserID ASC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapUser(rs));
            }
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws Exception {
        String role = "USER";
        try {
            String r = rs.getString("Role");
            if (r != null && !r.trim().isEmpty()) {
                role = r.toUpperCase();
            }
        } catch (Exception ignored) {
            // Trường hợp DB chưa kịp migrate cột Role
        }

        return new User(
            rs.getInt("UserID"),
            rs.getString("Username"),
            rs.getString("Password"),
            role,
            rs.getTimestamp("CreatedAt")
        );
    }

    /**
     * Tự động khởi tạo hoặc đồng bộ tài khoản Admin từ biến môi trường:
     * ADMIN_USERNAME và ADMIN_PASSWORD.
     * Mật khẩu được băm an toàn qua SHA-256 (PasswordUtils.hash).
     */
    public void seedAdminFromEnv() {
        String adminUser = System.getenv("ADMIN_USERNAME");
        String adminPass = System.getenv("ADMIN_PASSWORD");

        if (adminUser == null || adminUser.trim().isEmpty() ||
            adminPass == null || adminPass.trim().isEmpty()) {
            return;
        }

        adminUser = adminUser.trim();
        adminPass = adminPass.trim();

        try {
            User existing = getByUsername(adminUser);
            if (existing == null) {
                int id = register(adminUser, adminPass, "ADMIN");
                if (id > 0) {
                    System.out.println("[Security] Khởi tạo tài khoản ADMIN thành công từ ENV: " + adminUser);
                }
            } else {
                if (!"ADMIN".equalsIgnoreCase(existing.getRole())) {
                    updateRole(existing.getUserId(), "ADMIN");
                }
                updatePassword(existing.getUserId(), adminPass);
                System.out.println("[Security] Đồng bộ tài khoản ADMIN từ ENV: " + adminUser);
            }
        } catch (Exception e) {
            System.err.println("[Security] Lỗi khi seed tài khoản ADMIN: " + e.getMessage());
        }
    }
}
