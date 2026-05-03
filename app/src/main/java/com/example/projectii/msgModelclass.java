package com.example.projectii;

// Model class for each chat message
public class msgModelclass {

    // ===== Variables =====
    String message;      // actual text message
    String senderid;     // UID of sender
    Long timeStamp;      // timestamp of message
    String messageId;    // Firebase unique key (needed for edit/delete)
    boolean isPhoto;     // true if message is a photo
    boolean isViewOnce;  // true if photo is view once
    boolean isViewed;    // true if view once photo has been viewed

    // Empty constructor REQUIRED by Firebase
    public msgModelclass() { }

    // Constructor for text messages
    public msgModelclass(String message, String senderid, Long timeStamp) {
        this.message = message;
        this.senderid = senderid;
        this.timeStamp = timeStamp;
        this.isPhoto = false;
        this.isViewOnce = false;
        this.isViewed = false;
    }

    // Constructor for photo messages
    public msgModelclass(String message, String senderid, Long timeStamp, boolean isPhoto, boolean isViewOnce) {
        this.message = message;   // Base64 image string
        this.senderid = senderid;
        this.timeStamp = timeStamp;
        this.isPhoto = isPhoto;
        this.isViewOnce = isViewOnce;
        this.isViewed = false;
    }

    // ===== Getters & Setters =====
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSenderid() { return senderid; }
    public void setSenderid(String senderid) { this.senderid = senderid; }

    public Long getTimeStamp() { return timeStamp; }
    public void setTimeStamp(Long timeStamp) { this.timeStamp = timeStamp; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public boolean isPhoto() { return isPhoto; }
    public void setPhoto(boolean photo) { isPhoto = photo; }

    public boolean isViewOnce() { return isViewOnce; }
    public void setViewOnce(boolean viewOnce) { isViewOnce = viewOnce; }

    public boolean isViewed() { return isViewed; }
    public void setViewed(boolean viewed) { isViewed = viewed; }
}