package model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EmailMessage {
    private long id;
    private String messageId;
    private String sender;
    private String recipient;
    private String subject;
    private String body;
    private Date sentAt;
    private boolean isRead;
    private boolean isStarred;
    private String folder;      // INBOX, SENT, TRASH
    private String protocol;    // SMTP_GMAIL, SMTP_LOCAL, POP3_GMAIL, POP3_LOCAL
    private String ownerEmail;

    public EmailMessage() {
        this.sentAt = new Date();
        this.isRead = false;
        this.isStarred = false;
        this.folder = "INBOX";
        this.protocol = "LOCAL";
    }

    public EmailMessage(String sender, String recipient, String subject, String body, String folder, String protocol, String ownerEmail) {
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.sentAt = new Date();
        this.isRead = false;
        this.isStarred = false;
        this.folder = folder;
        this.protocol = protocol;
        this.ownerEmail = ownerEmail;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isStarred() { return isStarred; }
    public void setStarred(boolean starred) { isStarred = starred; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getFormattedDate() {
        if (sentAt == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(sentAt);
    }

    public String getShortDate() {
        if (sentAt == null) return "";
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM");
        SimpleDateFormat sdfToday = new SimpleDateFormat("HH:mm");
        SimpleDateFormat fullSdf = new SimpleDateFormat("yyyyMMdd");
        
        if (fullSdf.format(sentAt).equals(fullSdf.format(new Date()))) {
            return sdfToday.format(sentAt);
        } else {
            return sdfDate.format(sentAt);
        }
    }

    public String getSnippet(int maxLength) {
        if (body == null || body.trim().isEmpty()) return "(Không có nội dung)";
        String clean = body.replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxLength) return clean;
        return clean.substring(0, maxLength) + "...";
    }
}

