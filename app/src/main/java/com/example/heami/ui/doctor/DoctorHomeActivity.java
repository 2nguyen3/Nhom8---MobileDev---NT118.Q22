package com.example.heami.ui.doctor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.data.models.DoctorHomeSummaryModel;
import com.example.heami.data.repositories.DoctorHomeRepository;
import com.example.heami.utils.ExitDialogHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;

public class DoctorHomeActivity extends AppCompatActivity {

    private TextView txtDoctorGreetingTime;
    private TextView txtDoctorGreetingTitle;
    private ImageView imgDoctorAvatar;

    private TextView txtDoctorTodaySessions;
    private TextView txtDoctorPendingSessions;
    private TextView txtDoctorOngoingSessions;
    private TextView txtDoctorUnreadMessages;

    private TextView btnDoctorSeeAll;
    private LinearLayout layoutDoctorUpcomingContainer;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private DoctorHomeRepository doctorHomeRepository;

    private ListenerRegistration doctorHomeListener;
    private String currentDoctorId = "doc_001";
    private LinearLayout layoutDoctorAttentionContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        doctorHomeRepository = new DoctorHomeRepository();

        resolveCurrentDoctorId();
        initViews();
        setupActions();
        loadDoctorProfile();

        DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_OVERVIEW);
        ExitDialogHelper.registerExitHandler(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startDoctorHomeListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopDoctorHomeListener();
    }

    private void resolveCurrentDoctorId() {
        SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);

        if (isDoctor) {
            currentDoctorId = safeText(prefs.getString("doctor_id", "doc_001"), "doc_001");
            return;
        }

        if (auth.getCurrentUser() != null) {
            currentDoctorId = safeText(auth.getCurrentUser().getUid(), "doc_001");
        } else {
            currentDoctorId = "doc_001";
        }
    }

    private void initViews() {
        txtDoctorGreetingTime = findViewById(R.id.txtDoctorGreetingTime);
        txtDoctorGreetingTitle = findViewById(R.id.txtDoctorGreetingTitle);
        imgDoctorAvatar = findViewById(R.id.imgDoctorAvatar);

        txtDoctorTodaySessions = findViewById(R.id.txtDoctorTodaySessions);
        txtDoctorPendingSessions = findViewById(R.id.txtDoctorPendingSessions);
        txtDoctorOngoingSessions = findViewById(R.id.txtDoctorOngoingSessions);
        txtDoctorUnreadMessages = findViewById(R.id.txtDoctorUnreadMessages);

        btnDoctorSeeAll = findViewById(R.id.btnDoctorSeeAll);
        layoutDoctorUpcomingContainer = findViewById(R.id.layoutDoctorUpcomingContainer);
        layoutDoctorAttentionContainer = findViewById(R.id.layoutDoctorAttentionContainer);
    }

    private void setupActions() {
        updateTimeGreeting();

        if (btnDoctorSeeAll != null) {
            btnDoctorSeeAll.setOnClickListener(v -> {
                Intent intent = new Intent(this, DoctorScheduleManagementActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void loadDoctorProfile() {
        firestore.collection("doctors")
                .document(currentDoctorId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("full_name");
                        String avatarUrl = documentSnapshot.getString("avatar_url");

                        txtDoctorGreetingTitle.setText(
                                fullName != null && !fullName.trim().isEmpty()
                                        ? fullName.trim()
                                        : "Bác sĩ Heami"
                        );

                        if (avatarUrl != null && !avatarUrl.trim().isEmpty() && imgDoctorAvatar != null) {
                            Glide.with(DoctorHomeActivity.this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.img_doctor_1)
                                    .error(R.drawable.img_doctor_1)
                                    .into(imgDoctorAvatar);
                        }
                    } else {
                        txtDoctorGreetingTitle.setText("Bác sĩ Heami");
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    txtDoctorGreetingTitle.setText("Bác sĩ Heami");
                });
    }

    private void startDoctorHomeListener() {
        stopDoctorHomeListener();

        doctorHomeListener = doctorHomeRepository.observeDoctorHomeSummary(
                currentDoctorId,
                new DoctorHomeRepository.DoctorHomeCallback() {
                    @Override
                    public void onChanged(@NonNull DoctorHomeSummaryModel summary) {
                        bindSummary(summary);
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        bindSummary(new DoctorHomeSummaryModel());
                    }
                }
        );
    }

    private void stopDoctorHomeListener() {
        if (doctorHomeListener != null) {
            doctorHomeListener.remove();
            doctorHomeListener = null;
        }
    }

    private void bindSummary(@NonNull DoctorHomeSummaryModel summary) {
        if (txtDoctorTodaySessions != null) {
            txtDoctorTodaySessions.setText(summary.getTodaySessions() + " phiên");
        }

        if (txtDoctorPendingSessions != null) {
            txtDoctorPendingSessions.setText(summary.getPendingSessions() + " phiên");
        }

        if (txtDoctorOngoingSessions != null) {
            txtDoctorOngoingSessions.setText(summary.getOngoingSessions() + " phiên");
        }

        if (txtDoctorUnreadMessages != null) {
            txtDoctorUnreadMessages.setText(summary.getUnreadMessages() + " tin");
        }

        renderUpcomingSessions(summary);
        renderAttentionPatients(summary);
    }

    private void renderUpcomingSessions(@NonNull DoctorHomeSummaryModel summary) {
        if (layoutDoctorUpcomingContainer == null) {
            return;
        }

        layoutDoctorUpcomingContainer.removeAllViews();

        if (summary.getUpcomingSessions() == null || summary.getUpcomingSessions().isEmpty()) {
            layoutDoctorUpcomingContainer.addView(createEmptyStateView(
                    "Hôm nay chưa có lịch sắp tới",
                    "Khi người dùng đặt lịch tư vấn, phiên gần nhất sẽ hiển thị ở đây."
            ));
            return;
        }

        for (DoctorHomeSummaryModel.UpcomingSessionItem item : summary.getUpcomingSessions()) {
            layoutDoctorUpcomingContainer.addView(createUpcomingSessionView(item));
        }
    }

    private void renderAttentionPatients(@NonNull DoctorHomeSummaryModel summary) {
        if (layoutDoctorAttentionContainer == null) {
            return;
        }

        layoutDoctorAttentionContainer.removeAllViews();

        if (summary.getAttentionPatients() == null || summary.getAttentionPatients().isEmpty()) {
            layoutDoctorAttentionContainer.addView(createEmptyStateView(
                    "Chưa có bệnh nhân cần chú ý",
                    "Các bệnh nhân có mood tiêu cực hoặc năng lượng thấp sẽ hiển thị tại đây."
            ));
            return;
        }

        for (DoctorHomeSummaryModel.AttentionPatientItem item : summary.getAttentionPatients()) {
            layoutDoctorAttentionContainer.addView(createAttentionPatientView(item));
        }
    }

    @NonNull
    private View createAttentionPatientView(@NonNull DoctorHomeSummaryModel.AttentionPatientItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(resolveAttentionCardBg(item.getPriority()));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(resolveAttentionDotBg(item.getPriority()));

        LinearLayout infoBox = new LinearLayout(this);
        infoBox.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        infoParams.setMargins(dp(14), 0, dp(10), 0);
        infoBox.setLayoutParams(infoParams);

        TextView name = new TextView(this);
        name.setText(item.getAvatarEmoji() + " " + safeText(item.getPatientName(), "Bệnh nhân Heami"));
        name.setTextColor(android.graphics.Color.parseColor("#1A2530"));
        name.setTextSize(15);
        name.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView reason = new TextView(this);
        reason.setText(safeText(item.getReasonText(), "Cần theo dõi trạng thái gần đây"));
        reason.setTextColor(android.graphics.Color.parseColor("#7F8C8D"));
        reason.setTextSize(13);
        reason.setPadding(0, dp(2), 0, 0);

        infoBox.addView(name);
        infoBox.addView(reason);

        TextView badge = new TextView(this);
        badge.setText(item.getPriority() >= 3 ? "Ưu tiên" : "Theo dõi");
        badge.setTextColor(android.graphics.Color.parseColor(item.getPriority() >= 3 ? "#FF5A5F" : "#D97706"));
        badge.setTextSize(12);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setBackgroundResource(item.getPriority() >= 3
                ? R.drawable.bg_attention_card_pink
                : R.drawable.bg_attention_card_yellow);
        badge.setPadding(dp(10), dp(6), dp(10), dp(6));

        card.addView(dot);
        card.addView(infoBox);
        card.addView(badge);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctorPatientDetailActivity.class);
            intent.putExtra("patient_user_id", item.getUserId());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        return card;
    }

    private int resolveAttentionCardBg(int priority) {
        if (priority >= 3) {
            return R.drawable.bg_attention_card_pink;
        }

        return R.drawable.bg_attention_card_yellow;
    }

    private int resolveAttentionDotBg(int priority) {
        if (priority >= 3) {
            return R.drawable.bg_dot_pink_small;
        }

        return R.drawable.bg_dot_yellow_small;
    }

    @NonNull
    private View createUpcomingSessionView(@NonNull DoctorHomeSummaryModel.UpcomingSessionItem item) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(dp(16));
        cardView.setCardElevation(0);
        cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
        cardView.setUseCompatPadding(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));

        FrameLayout avatarBox = new FrameLayout(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatarBox.setLayoutParams(avatarParams);
        avatarBox.setBackgroundResource(resolveAvatarBg(item.getFormatType()));

        TextView emoji = new TextView(this);
        emoji.setText(safeText(item.getAvatarEmoji(), "😊"));
        emoji.setTextSize(22);
        FrameLayout.LayoutParams emojiParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );
        avatarBox.addView(emoji, emojiParams);

        LinearLayout infoBox = new LinearLayout(this);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        infoParams.setMargins(dp(14), 0, dp(10), 0);
        infoBox.setLayoutParams(infoParams);

        TextView name = new TextView(this);
        name.setText(safeText(item.getPatientName(), "Bệnh nhân Heami"));
        name.setTextColor(android.graphics.Color.parseColor("#1A2530"));
        name.setTextSize(15);
        name.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText(safeText(item.getFormatText(), "Phiên tư vấn"));
        subtitle.setTextColor(android.graphics.Color.parseColor("#9EA8B6"));
        subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(2), 0, 0);

        infoBox.addView(name);
        infoBox.addView(subtitle);

        TextView time = new TextView(this);
        time.setText(safeText(item.getTimeText(), "--:--"));
        time.setTextColor(android.graphics.Color.parseColor("#09A38C"));
        time.setTextSize(13);
        time.setTypeface(null, android.graphics.Typeface.BOLD);
        time.setBackgroundResource(R.drawable.bg_stat_icon_green);
        time.setPadding(dp(16), dp(8), dp(16), dp(8));

        row.addView(avatarBox);
        row.addView(infoBox);
        row.addView(time);

        cardView.addView(row);

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, DoctorChatDetailActivity.class);
            intent.putExtra(DoctorMessagesActivity.EXTRA_SESSION_ID, item.getSessionId());
            intent.putExtra(DoctorMessagesActivity.EXTRA_PARTNER_NAME, item.getPatientName());
            intent.putExtra(DoctorMessagesActivity.EXTRA_USER_ID, item.getUserId());
            intent.putExtra(DoctorMessagesActivity.EXTRA_FORMAT_TYPE, item.getFormatType());
            intent.putExtra(DoctorMessagesActivity.EXTRA_ROOM_STATUS, item.getStatus());
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        return cardView;
    }

    @NonNull
    private View createEmptyStateView(@NonNull String title, @NonNull String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(18), dp(20), dp(18), dp(20));
        box.setBackgroundResource(R.drawable.bg_doctor_card);

        TextView txtTitle = new TextView(this);
        txtTitle.setText(title);
        txtTitle.setTextColor(android.graphics.Color.parseColor("#1A2530"));
        txtTitle.setTextSize(15);
        txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        txtTitle.setGravity(Gravity.CENTER);

        TextView txtSubtitle = new TextView(this);
        txtSubtitle.setText(subtitle);
        txtSubtitle.setTextColor(android.graphics.Color.parseColor("#7D8BB7"));
        txtSubtitle.setTextSize(12);
        txtSubtitle.setGravity(Gravity.CENTER);
        txtSubtitle.setPadding(0, dp(6), 0, 0);

        box.addView(txtTitle);
        box.addView(txtSubtitle);

        return box;
    }

    private int resolveAvatarBg(String formatType) {
        String normalized = safeText(formatType, "").toLowerCase();

        if (normalized.contains("call") || normalized.contains("video") || normalized.contains("gọi")) {
            return R.drawable.bg_stat_icon_pink;
        }

        return R.drawable.bg_stat_icon_blue;
    }

    private void updateTimeGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (hour >= 4 && hour < 10) {
            greeting = "Chào buổi sáng";
        } else if (hour >= 10 && hour < 13) {
            greeting = "Chào buổi trưa";
        } else if (hour >= 13 && hour < 18) {
            greeting = "Chào buổi chiều";
        } else {
            greeting = "Chào buổi tối";
        }

        if (txtDoctorGreetingTime != null) {
            txtDoctorGreetingTime.setText(greeting);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @NonNull
    private String safeText(String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }
}