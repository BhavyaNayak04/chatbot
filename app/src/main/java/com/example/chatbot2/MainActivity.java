package com.example.chatbot2;

import android.Manifest; // <-- Add
import android.content.Intent; // <-- Add
import android.content.pm.PackageManager; // <-- Add
import android.graphics.Bitmap;
import android.net.Uri; // <-- Add
import android.os.Build; // <-- Add
import android.os.Bundle;
import android.provider.MediaStore; // <-- Add
import android.speech.RecognizerIntent; // <-- Add
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // <-- Add
import androidx.activity.result.contract.ActivityResultContracts; // <-- Add
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat; // <-- Add
import androidx.core.content.ContextCompat; // <-- Add
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.io.IOException; // <-- Add
import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // <-- Add
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

    // --- New Global Variables ---
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Bitmap selectedImageBitmap; // To hold the image
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> speechToTextLauncher;


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
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_new_chat) {
                // TODO: Logic to clear current chat and start a new one
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // --- Setup Chat & Gemini ---
        setupChatRecyclerView();
        setupGemini();

        // --- ADD THESE ---
        initActivityLaunchers();
        requestPermissions();

        // --- Setup Input Button Listeners (UPDATED) ---
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();

            // Check if there is either text or an image
            if (!message.isEmpty() || selectedImageBitmap != null) {
                // Add user message to list
                addMessage(message, selectedImageBitmap, ChatMessage.Sender.USER);

                // Call Gemini
                callGeminiApi(message, selectedImageBitmap);

                // Clear input and image
                messageInput.setText("");
                selectedImageBitmap = null;
                // TODO: (Optional) Hide an image preview if you add one
            }
        });

        attachButton.setOnClickListener(v -> {
            openGallery();
        });

        micButton.setOnClickListener(v -> {
            openSpeechToText();
        });

        // --- Add a welcome message ---
        addMessage("Hi there! How can I help you today?", null, ChatMessage.Sender.BOT);
    }

    // --- ADD ALL THESE NEW METHODS ---

    /**
     * Initializes the ActivityResultLaunchers for image picking and speech-to-text.
     */
    private void initActivityLaunchers() {
        // Launcher for picking an image from the gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            // Convert Uri to Bitmap
                            selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            // Show a toast or update UI to show a preview
                            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                            // TODO: (Optional) Show a small thumbnail preview above the EditText
                        } catch (IOException e) {
                            Log.e("ImagePicker", "Error converting Uri to Bitmap", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Launcher for speech-to-text
        speechToTextLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> speechResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (speechResults != null && !speechResults.isEmpty()) {
                            // Set the recognized text into the input field
                            messageInput.setText(speechResults.get(0));
                            messageInput.setSelection(messageInput.length()); // Move cursor to end
                        }
                    }
                }
        );
    }

    /**
     * Launches an Intent to pick an image from the gallery.
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Launches the speech-to-text recognizer.
     */
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


    /**
     * Checks and requests necessary permissions.
     */
    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        // Image permission (depends on Android version)
        String imagePermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            imagePermission = Manifest.permission.READ_MEDIA_IMAGES;
        } else { // API 32 and below
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

    /**
     * Handles the result of the permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all requested permissions were granted
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied. Some features may not work.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Log.d("Permissions", "All permissions granted.");
        }
    }

    // --- Unchanged Methods from Part 3 ---

    private void setupGemini() {
        // Create an executor for background tasks
        backgroundExecutor = Executors.newSingleThreadExecutor();
        Log.d("GeminiAPI_KeyCheck", "The API Key being used is: [" + BuildConfig.GEMINI_API_KEY + "]");

        // Build GenerationConfig (optional, but good to have)
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.9f;
        configBuilder.topK = 1;
        configBuilder.topP = 1.0f;

        // Initialize the GenerativeModel
        GenerativeModel gm = new GenerativeModel(
                "gemini-2.5-flash", // Correct model
                BuildConfig.GEMINI_API_KEY,
                configBuilder.build()
        );

        geminiModel = GenerativeModelFutures.from(gm);
        geminiChat = geminiModel.startChat();
    }

    private void callGeminiApi(String message, Bitmap image) {
        addLoadingIndicator(); // Show loading bubble

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

    private void setupChatRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
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
    }

    private void addLoadingIndicator() {
        ChatMessage loadingMessage = new ChatMessage(ChatMessage.Sender.BOT, true);
        messageList.add(loadingMessage);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
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

    private void updateLastBotMessage(String newText) {
        // ... (This method isn't used right now, but good to keep for streaming)
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}