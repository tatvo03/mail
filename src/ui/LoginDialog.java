package ui;

import config.AppConfig;
import protocol.Pop3Receiver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.Socket;

public class LoginDialog extends JDialog {
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JTextField txtDisplayName;
    private JTextField txtServerIp;
    private JTextField txtSmtpPort;
    private JTextField txtPop3Port;

    private JButton btnLogin;
    private JButton btnTestServer;
    private JLabel lblStatus;

    private boolean loginSuccessful = false;

    public LoginDialog(Frame parent) {
        super(parent, "Đăng nhập Mail Client (Bằng Gmail Thật)", true);
        initComponents();
        loadExistingConfig();
        setSize(560, 620);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header Panel phong cách Google
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        brandPanel.setOpaque(false);

        JLabel lblLogo = new JLabel("✉");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblLogo.setForeground(new Color(234, 67, 53)); // Gmail Red
        brandPanel.add(lblLogo);

        JLabel lblTitle = new JLabel("Đăng nhập Gmail");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(32, 33, 36));
        brandPanel.add(lblTitle);

        headerPanel.add(brandPanel, BorderLayout.NORTH);

        JLabel lblSub = new JLabel("Đăng nhập bằng Gmail thật để gửi và nhận thông báo mail thật từ các máy khác", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(95, 99, 104));
        headerPanel.add(lblSub, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Form Fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 25, 10, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // 1. Gmail
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        JLabel lblEmail = new JLabel("Địa chỉ Gmail thật:");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblEmail, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtEmail = new JTextField();
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtEmail.setToolTipText("Ví dụ: studentA@gmail.com");
        form.add(txtEmail, gbc);

        // 2. App Password
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.35;
        JLabel lblPass = new JLabel("Mật khẩu ứng dụng / Mật khẩu:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblPass, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtPassword = new JPasswordField("123456");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPassword.setToolTipText("Mật khẩu ứng dụng 16 ký tự Google (để báo về mail thật) hoặc mật khẩu bất kỳ");
        form.add(txtPassword, gbc);

        // Guide Button
        gbc.gridx = 1; gbc.gridy = 2;
        JButton btnGuide = new JButton("❓ Bị lỗi 'Tùy chọn không khả dụng' (Robot vỡ)? Bấm xem cách sửa");
        btnGuide.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnGuide.setForeground(new Color(217, 48, 37)); // Red notice
        btnGuide.setBorderPainted(false);
        btnGuide.setContentAreaFilled(false);
        btnGuide.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuide.addActionListener(e -> showAppPasswordGuide());
        form.add(btnGuide, gbc);

        // 3. Display Name
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.35;
        JLabel lblName = new JLabel("Tên hiển thị của bạn:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblName, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtDisplayName = new JTextField();
        txtDisplayName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(txtDisplayName, gbc);

        // Separator
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JSeparator sep = new JSeparator();
        sep.setBorder(new EmptyBorder(10, 0, 10, 0));
        form.add(sep, gbc);

        // 4. Server IP
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.35;
        JLabel lblServer = new JLabel("Địa chỉ IP của Mail Server:");
        lblServer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblServer, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        txtServerIp = new JTextField("localhost");
        txtServerIp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtServerIp.setToolTipText("Nhập IP máy chủ trong mạng LAN (ví dụ: 192.168.1.15) hoặc localhost");
        form.add(txtServerIp, gbc);

        // 5. Ports (SMTP / POP3)
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.35;
        JLabel lblPorts = new JLabel("Cổng SMTP / POP3 Server:");
        lblPorts.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblPorts, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        JPanel portsPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        txtSmtpPort = new JTextField("2525");
        txtPop3Port = new JTextField("1110");
        portsPanel.add(txtSmtpPort);
        portsPanel.add(txtPop3Port);
        form.add(portsPanel, gbc);

        // 6. Test Button
        gbc.gridx = 1; gbc.gridy = 7;
        btnTestServer = new JButton("⚡ Kiểm tra kết nối tới Mail Server");
        btnTestServer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnTestServer.addActionListener(e -> testServerConnection());
        form.add(btnTestServer, gbc);

        // 7. Status Label
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2;
        lblStatus = new JLabel("Sẵn sàng đăng nhập.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblStatus.setForeground(new Color(100, 100, 100));
        form.add(lblStatus, gbc);

        add(form, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));

        JButton btnExit = new JButton("Thoát");
        btnExit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnExit.addActionListener(e -> {
            loginSuccessful = false;
            dispose();
        });
        bottom.add(btnExit);

        btnLogin = new JButton("Đăng nhập & Vào ứng dụng ➔");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setBackground(new Color(26, 115, 232));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> handleLogin());
        bottom.add(btnLogin);

        add(bottom, BorderLayout.SOUTH);
    }

    private void loadExistingConfig() {
        AppConfig config = AppConfig.getInstance();
        txtEmail.setText(config.getUserEmail());
        txtPassword.setText(config.getUserAppPassword());
        txtDisplayName.setText(config.getDisplayName());
        txtServerIp.setText(config.getServerHost());
        txtSmtpPort.setText(String.valueOf(config.getServerSmtpPort()));
        txtPop3Port.setText(String.valueOf(config.getServerPop3Port()));
    }

    private void testServerConnection() {
        String host = txtServerIp.getText().trim();
        int smtpP, pop3P;
        try {
            smtpP = Integer.parseInt(txtSmtpPort.getText().trim());
            pop3P = Integer.parseInt(txtPop3Port.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Cổng phải là số nguyên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnTestServer.setEnabled(false);
        lblStatus.setText("Đang kiểm tra kết nối tới Server " + host + "...");
        lblStatus.setForeground(new Color(26, 115, 232));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Test SMTP socket
                try (Socket s = new Socket(host, smtpP)) {
                    s.setSoTimeout(3000);
                }
                // Test POP3 socket
                try (Socket s = new Socket(host, pop3P)) {
                    s.setSoTimeout(3000);
                }
                return null;
            }

            @Override
            protected void done() {
                btnTestServer.setEnabled(true);
                try {
                    get();
                    lblStatus.setText("✓ Kết nối máy chủ " + host + " thành công (Cổng " + smtpP + " & " + pop3P + " mở)!");
                    lblStatus.setForeground(new Color(34, 139, 34));
                } catch (Exception ex) {
                    lblStatus.setText("✗ Không thể kết nối tới Server " + host + ": " + ex.getMessage());
                    lblStatus.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(LoginDialog.this,
                            "Không thể kết nối tới Mail Server tại " + host + ":\n" +
                            "Hãy đảm bảo máy Server đã bật file 'run_server.bat' và cùng mạng Wi-Fi/LAN.",
                            "Lỗi kết nối Server", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleLogin() {
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        String name = txtDisplayName.getText().trim();
        String host = txtServerIp.getText().trim();

        if (email.isEmpty() || !email.contains("@")) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập địa chỉ Email hợp lệ!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocus();
            return;
        }

        if (pass.isEmpty()) {
            pass = "123456";
        }

        if (name.isEmpty()) {
            name = email.split("@")[0];
        }

        if (host.isEmpty()) {
            host = "localhost";
        }

        int smtpP = 2525;
        int pop3P = 1110;
        try {
            smtpP = Integer.parseInt(txtSmtpPort.getText().trim());
            pop3P = Integer.parseInt(txtPop3Port.getText().trim());
        } catch (NumberFormatException ignored) {}

        // Lưu cấu hình
        AppConfig config = AppConfig.getInstance();
        config.setUserEmail(email);
        config.setUserAppPassword(pass);
        config.setDisplayName(name);
        config.setServerHost(host);
        config.setServerSmtpPort(smtpP);
        config.setServerPop3Port(pop3P);
        config.save();

        loginSuccessful = true;
        dispose();
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }

    private void showAppPasswordGuide() {
        String guide = "TẠI SAO GẶP LỖI 'TÙY CHỌN KHÔNG KHẢ DỤNG' (HÌNH CON ROBOT VỠ)?\n\n" +
                "1. NGUYÊN NHÂN CHÍNH:\n" +
                "   Tài khoản Gmail của bạn CHƯA BẬT 'Xác minh 2 bước' (2-Step Verification)!\n" +
                "   Google chỉ cho phép tạo Mật khẩu ứng dụng sau khi tài khoản đã bật Xác minh 2 bước.\n\n" +
                "2. CÁCH KHẮC PHỤC NGAY (CHỈ MẤT 1 PHÚT):\n" +
                "   - Bước 1: Mở trình duyệt và vào link sau để BẬT Xác minh 2 bước:\n" +
                "     https://myaccount.google.com/signinoptions/two-step-verification\n" +
                "   - Bước 2: Bấm 'Bắt đầu', nhập số điện thoại và mã OTP gửi về SMS để bật.\n" +
                "   - Bước 3: Sau khi bật xong, vào lại link:\n" +
                "     https://myaccount.google.com/apppasswords\n" +
                "     -> Lúc này HÌNH ROBOT VỠ SẼ BIẾN MẤT!\n" +
                "   - Bước 4: Nhập tên bất kỳ (ví dụ: 'MailClient') rồi bấm 'Tạo'. Google sẽ cấp cho bạn 16 chữ cái màu vàng!\n\n" +
                "3. LƯU Ý QUAN TRỌNG:\n" +
                "   - Nếu bạn dùng email trường học / cơ quan (@edu.vn), nhà trường đã khóa tính năng này. Hãy đổi sang dùng tài khoản Gmail cá nhân bình thường (@gmail.com).\n" +
                "   - Nếu chưa kịp lấy 16 ký tự, bạn vẫn có thể nhập mật khẩu bất kỳ (ví dụ '123456') để đăng nhập và gửi nhận tin nhắn giữa các máy qua Mail Server bình thường!";
        JTextArea area = new JTextArea(guide);
        area.setRows(18);
        area.setColumns(50);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Hướng dẫn sửa lỗi Robot vỡ & Lấy Google App Password", JOptionPane.INFORMATION_MESSAGE);
    }
}

