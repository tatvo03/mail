package ui;

import database.DatabaseManager;
import database.EmailDAO;
import database.UserDAO;
import model.EmailMessage;
import model.UserAccount;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Vector;

public class DatabaseViewerDialog extends JDialog {
    private final EmailDAO emailDAO;
    private final UserDAO userDAO;

    private JTable tblEmails;
    private DefaultTableModel modelEmails;

    private JTable tblUsers;
    private DefaultTableModel modelUsers;

    private JTable tblLogs;
    private DefaultTableModel modelLogs;

    private JTextArea txtSqlQuery;
    private JTable tblCustomQuery;
    private DefaultTableModel modelCustomQuery;

    public DatabaseViewerDialog(Frame owner) {
        super(owner, "🗄️ Quản lý Cơ sở dữ liệu của Server (SQLite: mail_server.db)", true);
        this.emailDAO = new EmailDAO();
        this.userDAO = new UserDAO();

        initComponents();
        loadAllData();
        setSize(950, 620);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 90, 188));
        headerPanel.setBorder(new EmptyBorder(12, 15, 12, 15));

        JLabel lblTitle = new JLabel("Cơ sở dữ liệu của Mail Server (Database Inspector)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(Color.WHITE);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        JLabel lblSub = new JLabel("SQLite: mail_server.db (Chứng minh dữ liệu đã được lưu trữ thực sự trên Server)");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(220, 235, 255));
        headerPanel.add(lblSub, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // TAB 1: Bảng Emails
        JPanel panelEmails = new JPanel(new BorderLayout(5, 5));
        panelEmails.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] emailCols = {"ID", "Người gửi", "Người nhận", "Tiêu đề", "Thư mục", "Giao thức", "Thời gian", "Chủ sở hữu"};
        modelEmails = new DefaultTableModel(emailCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblEmails = new JTable(modelEmails);
        tblEmails.setRowHeight(24);
        tblEmails.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblEmails.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblEmails.getColumnModel().getColumn(0).setMaxWidth(50);
        tblEmails.getColumnModel().getColumn(6).setPreferredWidth(150);
        panelEmails.add(new JScrollPane(tblEmails), BorderLayout.CENTER);

        JPanel actionEmails = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefreshEmails = new JButton("🔄 Làm mới danh sách");
        btnRefreshEmails.addActionListener(e -> loadEmails());
        actionEmails.add(btnRefreshEmails);
        panelEmails.add(actionEmails, BorderLayout.SOUTH);

        tabs.addTab("📧 Bảng Emails (" + modelEmails.getRowCount() + ")", panelEmails);

        // TAB 2: Bảng Users
        JPanel panelUsers = new JPanel(new BorderLayout(5, 5));
        panelUsers.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] userCols = {"ID", "Email", "Tên hiển thị", "Mật khẩu", "Ngày tạo"};
        modelUsers = new DefaultTableModel(userCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblUsers = new JTable(modelUsers);
        tblUsers.setRowHeight(24);
        tblUsers.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblUsers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panelUsers.add(new JScrollPane(tblUsers), BorderLayout.CENTER);

        JPanel actionUsers = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefreshUsers = new JButton("🔄 Làm mới người dùng");
        btnRefreshUsers.addActionListener(e -> loadUsers());
        actionUsers.add(btnRefreshUsers);
        panelUsers.add(actionUsers, BorderLayout.SOUTH);

        tabs.addTab("👥 Bảng Users", panelUsers);

        // TAB 3: Server Logs
        JPanel panelLogs = new JPanel(new BorderLayout(5, 5));
        panelLogs.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] logCols = {"ID", "Thời gian", "Giao thức", "Client IP", "Hành động", "Chi tiết"};
        modelLogs = new DefaultTableModel(logCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblLogs = new JTable(modelLogs);
        tblLogs.setRowHeight(24);
        tblLogs.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblLogs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblLogs.getColumnModel().getColumn(0).setMaxWidth(50);
        tblLogs.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblLogs.getColumnModel().getColumn(5).setPreferredWidth(350);
        panelLogs.add(new JScrollPane(tblLogs), BorderLayout.CENTER);

        JPanel actionLogs = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefreshLogs = new JButton("🔄 Làm mới nhật ký socket");
        btnRefreshLogs.addActionListener(e -> loadLogs());
        actionLogs.add(btnRefreshLogs);
        panelLogs.add(actionLogs, BorderLayout.SOUTH);

        tabs.addTab("📋 Server Logs (SMTP/POP3)", panelLogs);

        // TAB 4: Truy vấn SQL trực tiếp
        JPanel panelQuery = new JPanel(new BorderLayout(5, 5));
        panelQuery.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel sqlTop = new JPanel(new BorderLayout(5, 5));
        txtSqlQuery = new JTextArea("SELECT * FROM emails ORDER BY id DESC LIMIT 20;");
        txtSqlQuery.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtSqlQuery.setRows(3);
        sqlTop.add(new JScrollPane(txtSqlQuery), BorderLayout.CENTER);

        JButton btnRunSql = new JButton("▶ Thực thi SQL");
        btnRunSql.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRunSql.addActionListener(e -> executeCustomSql());
        sqlTop.add(btnRunSql, BorderLayout.EAST);
        panelQuery.add(sqlTop, BorderLayout.NORTH);

        modelCustomQuery = new DefaultTableModel();
        tblCustomQuery = new JTable(modelCustomQuery);
        tblCustomQuery.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panelQuery.add(new JScrollPane(tblCustomQuery), BorderLayout.CENTER);

        tabs.addTab("🔍 Truy vấn SQL trực tiếp", panelQuery);

        add(tabs, BorderLayout.CENTER);

        // Bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadAllData() {
        loadEmails();
        loadUsers();
        loadLogs();
    }

    private void loadEmails() {
        modelEmails.setRowCount(0);
        List<EmailMessage> list = emailDAO.getAllServerEmails();
        for (EmailMessage m : list) {
            modelEmails.addRow(new Object[]{
                    m.getId(),
                    m.getSender(),
                    m.getRecipient(),
                    m.getSubject(),
                    m.getFolder(),
                    m.getProtocol(),
                    m.getFormattedDate(),
                    m.getOwnerEmail()
            });
        }
    }

    private void loadUsers() {
        modelUsers.setRowCount(0);
        List<UserAccount> list = userDAO.getAllUsers();
        for (UserAccount u : list) {
            modelUsers.addRow(new Object[]{
                    u.getId(),
                    u.getEmail(),
                    u.getDisplayName(),
                    "****** (" + u.getPassword().length() + " ký tự)",
                    u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
            });
        }
    }

    private void loadLogs() {
        modelLogs.setRowCount(0);
        String sql = "SELECT * FROM server_logs ORDER BY id DESC LIMIT 100;";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                modelLogs.addRow(new Object[]{
                        rs.getLong("id"),
                        rs.getString("timestamp"),
                        rs.getString("protocol"),
                        rs.getString("client_ip"),
                        rs.getString("action"),
                        rs.getString("details")
                });
            }
        } catch (Exception ex) {
            System.err.println("Lỗi đọc logs: " + ex.getMessage());
        }
    }

    private void executeCustomSql() {
        String query = txtSqlQuery.getText().trim();
        if (query.isEmpty()) return;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            if (query.toLowerCase().startsWith("select")) {
                ResultSet rs = stmt.executeQuery(query);
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                Vector<String> colNames = new Vector<>();
                for (int i = 1; i <= colCount; i++) {
                    colNames.add(meta.getColumnLabel(i));
                }

                Vector<Vector<Object>> data = new Vector<>();
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    data.add(row);
                }

                modelCustomQuery.setDataVector(data, colNames);
            } else {
                int affected = stmt.executeUpdate(query);
                JOptionPane.showMessageDialog(this, "Thực thi thành công! Số dòng bị ảnh hưởng: " + affected, "SQL Info", JOptionPane.INFORMATION_MESSAGE);
                loadAllData();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi SQL:\n" + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
        }
    }
}

