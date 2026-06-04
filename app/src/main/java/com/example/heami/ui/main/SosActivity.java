package com.example.heami.ui.main;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.ui.consultation.ConsultationsActivity;
import com.example.heami.ui.therapy.BreathingActivity;
import com.google.android.material.button.MaterialButton;

public class SosActivity extends AppCompatActivity {

    private static final String PREF_SOS = "HeamiSOS";
    private static final String KEY_TRUSTED_CONTACT = "trusted_contact_phone";

    private static final String EMERGENCY_MEDICAL_PHONE = "115";
    private static final String CHILD_PROTECTION_PHONE = "111";

    private TextView txtTrustedContactStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        bindViews();
        setupActions();
        refreshTrustedContactStatus();
    }

    private void bindViews() {
        txtTrustedContactStatus = findViewById(R.id.txtTrustedContactStatus);
    }

    private void setupActions() {
        ImageButton btnBack = findViewById(R.id.btnBackSos);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnCallEmergency = findViewById(R.id.btnCallEmergency);
        if (btnCallEmergency != null) {
            btnCallEmergency.setOnClickListener(v -> showCallConfirmDialog(
                    "Gọi cấp cứu 115?",
                    "Heami sẽ mở màn hình gọi điện. Bạn chỉ cần bấm gọi khi đã sẵn sàng.",
                    EMERGENCY_MEDICAL_PHONE
            ));
        }

        MaterialButton btnCallChildProtection = findViewById(R.id.btnCallChildProtection);
        if (btnCallChildProtection != null) {
            btnCallChildProtection.setOnClickListener(v -> showCallConfirmDialog(
                    "Gọi tổng đài 111?",
                    "Đây là kênh hỗ trợ, bảo vệ trẻ em. Heami sẽ mở màn hình gọi điện cho bạn.",
                    CHILD_PROTECTION_PHONE
            ));
        }

        MaterialButton btnCallTrustedContact = findViewById(R.id.btnCallTrustedContact);
        if (btnCallTrustedContact != null) {
            btnCallTrustedContact.setOnClickListener(v -> handleTrustedContactCall());
        }

        TextView btnEditTrustedContact = findViewById(R.id.btnEditTrustedContact);
        if (btnEditTrustedContact != null) {
            btnEditTrustedContact.setOnClickListener(v -> showTrustedContactDialog());
        }

        MaterialButton btnOpenConsultation = findViewById(R.id.btnOpenConsultation);
        if (btnOpenConsultation != null) {
            btnOpenConsultation.setOnClickListener(v -> {
                Intent intent = new Intent(this, ConsultationsActivity.class);
                startActivity(intent);
            });
        }

        MaterialButton btnOpenBreathing = findViewById(R.id.btnOpenBreathing);
        if (btnOpenBreathing != null) {
            btnOpenBreathing.setOnClickListener(v -> {
                Intent intent = new Intent(this, BreathingActivity.class);
                startActivity(intent);
            });
        }
    }

    private void handleTrustedContactCall() {
        String phone = getTrustedContactPhone();

        if (phone.isEmpty()) {
            showTrustedContactDialog();
            return;
        }

        showCallConfirmDialog(
                "Gọi người tin cậy?",
                "Heami sẽ mở màn hình gọi đến số: " + phone,
                phone
        );
    }

    private void showTrustedContactDialog() {
        Dialog dialog = new Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_sos_trusted_contact);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        EditText edtTrustedPhone = dialog.findViewById(R.id.edtTrustedPhone);
        MaterialButton btnSaveTrustedPhone = dialog.findViewById(R.id.btnSaveTrustedPhone);
        TextView btnCancelTrustedPhone = dialog.findViewById(R.id.btnCancelTrustedPhone);

        String currentPhone = getTrustedContactPhone();
        if (edtTrustedPhone != null && !currentPhone.isEmpty()) {
            edtTrustedPhone.setText(currentPhone);
            edtTrustedPhone.setSelection(currentPhone.length());
        }

        if (btnCancelTrustedPhone != null) {
            btnCancelTrustedPhone.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSaveTrustedPhone != null) {
            btnSaveTrustedPhone.setOnClickListener(v -> {
                String phone = edtTrustedPhone != null
                        ? edtTrustedPhone.getText().toString().trim()
                        : "";

                if (phone.isEmpty()) {
                    Toast.makeText(this, "Bạn hãy nhập số điện thoại người tin cậy", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveTrustedContactPhone(phone);
                refreshTrustedContactStatus();
                dialog.dismiss();

                Toast.makeText(this, "Đã lưu người tin cậy", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void showCallConfirmDialog(String title, String message, String phoneNumber) {
        Dialog dialog = new Dialog(this, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_sos_call_confirm);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        TextView txtSosCallTitle = dialog.findViewById(R.id.txtSosCallTitle);
        TextView txtSosCallMessage = dialog.findViewById(R.id.txtSosCallMessage);
        MaterialButton btnConfirmCall = dialog.findViewById(R.id.btnConfirmCall);
        TextView btnCancelCall = dialog.findViewById(R.id.btnCancelCall);

        if (txtSosCallTitle != null) txtSosCallTitle.setText(title);
        if (txtSosCallMessage != null) txtSosCallMessage.setText(message);

        if (btnCancelCall != null) {
            btnCancelCall.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirmCall != null) {
            btnConfirmCall.setOnClickListener(v -> {
                dialog.dismiss();
                openDialer(phoneNumber);
            });
        }

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void openDialer(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Toast.makeText(this, "Không có số điện thoại hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber.trim()));

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở ứng dụng gọi điện", Toast.LENGTH_SHORT).show();
        }
    }

    private String getTrustedContactPhone() {
        SharedPreferences prefs = getSharedPreferences(PREF_SOS, MODE_PRIVATE);
        return prefs.getString(KEY_TRUSTED_CONTACT, "");
    }

    private void saveTrustedContactPhone(String phone) {
        SharedPreferences prefs = getSharedPreferences(PREF_SOS, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_TRUSTED_CONTACT, phone)
                .apply();
    }

    private void refreshTrustedContactStatus() {
        if (txtTrustedContactStatus == null) return;

        String phone = getTrustedContactPhone();
        if (phone.isEmpty()) {
            txtTrustedContactStatus.setText("Chưa thiết lập người tin cậy");
        } else {
            txtTrustedContactStatus.setText("Người tin cậy: " + phone);
        }
    }
}