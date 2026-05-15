package com.example.heami.ui.consultation;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class CallNoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "extra_session_id";
    public static final String EXTRA_ROLE = "extra_role";

    private static final String CALL_NOTE_PREFS = "HeamiCallNotes";
    private static final String ROLE_USER = "USER";

    private ImageButton btnBack;
    private TextView txtTitle;
    private TextView txtSubtitle;
    private TextView txtPartnerName;
    private TextView txtFormatType;
    private TextView txtUpdatedAt;
    private TextView txtNoteContent;
    private TextView txtEmptyState;

    private String sessionId = "";
    private String role = ROLE_USER;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_note_detail);

        bindViews();
        readExtras();
        setupActions();
        renderNote();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnCallNoteBack);
        txtTitle = findViewById(R.id.txtCallNoteDetailTitle);
        txtSubtitle = findViewById(R.id.txtCallNoteDetailSubtitle);
        txtPartnerName = findViewById(R.id.txtCallNotePartnerName);
        txtFormatType = findViewById(R.id.txtCallNoteFormatType);
        txtUpdatedAt = findViewById(R.id.txtCallNoteUpdatedAt);
        txtNoteContent = findViewById(R.id.txtCallNoteContent);
        txtEmptyState = findViewById(R.id.txtCallNoteEmptyState);
    }

    private void readExtras() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) return;

        sessionId = safeText(extras.getString(EXTRA_SESSION_ID), "");
        role = safeText(extras.getString(EXTRA_ROLE), ROLE_USER);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void renderNote() {
        txtTitle.setText("Note cuộc gọi");
        txtSubtitle.setText(
                "Ghi chú riêng tư trên thiết bị này • Session " +
                        (sessionId.isEmpty() ? "không xác định" : sessionId)
        );

        JSONObject noteRecord = getLocalCallNoteRecord();
        if (noteRecord == null) {
            showEmptyState();
            return;
        }

        String partnerName = safeText(noteRecord.optString("partnerName", ""), "");
        String formatType = safeText(noteRecord.optString("formatType", ""), "CALL");
        String note = safeText(noteRecord.optString("note", ""), "");
        long updatedAt = noteRecord.optLong("updatedAt", 0L);

        txtPartnerName.setText(
                partnerName.isEmpty() ? "Người tham gia không xác định" : partnerName
        );
        txtFormatType.setText("Hình thức: " + formatType);
        txtUpdatedAt.setText(
                updatedAt > 0
                        ? "Cập nhật: " + formatTimestamp(updatedAt)
                        : "Cập nhật: Không xác định"
        );

        if (note.isEmpty()) {
            showEmptyState();
            return;
        }

        txtEmptyState.setVisibility(TextView.GONE);
        txtNoteContent.setVisibility(TextView.VISIBLE);
        txtNoteContent.setText(note);
    }

    private void showEmptyState() {
        txtNoteContent.setVisibility(TextView.GONE);
        txtEmptyState.setVisibility(TextView.VISIBLE);
        txtEmptyState.setText("Chưa có note nào cho cuộc gọi này.");
    }

    @Nullable
    private JSONObject getLocalCallNoteRecord() {
        SharedPreferences prefs = getSharedPreferences(CALL_NOTE_PREFS, MODE_PRIVATE);
        String raw = safeText(prefs.getString(buildLocalCallNoteKey(), ""), "");

        if (raw.isEmpty()) {
            return null;
        }

        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return null;
        }
    }

    @NonNull
    private String buildLocalCallNoteKey() {
        return "call_note_" + safeText(sessionId, "unknown_session") + "_" + safeText(role, "UNKNOWN_ROLE");
    }

    @NonNull
    private String formatTimestamp(long millis) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                new Locale("vi", "VN")
        );
        return dateFormat.format(new Date(millis));
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}