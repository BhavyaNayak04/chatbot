package com.example.chatbot2.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_sessions")
public class ChatSession {

    @PrimaryKey(autoGenerate = true)
    public long sessionId;

    public String title;
    public long timestamp;

    public ChatSession(String title, long timestamp) {
        this.title = title;
        this.timestamp = timestamp;
    }
}