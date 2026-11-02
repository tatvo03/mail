package database;

import model.UserAccount;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public UserAccount getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn user: " + e.getMessage());
        }
        return null;
    }

    public boolean authenticate(String email, String password) {
        UserAccount user = getUserByEmail(email);
        if (user == null) return false;
        return user.getPassword().equals(password);
    }

    public boolean createUser(String email, String password, String displayName) {
        String sql = "INSERT INTO users (email, password, display_name) VALUES (?, ?, ?);";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim().toLowerCase());
            pstmt.setString(2, password);
            pstmt.setString(3, displayName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi tạo user: " + e.getMessage());
            return false;
        }
    }

    public List<UserAccount> getAllUsers() {
        List<UserAccount> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy danh sách users: " + e.getMessage());
        }
        return list;
    }

    private UserAccount mapResultSetToUser(ResultSet rs) throws SQLException {
        UserAccount u = new UserAccount();
        u.setId(rs.getLong("id"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setDisplayName(rs.getString("display_name"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            u.setCreatedAt(new java.util.Date(ts.getTime()));
        }
        return u;
    }
}

