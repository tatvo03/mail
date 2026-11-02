package protocol;

import config.AppConfig;
import database.DatabaseManager;
import database.EmailDAO;
import model.EmailMessage;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

public class SmtpSender {
    private final EmailDAO emailDAO;

    public SmtpSender() {
        this.emailDAO = new EmailDAO();
    }

    /**
     * Gửi tin nhắn giữa các Client qua Mail Server (Socket SMTP)
     * ĐỒNG THỜI tự động gửi một email thông báo thật sự đến hòm thư Gmail của người nhận!
     *
     * @param serverHost Địa chỉ IP của máy chủ Mail Server (ví dụ 192.168.1.15 hoặc localhost)
     * @param serverPort Cổng SMTP của Mail Server (mặc định 2525)
     * @param fromEmail  Gmail thật của người gửi
     * @param appPassword Mật khẩu ứng dụng 16 ký tự của người gửi
     * @param toEmail    Gmail thật của người nhận
     * @param subject    Tiêu đề tin nhắn
     * @param body       Nội dung tin nhắn
     * @param notifyRealEmail Có gửi thông báo tới Gmail thật hay không (mặc định true)
     */
    public SendResult sendMessage(String serverHost, int serverPort, String fromEmail, String appPassword,
                                  String toEmail, String subject, String body, boolean notifyRealEmail) throws Exception {

        boolean serverSaved = false;
        boolean realEmailNotified = false;
        StringBuilder logDetails = new StringBuilder();

        // 1. GỬI TIN NHẮN TỚI MAIL SERVER QUA SOCKET SMTP (:2525)
        try {
            sendViaServerSmtpSocket(serverHost, serverPort, fromEmail, toEmail, subject, body);
            serverSaved = true;
            logDetails.append("✓ Đã lưu tin nhắn vào CSDL của Server (").append(serverHost).append(":").append(serverPort).append(")\n");
        } catch (Exception e) {
            logDetails.append("✗ Lỗi gửi tới Mail Server: ").append(e.getMessage()).append("\n");
            throw new Exception("Không thể kết nối tới Mail Server tại " + serverHost + ":" + serverPort + ": " + e.getMessage());
        }

        // 2. GỬI THÔNG BÁO TỚI HỘP THƯ GMAIL THẬT CỦA NGƯỜI NHẬN QUA GOOGLE SMTP (:587)
        if (notifyRealEmail) {
            String senderPass = appPassword != null ? appPassword.trim() : "";
            String senderGmail = fromEmail != null ? fromEmail.trim() : "";

            // Nếu client dùng mật khẩu mặc định (123456), kiểm tra xem Server có cấu hình Relay Email trung tâm không
            if (("123456".equals(senderPass) || senderPass.isEmpty()) && AppConfig.getInstance().hasServerRelayCredentials()) {
                senderGmail = AppConfig.getInstance().getServerRelayEmail();
                senderPass = AppConfig.getInstance().getServerRelayAppPassword();
            }

            if (senderPass.isEmpty() || "123456".equals(senderPass)) {
                logDetails.append("⚠ Chưa gửi thông báo về Gmail thật do mật khẩu đang để '123456' (mặc định).\nGoogle yêu cầu Mật khẩu ứng dụng 16 ký tự để cấp quyền gửi email thật.");
            } else {
                try {
                    sendRealGmailNotification(senderGmail, senderPass, toEmail, subject, body, serverHost);
                    realEmailNotified = true;
                    logDetails.append("✓ Đã gửi thông báo thành công tới hộp thư Gmail thật của ").append(toEmail);
                } catch (Throwable ex) {
                    String err = ex.getMessage();
                    if (err != null && err.contains("535-5.7.8")) {
                        err = "Google từ chối mật khẩu (Cần sử dụng Mật khẩu ứng dụng 16 ký tự do Google cấp, không phải mật khẩu bình thường).";
                    }
                    logDetails.append("⚠ Không thể gửi thông báo Gmail thật: ").append(err);
                    System.err.println("Lỗi gửi thông báo Gmail thật: " + ex.getMessage());
                }
            }
        }

        return new SendResult(serverSaved, realEmailNotified, logDetails.toString());
    }

