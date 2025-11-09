package com.example.chatbot2.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface ChatDao {

    // --- Chat Session Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSession(ChatSession session); // Returns the new session ID

    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    List<ChatSession> getAllSessions();

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId")
    ChatSession getSessionById(long sessionId);

    @Query("UPDATE chat_sessions SET title = :title WHERE sessionId = :sessionId")
    void updateSessionTitle(long sessionId, String title);

    // --- Chat Message Operations ---

    @Insert
    void insertMessage(ChatMessageEntity message);

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<ChatMessageEntity> getMessagesForSession(long sessionId);

}