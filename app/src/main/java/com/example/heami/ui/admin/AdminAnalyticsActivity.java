package com.example.heami.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.AdminAnalyticsOverview;
import com.example.heami.data.models.AdminTopPostItem;
import com.example.heami.data.repositories.AdminAnalyticsRepository;

import android.widget.ProgressBar;

import java.util.List;

public class AdminAnalyticsActivity extends AppCompatActivity {

    private ImageView btnBackAnalytics;
    private View layoutLoadingAnalytics;
    private ScrollView layoutContentAnalytics;

    private TextView txtTotalAccounts;
    private TextView txtTotalUsers;
    private TextView txtTotalDoctors;
    private TextView txtTotalAdmins;
    private TextView txtActive24h;
    private TextView txtOnlineNow;

    private TextView txtActiveRate;
    private TextView txtOnlineRate;
    private TextView txtReportRate;
    private TextView txtCompletionRate;

    private TextView txtTotalConsultations;
    private TextView txtOngoingConsultations;
    private TextView txtCompletedConsultations;
    private TextView txtBookedConsultations;
    private TextView txtChatConsultations;
    private TextView txtCallConsultations;

    private TextView txtTotalPosts;
    private TextView txtReportedPosts;
    private TextView txtHiddenPosts;
    private TextView txtDeletedPosts;

    private TextView txtBookedBarValue;
    private TextView txtOngoingBarValue;
    private TextView txtCompletedBarValue;
    private TextView txtCancelledBarValue;

    private TextView txtVisibleBarValue;
    private TextView txtReportedBarValue;
    private TextView txtHiddenBarValue;
    private TextView txtDeletedBarValue;

    private ProgressBar progressBooked;
    private ProgressBar progressOngoing;
    private ProgressBar progressCompleted;
    private ProgressBar progressCancelled;

    private ProgressBar progressVisible;
    private ProgressBar progressReported;
    private ProgressBar progressHidden;
    private ProgressBar progressDeleted;

    private RecyclerView rvTopLikedPosts;
    private RecyclerView rvTopReportedPosts;
    private RecyclerView rvTopCommentedPosts;

    private AdminTopPostAdapter topLikedAdapter;
    private AdminTopPostAdapter topReportedAdapter;
    private AdminTopPostAdapter topCommentedAdapter;
    private AdminAnalyticsRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        repository = new AdminAnalyticsRepository();

