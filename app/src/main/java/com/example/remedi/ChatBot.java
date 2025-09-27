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
    private final String stringAPIKey = "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";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        chatTextView = findViewById(R.id.chatTextView);
        userInput = findViewById(R.id.userInput);
        Button sendButton = findViewById(R.id.sendButton);
        chatScrollView = findViewById(R.id.chatScrollView);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = userInput.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    chatTextView.append("You: " + userMessage + "\n");
                    userInput.setText("");
                    callChatGPT(userMessage);
                }
            }
        });
    }

    private void callChatGPT(String userMessage) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("model", "gpt-4o-mini");
            JSONArray jsonArrayMessage = new JSONArray();
            JSONObject jsonObjectMessage = new JSONObject();
            jsonObjectMessage.put("role", "user");
            jsonObjectMessage.put("content", userMessage);
            jsonArrayMessage.put(jsonObjectMessage);

            jsonObject.put("messages", jsonArrayMessage);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String stringURLEndPoint = "https://api.openai.com/v1/chat/completions";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                stringURLEndPoint, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String reply = response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    chatTextView.append("Bot: " + reply.trim() + "\n\n");

                    // Auto-scroll to bottom
                    chatScrollView.post(() -> chatScrollView.fullScroll(View.FOCUS_DOWN));

                } catch (JSONException e) {
                    chatTextView.append("Bot: [Error parsing response]\n");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                chatTextView.append("Bot: [Error: " + volleyError.toString() + "]\n");
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> mapHeader = new HashMap<>();
                mapHeader.put("Authorization", "Bearer " + stringAPIKey);
                mapHeader.put("Content-Type", "application/json");
                return mapHeader;
            }
        };

        int intTimeoutPeriod = 60000;
        RetryPolicy retryPolicy = new DefaultRetryPolicy(
                intTimeoutPeriod,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(retryPolicy);

        Volley.newRequestQueue(getApplicationContext()).add(jsonObjectRequest);
    }
}