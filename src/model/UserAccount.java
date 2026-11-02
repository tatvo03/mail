package model;

import java.util.Date;

public class UserAccount {
    private long id;
    private String email;
    private String password;
    private String displayName;
    private Date createdAt;

    public UserAccount() {
        this.createdAt = new Date();
    }

    public UserAccount(String email, String password, String displayName) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.createdAt = new Date();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return displayName + " <" + email + ">";
    }
}