        bindViews();
        setupRecyclerViews();
        setupClicks();
        loadAnalytics();
    }

    private void bindViews() {
        btnBackAnalytics = findViewById(R.id.btnBackAnalytics);
        layoutLoadingAnalytics = findViewById(R.id.layoutLoadingAnalytics);
        layoutContentAnalytics = findViewById(R.id.layoutContentAnalytics);

        txtTotalAccounts = findViewById(R.id.txtTotalAccounts);
        txtTotalUsers = findViewById(R.id.txtTotalUsers);
        txtTotalDoctors = findViewById(R.id.txtTotalDoctors);
        txtTotalAdmins = findViewById(R.id.txtTotalAdmins);
        txtActive24h = findViewById(R.id.txtActive24h);
        txtOnlineNow = findViewById(R.id.txtOnlineNow);

        txtActiveRate = findViewById(R.id.txtActiveRate);
        txtOnlineRate = findViewById(R.id.txtOnlineRate);
        txtReportRate = findViewById(R.id.txtReportRate);
        txtCompletionRate = findViewById(R.id.txtCompletionRate);

        txtTotalConsultations = findViewById(R.id.txtTotalConsultations);
        txtOngoingConsultations = findViewById(R.id.txtOngoingConsultations);
        txtCompletedConsultations = findViewById(R.id.txtCompletedConsultations);
        txtBookedConsultations = findViewById(R.id.txtBookedConsultations);
        txtChatConsultations = findViewById(R.id.txtChatConsultations);
        txtCallConsultations = findViewById(R.id.txtCallConsultations);

        txtTotalPosts = findViewById(R.id.txtTotalPosts);
        txtReportedPosts = findViewById(R.id.txtReportedPosts);
        txtHiddenPosts = findViewById(R.id.txtHiddenPosts);
        txtDeletedPosts = findViewById(R.id.txtDeletedPosts);

        txtBookedBarValue = findViewById(R.id.txtBookedBarValue);
        txtOngoingBarValue = findViewById(R.id.txtOngoingBarValue);
        txtCompletedBarValue = findViewById(R.id.txtCompletedBarValue);
        txtCancelledBarValue = findViewById(R.id.txtCancelledBarValue);

        txtVisibleBarValue = findViewById(R.id.txtVisibleBarValue);
        txtReportedBarValue = findViewById(R.id.txtReportedBarValue);
        txtHiddenBarValue = findViewById(R.id.txtHiddenBarValue);
        txtDeletedBarValue = findViewById(R.id.txtDeletedBarValue);

        progressBooked = findViewById(R.id.progressBooked);
        progressOngoing = findViewById(R.id.progressOngoing);
        progressCompleted = findViewById(R.id.progressCompleted);
        progressCancelled = findViewById(R.id.progressCancelled);

        progressVisible = findViewById(R.id.progressVisible);
        progressReported = findViewById(R.id.progressReported);
        progressHidden = findViewById(R.id.progressHidden);
        progressDeleted = findViewById(R.id.progressDeleted);

        rvTopLikedPosts = findViewById(R.id.rvTopLikedPosts);
        rvTopReportedPosts = findViewById(R.id.rvTopReportedPosts);
        rvTopCommentedPosts = findViewById(R.id.rvTopCommentedPosts);
    }

    private void setupRecyclerViews() {
        topLikedAdapter = new AdminTopPostAdapter();
        topReportedAdapter = new AdminTopPostAdapter();
        topCommentedAdapter = new AdminTopPostAdapter();

        rvTopLikedPosts.setLayoutManager(new LinearLayoutManager(this));
        rvTopLikedPosts.setAdapter(topLikedAdapter);
        rvTopLikedPosts.setNestedScrollingEnabled(false);

        rvTopReportedPosts.setLayoutManager(new LinearLayoutManager(this));
        rvTopReportedPosts.setAdapter(topReportedAdapter);
        rvTopReportedPosts.setNestedScrollingEnabled(false);

        rvTopCommentedPosts.setLayoutManager(new LinearLayoutManager(this));
        rvTopCommentedPosts.setAdapter(topCommentedAdapter);
        rvTopCommentedPosts.setNestedScrollingEnabled(false);
    }

    private void setupClicks() {
        btnBackAnalytics.setOnClickListener(v -> finish());
    }

    private void loadAnalytics() {
        showLoading(true);

        repository.loadAnalytics(new AdminAnalyticsRepository.LoadAnalyticsListener() {
            @Override
            public void onSuccess(
                    @NonNull AdminAnalyticsOverview overview,
                    @NonNull List<AdminTopPostItem> topLikedPosts,
                    @NonNull List<AdminTopPostItem> topReportedPosts,
                    @NonNull List<AdminTopPostItem> topCommentedPosts
            ) {
                showLoading(false);
                bindOverview(overview);
                topLikedAdapter.submitList(topLikedPosts);
                topReportedAdapter.submitList(topReportedPosts);
                topCommentedAdapter.submitList(topCommentedPosts);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                showLoading(false);
                Toast.makeText(AdminAnalyticsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindOverview(@NonNull AdminAnalyticsOverview overview) {
        txtTotalAccounts.setText(String.valueOf(overview.getTotalAccounts()));
        txtTotalUsers.setText(String.valueOf(overview.getTotalUsers()));
        txtTotalDoctors.setText(String.valueOf(overview.getTotalDoctors()));
        txtTotalAdmins.setText(String.valueOf(overview.getTotalAdmins()));
        txtActive24h.setText(String.valueOf(overview.getActive24hUsers()));
        txtOnlineNow.setText(String.valueOf(overview.getOnlineNowUsers()));

        txtActiveRate.setText(overview.getActiveRatePercent() + "%");
        txtOnlineRate.setText(overview.getOnlineRatePercent() + "%");
        txtReportRate.setText(overview.getReportRatePercent() + "%");
        txtCompletionRate.setText(overview.getCompletionRatePercent() + "%");

        txtTotalConsultations.setText(String.valueOf(overview.getTotalConsultations()));
        txtOngoingConsultations.setText(String.valueOf(overview.getOngoingConsultations()));
        txtCompletedConsultations.setText(String.valueOf(overview.getCompletedConsultations()));
        txtBookedConsultations.setText(String.valueOf(overview.getBookedConsultations()));
        txtChatConsultations.setText(String.valueOf(overview.getChatConsultations()));
        txtCallConsultations.setText(String.valueOf(overview.getCallConsultations()));

        txtTotalPosts.setText(String.valueOf(overview.getTotalPosts()));
        txtReportedPosts.setText(String.valueOf(overview.getReportedPosts()));
        txtHiddenPosts.setText(String.valueOf(overview.getHiddenPosts()));
        txtDeletedPosts.setText(String.valueOf(overview.getDeletedPosts()));

        bindConsultationChart(overview);
        bindModerationChart(overview);
    }

    private void bindConsultationChart(@NonNull AdminAnalyticsOverview overview) {
        int total = overview.getTotalConsultations();

        bindBreakdownBar(progressBooked, txtBookedBarValue, overview.getBookedConsultations(), total);
        bindBreakdownBar(progressOngoing, txtOngoingBarValue, overview.getOngoingConsultations(), total);
        bindBreakdownBar(progressCompleted, txtCompletedBarValue, overview.getCompletedConsultations(), total);
        bindBreakdownBar(progressCancelled, txtCancelledBarValue, overview.getCancelledConsultations(), total);
    }

    private void bindModerationChart(@NonNull AdminAnalyticsOverview overview) {
        int totalPosts = overview.getTotalPosts();
        int deleted = overview.getDeletedPosts();
        int hidden = overview.getHiddenPosts();
        int reported = overview.getReportedPosts();
        int visible = Math.max(0, totalPosts - hidden - deleted);

        bindBreakdownBar(progressVisible, txtVisibleBarValue, visible, totalPosts);
        bindBreakdownBar(progressReported, txtReportedBarValue, reported, totalPosts);
        bindBreakdownBar(progressHidden, txtHiddenBarValue, hidden, totalPosts);
        bindBreakdownBar(progressDeleted, txtDeletedBarValue, deleted, totalPosts);
    }

    private void bindBreakdownBar(
            @NonNull ProgressBar progressBar,
            @NonNull TextView valueView,
            int value,
            int total
    ) {
        int safeTotal = Math.max(total, 1);
        int safeValue = Math.max(value, 0);
        int percent = Math.round((safeValue * 100f) / safeTotal);

        progressBar.setMax(safeTotal);
        progressBar.setProgress(Math.min(safeValue, safeTotal));
        valueView.setText(safeValue + " • " + percent + "%");
    }

    private void showLoading(boolean isLoading) {
        layoutLoadingAnalytics.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        layoutContentAnalytics.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}