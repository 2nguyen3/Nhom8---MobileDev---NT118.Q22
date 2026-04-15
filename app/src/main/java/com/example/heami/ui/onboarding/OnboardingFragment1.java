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

public class OnboardingFragment1 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);

        // Nút Tiếp theo
        view.findViewById(R.id.btnNext).setOnClickListener(v -> {
            ViewPager2 vp = getActivity().findViewById(R.id.viewPager);
            if (vp != null) vp.setCurrentItem(1, true);
        });

        // Nút Bỏ qua
        view.findViewById(R.id.btnSkip).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), PermissionActivity.class));
            if (getActivity() != null) getActivity().finish();
        });

        // Hiệu ứng mây (ID trang 1: ivOnboarding)
        ImageView ivCloud = view.findViewById(R.id.ivOnboarding);
        if (ivCloud != null) startFloatingAnimation(ivCloud);

        return view;
    }

    private void startFloatingAnimation(View view) {
        if (view == null) return;
        view.animate()
                .translationY(view.getTranslationY() == 0 ? -40f : 0f)
                .setDuration(2000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> startFloatingAnimation(view))
                .start();
    }
}