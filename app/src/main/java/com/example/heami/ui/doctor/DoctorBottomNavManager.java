package com.example.heami.ui.doctor;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.heami.R;

public class DoctorBottomNavManager {

    public static final String TAB_OVERVIEW = "overview";
    public static final String TAB_APPOINTMENTS = "appointments";
    public static final String TAB_PATIENTS = "patients";
    public static final String TAB_MESSAGES = "messages";
    public static final String TAB_PROFILE = "profile";

    public static void setup(Activity activity, String activeTab) {
        if (activity == null) return;

        LinearLayout navOverview = activity.findViewById(R.id.navDoctorOverview);
        LinearLayout navAppointments = activity.findViewById(R.id.navDoctorAppointments);
        LinearLayout navPatients = activity.findViewById(R.id.navDoctorPatients);
        LinearLayout navMessages = activity.findViewById(R.id.navDoctorMessages);
        LinearLayout navProfile = activity.findViewById(R.id.navDoctorProfile);

        ImageView navOverviewIcon = activity.findViewById(R.id.navDoctorOverviewIcon);
        ImageView navAppointmentsIcon = activity.findViewById(R.id.navDoctorAppointmentsIcon);
        ImageView navPatientsIcon = activity.findViewById(R.id.navDoctorPatientsIcon);
        ImageView navMessagesIcon = activity.findViewById(R.id.navDoctorMessagesIcon);
        ImageView navProfileIcon = activity.findViewById(R.id.navDoctorProfileIcon);

        TextView navOverviewText = activity.findViewById(R.id.navDoctorOverviewText);
        TextView navAppointmentsText = activity.findViewById(R.id.navDoctorAppointmentsText);
        TextView navPatientsText = activity.findViewById(R.id.navDoctorPatientsText);
        TextView navMessagesText = activity.findViewById(R.id.navDoctorMessagesText);
        TextView navProfileText = activity.findViewById(R.id.navDoctorProfileText);

        resetItem(navOverview, navOverviewIcon, navOverviewText, R.drawable.ic_nav_doctor_overview);
        resetItem(navAppointments, navAppointmentsIcon, navAppointmentsText, R.drawable.ic_nav_doctor_appointments);
        resetItem(navPatients, navPatientsIcon, navPatientsText, R.drawable.ic_nav_doctor_patients);
        resetItem(navMessages, navMessagesIcon, navMessagesText, R.drawable.ic_nav_doctor_messages);
        resetItem(navProfile, navProfileIcon, navProfileText, R.drawable.ic_nav_doctor_profile);

        switch (activeTab) {
            case TAB_OVERVIEW:
                setActiveItem(navOverview, navOverviewIcon, navOverviewText, R.drawable.ic_nav_doctor_overview_active);
                break;
            case TAB_APPOINTMENTS:
                setActiveItem(navAppointments, navAppointmentsIcon, navAppointmentsText, R.drawable.ic_nav_doctor_appointments_active);
                break;
            case TAB_PATIENTS:
                setActiveItem(navPatients, navPatientsIcon, navPatientsText, R.drawable.ic_nav_doctor_patients_active);
                break;
            case TAB_MESSAGES:
                setActiveItem(navMessages, navMessagesIcon, navMessagesText, R.drawable.ic_nav_doctor_messages_active);
                break;
            case TAB_PROFILE:
                setActiveItem(navProfile, navProfileIcon, navProfileText, R.drawable.ic_nav_doctor_profile_active);
                break;
        }

        if (navOverview != null) {
            navOverview.setOnClickListener(v -> {
                if (!(activity instanceof DoctorHomeActivity)) {
                    activity.startActivity(new Intent(activity, DoctorHomeActivity.class));
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (navAppointments != null) {
            navAppointments.setOnClickListener(v -> {
                if (!(activity instanceof DoctorScheduleManagementActivity)) {
                    activity.startActivity(new Intent(activity, DoctorScheduleManagementActivity.class));
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (navPatients != null) {
            navPatients.setOnClickListener(v -> {
                if (!(activity instanceof DoctorPatientsActivity)) {
                    activity.startActivity(new Intent(activity, DoctorPatientsActivity.class));
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (navMessages != null) {
            navMessages.setOnClickListener(v -> {
                if (!(activity instanceof DoctorMessagesActivity)) {
                    activity.startActivity(new Intent(activity, DoctorMessagesActivity.class));
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                if (!(activity instanceof DoctorProfileActivity)) {
                    activity.startActivity(new Intent(activity, DoctorProfileActivity.class));
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
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
            item.setBackgroundResource(R.drawable.bg_doctor_nav_item_active);
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
            text.setTextColor(0xFF009688); // Active text color is teal
        }
    }
}
