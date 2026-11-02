package ui;

import config.AppConfig;
import database.EmailDAO;
import protocol.SmtpSender;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ComposeDialog extends JDialog {
    private final Frame owner;
    private final Runnable onSuccessCallback;
    private final SmtpSender smtpSender;

    private JTextField txtTo;
    private JTextField txtSubject;
    private JTextArea txtBody;
    private JCheckBox chkNotifyRealEmail;
    private JButton btnSend;
    private JLabel lblStatus;

    public ComposeDialog(Frame owner, Runnable onSuccessCallback) {
        this(owner, onSuccessCallback, "", "", "");
    }

    public ComposeDialog(Frame owner, Runnable onSuccessCallback, String to, String subject, String body) {
        super(owner, "Thư mới - Soạn tin nhắn", true);
        this.owner = owner;
        this.onSuccessCallback = onSuccessCallback;
        this.smtpSender = new SmtpSender();

        initComponents();
        if (to != null && !to.isEmpty()) txtTo.setText(to);
        if (subject != null && !subject.isEmpty()) txtSubject.setText(subject);
        if (body != null && !body.isEmpty()) txtBody.setText(body);

        setSize(680, 560);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header Panel phong cách Gmail
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(60, 64, 67));
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel lblTitle = new JLabel("Thư mới (Gửi qua Mail Server & Báo về Gmail Thật)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(Color.WHITE);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // 1. From (Read-only)
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        JLabel lblFrom = new JLabel("Người gửi (Từ):");
        lblFrom.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblFrom, gbc);

        gbc.gridx = 1; gbc.weightx = 0.8;
        AppConfig config = AppConfig.getInstance();
        JLabel lblFromVal = new JLabel(config.getDisplayName() + " <" + config.getUserEmail() + ">  [Máy chủ: " + config.getServerHost() + "]");
        lblFromVal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFromVal.setForeground(new Color(24, 90, 188));
        formPanel.add(lblFromVal, gbc);

        // 2. To Field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        JLabel lblTo = new JLabel("Người nhận (To):");
        lblTo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblTo, gbc);

        gbc.gridx = 1; gbc.weightx = 0.8;
        txtTo = new JTextField();
        txtTo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtTo.setToolTipText("Nhập địa chỉ Gmail thật của người nhận (ví dụ: friend@gmail.com)");
        formPanel.add(txtTo, gbc);

        // 3. Subject Field
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2;
        JLabel lblSubj = new JLabel("Tiêu đề:");
        lblSubj.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblSubj, gbc);

        gbc.gridx = 1; gbc.weightx = 0.8;
        txtSubject = new JTextField();
        txtSubject.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtSubject, gbc);

        // 4. Real Email Notification Checkbox
        gbc.gridx = 1; gbc.gridy = 3;
        chkNotifyRealEmail = new JCheckBox("🔔 Gửi kèm thông báo đến hộp thư Gmail thật của người nhận (Để người nhận reo chuông điện thoại)");
        chkNotifyRealEmail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkNotifyRealEmail.setForeground(new Color(15, 120, 50));
        chkNotifyRealEmail.setSelected(true);
        formPanel.add(chkNotifyRealEmail, gbc);

        // 5. Body Field
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel lblBody = new JLabel("Nội dung:");
        lblBody.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(lblBody, gbc);

        gbc.gridx = 1; gbc.weightx = 0.8; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        txtBody = new JTextArea();
        txtBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtBody.setLineWrap(true);
        txtBody.setWrapStyleWord(true);
        JScrollPane scrollBody = new JScrollPane(txtBody);
        scrollBody.setPreferredSize(new Dimension(420, 220));
        formPanel.add(scrollBody, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Bottom Actions Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(10, 15, 12, 15));

        lblStatus = new JLabel("Sẵn sàng gửi tin nhắn.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblStatus.setForeground(new Color(100, 100, 100));
        bottomPanel.add(lblStatus, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.addActionListener(e -> dispose());
        btnPanel.add(btnCancel);

        btnSend = new JButton(" Gửi tin nhắn ✈ ");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.setBackground(new Color(26, 115, 232));
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.addActionListener(this::handleSend);
        btnPanel.add(btnSend);

        bottomPanel.add(btnPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void handleSend(ActionEvent e) {
        String to = txtTo.getText().trim();
        String subject = txtSubject.getText().trim();
        String body = txtBody.getText();
        boolean notifyReal = chkNotifyRealEmail.isSelected();

        if (to.isEmpty() || !to.contains("@")) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập địa chỉ Email người nhận hợp lệ!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            txtTo.requestFocus();
            return;
        }

        if (subject.isEmpty()) {
            subject = "(Không có tiêu đề)";
        }

        final String finalSubject = subject;

        btnSend.setEnabled(false);
        lblStatus.setText("Đang gửi qua Server và phát thông báo Gmail...");
        lblStatus.setForeground(new Color(26, 115, 232));

        SwingWorker<SmtpSender.SendResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SmtpSender.SendResult doInBackground() throws Exception {
                AppConfig cfg = AppConfig.getInstance();
                return smtpSender.sendMessage(
                        cfg.getServerHost(),
                        cfg.getServerSmtpPort(),
                        cfg.getUserEmail(),
                        cfg.getUserAppPassword(),
                        to,
                        finalSubject,
                        body,
                        notifyReal
                );
            }

            @Override
            protected void done() {
                btnSend.setEnabled(true);
                try {
                    SmtpSender.SendResult result = get();
                    StringBuilder msg = new StringBuilder();
                    msg.append("✓ Gửi tin nhắn thành công!\n\n");
                    msg.append("1. Tin nhắn đã được lưu trữ trong CSDL Mail Server (").append(AppConfig.getInstance().getServerHost()).append(").\n");
                    if (result.realEmailNotified) {
                        msg.append("2. ✓ ĐÃ PHÁT THÔNG BÁO THÀNH CÔNG VỀ GMAIL THẬT!\n   (Người nhận ").append(to).append(" sẽ nhận được email và reo chuông điện thoại).");
                    } else {
                        if (result.details != null && result.details.contains("⚠")) {
                            msg.append("2. ").append(result.details.substring(result.details.indexOf("⚠")));
                        } else {
                            msg.append("2. ⚠ Chưa gửi thông báo về Gmail thật (vui lòng kiểm tra Mật khẩu ứng dụng 16 ký tự).");
                        }
                    }

                    JOptionPane.showMessageDialog(ComposeDialog.this, msg.toString(), "Kết quả gửi tin nhắn", JOptionPane.INFORMATION_MESSAGE);
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lblStatus.setText("Lỗi gửi tin: " + cause.getMessage());
                    lblStatus.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(ComposeDialog.this,
                            "Không thể gửi tin nhắn:\n" + cause.getMessage(),
                            "Lỗi gửi tin nhắn", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }
}
