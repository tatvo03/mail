package protocol;

import config.AppConfig;
import database.DatabaseManager;
import database.EmailDAO;
import model.EmailMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Pop3Receiver {
    private final EmailDAO emailDAO;

    public Pop3Receiver() {
        this.emailDAO = new EmailDAO();
    }

    /**
     * Tải thư trực tiếp từ Gmail qua giao thức POP3 SSL (pop.gmail.com:995)
     */
    public int fetchFromGmail(String email, String appPassword, int maxMessages) throws Exception {
        if (email == null || email.trim().isEmpty() || appPassword == null || appPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng cấu hình Địa chỉ Gmail và Mật khẩu ứng dụng trong Cài đặt!");
        }

        String cleanPassword = appPassword.replaceAll("\\s+", "");

        java.util.Properties props = new java.util.Properties();
        props.put("mail.pop3.host", "pop.gmail.com");
        props.put("mail.pop3.port", "995");
        props.put("mail.pop3.starttls.enable", "true");
        props.put("mail.pop3.ssl.enable", "true");
        props.put("mail.pop3.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.pop3.ssl.trust", "pop.gmail.com");
        props.put("mail.pop3.connectiontimeout", "10000");
        props.put("mail.pop3.timeout", "10000");

        javax.mail.Session session = javax.mail.Session.getInstance(props);
        javax.mail.Store store = session.getStore("pop3s");
        store.connect("pop.gmail.com", email.trim(), cleanPassword);

        javax.mail.Folder inbox = store.getFolder("INBOX");
        inbox.open(javax.mail.Folder.READ_ONLY);

        int totalMessages = inbox.getMessageCount();
        inbox.close(false);
        store.close();
        return totalMessages;
    }

    /**
     * Tải thư từ Mail Server qua giao thức POP3 Socket (:1110)
     */
    public int fetchFromServer(String serverHost, int pop3Port, String email, String password) throws Exception {
        int fetchedCount = 0;
        try (Socket socket = new Socket(serverHost, pop3Port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            socket.setSoTimeout(10000);

            // Đọc +OK từ POP3 Server
            String response = reader.readLine();
            if (response == null || !response.startsWith("+OK")) {
                throw new Exception("Mail Server POP3 không sẵn sàng: " + response);
            }

            // Gửi USER
            writer.println("USER " + email.trim());
            response = reader.readLine();
            if (response == null || !response.startsWith("+OK")) {
                throw new Exception("Lỗi USER POP3: " + response);
            }

            // Gửi PASS
            writer.println("PASS " + password);
            response = reader.readLine();
            if (response == null || !response.startsWith("+OK")) {
                throw new Exception("Lỗi xác thực mật khẩu POP3: " + response);
            }

            // Gửi STAT để lấy số lượng thư
            writer.println("STAT");
            response = reader.readLine();
            if (response == null || !response.startsWith("+OK")) {
                throw new Exception("Lỗi STAT: " + response);
            }

            String[] parts = response.split("\\s+");
            int totalMsg = 0;
            if (parts.length >= 2) {
                try {
                    totalMsg = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
            }

            // Lấy từng thư qua lệnh RETR
            for (int i = 1; i <= totalMsg; i++) {
                writer.println("RETR " + i);
                String retrResp = reader.readLine();
                if (retrResp != null && retrResp.startsWith("+OK")) {
                    StringBuilder rawMsg = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.equals(".")) break;
                        rawMsg.append(line).append("\n");
                    }

                    // Parse header and body
                    EmailMessage msg = parseRfcMessage(rawMsg.toString(), email);
                    if (msg != null && !emailDAO.emailExists(email, msg.getMessageId())) {
                        emailDAO.insertEmail(msg);
                        fetchedCount++;
                    }
                }
            }

            // Gửi QUIT
            writer.println("QUIT");
            reader.readLine();

            DatabaseManager.getInstance().logServerEvent("POP3_CLIENT", serverHost + ":" + pop3Port, "FETCH_OK",
                    "Đã tải " + fetchedCount + " tin nhắn mới từ Mail Server cho " + email);
        }
        return fetchedCount;
    }

    private EmailMessage parseRfcMessage(String raw, String ownerEmail) {
        String subject = "(Không có tiêu đề)";
        String sender = "unknown";
        String recipient = ownerEmail;
        String messageId = "pop3-" + System.currentTimeMillis();
        Date sentDate = new Date();
        String body = raw;

        int headerEndIdx = raw.indexOf("\n\n");
        if (headerEndIdx != -1) {
            String headerSection = raw.substring(0, headerEndIdx);
            body = raw.substring(headerEndIdx + 2);

            String[] headers = headerSection.split("\n");
            for (String h : headers) {
                String lower = h.toLowerCase();
                if (lower.startsWith("subject:")) {
                    subject = h.substring(8).trim();
                } else if (lower.startsWith("from:")) {
                    sender = h.substring(5).trim();
                } else if (lower.startsWith("to:")) {
                    recipient = h.substring(3).trim();
                } else if (lower.startsWith("message-id:")) {
                    messageId = h.substring(11).replace("<", "").replace(">", "").trim();
                }
            }
        }

        EmailMessage msg = new EmailMessage();
        msg.setMessageId(messageId);
        msg.setSender(sender);
        msg.setRecipient(recipient);
        msg.setSubject(subject);
        msg.setBody(body);
        msg.setSentAt(sentDate);
        msg.setRead(false);
        msg.setFolder("INBOX");
        msg.setProtocol("POP3_SERVER");
        msg.setOwnerEmail(ownerEmail);
        return msg;
    }
}
