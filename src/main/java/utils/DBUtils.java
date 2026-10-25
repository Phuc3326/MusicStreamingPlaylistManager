package utils;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Tiện ích quản lý kết nối PostgreSQL cho SoundWave.
 * Tối ưu hóa chuyên biệt cho môi trường Docker Compose.
 *
 * Mặc định tự động kết nối:
 * - Trong Docker container: host 'music-db', port 5432, db 'music_db'
 * - Chạy ngoài máy local: host 'localhost', port 5436 (map từ docker compose)
 */
public class DBUtils {

    // Cấu hình mặc định khớp 100% với docker-compose.yml
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "5436"; // Port map ngoài của soundwave-postgres
    private static final String DEFAULT_DB = "music_db";
    private static final String DEFAULT_USER = "music_user";
    private static final String DEFAULT_PASS = "MusicSecret2026!";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("FATAL: Không tìm thấy PostgreSQL JDBC Driver!");
        }
    }

    public static Connection getConnection() throws SQLException {
        // 1. Ưu tiên biến DB_URL được truyền trực tiếp từ docker-compose.yml
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            String user = (dbUser != null) ? dbUser : DEFAULT_USER;
            String pass = (dbPass != null) ? dbPass : DEFAULT_PASS;
            return DriverManager.getConnection(dbUrl, user, pass);
        }

        // 2. Định dạng DATABASE_URL (nếu chạy cloud PaaS)
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.trim().isEmpty()) {
            return getConnectionFromDatabaseUrl(databaseUrl.trim());
        }

        // 3. Fallback cấu hình linh hoạt hoặc mặc định máy local test cổng 5436
        String host = System.getenv("DB_HOST");
        String port = System.getenv("DB_PORT");
        String dbName = System.getenv("DB_NAME");

        host = (host != null && !host.trim().isEmpty()) ? host : DEFAULT_HOST;
        port = (port != null && !port.trim().isEmpty()) ? port : DEFAULT_PORT;
        dbName = (dbName != null && !dbName.trim().isEmpty()) ? dbName : DEFAULT_DB;
        String user = (dbUser != null && !dbUser.trim().isEmpty()) ? dbUser : DEFAULT_USER;
        String pass = (dbPass != null && !dbPass.trim().isEmpty()) ? dbPass : DEFAULT_PASS;

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        return DriverManager.getConnection(jdbcUrl, user, pass);
    }

    private static Connection getConnectionFromDatabaseUrl(String rawUrl) throws SQLException {
        try {
            URI uri = new URI(rawUrl);
            String userInfo = uri.getUserInfo();
            String user = "";
            String pass = "";
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                user = parts[0];
                pass = parts[1];
            }

            int port = uri.getPort();
            if (port <= 0) port = 5432;

            String path = uri.getPath();
            String dbName = (path != null && path.length() > 1) ? path.substring(1) : DEFAULT_DB;
            String query = uri.getQuery();

            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + dbName;
            if (query != null && !query.isEmpty()) {
                jdbcUrl += "?" + query;
            }

            return DriverManager.getConnection(jdbcUrl, user, pass);
        } catch (Exception e) {
            return DriverManager.getConnection(rawUrl);
        }
    }
}
