package com.example.projectii;

public class Users {

    String profilepic, mail, userName, password, userId, lastMessage, status;
    Boolean online; // true = user is currently active in the app

    // Empty constructor needed for Firebase
    public Users() { }

    public Users(String userId, String userName, String mail, String password,
                 String profilepic, String status) {
        this.userId = userId;
        this.userName = userName;
        this.mail = mail;
        this.password = password;
        this.profilepic = profilepic;
        this.status = status;
        this.lastMessage = "";
        this.online = false; // default offline
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return userName; }
    public void setUsername(String userName) { this.userName = userName; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProfilepic() { return profilepic; }
    public void setProfilepic(String profilepic) { this.profilepic = profilepic; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    // ===== Online status getter and setter =====
    public Boolean isOnline() { return online != null && online; }
    public void setOnline(boolean online) { this.online = online; }
}