package com.example.remedi;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChatBot extends AppCompatActivity {
    private TextView chatTextView;
    private EditText userInput;
    private ScrollView chatScrollView;

    // ⚠️ Replace this with your API key
    private final String stringAPIKey = "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"; // use valid key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        chatTextView = findViewById(R.id.chatTextView);
        userInput = findViewById(R.id.userInput);
        Button sendButton = findViewById(R.id.sendButton);
        chatScrollView = findViewById(R.id.chatScrollView);

        sendButton.setOnClickListener(v -> {
            String userMessage = userInput.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                chatTextView.append("You: " + userMessage + "\n");
                userInput.setText("");
                callChatGPT(userMessage);
            }
        });
    }

    private void callChatGPT(String userMessage) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-4o-mini"); // ✅ changed to 4.0 mini
            JSONArray messagesArray = new JSONArray();
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            messageObject.put("content", userMessage);
            messagesArray.put(messageObject);
            jsonObject.put("messages", messagesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "https://api.openai.com/v1/chat/completions";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                response -> {
                    try {
                        String reply = response.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        chatTextView.append("Bot: " + reply.trim() + "\n\n");
                        chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));
                    } catch (JSONException e) {
                        chatTextView.append("Bot: [Error parsing response]\n");
                    }
                },
                error -> chatTextView.append("Bot: [Error: " + error.toString() + "]\n")
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + stringAPIKey.trim());
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Increase timeout to 90 seconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(
                90000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
        request.setRetryPolicy(retryPolicy);

        Volley.newRequestQueue(getApplicationContext()).add(request);
    }
}
