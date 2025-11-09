package com.example.chatbot2.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages",
        foreignKeys = @ForeignKey(entity = ChatSession.class,
                parentColumns = "sessionId",
                childColumns = "sessionId",
                onDelete = ForeignKey.CASCADE))
public class ChatMessageEntity {

    @PrimaryKey(autoGenerate = true)
    public long messageId;

    public long sessionId; // Foreign key to link to ChatSession

    public String message;
    public byte[] image; // Store bitmap as byte array
    public String sender; // Store enum as String ("USER" or "BOT")
    public long timestamp;

    // Constructor
    public ChatMessageEntity(long sessionId, String message, byte[] image, String sender, long timestamp) {
        this.sessionId = sessionId;
        this.message = message;
        this.image = image;
        this.sender = sender;
        this.timestamp = timestamp;
    }
}