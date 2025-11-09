package com.example.chatbot2;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    private List<ChatMessage> messageList;
    private ChatAdapter chatAdapter;

    // Gemini components
    private GenerativeModelFutures geminiModel;
    private ChatFutures geminiChat;
    private Executor backgroundExecutor;

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

        // --- Setup Chat & Gemini ---
        setupChatRecyclerView();
        setupGemini();

        // --- Setup Input Button Listeners ---
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessage(message, null, ChatMessage.Sender.USER); // null for image
                messageInput.setText("");
                callGeminiApi(message, null); // null for image
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
        addMessage("Hi there! How can I help you today?", null, ChatMessage.Sender.BOT);
    }

    private void setupGemini() {
        // Create an executor for background tasks
        backgroundExecutor = Executors.newSingleThreadExecutor();

        // Build GenerationConfig (optional, but good to have)
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.9f;
        configBuilder.topK = 1;
        configBuilder.topP = 1.0f;

        // Initialize the GenerativeModel
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.GEMINI_API_KEY,
                configBuilder.build()
        );

        // Wrap the model in a Futures-based class for Java-friendly async calls
        geminiModel = GenerativeModelFutures.from(gm);

        // Start a new chat session
        geminiChat = geminiModel.startChat();
        Log.d("GeminiAPI_KeyCheck", "The API Key being used is: [" + BuildConfig.GEMINI_API_KEY + "]");
    }

    private void callGeminiApi(String message, Bitmap image) {
        addLoadingIndicator(); // Show loading bubble

        // Create the content
        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(message);
        if (image != null) {
            contentBuilder.addImage(image);
        }
        Content content = contentBuilder.build();

        // --- Send message to Gemini (async) ---
        ListenableFuture<GenerateContentResponse> responseFuture = geminiChat.sendMessage(content);

        // Add a callback to handle the response
        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                // Get the text from the response
                String responseText = result.getText();

                // Update the UI on the main thread
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    addMessage(responseText, null, ChatMessage.Sender.BOT);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                // Handle the error
                Log.e("GeminiAPI", "Error: " + t.getMessage(), t);
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    addMessage("Sorry, something went wrong. Please try again.", null, ChatMessage.Sender.BOT);
                });
            }
        }, backgroundExecutor);
    }

    private void setupChatRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    // Updated addMessage to handle both text and image
    private void addMessage(String message, Bitmap image, ChatMessage.Sender sender) {
        ChatMessage chatMessage;
        if (image != null) {
            chatMessage = new ChatMessage(message, image, sender);
        } else {
            chatMessage = new ChatMessage(message, sender);
        }
        messageList.add(chatMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1); // Scroll to bottom
    }

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