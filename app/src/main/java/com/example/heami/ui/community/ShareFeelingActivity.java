package com.example.heami.ui.community;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

public class ShareFeelingActivity extends AppCompatActivity {

    private View layoutShareFeelingSheet;
    private Button btnSubmitAnonymous;

    private View cardMoodHappy;
    private View cardMoodSad;
    private View cardMoodStress;
    private View cardMoodFear;
    private View cardMoodDisgust;
    private View cardMoodAngry;

    private TextView txtMoodHappy;
    private TextView txtMoodSad;
    private TextView txtMoodStress;
    private TextView txtMoodFear;
    private TextView txtMoodDisgust;
    private TextView txtMoodAngry;

    private EditText edtShareFeeling;
    private String selectedMood = "happy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_feeling);

        bindViews();
        prepareSheetIntro();
        setupActions();
        setupMoodSelection();
        setupInputWatcher();
        updateSubmitButtonState();
        startSheetIntroAnimation();
    }

    private void bindViews() {
        layoutShareFeelingSheet = findViewById(R.id.layoutShareFeelingSheet);
        btnSubmitAnonymous = findViewById(R.id.btnSubmitAnonymous);

        cardMoodHappy = findViewById(R.id.cardMoodHappy);
        cardMoodSad = findViewById(R.id.cardMoodSad);
        cardMoodStress = findViewById(R.id.cardMoodStress);
        cardMoodFear = findViewById(R.id.cardMoodFear);
        cardMoodDisgust = findViewById(R.id.cardMoodDisgust);
        cardMoodAngry = findViewById(R.id.cardMoodAngry);

        txtMoodHappy = findViewById(R.id.txtMoodHappy);
        txtMoodSad = findViewById(R.id.txtMoodSad);
        txtMoodStress = findViewById(R.id.txtMoodStress);
        txtMoodFear = findViewById(R.id.txtMoodFear);
        txtMoodDisgust = findViewById(R.id.txtMoodDisgust);
        txtMoodAngry = findViewById(R.id.txtMoodAngry);

        edtShareFeeling = findViewById(R.id.edtShareFeeling);
    }

    private void prepareSheetIntro() {
        if (layoutShareFeelingSheet != null) {
            layoutShareFeelingSheet.setTranslationY(1400f);
            layoutShareFeelingSheet.setAlpha(1f);
        }
    }

    private void startSheetIntroAnimation() {
        if (layoutShareFeelingSheet != null) {
            layoutShareFeelingSheet.animate()
                    .translationY(0f)
                    .setDuration(420)
                    .start();
        }
    }

    private void setupActions() {
        if (btnSubmitAnonymous != null) {
            btnSubmitAnonymous.setOnClickListener(v -> submitAnonymousPost());
        }

        View root = findViewById(R.id.shareFeelingRoot);
        if (root != null) {
            root.setOnClickListener(v -> finish());
        }

        if (layoutShareFeelingSheet != null) {
            layoutShareFeelingSheet.setOnClickListener(v -> {
                // chặn click xuyên xuống nền ngoài
            });
        }
    }

    private void setupMoodSelection() {
        if (cardMoodHappy != null) {
            cardMoodHappy.setOnClickListener(v -> setActiveMood("happy"));
        }
        if (cardMoodSad != null) {
            cardMoodSad.setOnClickListener(v -> setActiveMood("sad"));
        }
        if (cardMoodStress != null) {
            cardMoodStress.setOnClickListener(v -> setActiveMood("stress"));
        }
        if (cardMoodFear != null) {
            cardMoodFear.setOnClickListener(v -> setActiveMood("fear"));
        }
        if (cardMoodDisgust != null) {
            cardMoodDisgust.setOnClickListener(v -> setActiveMood("disgust"));
        }
        if (cardMoodAngry != null) {
            cardMoodAngry.setOnClickListener(v -> setActiveMood("angry"));
        }

        setActiveMood("happy");
    }

    private void setActiveMood(String mood) {
        selectedMood = mood;
        resetAllMoods();

        switch (mood) {
            case "happy":
                setMoodActive(cardMoodHappy, txtMoodHappy, R.drawable.bg_share_mood_happy_active, 0xFFE0A03D);
                break;
            case "sad":
                setMoodActive(cardMoodSad, txtMoodSad, R.drawable.bg_share_mood_sad_active, 0xFF9EB8E8);
                break;
            case "stress":
                setMoodActive(cardMoodStress, txtMoodStress, R.drawable.bg_share_mood_stress_active, 0xFFB79AC9);
                break;
            case "fear":
                setMoodActive(cardMoodFear, txtMoodFear, R.drawable.bg_share_mood_fear_active, 0xFF9EC7C4);
                break;
            case "disgust":
                setMoodActive(cardMoodDisgust, txtMoodDisgust, R.drawable.bg_share_mood_disgust_active, 0xFFA2BB8F);
                break;
            case "angry":
                setMoodActive(cardMoodAngry, txtMoodAngry, R.drawable.bg_share_mood_angry_active, 0xFFE49797);
                break;
        }
        updateSubmitButtonState();
    }

    private void resetAllMoods() {
        setMoodInactive(cardMoodHappy, txtMoodHappy, 0xFFE0A03D);
        setMoodInactive(cardMoodSad, txtMoodSad, 0xFF9EB8E8);
        setMoodInactive(cardMoodStress, txtMoodStress, 0xFFB79AC9);
        setMoodInactive(cardMoodFear, txtMoodFear, 0xFF9EC7C4);
        setMoodInactive(cardMoodDisgust, txtMoodDisgust, 0xFFA2BB8F);
        setMoodInactive(cardMoodAngry, txtMoodAngry, 0xFFE49797);
    }

    private void setMoodActive(View card, TextView label, int bgRes, int textColor) {
        if (card != null) {
            card.setBackgroundResource(bgRes);
        }
        if (label != null) {
            label.setTextColor(textColor);
        }
    }

    private void setMoodInactive(View card, TextView label, int textColor) {
        if (card != null) {
            card.setBackgroundResource(R.drawable.bg_share_mood_default);
        }
        if (label != null) {
            label.setTextColor(textColor);
        }
    }

    private void setupInputWatcher() {
        if (edtShareFeeling == null) return;

        edtShareFeeling.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSubmitButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void updateSubmitButtonState() {
        if (btnSubmitAnonymous == null) return;

        boolean hasMood = selectedMood != null && !selectedMood.isEmpty();
        boolean hasContent = edtShareFeeling != null
                && edtShareFeeling.getText() != null
                && !edtShareFeeling.getText().toString().trim().isEmpty();

        boolean isEnabled = hasMood && hasContent;

        btnSubmitAnonymous.setEnabled(isEnabled);

        if (isEnabled) {
            btnSubmitAnonymous.setBackgroundResource(R.drawable.bg_share_feeling_submit_active);
            btnSubmitAnonymous.setTextColor(0xFFFFFFFF);
            btnSubmitAnonymous.setAlpha(1f);
        } else {
            btnSubmitAnonymous.setBackgroundResource(R.drawable.bg_share_feeling_submit);
            btnSubmitAnonymous.setTextColor(0xFFB39CCC);
            btnSubmitAnonymous.setAlpha(1f);
        }
    }

    private void submitAnonymousPost() {
        if (edtShareFeeling == null) return;

        String content = edtShareFeeling.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Hãy viết điều bạn muốn chia sẻ nhé", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(ShareFeelingActivity.this, CommunityActivity.class);
        intent.putExtra("new_post_content", content);
        intent.putExtra("new_post_mood", selectedMood);
        intent.putExtra("from_share_feeling", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}