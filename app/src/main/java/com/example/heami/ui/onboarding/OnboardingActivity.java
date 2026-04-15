package com.example.heami.ui.onboarding;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.heami.R;

public class OnboardingActivity extends AppCompatActivity {
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding); // File chứa ViewPager

        viewPager = findViewById(R.id.viewPager);

        // Thiết lập Adapter để hiện các Fragment màu hồng lên
        OnboardingAdapter adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);
    }
}