package com.example.heami.activities;

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

public class OnboardingFragment3 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp layout
        View view = inflater.inflate(R.layout.fragment_onboarding_therapy, container, false);

        // 1. Ánh xạ các View
        TextView btnNext = view.findViewById(R.id.btnNextTherapy);
        ImageButton btnBack = view.findViewById(R.id.btnBackTherapy);
        TextView btnSkip = view.findViewById(R.id.btnSkip);
        ViewPager2 viewPager = getActivity() != null ? getActivity().findViewById(R.id.viewPager) : null;

        // 2. Nút Tiếp theo -> Sang trang 4 (Index 3)
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (viewPager != null) viewPager.setCurrentItem(3, true);
            });
        }

        // 3. Nút Quay lại -> Về trang 2 (Index 1)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (viewPager != null) viewPager.setCurrentItem(1, true);
            });
        }

        // 4. Nút Bỏ qua
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), PermissionActivity.class));
                if (getActivity() != null) getActivity().finish();
            });
        }

        // 5. Hiệu ứng mây bay (ID phải khớp hoàn toàn với XML)
        ImageView ivCloud = view.findViewById(R.id.ivOnboardingTherapy);
        if (ivCloud != null) {
            startFloatingAnimation(ivCloud);
        }

        // QUAN TRỌNG: Phải có dòng return view này
        return view;
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