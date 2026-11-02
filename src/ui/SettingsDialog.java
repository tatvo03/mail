package ui;

import config.AppConfig;
import database.DatabaseManager;
import database.UserDAO;
import model.UserAccount;
import protocol.Pop3Receiver;
import protocol.SmtpSender;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class SettingsDialog extends JDialog {
    private final Frame owner;
    private final Runnable onSaveCallback;
    private final UserDAO userDAO;

    // Gmail components
    private JTextField txtGmailAddress;
    private JPasswordField txtGmailAppPassword;
    private JButton btnTestGmail;
    private JLabel lblGmailStatus;

    // Local components
    private JTextField txtLocalSmtpPort;
    private JTextField txtLocalPop3Port;
    private JComboBox<String> comboLocalUsers;
    private JRadioButton radModeLocal;
    private JRadioButton radModeGmail;

    public SettingsDialog(Frame owner, Runnable onSaveCallback) {
        super(owner, "Cài đặt tài khoản & Cấu hình Máy chủ", true);
        this.owner = owner;
        this.onSaveCallback = onSaveCallback;
        this.userDAO = new UserDAO();

        initComponents();
        loadCurrentConfig();
        setSize(600, 560);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // TAB 1: Cấu hình Gmail Thật
        JPanel gmailPanel = new JPanel(new BorderLayout(10, 10));
        gmailPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel gmailForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblGAddress = new JLabel("Địa chỉ Gmail:");
        lblGAddress.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gmailForm.add(lblGAddress, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtGmailAddress = new JTextField();
        txtGmailAddress.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtGmailAddress.setToolTipText("Ví dụ: yourname@gmail.com");
        gmailForm.add(txtGmailAddress, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel lblGPass = new JLabel("Mật khẩu ứng dụng (App Password):");
        lblGPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gmailForm.add(lblGPass, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtGmailAppPassword = new JPasswordField();
        txtGmailAppPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtGmailAppPassword.setToolTipText("Mật khẩu ứng dụng 16 ký tự Google cấp (không phải mật khẩu đăng nhập bình thường)");
        gmailForm.add(txtGmailAppPassword, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        JButton btnGuide = new JButton("📖 Xem hướng dẫn lấy Mật khẩu ứng dụng 16 ký tự");
        btnGuide.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnGuide.setForeground(new Color(26, 115, 232));
        btnGuide.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuide.addActionListener(e -> showAppPasswordGuide());
        gmailForm.add(btnGuide, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        btnTestGmail = new JButton("⚡ Kiểm tra kết nối Gmail (Test SMTP / POP3)");
        btnTestGmail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnTestGmail.addActionListener(e -> testGmailConnection());
        gmailForm.add(btnTestGmail, gbc);

        gbc.gridx = 1; gbc.gridy = 4;
        lblGmailStatus = new JLabel("Trạng thái: Chưa kiểm tra");
        lblGmailStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblGmailStatus.setForeground(Color.GRAY);
        gmailForm.add(lblGmailStatus, gbc);

        gmailPanel.add(gmailForm, BorderLayout.NORTH);

        JTextArea txtNotice = new JTextArea(
                "Lưu ý quan trọng từ Google:\n" +
                "1. Để gửi/nhận qua Gmail thật bằng Java, bạn cần bật Xác minh 2 bước trong tài khoản Google.\n" +
                "2. Sau đó vào 'Bảo mật' -> 'Mật khẩu ứng dụng' để tạo một mật khẩu 16 chữ cái (dán vào ô trên).\n" +
                "3. Khi gửi thư bằng chế độ Gmail Thật, email thật sẽ được chuyển phát tới hộp thư của người nhận!");
        txtNotice.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtNotice.setEditable(false);
        txtNotice.setBackground(new Color(245, 245, 245));
        txtNotice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)));
        gmailPanel.add(txtNotice, BorderLayout.CENTER);

        tabbedPane.addTab("🌐 Cấu hình Gmail Thật", gmailPanel);

        // TAB 2: Cấu hình Localhost Server
        JPanel localPanel = new JPanel(new BorderLayout(10, 10));
        localPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel localForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.fill = GridBagConstraints.HORIZONTAL;
        gbcL.insets = new Insets(8, 8, 8, 8);

        gbcL.gridx = 0; gbcL.gridy = 0; gbcL.weightx = 0.3;
        JLabel lblSmtpPort = new JLabel("Cổng Local SMTP Server:");
        lblSmtpPort.setFont(new Font("Segoe UI", Font.BOLD, 12));
        localForm.add(lblSmtpPort, gbcL);

        gbcL.gridx = 1; gbcL.weightx = 0.7;
        txtLocalSmtpPort = new JTextField("2525");
        txtLocalSmtpPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        localForm.add(txtLocalSmtpPort, gbcL);

        gbcL.gridx = 0; gbcL.gridy = 1; gbcL.weightx = 0.3;
        JLabel lblPop3Port = new JLabel("Cổng Local POP3 Server:");
        lblPop3Port.setFont(new Font("Segoe UI", Font.BOLD, 12));
        localForm.add(lblPop3Port, gbcL);

        gbcL.gridx = 1; gbcL.weightx = 0.7;
        txtLocalPop3Port = new JTextField("1110");
        txtLocalPop3Port.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        localForm.add(txtLocalPop3Port, gbcL);

        gbcL.gridx = 0; gbcL.gridy = 2; gbcL.weightx = 0.3;
        JLabel lblUser = new JLabel("Tài khoản Local hiện tại:");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        localForm.add(lblUser, gbcL);

        gbcL.gridx = 1; gbcL.weightx = 0.7;
        JPanel userSelectPanel = new JPanel(new BorderLayout(5, 0));
        comboLocalUsers = new JComboBox<>();
        comboLocalUsers.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loadUsersCombo();
        userSelectPanel.add(comboLocalUsers, BorderLayout.CENTER);

        JButton btnNewUser = new JButton("+ Tạo user");
        btnNewUser.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnNewUser.addActionListener(e -> showCreateUserDialog());
        userSelectPanel.add(btnNewUser, BorderLayout.EAST);
        localForm.add(userSelectPanel, gbcL);

        localPanel.add(localForm, BorderLayout.NORTH);

        JTextArea txtLocalInfo = new JTextArea(
                "Thông tin Máy chủ Localhost:\n" +
                "- Máy chủ SMTP chạy trên 127.0.0.1:2525, hỗ trợ các lệnh RFC 5321 (HELO, MAIL FROM, RCPT TO, DATA...).\n" +
                "- Máy chủ POP3 chạy trên 127.0.0.1:1110, hỗ trợ các lệnh RFC 1939 (USER, PASS, STAT, LIST, RETR...).\n" +
                "- Toàn bộ email gửi qua localhost được lưu trực tiếp vào CSDL SQLite (mail_server.db).");
        txtLocalInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtLocalInfo.setEditable(false);
        txtLocalInfo.setBackground(new Color(245, 245, 245));
        txtLocalInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(10, 10, 10, 10)));
        localPanel.add(txtLocalInfo, BorderLayout.CENTER);

        tabbedPane.addTab("💻 Cấu hình Localhost Server", localPanel);

        // TAB 3: Chế độ hoạt động chính
        JPanel modePanel = new JPanel(new GridLayout(2, 1, 10, 10));
        modePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        ButtonGroup group = new ButtonGroup();
        radModeLocal = new JRadioButton("Chế độ Localhost (Sử dụng máy chủ nội bộ :2525 & CSDL SQLite)");
        radModeLocal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        radModeGmail = new JRadioButton("Chế độ Gmail Thật (Kết nối smtp.gmail.com và pop.gmail.com)");
        radModeGmail.setFont(new Font("Segoe UI", Font.BOLD, 13));

        group.add(radModeLocal);
        group.add(radModeGmail);

        modePanel.add(radModeLocal);
        modePanel.add(radModeGmail);

        tabbedPane.addTab("⚙️ Chế độ mặc định", modePanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.addActionListener(e -> dispose());
        actionPanel.add(btnCancel);

        JButton btnSave = new JButton("Lưu cấu hình");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(new Color(26, 115, 232));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> saveConfig());
        actionPanel.add(btnSave);

        add(actionPanel, BorderLayout.SOUTH);
    }

    private void loadCurrentConfig() {
        AppConfig config = AppConfig.getInstance();
        txtGmailAddress.setText(config.getGmailAddress());
        txtGmailAppPassword.setText(config.getGmailAppPassword());
        txtLocalSmtpPort.setText(String.valueOf(config.getLocalSmtpPort()));
        txtLocalPop3Port.setText(String.valueOf(config.getLocalPop3Port()));

        if ("GMAIL".equalsIgnoreCase(config.getActiveMode())) {
            radModeGmail.setSelected(true);
        } else {
            radModeLocal.setSelected(true);
        }

        // Select current local user in combo
        for (int i = 0; i < comboLocalUsers.getItemCount(); i++) {
            if (comboLocalUsers.getItemAt(i).contains(config.getCurrentUserEmail())) {
                comboLocalUsers.setSelectedIndex(i);
                break;
            }
        }
    }

    private void loadUsersCombo() {
        comboLocalUsers.removeAllItems();
        List<UserAccount> users = userDAO.getAllUsers();
        for (UserAccount u : users) {
            comboLocalUsers.addItem(u.getDisplayName() + " (" + u.getEmail() + ")");
        }
    }

    private void showCreateUserDialog() {
        JTextField txtEmail = new JTextField();
        JTextField txtName = new JTextField();
        JPasswordField txtPass = new JPasswordField("123456");

        JPanel p = new JPanel(new GridLayout(3, 2, 5, 5));
        p.add(new JLabel("Email (e.g. user@localhost):"));
        p.add(txtEmail);
        p.add(new JLabel("Họ tên hiển thị:"));
        p.add(txtName);
        p.add(new JLabel("Mật khẩu:"));
        p.add(txtPass);

        int res = JOptionPane.showConfirmDialog(this, p, "Tạo tài khoản Local mới", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String email = txtEmail.getText().trim();
            String name = txtName.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            if (email.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!email.contains("@")) {
                email += "@localhost";
            }
            if (userDAO.createUser(email, pass, name)) {
                JOptionPane.showMessageDialog(this, "Đã tạo tài khoản thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadUsersCombo();
                comboLocalUsers.setSelectedItem(name + " (" + email + ")");
            } else {
                JOptionPane.showMessageDialog(this, "Tài khoản email đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void testGmailConnection() {
        String email = txtGmailAddress.getText().trim();
        String pass = new String(txtGmailAppPassword.getPassword()).trim();

        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Gmail và Mật khẩu ứng dụng!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnTestGmail.setEnabled(false);
        lblGmailStatus.setText("Đang kiểm tra kết nối tới Google...");
        lblGmailStatus.setForeground(new Color(26, 115, 232));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Test POP3 SSL connection
                Pop3Receiver receiver = new Pop3Receiver();
                receiver.fetchFromGmail(email, pass, 1);
                return null;
            }

            @Override
            protected void done() {
                btnTestGmail.setEnabled(true);
                try {
                    get();
                    lblGmailStatus.setText("✓ Kết nối Gmail thành công!");
                    lblGmailStatus.setForeground(new Color(34, 139, 34));
                    JOptionPane.showMessageDialog(SettingsDialog.this,
                            "Kết nối máy chủ Gmail thành công!\nBạn đã có thể gửi và nhận email thật từ ứng dụng.",
                            "Kiểm tra thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lblGmailStatus.setText("✗ Kết nối thất bại: " + cause.getMessage());
                    lblGmailStatus.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(SettingsDialog.this,
                            "Không thể kết nối Gmail:\n" + cause.getMessage() +
                            "\n\nLưu ý: Hãy chắc chắn bạn sử dụng Mật khẩu ứng dụng (App Password) 16 ký tự, không phải mật khẩu đăng nhập thông thường.",
                            "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showAppPasswordGuide() {
        String guide = "CÁCH LẤY MẬT KHẨU ỨNG DỤNG GOOGLE (APP PASSWORD):\n\n" +
                "1. Truy cập https://myaccount.google.com/ và đăng nhập Gmail của bạn.\n" +
                "2. Vào mục 'Bảo mật' (Security).\n" +
                "3. Bật 'Xác minh 2 bước' (2-Step Verification) nếu chưa bật.\n" +
                "4. Tìm kiếm từ khóa 'Mật khẩu ứng dụng' (App passwords) trên thanh tìm kiếm hoặc vào:\n" +
                "   https://myaccount.google.com/apppasswords\n" +
                "5. Nhập tên ứng dụng bất kỳ (ví dụ: 'JavaMailClient') rồi bấm 'Tạo' (Create).\n" +
                "6. Google sẽ cung cấp 1 mật khẩu gồm 16 ký tự (ví dụ: abcd efgh ijkl mnop).\n" +
                "7. Copy 16 ký tự đó dán vào ô 'Mật khẩu ứng dụng' trong ứng dụng này.\n\n" +
                "Sau khi lưu, ứng dụng sẽ có toàn quyền gửi và nhận thư thật qua hộp thư Gmail của bạn!";
        JTextArea area = new JTextArea(guide);
        area.setRows(14);
        area.setColumns(45);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Hướng dẫn lấy Google App Password", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveConfig() {
        try {
            AppConfig config = AppConfig.getInstance();
            config.setGmailAddress(txtGmailAddress.getText().trim());
            config.setGmailAppPassword(new String(txtGmailAppPassword.getPassword()).trim());

            config.setLocalSmtpPort(Integer.parseInt(txtLocalSmtpPort.getText().trim()));
            config.setLocalPop3Port(Integer.parseInt(txtLocalPop3Port.getText().trim()));

            config.setActiveMode(radModeGmail.isSelected() ? "GMAIL" : "LOCALHOST");

            if (comboLocalUsers.getSelectedItem() != null) {
                String selected = (String) comboLocalUsers.getSelectedItem();
                int start = selected.indexOf('(');
                int end = selected.indexOf(')');
                if (start != -1 && end != -1) {
                    String email = selected.substring(start + 1, end).trim();
                    String name = selected.substring(0, start).trim();
                    config.setCurrentUserEmail(email);
                    config.setCurrentDisplayName(name);
                }
            }

            config.save();
            JOptionPane.showMessageDialog(this, "Đã lưu cấu hình thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi lưu cấu hình: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}

