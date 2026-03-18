package com.example.heami.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SetupGoalsActivity extends AppCompatActivity {

    private GridLayout gridGoals;
    private TextView tvCount, btnSkip;
    private MaterialButton btnFinishSetup;
    private ImageButton btnBack;
    private View step1, step2, step3;
    private List<MaterialCardView> selectedGoalCards = new ArrayList<>();
    private final int MAX_GOALS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_goals);

        initViews();
        setupClickListeners();
        updateUI();
    }

    private void initViews() {
        gridGoals = findViewById(R.id.gridGoals);
        tvCount = findViewById(R.id.tvCount);
        btnFinishSetup = findViewById(R.id.btnFinishSetup);
        btnSkip = findViewById(R.id.btnSkipSetup);
        btnBack = findViewById(R.id.btnBack);

        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
    }

    private void setupClickListeners() {
        for (int i = 0; i < gridGoals.getChildCount(); i++) {
            View child = gridGoals.getChildAt(i);
            if (child instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) child;
                card.setOnClickListener(v -> toggleGoalSelection(card));
            }
        }

        btnBack.setOnClickListener(v -> onBackPressed());
        btnSkip.setOnClickListener(v -> navigateToHome());
        btnFinishSetup.setOnClickListener(v -> saveGoalsAndFinish());
    }

    private void toggleGoalSelection(MaterialCardView card) {
        if (selectedGoalCards.contains(card)) {
            selectedGoalCards.remove(card);
            deselectCard(card);
        } else if (selectedGoalCards.size() < MAX_GOALS) {
            selectedGoalCards.add(card);
            selectCard(card);
        } else {
            Toast.makeText(this, "Chỉ chọn tối đa 3 mục tiêu", Toast.LENGTH_SHORT).show();
        }
        updateUI();
    }

    private void selectCard(MaterialCardView card) {
        card.setStrokeColor(Color.parseColor("#E86FA0"));
        card.setStrokeWidth(4);
        card.setCardBackgroundColor(Color.parseColor("#FFF0F5"));
    }

    private void deselectCard(MaterialCardView card) {
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(Color.WHITE);
    }

    private void updateUI() {
        int count = selectedGoalCards.size();
        tvCount.setText(count + "/" + MAX_GOALS);

        step1.setBackgroundColor(count >= 1 ? Color.parseColor("#E86FA0") : Color.parseColor("#E0E0E0"));
        step2.setBackgroundColor(count >= 2 ? Color.parseColor("#E86FA0") : Color.parseColor("#E0E0E0"));
        step3.setBackgroundColor(count >= 3 ? Color.parseColor("#E86FA0") : Color.parseColor("#E0E0E0"));

        boolean hasSelected = count >= 1;
        btnFinishSetup.setEnabled(hasSelected);
        btnFinishSetup.setAlpha(hasSelected ? 1.0f : 0.5f);
    }

    private void saveGoalsAndFinish() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        btnFinishSetup.setEnabled(false);
        btnFinishSetup.setText("Đang lưu...");

        List<String> goalsText = new ArrayList<>();
        for (MaterialCardView card : selectedGoalCards) {
            LinearLayout layout = (LinearLayout) card.getChildAt(0);
            TextView emojiTv = (TextView) layout.getChildAt(0);
            TextView textTv = (TextView) layout.getChildAt(1);
            goalsText.add(emojiTv.getText().toString() + " " + textTv.getText().toString());
        }

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .update("goals", goalsText)
                .addOnSuccessListener(aVoid -> navigateToHome())
                .addOnFailureListener(e -> {
                    btnFinishSetup.setEnabled(true);
                    btnFinishSetup.setText("Hoàn tất");
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}