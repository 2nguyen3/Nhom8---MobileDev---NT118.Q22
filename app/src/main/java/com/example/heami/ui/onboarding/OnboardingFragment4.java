package com.example.heami.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.heami.R;
import com.example.heami.ui.main.PermissionActivity;

public class OnboardingFragment4 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_community, container, false);

        // 1. Ánh xạ các View
        TextView btnStart = view.findViewById(R.id.btnStart);
        ImageButton btnBack = view.findViewById(R.id.btnBackCommunity);
        TextView btnSkip = view.findViewById(R.id.btnSkip);
        ViewPager2 viewPager = getActivity() != null ? getActivity().findViewById(R.id.viewPager) : null;

        // 2. Nút Bắt đầu & Bỏ qua
        if (btnStart != null) btnStart.setOnClickListener(v -> navigateToNextStep());
        if (btnSkip != null) btnSkip.setOnClickListener(v -> navigateToNextStep());

        // 3. Nút Quay lại -> Về trang 3 (Index 2)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (viewPager != null) viewPager.setCurrentItem(2, true);
            });
        }

        // 4. Hiệu ứng mây bay (Kiểm tra ID ivOnboardingCommunity trong XML)
        ImageView ivCloud = view.findViewById(R.id.ivOnboardingCommunity);
        if (ivCloud != null) startFloatingAnimation(ivCloud);

        return view;
    }

    private void navigateToNextStep() {
        if (getActivity() != null) {
            startActivity(new Intent(getActivity(), PermissionActivity.class));
            getActivity().finish();
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    private void startFloatingAnimation(View view) {
        if (view == null) return;
        float targetY = (view.getTranslationY() == 0) ? -40f : 0f;
        view.animate()
                .translationY(targetY)
                .setDuration(2000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> startFloatingAnimation(view))
                .start();
    }
}