package com.example.chatbot2;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager; // <-- Add this
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList; // <-- Add this
import java.util.List; // <-- Add this

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;

    // Chat components
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton attachButton;
    private ImageButton micButton;

    // --- Add these ---
    private List<ChatMessage> messageList;
    private ChatAdapter chatAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Initialize Views ---
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        attachButton = findViewById(R.id.attach_button);
        micButton = findViewById(R.id.mic_button);

        // --- Setup Toolbar ---
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        // --- Setup Navigation Drawer ---
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // --- Setup Drawer Item Click Listener ---
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_new_chat) {
                    Toast.makeText(MainActivity.this, "New Chat Clicked", Toast.LENGTH_SHORT).show();
                    // TODO: Logic to clear current chat and start a new one
                } else if (id == R.id.placeholder_chat_1) {
                    Toast.makeText(MainActivity.this, "History Item 1 Clicked", Toast.LENGTH_SHORT).show();
                    // TODO: Logic to load this chat session
                }
                drawerLayout.closeDrawers();
                return true;
            }
        });

        // --- ADD/UPDATE THIS SECTION ---
        setupChatRecyclerView();

        // --- Setup Input Button Listeners ---
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                // Add user message to list
                addMessage(message, ChatMessage.Sender.USER);
                messageInput.setText("");

                // TODO: Call Gemini API
                // For now, let's just add a dummy loading and response
                addLoadingIndicator();

                // (This is where the API call will go)
                // (After API call, remove loading and add bot response)
                // removeLoadingIndicator();
                // addMessage("This is a bot response.", ChatMessage.Sender.BOT);
            }
        });

        attachButton.setOnClickListener(v -> {
            Toast.makeText(this, "Attach Image Clicked", Toast.LENGTH_SHORT).show();
            // TODO: Launch image picker
        });

        micButton.setOnClickListener(v -> {
            Toast.makeText(this, "Mic Clicked", Toast.LENGTH_SHORT).show();
            // TODO: Launch speech-to-text
        });

        // --- Add a welcome message ---
        addMessage("Hi there! How can I help you today?", ChatMessage.Sender.BOT);
    }

    // --- ADD THESE NEW METHODS ---

    private void setupChatRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    // Add a text message to the list
    private void addMessage(String message, ChatMessage.Sender sender) {
        ChatMessage chatMessage = new ChatMessage(message, sender);
        messageList.add(chatMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1); // Scroll to bottom
    }

    // Add an image message to the list
    // We will use this in a later step
    /*
    private void addImageMessage(String message, Bitmap image, ChatMessage.Sender sender) {
        ChatMessage chatMessage = new ChatMessage(message, image, sender);
        messageList.add(chatMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }
    */

    // Add a loading indicator
    private void addLoadingIndicator() {
        ChatMessage loadingMessage = new ChatMessage(ChatMessage.Sender.BOT, true);
        messageList.add(loadingMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

    // Remove the loading indicator
    private void removeLoadingIndicator() {
        if (messageList.isEmpty()) return;

        int lastPosition = messageList.size() - 1;
        ChatMessage lastMessage = messageList.get(lastPosition);

        if (lastMessage.isLoading()) {
            messageList.remove(lastPosition);
            chatAdapter.notifyItemRemoved(lastPosition);
        }
    }

    // Update the last bot message (used for streaming)
    private void updateLastBotMessage(String newText) {
        if (messageList.isEmpty()) return;

        int lastPosition = messageList.size() - 1;
        ChatMessage lastMessage = messageList.get(lastPosition);

        if (lastMessage.getSender() == ChatMessage.Sender.BOT) {
            lastMessage.setLoading(false); // No longer loading
            lastMessage.setMessage(newText);
            chatAdapter.notifyItemChanged(lastPosition);
        }
    }


    // Needed for the drawer toggle
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}