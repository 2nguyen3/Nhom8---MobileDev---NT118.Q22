package com.example.heami.ui.doctor;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.utils.ExitDialogHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DoctorHomeActivity extends AppCompatActivity {

    private static final String TAG = "DoctorHomeActivity";

    private TextView txtDoctorGreetingTime;
    private TextView txtDoctorGreetingTitle;
    private ImageView imgDoctorAvatar;
    private ImageView decorFlowerPinkTop, decorFlowerMintMid;

    private TextView txtLichHomNay;
    private TextView txtCaDaHoanThanh;
    private TextView txtCaSapDienRa;
    private TextView txtTinNhanMoi;

    private TextView btnDoctorSeeAll;

    private LinearLayout containerUpcoming;
    private LinearLayout containerAttention;

    private FirebaseFirestore db;
    private String currentDoctorId = "DOC_001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupActions();
        startDecorAnimations();

        loadDoctorProfile();
        loadRealtimeStatsAndAppointments();

        DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_OVERVIEW);
        ExitDialogHelper.registerExitHandler(this);
    }

    private void initViews() {
        txtDoctorGreetingTime = findViewById(R.id.txtDoctorGreetingTime);
        txtDoctorGreetingTitle = findViewById(R.id.txtDoctorGreetingTitle);
        imgDoctorAvatar = findViewById(R.id.imgDoctorAvatar);

        decorFlowerPinkTop = findViewById(R.id.decorFlowerPinkTop);
        decorFlowerMintMid = findViewById(R.id.decorFlowerMintMid);

        txtLichHomNay = findViewById(R.id.txtLichHomNay);
        txtCaDaHoanThanh = findViewById(R.id.txtCaDaHoanThanh);
        txtCaSapDienRa = findViewById(R.id.txtCaSapDienRa);
        txtTinNhanMoi = findViewById(R.id.txtTinNhanMoi);

        btnDoctorSeeAll = findViewById(R.id.btnDoctorSeeAll);

        containerUpcoming = findViewById(R.id.containerUpcoming);
        containerAttention = findViewById(R.id.containerAttention);
    }

    private void setupActions() {
        updateTimeGreeting();

        if (btnDoctorSeeAll != null) {
            btnDoctorSeeAll.setOnClickListener(v -> {
                Intent intent = new Intent(DoctorHomeActivity.this, DoctorAppointmentsActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void loadDoctorProfile() {
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        currentDoctorId = prefs.getString("doctor_id", "DOC_001");

        if (currentDoctorId == null || currentDoctorId.isEmpty() || currentDoctorId.equalsIgnoreCase("doc_001")) {
            currentDoctorId = "DOC_001";
        }

        db.collection("doctors").document(currentDoctorId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("full_name");
                        String avatarUrl = documentSnapshot.getString("avatar_url");

                        if (fullName != null && !fullName.isEmpty()) {
                            txtDoctorGreetingTitle.setText(fullName);
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty() && imgDoctorAvatar != null) {
                            Glide.with(DoctorHomeActivity.this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.img_doctor_1)
                                    .into(imgDoctorAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi lấy thông tin bác sĩ: ", e));
    }

    private void loadRealtimeStatsAndAppointments() {
        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.DAY_OF_MONTH, 1);
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        Timestamp startPeriod = new Timestamp(calStart.getTime());

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        Timestamp endPeriod = new Timestamp(calEnd.getTime());

        db.collection("lich_hen")
                .whereEqualTo("doctor_id", currentDoctorId)
                .whereGreaterThanOrEqualTo("start_time", startPeriod)
                .whereLessThanOrEqualTo("start_time", endPeriod)
                .addSnapshotListener((snapshots, e) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (e != null) {
                        Log.e(TAG, "Lỗi Snapshot Lịch hẹn: ", e);
                        return;
                    }
                    if (snapshots == null) return;

                    if (containerUpcoming != null) containerUpcoming.removeAllViews();

                    int countLichHomNay = 0;
                    int countCompleted = 0;
                    int countBooked = 0;

                    SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String todayStr = dayFormat.format(Calendar.getInstance().getTime());

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String status = doc.getString("status");
                        Timestamp startTimeTok = doc.getTimestamp("start_time");

                        String itemDateStr = "";
                        if (startTimeTok != null) {
                            itemDateStr = dayFormat.format(startTimeTok.toDate());
                        }

                        if (todayStr.equals(itemDateStr) && "booked".equalsIgnoreCase(status)) {
                            countLichHomNay++;
                        }

                        if ("completed".equalsIgnoreCase(status)) {
                            countCompleted++;
                        }

                        if ("booked".equalsIgnoreCase(status)) {
                            countBooked++;
                            fetchAndRenderUpcomingCard(doc);
                        }
                    }

                    if (txtLichHomNay != null) txtLichHomNay.setText(countLichHomNay + " ca");
                    if (txtCaDaHoanThanh != null) txtCaDaHoanThanh.setText(countCompleted + " ca");
                    if (txtCaSapDienRa != null) txtCaSapDienRa.setText(countBooked + " ca");
                    if (txtTinNhanMoi != null) txtTinNhanMoi.setText("0 tin");

                    generateAttentionItemProgrammatically();
                });
    }

    private void fetchAndRenderUpcomingCard(QueryDocumentSnapshot slotDoc) {
        final String slotId = slotDoc.getString("slot_id");
        final String sessionId = slotDoc.getString("session_id");

        if (sessionId == null || sessionId.isEmpty()) return;

        db.collection("consultations")
                .document(sessionId)
                .get()
                .addOnSuccessListener(sessionDoc -> {
                    if (isFinishing() || isDestroyed()) return;
                    renderUpcomingCardView(slotDoc, sessionDoc, slotId, sessionId);
                })
                .addOnFailureListener(err -> Log.e(TAG, "Lỗi fetch consultation: " + sessionId, err));
    }

    private void renderUpcomingCardView(QueryDocumentSnapshot slotDoc, DocumentSnapshot sessionDoc, String slotId, String sessionId) {
        if (containerUpcoming == null) return;

        String patientName = "Bệnh nhân Heami";
        String moodText = "Tư vấn";
        String moodEmoji = "🦋";
        String duration = "30 phút";

        if (sessionDoc.exists()) {
            String name = sessionDoc.getString("patient_name");
            if (name != null && !name.isEmpty()) patientName = name;

            String mood = sessionDoc.getString("mood_text");
            if (mood != null && !mood.isEmpty()) moodText = mood;

            String emoji = sessionDoc.getString("mood_emoji");
            if (emoji != null && !emoji.isEmpty()) moodEmoji = emoji;

            String dur = sessionDoc.getString("duration");
            if (dur != null && !dur.isEmpty()) duration = dur;
        }

        Timestamp startTime = slotDoc.getTimestamp("start_time");
        String timeDisplay = "00:00";
        if (startTime != null) {
            timeDisplay = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(startTime.toDate());
        }

        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 24);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(32);
        cardView.setCardElevation(0);
        cardView.setCardBackgroundColor(Color.WHITE);

        // 🌟 SỬA CHUẨN TRUYỀN ID: Gửi chính xác trường dữ liệu viết thường theo cách hứng của SessionDetailActivity
        cardView.setOnClickListener(v -> {

            Log.d("HEAMI_DEBUG", "slot_id = " + slotId);
            Log.d("HEAMI_DEBUG", "session_id = " + sessionId);

            Intent intent = new Intent(
                    DoctorHomeActivity.this,
                    SessionDetailActivity.class);

            intent.putExtra("slot_id", slotId);
            intent.putExtra("session_id", sessionId);

            startActivity(intent);
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(32, 32, 32, 32);

        FrameLayout emojiFrame = new FrameLayout(this);
        emojiFrame.setLayoutParams(new LinearLayout.LayoutParams(96, 96));
        emojiFrame.setBackgroundResource(R.drawable.bg_stat_icon_blue);

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(moodEmoji);
        tvEmoji.setTextSize(20);
        tvEmoji.setGravity(Gravity.CENTER);
        emojiFrame.addView(tvEmoji);
        root.addView(emojiFrame);

        LinearLayout textBox = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(24, 0, 24, 0);
        textBox.setLayoutParams(lp);
        textBox.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(patientName);
        tvName.setTextSize(15);
        tvName.setTextColor(Color.parseColor("#1A2530"));
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(moodText + " · " + duration);
        tvDesc.setTextSize(12);
        tvDesc.setTextColor(Color.parseColor("#9EA8B6"));

        textBox.addView(tvName);
        textBox.addView(tvDesc);
        root.addView(textBox);

        TextView tvTime = new TextView(this);
        tvTime.setText(timeDisplay);
        tvTime.setTextColor(Color.parseColor("#09A38C"));
        tvTime.setTextSize(13);
        tvTime.setTypeface(null, Typeface.BOLD);
        tvTime.setBackgroundResource(R.drawable.bg_stat_icon_green);
        tvTime.setPadding(32, 16, 32, 16);
        tvTime.setGravity(Gravity.CENTER);
        root.addView(tvTime);

        cardView.addView(root);
        containerUpcoming.addView(cardView);
    }

    private void generateAttentionItemProgrammatically() {
        if (containerAttention == null) return;

        containerAttention.removeAllViews();
        final java.util.HashSet<String> displayedUsers = new java.util.HashSet<>();

        db.collection("consultations")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (querySnapshots == null || querySnapshots.isEmpty()) return;

                    int count = 0;

                    for (QueryDocumentSnapshot consultDoc : querySnapshots) {
                        if (count >= 3) break;

                        String uId = consultDoc.getString("user_id");
                        String noteText = consultDoc.getString("note");

                        if (uId == null || uId.isEmpty()) continue;

                        if (displayedUsers.contains(uId)) {
                            continue;
                        }

                        displayedUsers.add(uId);
                        count++;

                        db.collection("users").document(uId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    if (!userDoc.exists()) return;

                                    String pNickname = userDoc.getString("nickname");
                                    if (pNickname == null || pNickname.isEmpty()) {
                                        pNickname = "Bệnh nhân ẩn danh";
                                    }

                                    LinearLayout row = new LinearLayout(this);
                                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    rowParams.setMargins(0, 0, 0, 24);
                                    row.setLayoutParams(rowParams);
                                    row.setOrientation(LinearLayout.HORIZONTAL);
                                    row.setGravity(Gravity.CENTER_VERTICAL);
                                    row.setPadding(32, 32, 32, 32);

                                    if (pNickname.equalsIgnoreCase("Nini") || (noteText != null && noteText.contains("áp lực"))) {
                                        row.setBackgroundColor(Color.parseColor("#FFF0F1"));
                                    } else {
                                        row.setBackgroundColor(Color.parseColor("#FFF9E6"));
                                    }

                                    View dot = new View(this);
                                    dot.setLayoutParams(new LinearLayout.LayoutParams(16, 16));
                                    int dotColor = pNickname.equalsIgnoreCase("Nini") ? Color.parseColor("#FF5A5F") : Color.parseColor("#FFA000");
                                    android.graphics.drawable.GradientDrawable dotDrawable = new android.graphics.drawable.GradientDrawable();
                                    dotDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                                    dotDrawable.setColor(dotColor);
                                    dot.setBackground(dotDrawable);
                                    row.addView(dot);

                                    LinearLayout tBox = new LinearLayout(this);
                                    LinearLayout.LayoutParams tBoxParams = new LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    tBoxParams.setMargins(28, 0, 0, 0);
                                    tBox.setLayoutParams(tBoxParams);
                                    tBox.setOrientation(LinearLayout.VERTICAL);

                                    TextView nText = new TextView(this);
                                    nText.setText(pNickname);
                                    nText.setTextColor(Color.parseColor("#1A2530"));
                                    nText.setTextSize(15);
                                    nText.setTypeface(null, Typeface.BOLD);
                                    tBox.addView(nText);

                                    TextView dText = new TextView(this);
                                    LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    dParams.setMargins(0, 4, 0, 0);
                                    dText.setLayoutParams(dParams);
                                    dText.setText(noteText != null && !noteText.isEmpty() ? noteText : "Tâm trạng tiêu cực kéo dài, cần theo dõi sát sao.");
                                    dText.setTextColor(Color.parseColor("#7F8C8D"));
                                    dText.setTextSize(13);
                                    tBox.addView(dText);

                                    row.addView(tBox);

                                    row.setOnClickListener(v -> {
                                        Intent intent = new Intent(DoctorHomeActivity.this, DoctorPatientDetailActivity.class);
                                        intent.putExtra("patient_user_id", uId);
                                        intent.putExtra("session_id", consultDoc.getId());
                                        startActivity(intent);
                                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                    });

                                    containerAttention.addView(row);
                                });
                    }
                })
                .addOnFailureListener(err -> Log.e("HEAMI_DEBUG", "Lỗi tải danh sách consultations chú ý: ", err));
    }

    private void updateTimeGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = "Chào buổi tối";
        if (hour >= 4 && hour < 10) greeting = "Chào buổi sáng";
        else if (hour >= 10 && hour < 13) greeting = "Chào buổi trưa";
        else if (hour >= 13 && hour < 18) greeting = "Chào buổi chiều";

        if (txtDoctorGreetingTime != null) txtDoctorGreetingTime.setText(greeting);
    }

    private void startDecorAnimations() {
        if (decorFlowerPinkTop != null) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(decorFlowerPinkTop, "translationY", 0f, -15f);
            animator.setDuration(1500);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        }
    }
}