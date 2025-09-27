package com.example.remedi;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMedication extends AppCompatActivity {

    private EditText medNameInput, totalQtyInput, dosageQtyInput, timeInput;
    private Button addButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        medNameInput = findViewById(R.id.medNameInput);
        totalQtyInput = findViewById(R.id.totalQtyInput);
        dosageQtyInput = findViewById(R.id.dosageQtyInput);
        timeInput = findViewById(R.id.timeInput);
        addButton = findViewById(R.id.button3);

        db = FirebaseFirestore.getInstance();

        // 1. Setup timeInput to show the TimePickerDialog when clicked
        // Make the EditText non-editable but clickable to act like a button
        timeInput.setFocusable(false);
        timeInput.setClickable(true);
        timeInput.setOnClickListener(v -> showTimePickerDialog());

        addButton.setOnClickListener(v -> addMedication());
    }

    /**
     * Shows a TimePickerDialog to allow the user to select the medication time.
     */
    private void showTimePickerDialog() {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    // This is the callback when the user clicks 'OK'
                    // Format the time as HH:mm (e.g., "09:05" or "14:30")
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                    timeInput.setText(formattedTime);
                }, hour, minute,
                true); // 'true' means 24-hour format (better for medical reminders)

        timePickerDialog.show();
    }

    private void addMedication() {
        String name = medNameInput.getText().toString().trim();
        String totalQty = totalQtyInput.getText().toString().trim();
        String dosageQty = dosageQtyInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();

        if (name.isEmpty() || totalQty.isEmpty() || dosageQty.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String patientUID = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous_user";

        // Generate a unique notification ID using the current timestamp
        int notificationId = (int) System.currentTimeMillis();

        Map<String, Object> med = new HashMap<>();
        med.put("notificationId", notificationId);
        med.put("name", name);
        med.put("totalQty", totalQty);
        med.put("dosageQty", dosageQty);
        med.put("time", time);
        med.put("taken", false);

        db.collection("reminders")
                .document(patientUID)
                .update("meds", FieldValue.arrayUnion(med))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Medication added", Toast.LENGTH_SHORT).show();
                    // FIX: Explicitly hide the keyboard before finishing the activity
                    hideKeyboardAndFinish();
                })
                .addOnFailureListener(e -> {
                    // Document doesn't exist, create a new one
                    ArrayList<Map<String, Object>> medArray = new ArrayList<>();
                    medArray.add(med);

                    Map<String, Object> newDoc = new HashMap<>();
                    newDoc.put("meds", medArray);

                    db.collection("reminders")
                            .document(patientUID)
                            .set(newDoc)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Medication added", Toast.LENGTH_SHORT).show();
                                // FIX: Explicitly hide the keyboard before finishing the activity
                                hideKeyboardAndFinish();
                            })
                            .addOnFailureListener(err ->
                                    Toast.makeText(this, "Error: " + err.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                });


    }

    /**
     * Helper method to hide the soft keyboard and then close the activity.
     * This prevents warnings about input connections when the activity closes.
     */
    private void hideKeyboardAndFinish() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        finish();
    }
}
