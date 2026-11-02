package database;

import model.EmailMessage;
import model.UserAccount;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:mail_server.db";
    private static DatabaseManager instance;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy SQLite JDBC Driver: " + e.getMessage());
        }
    }

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL;");
            s.execute("PRAGMA busy_timeout=5000;");
        }
        return conn;
    }

    private void initDatabase() {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                // 1. Users table
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "email TEXT UNIQUE NOT NULL, " +
                        "password TEXT NOT NULL, " +
                        "display_name TEXT, " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ");");

                // 2. Emails table
                stmt.execute("CREATE TABLE IF NOT EXISTS emails (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "message_id TEXT, " +
                        "sender TEXT NOT NULL, " +
                        "recipient TEXT NOT NULL, " +
                        "subject TEXT, " +
                        "body TEXT, " +
                        "sent_at DATETIME, " +
                        "is_read INTEGER DEFAULT 0, " +
                        "is_starred INTEGER DEFAULT 0, " +
                        "folder TEXT DEFAULT 'INBOX', " +
                        "protocol TEXT, " +
                        "owner_email TEXT NOT NULL" +
                        ");");

                // 3. Server Logs table
                stmt.execute("CREATE TABLE IF NOT EXISTS server_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "timestamp DATETIME DEFAULT (datetime('now', 'localtime')), " +
                        "protocol TEXT, " +
                        "client_ip TEXT, " +
                        "action TEXT, " +
                        "details TEXT" +
                        ");");
            }

            // Seed default users if empty
            boolean needSeed = false;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users;")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    needSeed = true;
                }
            }

            if (needSeed) {
                seedDefaultData(conn);
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khởi tạo Database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedDefaultData(Connection conn) throws SQLException {
        String insertUserSql = "INSERT INTO users (email, password, display_name) VALUES (?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql)) {
            // Admin
            pstmt.setString(1, "admin@localhost");
            pstmt.setString(2, "123456");
            pstmt.setString(3, "Quản trị viên Hệ thống");
            pstmt.executeUpdate();

            // Alice
            pstmt.setString(1, "alice@localhost");
            pstmt.setString(2, "123456");
            pstmt.setString(3, "Alice Nguyen");
            pstmt.executeUpdate();

            // Bob
            pstmt.setString(1, "bob@localhost");
            pstmt.setString(2, "123456");
            pstmt.setString(3, "Bob Tran");
            pstmt.executeUpdate();
        }

        // Seed welcome emails for admin
        String insertEmailSql = "INSERT INTO emails (message_id, sender, recipient, subject, body, sent_at, is_read, folder, protocol, owner_email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(insertEmailSql)) {
            // Email 1
            pstmt.setString(1, "msg-001");
            pstmt.setString(2, "system@localhost");
            pstmt.setString(3, "admin@localhost");
            pstmt.setString(4, "Chào mừng đến với Hệ thống Mail Client & Server!");
            pstmt.setString(5, "Xin chào bạn,\n\nĐây là hệ thống Mail Client & Server hỗ trợ cả Gmail và Localhost.\nCác tính năng nổi bật:\n- Giao thức gửi thư SMTP (cả qua Google và Local socket :2525)\n- Giao thức nhận thư POP3 (cả qua Google và Local socket :1110)\n- Lưu trữ toàn bộ dữ liệu vào Database SQLite của Server\n- Giao diện thân thiện tương tự Gmail.\n\nChúc bạn có một buổi bảo vệ đồ án thành công!");
            pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis() - 3600000 * 2));
            pstmt.setInt(7, 0); // unread
            pstmt.setString(8, "INBOX");
            pstmt.setString(9, "SMTP_LOCAL");
            pstmt.setString(10, "admin@localhost");
            pstmt.executeUpdate();

            // Email 2
            pstmt.setString(1, "msg-002");
            pstmt.setString(2, "alice@localhost");
            pstmt.setString(3, "admin@localhost");
            pstmt.setString(4, "Kiểm tra gửi thư nội bộ qua Socket SMTP");
            pstmt.setString(5, "Chào Admin,\n\nTôi vừa gửi thử một thư qua máy chủ Local SMTP Server port 2525. Hệ thống đã xử lý và lưu trực tiếp vào CSDL SQLite của Server.\n\nTrân trọng,\nAlice");
            pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis() - 1800000));
            pstmt.setInt(7, 1); // read
            pstmt.setString(8, "INBOX");
            pstmt.setString(9, "SMTP_LOCAL");
            pstmt.setString(10, "admin@localhost");
            pstmt.executeUpdate();
        }

        // Log init event with current connection
        String logSql = "INSERT INTO server_logs (timestamp, protocol, client_ip, action, details) VALUES (?, ?, ?, ?, ?);";
        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        try (PreparedStatement lpstmt = conn.prepareStatement(logSql)) {
            lpstmt.setString(1, nowStr);
            lpstmt.setString(2, "SYSTEM");
            lpstmt.setString(3, "127.0.0.1");
            lpstmt.setString(4, "INIT");
            lpstmt.setString(5, "Khởi tạo CSDL Mail Server thành công với 3 tài khoản mặc định");
            lpstmt.executeUpdate();
        }
    }

    public void logServerEvent(String protocol, String clientIp, String action, String details) {
        String sql = "INSERT INTO server_logs (timestamp, protocol, client_ip, action, details) VALUES (?, ?, ?, ?, ?);";
        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nowStr);
            pstmt.setString(2, protocol);
            pstmt.setString(3, clientIp);
            pstmt.setString(4, action);
            pstmt.setString(5, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi ghi log Server: " + e.getMessage());
        }
    }
}