    /**
     * Gửi qua Socket SMTP tới Mail Server (RFC 5321)
     */
    public void sendViaServerSmtpSocket(String host, int port, String fromEmail, String toEmail, String subject, String body) throws Exception {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            socket.setSoTimeout(10000);

            // Đọc lời chào của server (220)
            String response = readSmtpResponse(reader);
            if (!response.startsWith("220")) {
                throw new Exception("Máy chủ SMTP không phản hồi sẵn sàng: " + response);
            }

            // Gửi HELO
            writer.println("HELO client");
            response = readSmtpResponse(reader);
            if (!response.startsWith("250")) {
                throw new Exception("Lỗi HELO: " + response);
            }

            // Gửi MAIL FROM
            writer.println("MAIL FROM:<" + fromEmail.trim() + ">");
            response = readSmtpResponse(reader);
            if (!response.startsWith("250")) {
                throw new Exception("Lỗi MAIL FROM: " + response);
            }

            // Gửi RCPT TO
            writer.println("RCPT TO:<" + toEmail.trim() + ">");
            response = readSmtpResponse(reader);
            if (!response.startsWith("250")) {
                throw new Exception("Lỗi RCPT TO: " + response);
            }

            // Gửi DATA
            writer.println("DATA");
            response = readSmtpResponse(reader);
            if (!response.startsWith("354")) {
                throw new Exception("Lỗi DATA: " + response);
            }

            // Gửi Nội dung Email với các Header RFC chuẩn
            SimpleDateFormat rfcDateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
            writer.println("From: " + fromEmail.trim());
            writer.println("To: " + toEmail.trim());
            writer.println("Subject: " + subject);
            writer.println("Date: " + rfcDateFormat.format(new Date()));
            writer.println("MIME-Version: 1.0");
            writer.println("Content-Type: text/plain; charset=UTF-8");
            writer.println();
            writer.println(body);
            writer.println("."); // Kết thúc DATA theo RFC 5321

            response = readSmtpResponse(reader);
            if (!response.startsWith("250")) {
                throw new Exception("Lỗi khi gửi nội dung thư: " + response);
            }

            // Gửi QUIT
            writer.println("QUIT");
            readSmtpResponse(reader);

            // Lưu bản sao vào thư mục SENT của người gửi
            EmailMessage sentMsg = new EmailMessage();
            sentMsg.setMessageId("msg-" + System.currentTimeMillis());
            sentMsg.setSender(fromEmail.trim());
            sentMsg.setRecipient(toEmail.trim());
            sentMsg.setSubject(subject);
            sentMsg.setBody(body);
            sentMsg.setSentAt(new Date());
            sentMsg.setRead(true);
            sentMsg.setFolder("SENT");
            sentMsg.setProtocol("SMTP_SERVER");
            sentMsg.setOwnerEmail(fromEmail.trim());
            emailDAO.insertEmail(sentMsg);
        }
    }

    /**
     * Gửi email thông báo thật qua máy chủ Google (smtp.gmail.com:587) đến Gmail của người nhận
     */
    public void sendRealGmailNotification(String fromEmail, String appPassword, String toEmail,
                                          String subject, String body, String serverHost) throws Exception {

        final String cleanPassword = appPassword.replaceAll("\\s+", "");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail.trim(), cleanPassword);
            }
        });

        String notificationSubject = "[Thông báo Mail Server] " + subject;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

        String notificationBody =
                "Xin chào " + toEmail + ",\n\n" +
                "Bạn vừa nhận được một tin nhắn mới từ người dùng: " + fromEmail + " trên Hệ thống Mail Server!\n\n" +
                "======================================================================\n" +
                " THÔNG TIN TIN NHẮN TỪ MAIL SERVER\n" +
                "======================================================================\n" +
                "• Người gửi: " + fromEmail + " (" + AppConfig.getInstance().getDisplayName() + ")\n" +
                "• Người nhận: " + toEmail + "\n" +
                "• Tiêu đề: " + subject + "\n" +
                "• Thời gian: " + sdf.format(new Date()) + "\n" +
                "• Máy chủ gửi: " + serverHost + "\n\n" +
                "Nội dung tin nhắn:\n" +
                "----------------------------------------------------------------------\n" +
                body + "\n" +
                "----------------------------------------------------------------------\n\n" +
                "Tin nhắn này đã được lưu trữ trong Cơ sở dữ liệu của Mail Server.\n" +
                "Bạn có thể mở ứng dụng Java Mail Client trên máy tính của bạn và nhấn 'Nhận mail (POP3)' để xem và trả lời.\n\n" +
                "Trân trọng,\nHệ thống Mail Server Java";

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail.trim(), AppConfig.getInstance().getDisplayName(), "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail.trim()));
        message.setSubject(notificationSubject, "UTF-8");
        message.setText(notificationBody, "UTF-8");
        message.setSentDate(new Date());

        Transport.send(message);

        DatabaseManager.getInstance().logServerEvent("SMTP_GMAIL_NOTIFY", "smtp.gmail.com", "NOTIFY_OK",
                "Đã phát thông báo email thật tới " + toEmail + " cho tin nhắn: " + subject);
    }

    private String readSmtpResponse(BufferedReader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            if (line.length() >= 4 && line.charAt(3) == '-') {
                sb.append("\n");
                continue;
            }
            break;
        }
        return sb.toString().trim();
    }

    public static class SendResult {
        public final boolean serverSaved;
        public final boolean realEmailNotified;
        public final String details;

        public SendResult(boolean serverSaved, boolean realEmailNotified, String details) {
            this.serverSaved = serverSaved;
            this.realEmailNotified = realEmailNotified;
            this.details = details;
        }
    }
}
