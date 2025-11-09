package com.example.chatbot2;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech; // <-- Add
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// --- DB Imports ---
import com.example.chatbot2.db.AppDatabase;
import com.example.chatbot2.db.ChatMessageEntity;
import com.example.chatbot2.db.ChatSession;
import com.example.chatbot2.db.Converters;
// --------------------

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.android.material.navigation.NavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// --- UPDATE CLASS SIGNATURE ---
public class MainActivity extends AppCompatActivity implements ChatAdapter.BotMessageListener {

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

    // Activity Launchers
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Bitmap selectedImageBitmap;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> speechToTextLauncher;

    // Database variables
    private AppDatabase db;
    private long currentSessionId = -1;
    private Menu navMenu;

    // --- ADD THESE NEW VARIABLES ---
    private TextToSpeech textToSpeech;
    private ClipboardManager clipboardManager;
    // -------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Initialize Views ---
        drawerLayout = findViewById(R.id.drawer_layout);
        // ... (rest of view initializations) ...
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        attachButton = findViewById(R.id.attach_button);
        micButton = findViewById(R.id.mic_button);

        // --- Setup Toolbar ---
        setSupportActionBar(toolbar);
        // ... (rest of toolbar setup) ...
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
        // ... (rest of drawer setup) ...
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navMenu = navigationView.getMenu();
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_new_chat) {
                startNewChat();
            } else {
                loadChatSession(id);
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // --- Setup Chat & Gemini ---
        setupChatRecyclerView(); // This MUST be before setupGemini
        setupGemini();

        // --- Setup Launchers & Permissions ---
        initActivityLaunchers();
        requestPermissions();

        // --- Setup Database ---
        db = AppDatabase.getDatabase(this);
        loadChatHistory();
        startNewChat();

        // --- Setup Input Button Listeners ---
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty() || selectedImageBitmap != null) {
                addMessage(message, selectedImageBitmap, ChatMessage.Sender.USER);
                callGeminiApi(message, selectedImageBitmap);
                messageInput.setText("");
                selectedImageBitmap = null;
            }
        });
        attachButton.setOnClickListener(v -> openGallery());
        micButton.setOnClickListener(v -> openSpeechToText());

        // --- ADD THIS: Initialize TTS and Clipboard ---
        initTextToSpeech();
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // ------------------------------------------
    }

    // --- ADD THIS METHOD ---
    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                    Toast.makeText(this, "TTS Language not supported.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("TTS", "Initialization failed");
                Toast.makeText(this, "TTS Initialization failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- ADD THIS METHOD: Handle TTS Shutdown ---
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    // --- ADD THESE: Implement the Listener Methods ---
    @Override
    public void onSpeakClicked(String text) {
        if (textToSpeech != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onCopyClicked(String text) {
        if (clipboardManager != null && !text.isEmpty()) {
            ClipData clip = ClipData.newPlainText("ChatBot Message", text);
            clipboardManager.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
    // -----------------------------------------------

    // --- UPDATE setupChatRecyclerView ---
    private void setupChatRecyclerView() {
        messageList = new ArrayList<>();
        // Pass 'this' as the listener
        chatAdapter = new ChatAdapter(messageList, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }
    // ------------------------------------


    // --- (All other methods are unchanged) ---

    private void startNewChat() {
        // Clear UI
        messageList.clear();
        chatAdapter.notifyDataSetChanged();

        // Reset session state
        currentSessionId = -1;
        selectedImageBitmap = null;
        messageInput.setText("");

        // Reset Gemini's history
        geminiChat = geminiModel.startChat();

        // Add welcome message (without saving it)
        ChatMessage welcomeMsg = new ChatMessage("Hi there! How can I help you today?", ChatMessage.Sender.BOT);
        messageList.add(welcomeMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
    }

    private void loadChatHistory() {
        backgroundExecutor.execute(() -> {
            List<ChatSession> sessions = db.chatDao().getAllSessions();
            runOnUiThread(() -> {
                try {
                    navMenu.removeGroup(R.id.group_chat_history);

                    for (ChatSession session : sessions) {
                        navMenu.add(R.id.group_chat_history, (int) session.sessionId, Menu.NONE, session.title)
                                .setCheckable(true);
                    }
                } catch (Exception e) {
                    Log.e("ChatHistory", "Error updating chat history menu", e);
                }
            });
        });
    }

    private void loadChatSession(long sessionId) {
        backgroundExecutor.execute(() -> {
            currentSessionId = sessionId;
            List<ChatMessageEntity> entities = db.chatDao().getMessagesForSession(sessionId);

            List<ChatMessage> newMessages = new ArrayList<>();
            List<Content> geminiHistory = new ArrayList<>();

            for (ChatMessageEntity entity : entities) {
                ChatMessage.Sender sender = Converters.toSender(entity.sender);
                Bitmap image = Converters.toBitmap(entity.image);
                newMessages.add(new ChatMessage(entity.message, image, sender));

                Content.Builder contentBuilder = new Content.Builder();
                if (sender == ChatMessage.Sender.USER) {
                    contentBuilder.setRole("user");
                } else {
                    contentBuilder.setRole("model");
                }
                contentBuilder.addText(entity.message);
                if (image != null) {
                    contentBuilder.addImage(image);
                }
                geminiHistory.add(contentBuilder.build());
            }

            geminiChat = geminiModel.startChat(geminiHistory);

            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(newMessages);
                chatAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                }
            });
        });
    }

    private void addMessage(String message, Bitmap image, ChatMessage.Sender sender) {
        ChatMessage chatMessage;
        if (image != null) {
            chatMessage = new ChatMessage(message, image, sender);
        } else {
            chatMessage = new ChatMessage(message, sender);
        }
        messageList.add(chatMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        backgroundExecutor.execute(() -> {
            if (currentSessionId == -1 && sender == ChatMessage.Sender.USER) {
                String title = (message != null && !message.isEmpty()) ? message : "Image query";
                if (title.length() > 30) {
                    title = title.substring(0, 30) + "...";
                }

                ChatSession newSession = new ChatSession(title, System.currentTimeMillis());
                currentSessionId = db.chatDao().insertSession(newSession);

                loadChatHistory();
            }

            byte[] imageBytes = Converters.fromBitmap(image);
            String senderString = Converters.fromSender(sender);
            ChatMessageEntity entity = new ChatMessageEntity(
                    currentSessionId,
                    message,
                    imageBytes,
                    senderString,
                    System.currentTimeMillis()
            );
            db.chatDao().insertMessage(entity);
        });
    }

    private void addLoadingIndicator() {
        ChatMessage loadingMessage = new ChatMessage(ChatMessage.Sender.BOT, true);
        messageList.add(loadingMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void initActivityLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.e("ImagePicker", "Error converting Uri to Bitmap", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        speechToTextLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> speechResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (speechResults != null && !speechResults.isEmpty()) {
                            messageInput.setText(speechResults.get(0));
                            messageInput.setSelection(messageInput.length());
                        }
                    }
                }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void openSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            speechToTextLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Speech-to-text not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }


    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }
        String imagePermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagePermission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            imagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, imagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(imagePermission);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied. Some features may not work.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Log.d("Permissions", "All permissions granted.");
        }
    }

    private void setupGemini() {
        backgroundExecutor = Executors.newSingleThreadExecutor();
        Log.d("GeminiAPI_KeyCheck", "The API Key being used is: [" + BuildConfig.GEMINI_API_KEY + "]");

        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.9f;
        configBuilder.topK = 1;
        configBuilder.topP = 1.0f;

        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash",
                BuildConfig.GEMINI_API_KEY,
                configBuilder.build()
        );

        geminiModel = GenerativeModelFutures.from(gm);
    }

    private void callGeminiApi(String message, Bitmap image) {
        addLoadingIndicator();

        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        contentBuilder.addText(message);
        if (image != null) {
            contentBuilder.addImage(image);
        }
        Content content = contentBuilder.build();

        ListenableFuture<GenerateContentResponse> responseFuture = geminiChat.sendMessage(content);

        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String responseText = result.getText();
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    addMessage(responseText, null, ChatMessage.Sender.BOT);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("GeminiAPI", "Error: " + t.getMessage(), t);
                runOnUiThread(() -> {
                    removeLoadingIndicator();
                    addMessage("Sorry, something went wrong. Please try again.", null, ChatMessage.Sender.BOT);
                });
            }
        }, backgroundExecutor);
    }

    private void removeLoadingIndicator() {
        if (messageList.isEmpty()) return;
        int lastPosition = messageList.size() - 1;
        ChatMessage lastMessage = messageList.get(lastPosition);

        if (lastMessage.isLoading()) {
            messageList.remove(lastPosition);
            chatAdapter.notifyItemRemoved(lastPosition);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}