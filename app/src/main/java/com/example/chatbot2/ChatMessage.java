package com.example.chatbot2;

import android.graphics.Bitmap;

public class ChatMessage {

    // Enum to differentiate between user and bot messages
    public enum Sender {
        USER,
        BOT
    }

    private String message;
    private Bitmap image; // For user/bot to send an image
    private Sender sender;
    private boolean isLoading; // To show a loading indicator for bot response

    // Constructor for text message
    public ChatMessage(String message, Sender sender) {
        this.message = message;
        this.sender = sender;
        this.image = null;
        this.isLoading = false;
    }

    // Constructor for image message
    public ChatMessage(String message, Bitmap image, Sender sender) {
        this.message = message;
        this.image = image;
        this.sender = sender;
        this.isLoading = false;
    }

    // Constructor for loading message
    public ChatMessage(Sender sender, boolean isLoading) {
        this.message = "Typing..."; // Placeholder text
        this.sender = sender;
        this.image = null;
        this.isLoading = isLoading;
    }


    // --- Getters and Setters ---

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Sender getSender() {
        return sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }
}