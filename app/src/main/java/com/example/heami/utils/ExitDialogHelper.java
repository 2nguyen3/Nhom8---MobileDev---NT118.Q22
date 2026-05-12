package com.example.heami.utils;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.google.android.material.button.MaterialButton;

public class ExitDialogHelper {

    public static void registerExitHandler(AppCompatActivity activity) {
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog(activity);
            }
        });
    }

    private static void showExitDialog(Activity activity) {
        Dialog dialog = new Dialog(activity, R.style.HeamiDialogTheme);
        dialog.setContentView(R.layout.dialog_exit_confirmation);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        MaterialButton btnStay = dialog.findViewById(R.id.btnStay);
        TextView tvConfirmExit = dialog.findViewById(R.id.tvConfirmExit);

        if (btnStay != null) {
            btnStay.setOnClickListener(v -> dialog.dismiss());
        }
        if (tvConfirmExit != null) {
            tvConfirmExit.setOnClickListener(v -> {
                dialog.dismiss();
                activity.finishAffinity();
            });
        }
        dialog.show();
    }
}
