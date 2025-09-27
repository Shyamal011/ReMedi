package com.example.remedi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedAdapter extends RecyclerView.Adapter<MedAdapter.MedViewHolder> {

    private final List<Map<String, Object>> meds;

    public MedAdapter(List<Map<String, Object>> meds) {
        this.meds = meds;
    }

    @NonNull
    @Override
    public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.each_med, parent, false);
        return new MedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
        Map<String, Object> med = meds.get(position);
        holder.name.setText((String) med.get("name"));
        holder.time.setText((String) med.get("time"));
        holder.checkbox.setChecked(Boolean.TRUE.equals(med.get("taken")));

        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            med.put("taken", isChecked);

            FirebaseFirestore.getInstance()
                    .collection("reminders")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .set(new HashMap<String, Object>() {{
                        put("meds", meds);
                    }}, SetOptions.merge())
                    .addOnFailureListener(e ->
                            Toast.makeText(buttonView.getContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                    );
        });
    }

    @Override
    public int getItemCount() {
        return meds.size();
    }

    public static class MedViewHolder extends RecyclerView.ViewHolder {
        TextView name, time;
        CheckBox checkbox;

        public MedViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.medName);
            time = itemView.findViewById(R.id.medTime);
            checkbox = itemView.findViewById(R.id.medCheckbox);
        }
    }
}
