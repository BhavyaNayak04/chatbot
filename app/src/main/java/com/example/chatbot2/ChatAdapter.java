package com.example.chatbot2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private List<ChatMessage> chatMessages;

    // --- ADD THIS INTERFACE ---
    private BotMessageListener listener;
    public interface BotMessageListener {
        void onSpeakClicked(String text);
        void onCopyClicked(String text);
    }
    // -------------------------

    // --- UPDATE THE CONSTRUCTOR ---
    public ChatAdapter(List<ChatMessage> chatMessages, BotMessageListener listener) {
        this.chatMessages = chatMessages;
        this.listener = listener; // Initialize the listener
    }
    // -------------------------

    // --- ViewHolder Classes ---

    // ViewHolder for User messages (Unchanged)
    public static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView image;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.user_message_text);
            image = itemView.findViewById(R.id.user_image);
        }
    }

    // --- UPDATE Bot ViewHolder ---
    public static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView image;
        ProgressBar loadingIndicator;
        LinearLayout botActionsLayout; // The new layout
        ImageButton speakButton;
        ImageButton copyButton;

        public BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.bot_message_text);
            image = itemView.findViewById(R.id.bot_image);
            loadingIndicator = itemView.findViewById(R.id.loading_indicator);
            // Find the new views
            botActionsLayout = itemView.findViewById(R.id.bot_actions_layout);
            speakButton = itemView.findViewById(R.id.speak_button);
            copyButton = itemView.findViewById(R.id.copy_button);
        }
    }
    // -------------------------

    // --- Adapter Methods ---

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        if (message.getSender() == ChatMessage.Sender.USER) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_BOT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        } else { // VIEW_TYPE_BOT
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bot_message, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    // --- UPDATE onBindViewHolder ---
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);

        if (getItemViewType(position) == VIEW_TYPE_USER) {
            // ... (User holder logic is unchanged)
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                userHolder.messageText.setText(message.getMessage());
                userHolder.messageText.setVisibility(View.VISIBLE);
            } else {
                userHolder.messageText.setVisibility(View.GONE);
            }
            if (message.getImage() != null) {
                userHolder.image.setImageBitmap(message.getImage());
                userHolder.image.setVisibility(View.VISIBLE);
            } else {
                userHolder.image.setVisibility(View.GONE);
            }

        } else { // VIEW_TYPE_BOT
            BotMessageViewHolder botHolder = (BotMessageViewHolder) holder;

            // Handle loading state
            if (message.isLoading()) {
                botHolder.loadingIndicator.setVisibility(View.VISIBLE);
                botHolder.messageText.setVisibility(View.GONE);
                botHolder.image.setVisibility(View.GONE);
                botHolder.botActionsLayout.setVisibility(View.GONE); // Hide buttons when loading
            } else {
                botHolder.loadingIndicator.setVisibility(View.GONE);

                // Show buttons and text only if there is text
                if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                    botHolder.messageText.setText(message.getMessage());
                    botHolder.messageText.setVisibility(View.VISIBLE);
                    botHolder.botActionsLayout.setVisibility(View.VISIBLE); // Show buttons

                    // Set click listeners
                    botHolder.speakButton.setOnClickListener(v ->
                            listener.onSpeakClicked(message.getMessage())
                    );
                    botHolder.copyButton.setOnClickListener(v ->
                            listener.onCopyClicked(message.getMessage())
                    );

                } else {
                    botHolder.messageText.setVisibility(View.GONE);
                    botHolder.botActionsLayout.setVisibility(View.GONE); // Hide buttons if no text
                }

                // Handle image (unchanged)
                if (message.getImage() != null) {
                    Glide.with(botHolder.itemView.getContext())
                            .load(message.getImage())
                            .into(botHolder.image);
                    botHolder.image.setVisibility(View.VISIBLE);
                } else {
                    botHolder.image.setVisibility(View.GONE);
                }
            }
        }
    }
    // -------------------------

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
}