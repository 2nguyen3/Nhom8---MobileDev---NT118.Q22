package com.example.heami.ui.onboarding;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingAdapter extends FragmentStateAdapter {

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new OnboardingFragment1(); // Trang hồng (Welcome)
            case 1:
                return new OnboardingFragment2(); // Trang xanh (AI Check-in)
            case 2:
                return new OnboardingFragment3(); // Trang tím (Trị liệu)
            case 3:
                return new OnboardingFragment4(); // Trang cuối (Bắt đầu)
            default:
                return new OnboardingFragment1();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // Tổng cộng có 4 màn hình Onboarding
    }
}