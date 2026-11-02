package database;

import model.EmailMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmailDAO {
    private final DatabaseManager dbManager;

    public EmailDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public long insertEmail(EmailMessage msg) {
        String sql = "INSERT INTO emails (message_id, sender, recipient, subject, body, sent_at, is_read, is_starred, folder, protocol, owner_email) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        // Note: 10 parameters or 11 parameters? Count:
        // message_id (1), sender (2), recipient (3), subject (4), body (5), sent_at (6), is_read (7), is_starred (8), folder (9), protocol (10), owner_email (11) -> 11 parameters!
        String fixedSql = "INSERT INTO emails (message_id, sender, recipient, subject, body, sent_at, is_read, is_starred, folder, protocol, owner_email) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(fixedSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, msg.getMessageId() != null ? msg.getMessageId() : "msg-" + System.currentTimeMillis());
            pstmt.setString(2, msg.getSender());
            pstmt.setString(3, msg.getRecipient());
            pstmt.setString(4, msg.getSubject());
            pstmt.setString(5, msg.getBody());
            String sentAtStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(msg.getSentAt() != null ? msg.getSentAt() : new java.util.Date());
            pstmt.setString(6, sentAtStr);
            pstmt.setInt(7, msg.isRead() ? 1 : 0);
            pstmt.setInt(8, msg.isStarred() ? 1 : 0);
            pstmt.setString(9, msg.getFolder());
            pstmt.setString(10, msg.getProtocol());
            pstmt.setString(11, msg.getOwnerEmail());

            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                long id = rs.getLong(1);
                msg.setId(id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lưu email vào Database: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public List<EmailMessage> getEmails(String ownerEmail, String folder, String query) {
        List<EmailMessage> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM emails WHERE owner_email = ? AND folder = ? ");

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (subject LIKE ? OR sender LIKE ? OR recipient LIKE ? OR body LIKE ?) ");
        }
        sql.append("ORDER BY sent_at DESC;");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setString(1, ownerEmail);
            pstmt.setString(2, folder);

            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim() + "%";
                pstmt.setString(3, pattern);
                pstmt.setString(4, pattern);
                pstmt.setString(5, pattern);
                pstmt.setString(6, pattern);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                EmailMessage msg = mapResultSetToEmail(rs);
                list.add(msg);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn danh sách email: " + e.getMessage());
        }
        return list;
    }

    public int getUnreadCount(String ownerEmail) {
        String sql = "SELECT COUNT(*) FROM emails WHERE owner_email = ? AND folder = 'INBOX' AND is_read = 0;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ownerEmail);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đếm thư chưa đọc: " + e.getMessage());
        }
        return 0;
    }

    public void markAsRead(long id, boolean isRead) {
        String sql = "UPDATE emails SET is_read = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, isRead ? 1 : 0);
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật trạng thái đọc: " + e.getMessage());
        }
    }

    public void toggleStar(long id, boolean currentStarred) {
        String sql = "UPDATE emails SET is_starred = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentStarred ? 0 : 1);
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi đổi trạng thái gắn sao: " + e.getMessage());
        }
    }

    public void moveToFolder(long id, String newFolder) {
        String sql = "UPDATE emails SET folder = ? WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newFolder);
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi chuyển thư mục: " + e.getMessage());
        }
    }

    public void deletePermanently(long id) {
        String sql = "DELETE FROM emails WHERE id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi xóa vĩnh viễn email: " + e.getMessage());
        }
    }

    public boolean emailExists(String ownerEmail, String messageId) {
        if (messageId == null || messageId.isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM emails WHERE owner_email = ? AND message_id = ?;";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ownerEmail);
            pstmt.setString(2, messageId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra email tồn tại: " + e.getMessage());
        }
        return false;
    }

    // For Server inspection
    public List<EmailMessage> getAllServerEmails() {
        List<EmailMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM emails ORDER BY id DESC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToEmail(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy toàn bộ email Server: " + e.getMessage());
        }
        return list;
    }

    private EmailMessage mapResultSetToEmail(ResultSet rs) throws SQLException {
        EmailMessage msg = new EmailMessage();
        msg.setId(rs.getLong("id"));
        msg.setMessageId(rs.getString("message_id"));
        msg.setSender(rs.getString("sender"));
        msg.setRecipient(rs.getString("recipient"));
        msg.setSubject(rs.getString("subject"));
        msg.setBody(rs.getString("body"));
        String tsStr = rs.getString("sent_at");
        if (tsStr != null && !tsStr.isEmpty()) {
            try {
                java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(tsStr);
                msg.setSentAt(d);
            } catch (Exception ex) {
                try {
                    long millis = Long.parseLong(tsStr);
                    msg.setSentAt(new java.util.Date(millis));
                } catch (Exception ignored) {
                    Timestamp ts = rs.getTimestamp("sent_at");
                    if (ts != null) {
                        msg.setSentAt(new java.util.Date(ts.getTime()));
                    }
                }
            }
        }
        msg.setRead(rs.getInt("is_read") == 1);
        msg.setStarred(rs.getInt("is_starred") == 1);
        msg.setFolder(rs.getString("folder"));
        msg.setProtocol(rs.getString("protocol"));
        msg.setOwnerEmail(rs.getString("owner_email"));
        return msg;
    }
}

