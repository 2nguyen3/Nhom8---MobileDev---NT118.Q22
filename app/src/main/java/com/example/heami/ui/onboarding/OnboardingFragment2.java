package com.example.heami.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.example.heami.R;
import com.example.heami.ui.main.PermissionActivity;

public class OnboardingFragment2 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Nạp layout của trang AI
        View view = inflater.inflate(R.layout.fragment_onboarding_ai, container, false);

        // 2. Nút Tiếp theo -> Trang 3 (index 2)
        view.findViewById(R.id.btnNextAi).setOnClickListener(v -> {
            if (getActivity() != null) {
                ViewPager2 vp = getActivity().findViewById(R.id.viewPager);
                if (vp != null) vp.setCurrentItem(2, true);
            }
        });

        // 3. Nút Quay lại -> Trang 1 (index 0)
        view.findViewById(R.id.btnBackAi).setOnClickListener(v -> {
            if (getActivity() != null) {
                ViewPager2 vp = getActivity().findViewById(R.id.viewPager);
                if (vp != null) vp.setCurrentItem(0, true);
            }
        });

        // 4. Nút Bỏ qua -> Chuyển sang PermissionActivity
        view.findViewById(R.id.btnSkip).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PermissionActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        // 5. Hiệu ứng mây bay (ID trang 2 là ivOnboardingAi)
        ImageView ivCloud = view.findViewById(R.id.ivOnboardingAi);
        if (ivCloud != null) {
            startFloatingAnimation(ivCloud);
        }

        return view;
    }

    /**
     * Hàm xử lý hiệu ứng bay lên xuống vô hạn
     */
    private void startFloatingAnimation(View view) {
        if (view == null) return;

        // Nếu đang ở vị trí 0 thì bay lên -40, nếu đang ở trên thì bay về 0
        float targetY = (view.getTranslationY() == 0) ? -40f : 0f;

        view.animate()
                .translationY(targetY)
                .setDuration(2000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> startFloatingAnimation(view))
                .start();
    }
}