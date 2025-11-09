package com.example.chatbot2.db;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.room.TypeConverter;

import com.example.chatbot2.ChatMessage;

import java.io.ByteArrayOutputStream;

public class Converters {

    // --- Bitmap <-> Byte Array ---

    @TypeConverter
    public static byte[] fromBitmap(Bitmap bitmap) { // <-- ADD STATIC
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    @TypeConverter
    public static Bitmap toBitmap(byte[] bytes) { // <-- ADD STATIC
        if (bytes == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // --- Sender Enum <-> String ---

    @TypeConverter
    public static String fromSender(ChatMessage.Sender sender) { // <-- ADD STATIC
        if (sender == null) {
            return null;
        }
        return sender.name(); // Converts "Sender.USER" to "USER"
    }

    @TypeConverter
    public static ChatMessage.Sender toSender(String senderString) { // <-- ADD STATIC
        if (senderString == null) {
            return null;
        }
        return ChatMessage.Sender.valueOf(senderString); // Converts "USER" to Sender.USER
    }
}