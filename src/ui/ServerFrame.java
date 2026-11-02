package ui;

import database.DatabaseManager;
import database.EmailDAO;
import database.UserDAO;
import model.EmailMessage;
import model.UserAccount;
import protocol.LocalPop3Server;
import protocol.LocalSmtpServer;
import protocol.NetworkUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class ServerFrame extends JFrame {
    private final LocalSmtpServer smtpServer;
    private final LocalPop3Server pop3Server;
    private final EmailDAO emailDAO;
    private final UserDAO userDAO;

    private JLabel lblStatusBadge;
    private JButton btnToggleServer;
    private DefaultTableModel modelLogs;
    private DefaultTableModel modelEmails;
    private DefaultTableModel modelUsers;
    private JLabel lblIpAddress;
    private Timer refreshTimer;

    public ServerFrame(LocalSmtpServer smtpServer, LocalPop3Server pop3Server) {
        super("Mail Server Manager - Máy chủ SMTP & POP3");
        this.smtpServer = smtpServer;
        this.pop3Server = pop3Server;
        this.emailDAO = new EmailDAO();
        this.userDAO = new UserDAO();

        initComponents();
        loadData();

        // Tự động làm mới log và danh sách mỗi 2 giây
        refreshTimer = new Timer(2000, e -> loadData());
        refreshTimer.start();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 680);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // TOP HEADER
        JPanel topHeader = new JPanel(new BorderLayout(15, 0));
        topHeader.setBackground(new Color(30, 41, 59)); // Slate dark
        topHeader.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleBox.setOpaque(false);
        JLabel lblLogo = new JLabel("🖥️");
        lblLogo.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        titleBox.add(lblLogo);

        JLabel lblTitle = new JLabel("HỆ THỐNG MÁY CHỦ MAIL SERVER (JAVA)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(Color.WHITE);
        titleBox.add(lblTitle);

        topHeader.add(titleBox, BorderLayout.WEST);

        lblStatusBadge = new JLabel("🟢 SERVER ĐANG HOẠT ĐỘNG");
        lblStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatusBadge.setForeground(new Color(74, 222, 128)); // Bright green
        topHeader.add(lblStatusBadge, BorderLayout.EAST);

        add(topHeader, BorderLayout.NORTH);

        // HERO IP BOX (CỰC KỲ QUAN TRỌNG ĐỂ CÁC MÁY KHÁC BIẾT IP KẾT NỐI)
        JPanel heroPanel = new JPanel(new BorderLayout(10, 5));
        heroPanel.setBackground(new Color(241, 245, 249));
        heroPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(203, 213, 225)),
                new EmptyBorder(12, 20, 12, 20)
        ));

        String primaryIp = NetworkUtils.getPrimaryIpAddress();
        lblIpAddress = new JLabel("🌐 ĐỊA CHỈ IP MÁY CHỦ:  " + primaryIp + "    |    SMTP Port: 2525    |    POP3 Port: 1110");
        lblIpAddress.setFont(new Font("Consolas", Font.BOLD, 15));
        lblIpAddress.setForeground(new Color(15, 23, 42));
        heroPanel.add(lblIpAddress, BorderLayout.NORTH);

        JLabel lblInstruction = new JLabel("👉 Hướng dẫn: Các máy Client khác trong mạng Wi-Fi/LAN hãy nhập IP: " + primaryIp + " vào màn hình Đăng nhập để gửi và nhận tin nhắn!");
        lblInstruction.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblInstruction.setForeground(new Color(71, 85, 105));
        heroPanel.add(lblInstruction, BorderLayout.SOUTH);

        // CENTER TABS
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // TAB 1: Server Logs Real-time
        JPanel panelLogs = new JPanel(new BorderLayout(5, 5));
        panelLogs.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] logCols = {"ID", "Thời gian", "Giao thức", "Client IP", "Hành động", "Chi tiết sự kiện"};
        modelLogs = new DefaultTableModel(logCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblLogs = new JTable(modelLogs);
        tblLogs.setRowHeight(24);
        tblLogs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblLogs.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblLogs.getColumnModel().getColumn(0).setMaxWidth(55);
        tblLogs.getColumnModel().getColumn(1).setPreferredWidth(160); // Thời gian đầy đủ không bị cắt
        tblLogs.getColumnModel().getColumn(2).setPreferredWidth(105);
        tblLogs.getColumnModel().getColumn(3).setPreferredWidth(110);
        tblLogs.getColumnModel().getColumn(4).setPreferredWidth(95);
        tblLogs.getColumnModel().getColumn(5).setPreferredWidth(380);
        panelLogs.add(new JScrollPane(tblLogs), BorderLayout.CENTER);

        tabs.addTab("📋 Nhật ký Socket & Giao dịch (Real-time Logs)", panelLogs);

        // TAB 2: Database Emails
        JPanel panelEmails = new JPanel(new BorderLayout(5, 5));
        panelEmails.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] emailCols = {"ID", "Người gửi", "Người nhận", "Tiêu đề", "Giao thức", "Thời gian", "Thư mục"};
        modelEmails = new DefaultTableModel(emailCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblEmails = new JTable(modelEmails);
        tblEmails.setRowHeight(24);
        tblEmails.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblEmails.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblEmails.getColumnModel().getColumn(0).setMaxWidth(55);
        tblEmails.getColumnModel().getColumn(1).setPreferredWidth(150);
        tblEmails.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblEmails.getColumnModel().getColumn(3).setPreferredWidth(200);
        tblEmails.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblEmails.getColumnModel().getColumn(5).setPreferredWidth(150); // Thời gian
        tblEmails.getColumnModel().getColumn(6).setPreferredWidth(80);
        panelEmails.add(new JScrollPane(tblEmails), BorderLayout.CENTER);

        tabs.addTab("📧 CSDL Thư đã lưu (SQLite: mail_server.db)", panelEmails);

        // TAB 3: Registered Users
        JPanel panelUsers = new JPanel(new BorderLayout(5, 5));
        panelUsers.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] userCols = {"ID", "Gmail / Tài khoản", "Tên hiển thị", "Ngày tham gia"};
        modelUsers = new DefaultTableModel(userCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblUsers = new JTable(modelUsers);
        tblUsers.setRowHeight(24);
        tblUsers.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panelUsers.add(new JScrollPane(tblUsers), BorderLayout.CENTER);

        tabs.addTab("👥 Danh sách Người dùng", panelUsers);

        // Combine Center
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(heroPanel, BorderLayout.NORTH);
        centerPanel.add(tabs, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // BOTTOM CONTROLS
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnOpenClient = new JButton("✉ Mở Giao diện Client trên máy này");
        btnOpenClient.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenClient.addActionListener(e -> {
            config.AppConfig cfg = config.AppConfig.getInstance();
            if (!cfg.isLoggedIn()) {
                LoginDialog login = new LoginDialog(this);
                login.setVisible(true);
                if (login.isLoginSuccessful()) {
                    new MainFrame(smtpServer, pop3Server).setVisible(true);
                }
            } else {
                new MainFrame(smtpServer, pop3Server).setVisible(true);
            }
        });
        bottomBar.add(btnOpenClient);

        btnToggleServer = new JButton("Khởi động lại Server");
        btnToggleServer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnToggleServer.addActionListener(e -> restartServer());
        bottomBar.add(btnToggleServer);

        JButton btnServerRelay = new JButton("🔔 Cấu hình Gmail báo thật (Server Relay)");
        btnServerRelay.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnServerRelay.setToolTipText("Cấu hình 1 tài khoản trung tâm để Server tự động bắn thông báo thật tới mọi người nhận");
        btnServerRelay.addActionListener(e -> showRelayConfigDialog());
        bottomBar.add(btnServerRelay);

        JButton btnClearLogs = new JButton("Xóa nhật ký log CSDL");
        btnClearLogs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearLogs.addActionListener(e -> {
            int conf = JOptionPane.showConfirmDialog(this, "Bạn có muốn làm sạch toàn bộ nhật ký log trong CSDL?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     Statement st = conn.createStatement()) {
                    st.executeUpdate("DELETE FROM server_logs;");
                    DatabaseManager.getInstance().logServerEvent("SYSTEM", "127.0.0.1", "CLEAR_LOGS", "Đã làm sạch nhật ký cũ");
                    loadData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
                }
            }
        });
        bottomBar.add(btnClearLogs);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private void loadData() {
        // Load logs
        String sql = "SELECT * FROM server_logs ORDER BY id DESC LIMIT 50;";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            modelLogs.setRowCount(0);
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
        } catch (Exception ignored) {}

        // Load emails count
        List<EmailMessage> emails = emailDAO.getAllServerEmails();
        modelEmails.setRowCount(0);
        for (EmailMessage m : emails) {
            modelEmails.addRow(new Object[]{
                    m.getId(),
                    m.getSender(),
                    m.getRecipient(),
                    m.getSubject(),
                    m.getProtocol(),
                    m.getFormattedDate(),
                    m.getFolder()
            });
        }

        // Load users
        List<UserAccount> users = userDAO.getAllUsers();
        modelUsers.setRowCount(0);
        for (UserAccount u : users) {
            modelUsers.addRow(new Object[]{
                    u.getId(),
                    u.getEmail(),
                    u.getDisplayName(),
                    u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
            });
        }
    }

    private void restartServer() {
        smtpServer.stop();
        pop3Server.stop();
        smtpServer.start();
        pop3Server.start();
        JOptionPane.showMessageDialog(this, "Đã khởi động lại máy chủ Mail Server thành công!", "Server Status", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showRelayConfigDialog() {
        config.AppConfig cfg = config.AppConfig.getInstance();
        JTextField txtEmail = new JTextField(cfg.getServerRelayEmail());
        JPasswordField txtPass = new JPasswordField(cfg.getServerRelayAppPassword());

        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.add(new JLabel("Gmail Server phát thông báo:"));
        p.add(txtEmail);
        p.add(new JLabel("Mật khẩu ứng dụng 16 ký tự:"));
        p.add(txtPass);

        int res = JOptionPane.showConfirmDialog(this, p, "Cấu hình Email phát thông báo của Server", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            cfg.setServerRelayEmail(txtEmail.getText().trim());
            cfg.setServerRelayAppPassword(new String(txtPass.getPassword()).trim());
            cfg.save();
            JOptionPane.showMessageDialog(this, "Đã lưu cấu hình Relay thành công!\nTừ giờ, bất kỳ Client nào gửi tin nhắn qua Server, Server sẽ tự động phát thông báo thật tới hòm thư người nhận.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
