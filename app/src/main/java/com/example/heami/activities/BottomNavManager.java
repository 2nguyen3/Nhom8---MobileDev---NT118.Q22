package com.example.heami.activities;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.heami.R;

public class BottomNavManager {

    public static final String TAB_HOME = "home";
    public static final String TAB_THERAPY = "therapy";
    public static final String TAB_COMMUNITY = "community";
    public static final String TAB_DOCTOR = "doctor";
    public static final String TAB_PROFILE = "profile";

    public static void setup(Activity activity, String activeTab) {
        if (activity == null) return;

        LinearLayout navHome = activity.findViewById(R.id.navHome);
        LinearLayout navTherapy = activity.findViewById(R.id.navTherapy);
        LinearLayout navCommunity = activity.findViewById(R.id.navCommunity);
        LinearLayout navDoctor = activity.findViewById(R.id.navDoctor);
        LinearLayout navProfile = activity.findViewById(R.id.navProfile);

        ImageView navHomeIcon = activity.findViewById(R.id.navHomeIcon);
        ImageView navTherapyIcon = activity.findViewById(R.id.navTherapyIcon);
        ImageView navCommunityIcon = activity.findViewById(R.id.navCommunityIcon);
        ImageView navDoctorIcon = activity.findViewById(R.id.navDoctorIcon);
        ImageView navProfileIcon = activity.findViewById(R.id.navProfileIcon);

        TextView navHomeText = activity.findViewById(R.id.navHomeText);
        TextView navTherapyText = activity.findViewById(R.id.navTherapyText);
        TextView navCommunityText = activity.findViewById(R.id.navCommunityText);
        TextView navDoctorText = activity.findViewById(R.id.navDoctorText);
        TextView navProfileText = activity.findViewById(R.id.navProfileText);

        resetItem(navHome, navHomeIcon, navHomeText, R.drawable.ic_nav_home);
        resetItem(navTherapy, navTherapyIcon, navTherapyText, R.drawable.ic_nav_therapy);
        resetItem(navCommunity, navCommunityIcon, navCommunityText, R.drawable.ic_nav_community);
        resetItem(navDoctor, navDoctorIcon, navDoctorText, R.drawable.ic_nav_doctor);
        resetItem(navProfile, navProfileIcon, navProfileText, R.drawable.ic_nav_profile);

        switch (activeTab) {
            case TAB_HOME:
                setActiveItem(navHome, navHomeIcon, navHomeText, R.drawable.ic_nav_home_active);
                break;
            case TAB_COMMUNITY:
                setActiveItem(navCommunity, navCommunityIcon, navCommunityText, R.drawable.ic_nav_community_active);
                break;
            case TAB_THERAPY:
                setActiveItem(navTherapy, navTherapyIcon, navTherapyText, R.drawable.ic_nav_therapy_active);
                break;
            case TAB_DOCTOR:
                setActiveItem(navDoctor, navDoctorIcon, navDoctorText, R.drawable.ic_nav_doctor_active);
                break;
            case TAB_PROFILE:
                setActiveItem(navProfile, navProfileIcon, navProfileText, R.drawable.ic_nav_profile_active);
                break;
        }

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (!(activity instanceof HomeActivity)) {
                    activity.startActivity(new Intent(activity, HomeActivity.class));
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (navCommunity != null) {
            navCommunity.setOnClickListener(v -> {
                if (!(activity instanceof CommunityActivity)) {
                    activity.startActivity(new Intent(activity, CommunityActivity.class));
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (navTherapy != null) {
            navTherapy.setOnClickListener(v -> {
                // Màn chưa làm xong, tạm để trống
            });
        }

        if (navDoctor != null) {
            navDoctor.setOnClickListener(v -> {
                if (!(activity instanceof DoctorActivity)) {
                    activity.startActivity(new Intent(activity, DoctorActivity.class));
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                // Màn chưa làm xong, tạm để trống
            });
        }
    }

    private static void resetItem(LinearLayout item, ImageView icon, TextView text, int iconRes) {
        if (item != null) {
            item.setBackground(null);
        }
        if (icon != null) {
            icon.setImageResource(iconRes);
            icon.setScaleX(1f);
            icon.setScaleY(1f);
        }
        if (text != null) {
            text.setTextColor(0xFFB5BFD1);
        }
    }

    private static void setActiveItem(LinearLayout item, ImageView icon, TextView text, int iconRes) {
        if (item != null) {
            item.setBackgroundResource(R.drawable.bg_nav_item_active);

            item.setScaleX(0.95f);
            item.setScaleY(0.95f);
            item.setAlpha(0.88f);

            item.animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .alpha(1f)
                    .setDuration(140)
                    .withEndAction(() -> item.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(90)
                            .start())
                    .start();
        }

        if (icon != null) {
            icon.setImageResource(iconRes);

            icon.setScaleX(0.92f);
            icon.setScaleY(0.92f);

            icon.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(140)
                    .withEndAction(() -> icon.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(90)
                            .start())
                    .start();
        }

        if (text != null) {
            text.setTextColor(0xFFE86FA0);
        }
    }
}