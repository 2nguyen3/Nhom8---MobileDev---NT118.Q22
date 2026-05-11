package com.example.heami.ui.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.utils.ExitDialogHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorPatientsActivity extends AppCompatActivity {

    private List<PatientItem> patientItems = new ArrayList<>();
    private String currentFilter = "all"; // "all", "plan", "bad_mood", "chat", "video"
    private String searchQuery = "";

    private EditText edtSearchPatients;
    private TextView txtPatientCount;
    private TextView btnFilterAll;
    private TextView btnFilterHasPlan;
    private TextView btnFilterBadMood;
    private TextView btnFilterChat;
    private TextView btnFilterVideo;
    private LinearLayout layoutPatientsContainer;
    private LinearLayout layoutPatientsEmptyState;
    private TextView txtEmptyStateTitle;
    private TextView txtEmptyStateSubtitle;

    private FirebaseFirestore db;

    private static class PatientItem {
        View view;
        String name;
        boolean hasPlan;
        boolean hasBadMood;
        boolean isChat;
        boolean isVideo;

        PatientItem(View view, String name, boolean hasPlan, boolean hasBadMood, boolean isChat, boolean isVideo) {
            this.view = view;
            this.name = name;
            this.hasPlan = hasPlan;
            this.hasBadMood = hasBadMood;
            this.isChat = isChat;
            this.isVideo = isVideo;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_patients);

        db = FirebaseFirestore.getInstance();

        DoctorBottomNavManager.setup(this, DoctorBottomNavManager.TAB_PATIENTS);
        ExitDialogHelper.registerExitHandler(this);

        initViews();
        setupSearchAndFilters();
        loadPatientsFromFirestore();
    }

    private void initViews() {
        edtSearchPatients = findViewById(R.id.edtSearchPatients);
        txtPatientCount = findViewById(R.id.txtPatientCount);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterHasPlan = findViewById(R.id.btnFilterHasPlan);
        btnFilterBadMood = findViewById(R.id.btnFilterBadMood);
        btnFilterChat = findViewById(R.id.btnFilterChat);
        btnFilterVideo = findViewById(R.id.btnFilterVideo);
        layoutPatientsContainer = findViewById(R.id.layoutPatientsContainer);
        layoutPatientsEmptyState = findViewById(R.id.layoutPatientsEmptyState);
        txtEmptyStateTitle = findViewById(R.id.txtEmptyStateTitle);
        txtEmptyStateSubtitle = findViewById(R.id.txtEmptyStateSubtitle);
    }

    private void setupSearchAndFilters() {
        if (edtSearchPatients != null) {
            edtSearchPatients.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString();
                    applyFilterAndSearch();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnFilterAll != null) {
            btnFilterAll.setOnClickListener(v -> {
                currentFilter = "all";
                updateTabStyles(btnFilterAll, btnFilterHasPlan, btnFilterBadMood, btnFilterChat, btnFilterVideo);
                applyFilterAndSearch();
            });
        }

        if (btnFilterHasPlan != null) {
            btnFilterHasPlan.setOnClickListener(v -> {
                currentFilter = "plan";
                updateTabStyles(btnFilterHasPlan, btnFilterAll, btnFilterBadMood, btnFilterChat, btnFilterVideo);
                applyFilterAndSearch();
            });
        }

        if (btnFilterBadMood != null) {
            btnFilterBadMood.setOnClickListener(v -> {
                currentFilter = "bad_mood";
                updateTabStyles(btnFilterBadMood, btnFilterAll, btnFilterHasPlan, btnFilterChat, btnFilterVideo);
                applyFilterAndSearch();
            });
        }

        if (btnFilterChat != null) {
            btnFilterChat.setOnClickListener(v -> {
                currentFilter = "chat";
                updateTabStyles(btnFilterChat, btnFilterAll, btnFilterHasPlan, btnFilterBadMood, btnFilterVideo);
                applyFilterAndSearch();
            });
        }

        if (btnFilterVideo != null) {
            btnFilterVideo.setOnClickListener(v -> {
                currentFilter = "video";
                updateTabStyles(btnFilterVideo, btnFilterAll, btnFilterHasPlan, btnFilterBadMood, btnFilterChat);
                applyFilterAndSearch();
            });
        }
    }

    private void loadPatientsFromFirestore() {
        android.content.SharedPreferences prefs = getSharedPreferences("HeamiData", MODE_PRIVATE);
        boolean isDoctor = prefs.getBoolean("is_doctor", false);
        String uid = "doc_001";
        if (isDoctor) {
            uid = prefs.getString("doctor_id", "doc_001");
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (layoutPatientsContainer != null) {
            layoutPatientsContainer.removeAllViews();
        }
        patientItems.clear();

        final String doctorUid = uid;
        db.collection("consultations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        // Group consultations by unique user
                        Map<String, List<DocumentSnapshot>> grouped = new HashMap<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String cDocId = doc.getString("doctor_id");
                            String cDocName = doc.getString("doctor_name");
                            String cUserId = doc.getString("user_id");

                            boolean matches = false;
                            if (doctorUid.equals(cDocId)) {
                                matches = true;
                            } else if ((cDocId == null || cDocId.isEmpty()) && 
                                       (cDocName != null && cDocName.contains("Hoài Thu"))) {
                                matches = true;
                            }

                            if (matches) {
                                String patientUid = cUserId;
                                  if (patientUid != null && !patientUid.isEmpty()) {
                                    if (!grouped.containsKey(patientUid)) {
                                        grouped.put(patientUid, new ArrayList<>());
                                    }
                                    grouped.get(patientUid).add(doc);
                                }
                            }
                        }

                        // For each unique user, load details
                        for (Map.Entry<String, List<DocumentSnapshot>> entry : grouped.entrySet()) {
                            String patientUid = entry.getKey();
                            List<DocumentSnapshot> consults = entry.getValue();
                            
                            // Find latest consultation to show note
                            DocumentSnapshot latestConsult = consults.get(0);
                            for (DocumentSnapshot c : consults) {
                                if (c.getTimestamp("booked_at") != null && latestConsult.getTimestamp("booked_at") != null) {
                                    if (c.getTimestamp("booked_at").compareTo(latestConsult.getTimestamp("booked_at")) > 0) {
                                        latestConsult = c;
                                    }
                                }
                            }

                            inflatePatientCard(patientUid, consults.size(), latestConsult);
                        }
                    } else {
                        android.util.Log.d("DoctorPatients", "No consultations found at all");
                        if (txtPatientCount != null) {
                            txtPatientCount.setText("0 bệnh nhân đang theo dõi");
                        }
                        applyFilterAndSearch();
                    }
                })
                .addOnFailureListener(e -> {
                    applyFilterAndSearch();
                });
    }

    private void inflatePatientCard(String patientUid, int sessionCount, DocumentSnapshot latestConsultation) {
        if (layoutPatientsContainer == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.item_doctor_patient, layoutPatientsContainer, false);

        // Bind basic views
        TextView txtPatientName = cardView.findViewById(R.id.txtPatientName);
        TextView txtPatientSubtitle = cardView.findViewById(R.id.txtPatientSubtitle);
        TextView txtPatientMoodTag = cardView.findViewById(R.id.txtPatientMoodTag);
        TextView txtPatientSessionsCompleted = cardView.findViewById(R.id.txtPatientSessionsCompleted);
        TextView patientAvatarEmoji = cardView.findViewById(R.id.patientAvatarEmoji);
        View patientStatusDot = cardView.findViewById(R.id.patientStatusDot);
        LinearLayout layoutPatientPlanTag = cardView.findViewById(R.id.layoutPatientPlanTag);

        // Set sessions completed
        txtPatientSessionsCompleted.setText("Đã trị liệu: " + sessionCount + " phiên");

        // Set booking note
        String note = latestConsultation.getString("note");
        txtPatientSubtitle.setText(note != null && !note.isEmpty() ? note : "Không có ghi chú.");

        // Check if formatType is chat or video
        String formatType = latestConsultation.getString("format_type");
        boolean isChat = formatType != null && formatType.toLowerCase().contains("chat");
        boolean isVideo = formatType != null && formatType.toLowerCase().contains("video");

        // Assume hasPlan is true for demo if user has a consultation plan
        boolean hasPlan = latestConsultation.getString("package_type") != null && 
                          latestConsultation.getString("package_type").toLowerCase().contains("ngày");
        layoutPatientPlanTag.setVisibility(hasPlan ? View.VISIBLE : View.GONE);

        // Fetch User Info
        db.collection("users").document(patientUid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String nickname = userDoc.getString("nickname");
                        txtPatientName.setText(nickname != null ? nickname : "Bệnh nhân Heami");
                        
                        // Pick animal emoji based on nickname to keep the aesthetic
                        if (nickname != null) {
                            if (nickname.contains("Bướm")) patientAvatarEmoji.setText("🦋");
                            else if (nickname.contains("Cáo")) patientAvatarEmoji.setText("🦊");
                            else if (nickname.contains("Gấu")) patientAvatarEmoji.setText("🐨");
                            else if (nickname.contains("Ếch")) patientAvatarEmoji.setText("🐸");
                            else patientAvatarEmoji.setText("😊");
                        }
                    }

                    // Fetch latest check-in for current mood
                    db.collection("users").document(patientUid).collection("mood_history")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(checkinSnaps -> {
                                boolean hasBadMood = false;
                                if (checkinSnaps != null && !checkinSnaps.isEmpty()) {
                                    DocumentSnapshot checkin = checkinSnaps.getDocuments().get(0);
                                    String moodTag = checkin.getString("mood_tag");
                                    String moodEmoji = checkin.getString("mood_emoji");
                                    if (moodTag != null) {
                                        txtPatientMoodTag.setText(moodEmoji != null ? moodEmoji + " " + moodTag : moodTag);
                                        
                                        // Color tag and dot based on mood
                                        if (moodTag.contains("Buồn") || moodTag.contains("nặng lòng")) {
                                            txtPatientMoodTag.setBackgroundResource(R.drawable.bg_chip_blue);
                                            txtPatientMoodTag.setTextColor(Color.parseColor("#1976D2"));
                                            patientStatusDot.setBackgroundResource(R.drawable.bg_mood_dot_purple);
                                            hasBadMood = true;
                                        } else if (moodTag.contains("Căng thẳng") || moodTag.contains("Stress")) {
                                            txtPatientMoodTag.setBackgroundResource(R.drawable.bg_chip_purple);
                                            txtPatientMoodTag.setTextColor(Color.parseColor("#7C3AED"));
                                            patientStatusDot.setBackgroundResource(R.drawable.bg_mood_dot_purple);
                                            hasBadMood = true;
                                        } else if (moodTag.contains("Lo âu") || moodTag.contains("Sợ hãi")) {
                                            txtPatientMoodTag.setBackgroundResource(R.drawable.bg_chip_yellow);
                                            txtPatientMoodTag.setTextColor(Color.parseColor("#D97706"));
                                            patientStatusDot.setBackgroundResource(R.drawable.bg_mood_dot_yellow);
                                            hasBadMood = true;
                                        } else {
                                            txtPatientMoodTag.setBackgroundResource(R.drawable.bg_tag_teal);
                                            txtPatientMoodTag.setTextColor(Color.parseColor("#09A38C"));
                                            patientStatusDot.setBackgroundResource(R.drawable.bg_mood_dot_green);
                                        }
                                    }
                                } else {
                                    // Default fallback tags
                                    txtPatientMoodTag.setText("Bình thường");
                                    txtPatientMoodTag.setBackgroundResource(R.drawable.bg_tag_teal);
                                    txtPatientMoodTag.setTextColor(Color.parseColor("#09A38C"));
                                    patientStatusDot.setBackgroundResource(R.drawable.bg_mood_dot_green);
                                }

                                // Setup click listener
                                final boolean finalBadMood = hasBadMood;
                                cardView.setOnClickListener(v -> {
                                    Intent intent = new Intent(DoctorPatientsActivity.this, DoctorPatientDetailActivity.class);
                                    intent.putExtra("patient_user_id", patientUid);
                                    startActivity(intent);
                                });

                                // Add View to layout and cache for filter search
                                layoutPatientsContainer.addView(cardView);
                                String patientName = txtPatientName.getText().toString();
                                patientItems.add(new PatientItem(cardView, patientName, hasPlan, hasBadMood, isChat, isVideo));
                                applyFilterAndSearch();
                            });
                });
    }

    private void applyFilterAndSearch() {
        String lowerQuery = searchQuery.toLowerCase().trim();
        int visibleCount = 0;
        for (PatientItem p : patientItems) {
            if (p.view == null) continue;
            boolean matchesSearch = p.name.toLowerCase().contains(lowerQuery);
            boolean matchesFilter = false;
            switch (currentFilter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "plan":
                    matchesFilter = p.hasPlan;
                    break;
                case "bad_mood":
                    matchesFilter = p.hasBadMood;
                    break;
                case "chat":
                    matchesFilter = p.isChat;
                    break;
                case "video":
                    matchesFilter = p.isVideo;
                    break;
            }
            if (matchesSearch && matchesFilter) {
                p.view.setVisibility(View.VISIBLE);
                visibleCount++;
            } else {
                p.view.setVisibility(View.GONE);
            }
        }
        if (txtPatientCount != null) {
            txtPatientCount.setText(visibleCount + " bệnh nhân đang theo dõi");
        }

        if (layoutPatientsEmptyState != null && txtEmptyStateTitle != null && txtEmptyStateSubtitle != null) {
            if (visibleCount == 0) {
                layoutPatientsEmptyState.setVisibility(View.VISIBLE);
                if (patientItems.isEmpty()) {
                    txtEmptyStateTitle.setText("Chưa có bệnh nhân nào");
                    txtEmptyStateSubtitle.setText("Chưa có ai đăng ký lịch tư vấn với bạn");
                } else {
                    txtEmptyStateTitle.setText("Không tìm thấy bệnh nhân nào phù hợp");
                    txtEmptyStateSubtitle.setText("Hãy thử tìm kiếm bằng từ khoá khác nhé");
                }
            } else {
                layoutPatientsEmptyState.setVisibility(View.GONE);
            }
        }
    }

    private void updateTabStyles(TextView activeTab, TextView... inactiveTabs) {
        if (activeTab != null) {
            activeTab.setTextColor(Color.parseColor("#009688"));
            activeTab.setBackgroundResource(R.drawable.bg_chip_active_border);
            activeTab.setTypeface(android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.BOLD));
        }

        for (TextView tab : inactiveTabs) {
            if (tab != null) {
                tab.setTextColor(Color.parseColor("#7F8C8D"));
                tab.setBackgroundResource(R.drawable.bg_chip_inactive_border);
                tab.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
            }
        }
    }
}
