package com.example.heami.ui.community;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.data.models.CommunityPostModel;
import com.example.heami.data.repositories.CommunityRepository;
import com.example.heami.ui.main.BottomNavManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CommunityActivity extends AppCompatActivity {

    private LinearLayout layoutPostsContainer;
    private TextView txtPostsCount;
    private View btnCommunityChat;

    private CommunityRepository communityRepository;
    private ActivityResultLauncher<Intent> postCommentLauncher;

    private final List<CommunityPostModel> allPosts = new ArrayList<>();

    private final Map<String, Integer> myHugCountMap = new HashMap<>();
    private final Set<String> loadingMyHugCountPostIds = new HashSet<>();
    private final Set<String> processingHugPostIds = new HashSet<>();

    private final Map<String, Boolean> myEmpathyStateMap = new HashMap<>();
    private final Set<String> loadingMyEmpathyStatePostIds = new HashSet<>();
    private final Set<String> processingEmpathyPostIds = new HashSet<>();

    private final Set<String> processingReportPostIds = new HashSet<>();

    private final Set<String> processingMyPostActionPostIds = new HashSet<>();

    private String currentFilter = "all";
    private boolean isLoadingPosts = false;

    private static final int COLLAPSED_POST_MAX_LINES = 4;
    private final Set<String> expandedPostIds = new HashSet<>();

    private TextView txtOnlineCount;
    private TextView badgeCommunityChat;
    private TextView txtStatValue1;
    private TextView txtStatValue2;
    private TextView txtStatValue3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        BottomNavManager.setup(this, BottomNavManager.TAB_COMMUNITY);

        bindViews();
        initData();
        initLaunchers();
        allowOverflow();
        setupActions();
        setupFilters();
        startCommunityAnimations();
        loadCommunityPosts();
        loadCommunityDashboardStats();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null && intent.getBooleanExtra("refresh_community_feed", false)) {
            loadCommunityPosts();
            loadCommunityDashboardStats();
            intent.removeExtra("refresh_community_feed");
            intent.removeExtra("created_post_id");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCommunityDashboardStats();
    }

    private void bindViews() {
        layoutPostsContainer = findViewById(R.id.layoutPostsContainer);
        txtPostsCount = findViewById(R.id.txtPostsCount);
        btnCommunityChat = findViewById(R.id.btnCommunityChat);

        txtOnlineCount = findViewById(R.id.txtOnlineCount);
        badgeCommunityChat = findViewById(R.id.badgeCommunityChat);
        txtStatValue1 = findViewById(R.id.txtStatValue1);
        txtStatValue2 = findViewById(R.id.txtStatValue2);
        txtStatValue3 = findViewById(R.id.txtStatValue3);
    }

    private void initData() {
        communityRepository = new CommunityRepository();
    }

    private void initLaunchers() {
        postCommentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null
                            && result.getData().getBooleanExtra("refresh_community_feed", false)) {
                        loadCommunityPosts();
                    }
                }
        );
    }

    private void allowOverflow() {
        setClipChildrenRecursive(findViewById(R.id.communityRoot), false);
        setClipChildrenRecursive(findViewById(R.id.layoutMoodMatchCard), false);
        setClipChildrenRecursive(findViewById(R.id.layoutMoodMatchInner), false);
        setClipChildrenRecursive(findViewById(R.id.layoutMoodTop), false);
        setClipChildrenRecursive(findViewById(R.id.layoutShuffleWrap), false);
    }

    private void setClipChildrenRecursive(View view, boolean clip) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup vg = (ViewGroup) view;
        vg.setClipChildren(clip);
        vg.setClipToPadding(clip);
    }

    private void setupActions() {
        View moodMatchCard = findViewById(R.id.layoutMoodMatchCard);
        if (moodMatchCard != null) {
            moodMatchCard.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, MoodMatchActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        View btnShareYourFeeling = findViewById(R.id.btnShareYourFeeling);
        if (btnShareYourFeeling != null) {
            btnShareYourFeeling.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, ShareFeelingActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        if (btnCommunityChat != null) {
            btnCommunityChat.setOnClickListener(v -> {
                Intent intent = new Intent(CommunityActivity.this, CommunityChatListActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }
    }

    private void setupFilters() {
        View all = findViewById(R.id.filterTabAll);
        View happy = findViewById(R.id.filterTabHappy);
        View sad = findViewById(R.id.filterTabSad);
        View stress = findViewById(R.id.filterTabStress);
        View fear = findViewById(R.id.filterTabFear);
        View disgust = findViewById(R.id.filterTabDisgust);
        View angry = findViewById(R.id.filterTabAngry);

        if (all != null) all.setOnClickListener(v -> applyFilter("all"));
        if (happy != null) happy.setOnClickListener(v -> applyFilter("happy"));
        if (sad != null) sad.setOnClickListener(v -> applyFilter("sad"));
        if (stress != null) stress.setOnClickListener(v -> applyFilter("stress"));
        if (fear != null) fear.setOnClickListener(v -> applyFilter("fear"));
        if (disgust != null) disgust.setOnClickListener(v -> applyFilter("disgust"));
        if (angry != null) angry.setOnClickListener(v -> applyFilter("angry"));

        applyFilter("all");
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateFilterTabStyles(filter);
        renderPosts();
    }

    private void updateFilterTabStyles(String activeFilter) {
        updateSingleFilterTab(R.id.filterTabAll, activeFilter.equals("all"));
        updateSingleFilterTab(R.id.filterTabHappy, activeFilter.equals("happy"));
        updateSingleFilterTab(R.id.filterTabSad, activeFilter.equals("sad"));
        updateSingleFilterTab(R.id.filterTabStress, activeFilter.equals("stress"));
        updateSingleFilterTab(R.id.filterTabFear, activeFilter.equals("fear"));
        updateSingleFilterTab(R.id.filterTabDisgust, activeFilter.equals("disgust"));
        updateSingleFilterTab(R.id.filterTabAngry, activeFilter.equals("angry"));
    }

    private void updateSingleFilterTab(int viewId, boolean isActive) {
        TextView tab = findViewById(viewId);
        if (tab == null) return;

        if (isActive) {
            tab.setBackgroundResource(R.drawable.bg_filter_tab_active);
            tab.setTextColor(0xFFFFFFFF);
        } else {
            tab.setBackgroundResource(R.drawable.bg_filter_tab);
            tab.setTextColor(0xFFC0A8D0);
        }
    }

    private void loadCommunityPosts() {
        if (isLoadingPosts) return;

        isLoadingPosts = true;
        showLoadingState();

        communityRepository.getCommunityPosts(new CommunityRepository.LoadPostsListener() {
            @Override
            public void onSuccess(@NonNull List<CommunityPostModel> posts) {
                isLoadingPosts = false;

                allPosts.clear();
                allPosts.addAll(posts);

                myHugCountMap.clear();
                loadingMyHugCountPostIds.clear();
                processingHugPostIds.clear();

                myEmpathyStateMap.clear();
                loadingMyEmpathyStatePostIds.clear();
                processingEmpathyPostIds.clear();

                processingReportPostIds.clear();

                processingMyPostActionPostIds.clear();

                renderPosts();
                preloadMyInteractionStates(posts);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                isLoadingPosts = false;

                if (layoutPostsContainer != null) {
                    layoutPostsContainer.removeAllViews();
                    layoutPostsContainer.addView(createInfoTextView("Không thể tải bảng tin cộng đồng"));
                }

                updatePostsCount(0);

                Toast.makeText(
                        CommunityActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void preloadMyInteractionStates(@NonNull List<CommunityPostModel> posts) {
        for (CommunityPostModel post : posts) {
            if (post == null) continue;

            String postId = safeText(post.getPost_id(), "");
            if (postId.isEmpty()) continue;

            fetchMyHugCountForPost(postId, false);
            fetchMyEmpathyStateForPost(postId, false);
        }
    }

    private void fetchMyHugCountForPost(@NonNull String postId, boolean showErrorToast) {
        if (postId.isEmpty()) return;
        if (loadingMyHugCountPostIds.contains(postId)) return;

        loadingMyHugCountPostIds.add(postId);

        communityRepository.getMyHugCountForPost(postId, new CommunityRepository.LoadMyHugCountListener() {
            @Override
            public void onSuccess(int myHugCount) {
                loadingMyHugCountPostIds.remove(postId);
                myHugCountMap.put(postId, myHugCount);
                renderPosts();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                loadingMyHugCountPostIds.remove(postId);

                if (showErrorToast) {
                    Toast.makeText(
                            CommunityActivity.this,
                            errorMessage,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    private void fetchMyEmpathyStateForPost(@NonNull String postId, boolean showErrorToast) {
        if (postId.isEmpty()) return;
        if (loadingMyEmpathyStatePostIds.contains(postId)) return;

        loadingMyEmpathyStatePostIds.add(postId);

        communityRepository.getMyEmpathyStateForPost(postId, new CommunityRepository.LoadMyEmpathyStateListener() {
            @Override
            public void onSuccess(boolean hasEmpathy) {
                loadingMyEmpathyStatePostIds.remove(postId);
                myEmpathyStateMap.put(postId, hasEmpathy);
                renderPosts();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                loadingMyEmpathyStatePostIds.remove(postId);

                if (showErrorToast) {
                    Toast.makeText(
                            CommunityActivity.this,
                            errorMessage,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });
    }

    private void showLoadingState() {
        if (layoutPostsContainer == null) return;

        layoutPostsContainer.removeAllViews();
        layoutPostsContainer.addView(createInfoTextView("Đang tải chia sẻ cộng đồng..."));
        updatePostsCount(0);
    }

    private void renderPosts() {
        if (layoutPostsContainer == null) return;

        layoutPostsContainer.removeAllViews();

        List<CommunityPostModel> filteredPosts = getFilteredPosts();

        updatePostsCount(filteredPosts.size());

        if (filteredPosts.isEmpty()) {
            layoutPostsContainer.addView(createInfoTextView(getEmptyMessageByFilter()));
            return;
        }

        for (CommunityPostModel post : filteredPosts) {
            View postView = getLayoutInflater().inflate(
                    R.layout.item_post_shared,
                    layoutPostsContainer,
                    false
            );

            bindPostView(postView, post);
            layoutPostsContainer.addView(postView);
        }
    }

    @NonNull
    private List<CommunityPostModel> getFilteredPosts() {
        List<CommunityPostModel> filtered = new ArrayList<>();

        for (CommunityPostModel post : allPosts) {
            if (post == null) continue;

            String mood = safeText(post.getMood_tag(), "stress");

            if ("all".equals(currentFilter) || currentFilter.equals(mood)) {
                filtered.add(post);
            }
        }

        return filtered;
    }

    private void bindPostView(@NonNull View postView, @NonNull CommunityPostModel post) {
        View accent = postView.findViewById(R.id.viewPostAccent);
        View avatarStatus = postView.findViewById(R.id.viewPostAvatarStatus);

        TextView txtAvatarEmoji = postView.findViewById(R.id.txtPostAvatarEmoji);
        TextView txtUserName = postView.findViewById(R.id.txtPostUserName);
        TextView txtMoodTag = postView.findViewById(R.id.txtPostMoodTag);
        TextView txtPostTime = postView.findViewById(R.id.txtPostTime);
        TextView txtPostContent = postView.findViewById(R.id.txtPostContent);
        TextView txtPostHugCount = postView.findViewById(R.id.txtPostHugCount);
        TextView txtPostCommentCount = postView.findViewById(R.id.txtPostCommentCount);
        TextView txtPostEmpathyLabel = postView.findViewById(R.id.txtPostEmpathyLabel);
        TextView txtPostExpandToggle = postView.findViewById(R.id.txtPostExpandToggle);
        ImageView imgPostEmpathyIcon = postView.findViewById(R.id.imgPostEmpathyIcon);

        LinearLayout btnPostHugAction = postView.findViewById(R.id.btnPostHugAction);
        LinearLayout btnPostCommentAction = postView.findViewById(R.id.btnPostCommentAction);
        LinearLayout btnPostEmpathyAction = postView.findViewById(R.id.btnPostEmpathyAction);
        ImageButton btnPostReport = postView.findViewById(R.id.btnPostReport);

        String postId = safeText(post.getPost_id(), "");
        String mood = safeText(post.getMood_tag(), "stress");

        int myHugCount = myHugCountMap.containsKey(postId) ? myHugCountMap.get(postId) : 0;
        boolean isProcessingHug = processingHugPostIds.contains(postId);

        boolean hasEmpathy = myEmpathyStateMap.containsKey(postId)
                && Boolean.TRUE.equals(myEmpathyStateMap.get(postId));
        boolean isProcessingEmpathy = processingEmpathyPostIds.contains(postId);

        boolean isProcessingReport = processingReportPostIds.contains(postId);

        boolean isMyPost = isCurrentUserPost(post);
        boolean isProcessingMyPostAction = processingMyPostActionPostIds.contains(postId);

        if (txtUserName != null) {
            String displayName = post.isIs_anonymous()
                    ? "Ẩn danh"
                    : safeText(post.getAuthor_name(), "Người dùng Heami");
            txtUserName.setText(displayName);
        }

        if (txtMoodTag != null) {
            txtMoodTag.setText(getMoodLabel(mood));
            txtMoodTag.setBackgroundResource(getMoodTagBackground(mood));
            txtMoodTag.setTextColor(getMoodTextColor(mood));
        }

        if (txtPostTime != null) {
            txtPostTime.setText("· " + formatRelativeTime(post.getCreated_at()));
        }

        bindExpandablePostContent(post, txtPostContent, txtPostExpandToggle);

        if (txtPostHugCount != null) {
            txtPostHugCount.setText(buildHugLabel(post.getLike_count(), myHugCount));
        }

        if (txtPostCommentCount != null) {
            txtPostCommentCount.setText(String.valueOf(post.getComment_count()));
        }

        if (txtPostEmpathyLabel != null) {
            txtPostEmpathyLabel.setText(buildEmpathyLabel(post.getEmpathy_count(), hasEmpathy));
        }

        applyHugActionVisual(btnPostHugAction, txtPostHugCount, myHugCount > 0, isProcessingHug);
        applyCommentActionVisual(btnPostCommentAction, txtPostCommentCount);
        applyEmpathyActionVisual(
                btnPostEmpathyAction,
                txtPostEmpathyLabel,
                imgPostEmpathyIcon,
                hasEmpathy,
                isProcessingEmpathy
        );

        applyPostMoodStyle(mood, accent, txtAvatarEmoji, avatarStatus);

        postView.setOnClickListener(v -> openPostComments(post));

        if (btnPostHugAction != null) {
            btnPostHugAction.setEnabled(!isProcessingHug);
            btnPostHugAction.setAlpha(isProcessingHug ? 0.55f : 1f);

            btnPostHugAction.setOnClickListener(v -> submitAddHug(post));

            btnPostHugAction.setOnLongClickListener(v -> {
                handleHugLongPress(post);
                return true;
            });
        }

        if (btnPostCommentAction != null) {
            btnPostCommentAction.setOnClickListener(v -> openPostComments(post));
        }

        if (btnPostEmpathyAction != null) {
            btnPostEmpathyAction.setEnabled(!isProcessingEmpathy);
            btnPostEmpathyAction.setAlpha(isProcessingEmpathy ? 0.55f : 1f);
            btnPostEmpathyAction.setOnClickListener(v -> submitToggleEmpathy(post));
        }

        if (btnPostReport != null) {
            boolean disableButton = isMyPost ? isProcessingMyPostAction : isProcessingReport;

            btnPostReport.setEnabled(!disableButton);
            btnPostReport.setAlpha(disableButton ? 0.55f : 1f);

            if (isMyPost) {
                btnPostReport.setImageResource(R.drawable.ic_more_heami);
                btnPostReport.setBackgroundResource(R.drawable.bg_post_owner_menu_btn);
                btnPostReport.setColorFilter(0xFFB59FCB, PorterDuff.Mode.SRC_IN);
                btnPostReport.setContentDescription("Tùy chọn bài viết");
            } else {
                btnPostReport.setImageResource(R.drawable.ic_report_flag);
                btnPostReport.setBackgroundResource(R.drawable.bg_report_btn);
                btnPostReport.setColorFilter(0xFFB59FCB, PorterDuff.Mode.SRC_IN);
                btnPostReport.setContentDescription("Báo cáo");
            }

            btnPostReport.setOnClickListener(v -> {
                if (isMyPost) {
                    showMyPostOptions(post);
                } else {
                    showReportDialog(post);
                }
            });
        }
    }

    private void openPostComments(@NonNull CommunityPostModel post) {
        Intent intent = new Intent(CommunityActivity.this, PostCommentActivity.class);
        intent.putExtra("post_id", post.getPost_id());
        postCommentLauncher.launch(intent);
        overridePendingTransition(0, 0);
    }

    private void submitAddHug(@NonNull CommunityPostModel post) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để gửi ôm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingHugPostIds.contains(postId)) {
            return;
        }

        processingHugPostIds.add(postId);
        renderPosts();

        communityRepository.addHugToPost(postId, new CommunityRepository.AddHugListener() {
            @Override
            public void onSuccess(int newMyHugCount) {
                processingHugPostIds.remove(postId);

                myHugCountMap.put(postId, newMyHugCount);
                post.setLike_count(post.getLike_count() + 1);

                renderPosts();

                if (newMyHugCount <= 1) {
                    Toast.makeText(
                            CommunityActivity.this,
                            "Bạn đã gửi một cái ôm 🤗",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            CommunityActivity.this,
                            "Bạn đã ôm " + newMyHugCount + " lần 🤗",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                processingHugPostIds.remove(postId);
                renderPosts();

                Toast.makeText(
                        CommunityActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void handleHugLongPress(@NonNull CommunityPostModel post) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để huỷ ôm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingHugPostIds.contains(postId)) {
            return;
        }

        if (!myHugCountMap.containsKey(postId)) {
            fetchMyHugCountForPost(postId, true);
            Toast.makeText(
                    this,
                    "Đang tải số ôm của bạn, hãy nhấn giữ lại sau một chút",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int myHugCount = myHugCountMap.get(postId);

        if (myHugCount <= 0) {
            Toast.makeText(
                    this,
                    "Bạn chưa gửi ôm nào để huỷ",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        showUnhugDialog(post, myHugCount);
    }

    private void showUnhugDialog(@NonNull CommunityPostModel post, int myHugCount) {
        if (isFinishing()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_unhug_confirm, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView txtTitle = view.findViewById(R.id.txtUnhugDialogTitle);
        TextView txtSubtitle = view.findViewById(R.id.txtUnhugDialogSubtitle);
        TextView txtMessage = view.findViewById(R.id.txtUnhugDialogMessage);
        TextView btnKeepHug = view.findViewById(R.id.btnKeepHug);
        TextView btnConfirmUnhug = view.findViewById(R.id.btnConfirmUnhug);

        if (txtTitle != null) {
            txtTitle.setText(myHugCount == 1 ? "Huỷ cái ôm này?" : "Huỷ những cái ôm này?");
        }

        if (txtSubtitle != null) {
            txtSubtitle.setText(myHugCount == 1
                    ? "Bạn sắp rút lại 1 cái ôm đã gửi."
                    : "Bạn sắp rút lại " + myHugCount + " cái ôm đã gửi.");
        }

        if (txtMessage != null) {
            txtMessage.setText(buildUnhugDialogMessage(myHugCount));
        }

        if (btnKeepHug != null) {
            btnKeepHug.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirmUnhug != null) {
            btnConfirmUnhug.setOnClickListener(v -> {
                dialog.dismiss();
                clearMyHugs(post);
            });
        }

        dialog.show();
    }

    @NonNull
    private String buildUnhugDialogMessage(int myHugCount) {
        if (myHugCount <= 1) {
            return "Bạn muốn rút lại 1 cái ôm đã gửi cho bài chia sẻ này không?";
        }
        return "Bạn muốn rút lại " + myHugCount + " cái ôm đã gửi cho bài chia sẻ này không?";
    }

    private void clearMyHugs(@NonNull CommunityPostModel post) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để huỷ ôm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingHugPostIds.contains(postId)) {
            return;
        }

        processingHugPostIds.add(postId);
        renderPosts();

        communityRepository.clearMyHugsForPost(postId, new CommunityRepository.ClearMyHugsListener() {
            @Override
            public void onSuccess(int removedHugCount) {
                processingHugPostIds.remove(postId);

                if (removedHugCount > 0) {
                    myHugCountMap.put(postId, 0);
                    post.setLike_count(Math.max(0, post.getLike_count() - removedHugCount));
                    renderPosts();

                    Toast.makeText(
                            CommunityActivity.this,
                            "Đã huỷ " + removedHugCount + " cái ôm bạn đã gửi",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    renderPosts();

                    Toast.makeText(
                            CommunityActivity.this,
                            "Bạn chưa gửi ôm nào để huỷ",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                processingHugPostIds.remove(postId);
                renderPosts();

                Toast.makeText(
                        CommunityActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void submitToggleEmpathy(@NonNull CommunityPostModel post) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để đồng cảm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingEmpathyPostIds.contains(postId)) {
            return;
        }

        processingEmpathyPostIds.add(postId);
        renderPosts();

        communityRepository.toggleEmpathyForPost(postId, new CommunityRepository.ToggleEmpathyListener() {
            @Override
            public void onSuccess(boolean nowHasEmpathy) {
                processingEmpathyPostIds.remove(postId);

                myEmpathyStateMap.put(postId, nowHasEmpathy);

                if (nowHasEmpathy) {
                    post.setEmpathy_count(post.getEmpathy_count() + 1);
                    Toast.makeText(
                            CommunityActivity.this,
                            "Bạn đã gửi một sự đồng cảm 🌿",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    post.setEmpathy_count(Math.max(0, post.getEmpathy_count() - 1));
                    Toast.makeText(
                            CommunityActivity.this,
                            "Bạn đã bỏ đồng cảm",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                renderPosts();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                processingEmpathyPostIds.remove(postId);
                renderPosts();

                Toast.makeText(
                        CommunityActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void showReportDialog(@NonNull CommunityPostModel post) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingReportPostIds.contains(postId)) {
            return;
        }

        if (isFinishing()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_report_post, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView optionHarassment = view.findViewById(R.id.optionReportHarassment);
        TextView optionInappropriate = view.findViewById(R.id.optionReportInappropriate);
        TextView optionSpam = view.findViewById(R.id.optionReportSpam);
        TextView optionDangerous = view.findViewById(R.id.optionReportDangerous);
        TextView optionMisinformation = view.findViewById(R.id.optionReportMisinformation);
        TextView optionOther = view.findViewById(R.id.optionReportOther);

        EditText edtExtraNote = view.findViewById(R.id.edtReportExtraNote);
        TextView btnCancelReport = view.findViewById(R.id.btnCancelReport);
        TextView btnSubmitReport = view.findViewById(R.id.btnSubmitReport);

        TextView[] allOptions = new TextView[]{
                optionHarassment,
                optionInappropriate,
                optionSpam,
                optionDangerous,
                optionMisinformation,
                optionOther
        };

        final String[] selectedReasonCode = {""};
        final String[] selectedReasonLabel = {""};

        if (optionHarassment != null) {
            setupReportReasonOption(
                    optionHarassment,
                    allOptions,
                    "HARASSMENT",
                    "Quấy rối / xúc phạm",
                    selectedReasonCode,
                    selectedReasonLabel,
                    btnSubmitReport
            );
        }

        if (optionInappropriate != null) {
            setupReportReasonOption(
                    optionInappropriate,
                    allOptions,
                    "INAPPROPRIATE",
                    "Nội dung không phù hợp",
                    selectedReasonCode,
                    selectedReasonLabel,
                    btnSubmitReport
            );
        }

        if (optionSpam != null) {
            setupReportReasonOption(
                    optionSpam,
                    allOptions,
                    "SPAM",
                    "Spam / quảng cáo",
                    selectedReasonCode,
                    selectedReasonLabel,
                    btnSubmitReport
            );
        }

        if (optionDangerous != null) {
            setupReportReasonOption(
                    optionDangerous,
                    allOptions,
                    "DANGEROUS",
                    "Nội dung tiêu cực nguy hiểm",
                    selectedReasonCode,
                    selectedReasonLabel,
                    btnSubmitReport
            );
        }

        if (optionMisinformation != null) {
            setupReportReasonOption(
                    optionMisinformation,
                    allOptions,
                    "MISINFORMATION",
                    "Thông tin sai lệch",
                    selectedReasonCode,
                    selectedReasonLabel,
                    btnSubmitReport
            );
        }

        if (optionOther != null) {
            setupReportReasonOption(
                    optionOther,
                    allOptions,
                    "OTHER",
                    "Khác",
                    selectedReasonCode,
                    selectedReasonLabel,
                    btnSubmitReport
            );
        }

        updateReportSubmitButtonState(btnSubmitReport, false);

        if (btnCancelReport != null) {
            btnCancelReport.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmitReport != null) {
            btnSubmitReport.setOnClickListener(v -> {
                if (selectedReasonCode[0].trim().isEmpty()) {
                    return;
                }

                String extraNote = "";
                if (edtExtraNote != null && edtExtraNote.getText() != null) {
                    extraNote = edtExtraNote.getText().toString().trim();
                }

                dialog.dismiss();
                submitReportPost(post, selectedReasonCode[0], selectedReasonLabel[0], extraNote);
            });
        }

        dialog.show();
    }

    private void setupReportReasonOption(
            @NonNull TextView optionView,
            @NonNull TextView[] allOptions,
            @NonNull String reasonCode,
            @NonNull String reasonLabel,
            @NonNull String[] selectedReasonCode,
            @NonNull String[] selectedReasonLabel,
            TextView btnSubmitReport
    ) {
        optionView.setOnClickListener(v -> {
            for (TextView option : allOptions) {
                if (option == null) continue;
                option.setBackgroundResource(R.drawable.bg_report_reason_default);
                option.setTextColor(0xFF6D5A88);
            }

            optionView.setBackgroundResource(R.drawable.bg_report_reason_selected);
            optionView.setTextColor(0xFF7F5AF0);

            selectedReasonCode[0] = reasonCode;
            selectedReasonLabel[0] = reasonLabel;

            updateReportSubmitButtonState(btnSubmitReport, true);
        });
    }

    private void updateReportSubmitButtonState(TextView btnSubmitReport, boolean enabled) {
        if (btnSubmitReport == null) return;

        btnSubmitReport.setEnabled(enabled);
        btnSubmitReport.setAlpha(enabled ? 1f : 0.75f);
        btnSubmitReport.setBackgroundResource(
                enabled ? R.drawable.bg_report_submit_active : R.drawable.bg_report_submit_inactive
        );
    }

    private void submitReportPost(
            @NonNull CommunityPostModel post,
            @NonNull String reasonCode,
            @NonNull String reasonLabel,
            @NonNull String extraNote
    ) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingReportPostIds.contains(postId)) {
            return;
        }

        int removedIndex = findPostIndexInAllPosts(postId);

        processingReportPostIds.add(postId);
        renderPosts();

        communityRepository.submitPostReport(
                postId,
                reasonCode,
                reasonLabel,
                extraNote,
                new CommunityRepository.SubmitReportListener() {
                    @Override
                    public void onSuccess(boolean autoHidden) {
                        processingReportPostIds.remove(postId);
                        removePostFromFeed(postId);
                        renderPosts();
                        showReportUndoSnackbar(post, removedIndex, autoHidden);
                    }

                    @Override
                    public void onAlreadyReported() {
                        processingReportPostIds.remove(postId);
                        removePostFromFeed(postId);
                        renderPosts();

                        Toast.makeText(
                                CommunityActivity.this,
                                "Bạn đã báo cáo bài viết này rồi",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        processingReportPostIds.remove(postId);
                        renderPosts();

                        Toast.makeText(
                                CommunityActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private int findPostIndexInAllPosts(@NonNull String postId) {
        for (int i = 0; i < allPosts.size(); i++) {
            CommunityPostModel item = allPosts.get(i);
            if (item != null && postId.equals(safeText(item.getPost_id(), ""))) {
                return i;
            }
        }
        return -1;
    }

    private void showReportUndoSnackbar(
            @NonNull CommunityPostModel post,
            int removedIndex,
            boolean autoHidden
    ) {
        View anchor = findViewById(R.id.communityRoot);
        if (anchor == null) {
            anchor = findViewById(android.R.id.content);
        }

        String message = autoHidden
                ? "Đã báo cáo. Bài viết đã bị ẩn khỏi cộng đồng."
                : "Đã báo cáo. Bài viết đã bị ẩn khỏi feed của bạn.";

        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_LONG);
        snackbar.setDuration(5000);
        snackbar.setAction("Hoàn tác", v -> undoReportedPost(post, removedIndex));

        snackbar.setBackgroundTint(0xFFFFF7FB);
        snackbar.setTextColor(0xFF4E3A68);
        snackbar.setActionTextColor(0xFFE56AA6);
        snackbar.setTextMaxLines(3);

        View snackbarView = snackbar.getView();

        ViewGroup.LayoutParams rawParams = snackbarView.getLayoutParams();
        if (rawParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
            params.leftMargin = dp(16);
            params.rightMargin = dp(16);
            params.bottomMargin = dp(20);
            snackbarView.setLayoutParams(params);
        }

        snackbarView.setElevation(dp(6));

        snackbar.show();
    }

    private void undoReportedPost(@NonNull CommunityPostModel post, int removedIndex) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để hoàn tác", Toast.LENGTH_SHORT).show();
            return;
        }

        communityRepository.undoPostReport(postId, new CommunityRepository.UndoReportListener() {
            @Override
            public void onSuccess(boolean postVisibleAgain) {
                if (postVisibleAgain) {
                    restorePostToFeed(post, removedIndex);

                    Toast.makeText(
                            CommunityActivity.this,
                            "Đã hoàn tác báo cáo",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            CommunityActivity.this,
                            "Đã hoàn tác báo cáo nhưng bài viết vẫn bị ẩn do cộng đồng đã báo cáo",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                Toast.makeText(
                        CommunityActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void restorePostToFeed(@NonNull CommunityPostModel post, int insertIndex) {
        String postId = safeText(post.getPost_id(), "");

        for (CommunityPostModel item : allPosts) {
            if (item != null && postId.equals(safeText(item.getPost_id(), ""))) {
                return;
            }
        }

        if (insertIndex >= 0 && insertIndex <= allPosts.size()) {
            allPosts.add(insertIndex, post);
        } else {
            allPosts.add(0, post);
        }

        fetchMyHugCountForPost(postId, false);
        fetchMyEmpathyStateForPost(postId, false);
        renderPosts();
    }

    private void removePostFromFeed(@NonNull String postId) {
        allPosts.removeIf(post -> post != null && postId.equals(safeText(post.getPost_id(), "")));
        myHugCountMap.remove(postId);
        myEmpathyStateMap.remove(postId);
        loadingMyHugCountPostIds.remove(postId);
        loadingMyEmpathyStatePostIds.remove(postId);
        processingHugPostIds.remove(postId);
        processingEmpathyPostIds.remove(postId);
        processingReportPostIds.remove(postId);
        processingMyPostActionPostIds.remove(postId);
    }

    private boolean isCurrentUserPost(@NonNull CommunityPostModel post) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return false;

        String currentUid = safeText(firebaseUser.getUid(), "");
        String ownerUid = safeText(post.getUser_id(), "");

        return !currentUid.isEmpty() && currentUid.equals(ownerUid);
    }

    private void showMyPostOptions(@NonNull CommunityPostModel post) {
        if (isFinishing()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_my_post_actions, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View rowEditMyPost = view.findViewById(R.id.rowEditMyPost);
        View rowDeleteMyPost = view.findViewById(R.id.rowDeleteMyPost);
        View btnDismissMyPostActions = view.findViewById(R.id.btnDismissMyPostActions);

        if (rowEditMyPost != null) {
            rowEditMyPost.setOnClickListener(v -> {
                dialog.dismiss();
                showEditPostDialog(post);
            });
        }

        if (rowDeleteMyPost != null) {
            rowDeleteMyPost.setOnClickListener(v -> {
                dialog.dismiss();
                showDeletePostDialog(post);
            });
        }

        if (btnDismissMyPostActions != null) {
            btnDismissMyPostActions.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showEditPostDialog(@NonNull CommunityPostModel post) {
        String postId = safeText(post.getPost_id(), "");
        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để chỉnh sửa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingMyPostActionPostIds.contains(postId)) {
            return;
        }

        if (isFinishing()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_post, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText edtEditPostContent = view.findViewById(R.id.edtEditPostContent);
        View btnCancelEditPost = view.findViewById(R.id.btnCancelEditPost);
        View btnSaveEditPost = view.findViewById(R.id.btnSaveEditPost);

        if (edtEditPostContent != null) {
            edtEditPostContent.setText(safeText(post.getContent(), ""));
            edtEditPostContent.setSelection(edtEditPostContent.getText().length());
        }

        if (btnCancelEditPost != null) {
            btnCancelEditPost.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSaveEditPost != null) {
            btnSaveEditPost.setOnClickListener(v -> {
                String newContent = edtEditPostContent != null && edtEditPostContent.getText() != null
                        ? edtEditPostContent.getText().toString().trim()
                        : "";

                String oldContent = safeText(post.getContent(), "");

                if (newContent.isEmpty()) {
                    Toast.makeText(this, "Nội dung bài viết không được để trống", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newContent.equals(oldContent)) {
                    dialog.dismiss();
                    return;
                }

                dialog.dismiss();
                submitEditPost(post, newContent);
            });
        }

        dialog.show();
    }

    private void submitEditPost(@NonNull CommunityPostModel post, @NonNull String newContent) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để chỉnh sửa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingMyPostActionPostIds.contains(postId)) {
            return;
        }

        processingMyPostActionPostIds.add(postId);
        renderPosts();

        communityRepository.updateMyPostContent(postId, newContent, new CommunityRepository.UpdatePostListener() {
            @Override
            public void onSuccess(@NonNull Timestamp updatedAt) {
                processingMyPostActionPostIds.remove(postId);

                post.setContent(newContent.trim());
                post.setUpdated_at(updatedAt);
                expandedPostIds.remove(postId);

                renderPosts();

                Toast.makeText(
                        CommunityActivity.this,
                        "Đã cập nhật bài viết",
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                processingMyPostActionPostIds.remove(postId);
                renderPosts();

                Toast.makeText(
                        CommunityActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void showDeletePostDialog(@NonNull CommunityPostModel post) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để xóa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingMyPostActionPostIds.contains(postId)) {
            return;
        }

        if (isFinishing()) return;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_post, null);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnCancelDeletePost = view.findViewById(R.id.btnCancelDeletePost);
        View btnConfirmDeletePost = view.findViewById(R.id.btnConfirmDeletePost);

        if (btnCancelDeletePost != null) {
            btnCancelDeletePost.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirmDeletePost != null) {
            btnConfirmDeletePost.setOnClickListener(v -> {
                dialog.dismiss();
                submitDeletePost(post);
            });
        }

        dialog.show();
    }

    private void submitDeletePost(@NonNull CommunityPostModel post) {
        String postId = safeText(post.getPost_id(), "");

        if (postId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài viết để xóa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (processingMyPostActionPostIds.contains(postId)) {
            return;
        }

        processingMyPostActionPostIds.add(postId);
        renderPosts();

        communityRepository.deleteMyPost(postId, new CommunityRepository.DeletePostListener() {
            @Override
            public void onSuccess() {
                processingMyPostActionPostIds.remove(postId);
                expandedPostIds.remove(postId);
                removePostFromFeed(postId);
                renderPosts();

                Toast.makeText(
                        CommunityActivity.this,
                        "Đã xóa bài viết",
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                processingMyPostActionPostIds.remove(postId);
                renderPosts();

                Toast.makeText(
                        CommunityActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @NonNull
    private String buildHugLabel(int totalHugCount, int myHugCount) {
        if (myHugCount > 0) {
            return "Ôm " + totalHugCount + " • " + myHugCount;
        }
        return "Ôm " + totalHugCount;
    }

    @NonNull
    private String buildEmpathyLabel(int totalEmpathyCount, boolean hasEmpathy) {
        return "Đồng cảm " + totalEmpathyCount;
    }

    private TextView createInfoTextView(String message) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        textView.setPadding(dp(18), dp(18), dp(18), dp(18));
        textView.setText(message);
        textView.setTextSize(14f);
        textView.setTextColor(0xFF8E7AA7);
        return textView;
    }

    private void updatePostsCount(int count) {
        if (txtPostsCount == null) return;

        if (count <= 0) {
            txtPostsCount.setText("0 chia sẻ");
        } else if (count == 1) {
            txtPostsCount.setText("1 chia sẻ");
        } else {
            txtPostsCount.setText(String.format(Locale.getDefault(), "%d chia sẻ", count));
        }
    }

    private String getEmptyMessageByFilter() {
        switch (currentFilter) {
            case "happy":
                return "Chưa có chia sẻ nào ở mood vui vẻ.";
            case "sad":
                return "Chưa có chia sẻ nào ở mood buồn.";
            case "stress":
                return "Chưa có chia sẻ nào ở mood căng thẳng.";
            case "fear":
                return "Chưa có chia sẻ nào ở mood sợ hãi.";
            case "disgust":
                return "Chưa có chia sẻ nào ở mood ghê tởm.";
            case "angry":
                return "Chưa có chia sẻ nào ở mood tức giận.";
            default:
                return "Chưa có bài chia sẻ nào trong cộng đồng.";
        }
    }

    private void applyPostMoodStyle(String mood, View accent, TextView avatarEmoji, View avatarStatus) {
        if (accent == null || avatarEmoji == null || avatarStatus == null) return;

        switch (mood) {
            case "happy":
                accent.setBackgroundResource(R.drawable.bg_post_accent_orange);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_orange);
                avatarEmoji.setText("🌻");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_orange);
                break;

            case "sad":
                accent.setBackgroundResource(R.drawable.bg_post_accent_blue);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_blue);
                avatarEmoji.setText("🌧️");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_blue);
                break;

            case "stress":
                accent.setBackgroundResource(R.drawable.bg_post_accent_purple);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_purple);
                avatarEmoji.setText("🌪️");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_purple);
                break;

            case "fear":
                accent.setBackgroundResource(R.drawable.bg_post_accent_mint);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_mint);
                avatarEmoji.setText("😰");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_mint);
                break;

            case "disgust":
                accent.setBackgroundResource(R.drawable.bg_post_accent_green);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_green);
                avatarEmoji.setText("🤢");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_green);
                break;

            case "angry":
                accent.setBackgroundResource(R.drawable.bg_post_accent_red);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_red);
                avatarEmoji.setText("😠");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_red);
                break;

            default:
                accent.setBackgroundResource(R.drawable.bg_post_accent_purple);
                avatarEmoji.setBackgroundResource(R.drawable.bg_avatar_box_purple);
                avatarEmoji.setText("🌪️");
                avatarStatus.setBackgroundResource(R.drawable.bg_avatar_status_purple);
                break;
        }
    }

    private String getMoodLabel(String mood) {
        switch (mood) {
            case "happy":
                return "Đang vui";
            case "sad":
                return "Đang buồn";
            case "stress":
                return "Đang stress";
            case "fear":
                return "Đang sợ hãi";
            case "disgust":
                return "Đang ghê tởm";
            case "angry":
                return "Đang tức giận";
            default:
                return "Cảm xúc";
        }
    }

    private int getMoodTagBackground(String mood) {
        switch (mood) {
            case "happy":
                return R.drawable.bg_mood_tag_happy;
            case "sad":
                return R.drawable.bg_mood_tag_sad;
            case "stress":
                return R.drawable.bg_mood_tag_stress;
            case "fear":
                return R.drawable.bg_mood_tag_fear;
            case "disgust":
                return R.drawable.bg_mood_tag_disgust;
            case "angry":
                return R.drawable.bg_mood_tag_angry;
            default:
                return R.drawable.bg_mood_tag_stress;
        }
    }

    private int getMoodTextColor(String mood) {
        switch (mood) {
            case "happy":
                return 0xFFF5A623;
            case "sad":
                return 0xFF6B9EE8;
            case "stress":
                return 0xFFB06ED8;
            case "fear":
                return 0xFF4BBBAD;
            case "disgust":
                return 0xFF7FA56A;
            case "angry":
                return 0xFFE49797;
            default:
                return 0xFFB06ED8;
        }
    }

    private String formatRelativeTime(Timestamp timestamp) {
        if (timestamp == null) return "Vừa xong";

        long now = System.currentTimeMillis();
        long time = timestamp.toDate().getTime();
        long diff = Math.max(0L, now - time);

        long minute = 60_000L;
        long hour = 60 * minute;
        long day = 24 * hour;

        if (diff < minute) return "Vừa xong";
        if (diff < hour) return (diff / minute) + " phút trước";
        if (diff < day) return (diff / hour) + " giờ trước";
        return (diff / day) + " ngày trước";
    }

    private void applyHugActionVisual(
            LinearLayout button,
            TextView label,
            boolean hasMyHug,
            boolean isProcessing
    ) {
        if (button != null) {
            button.setBackgroundResource(
                    hasMyHug
                            ? R.drawable.bg_post_action_hug_active
                            : R.drawable.bg_post_action_neutral
            );
            button.setAlpha(isProcessing ? 0.55f : 1f);
        }

        if (label != null) {
            label.setTextColor(hasMyHug ? 0xFF7F5AF0 : 0xFF9E8DB5);
        }
    }

    private void applyCommentActionVisual(
            LinearLayout button,
            TextView label
    ) {
        if (button != null) {
            button.setBackgroundResource(R.drawable.bg_post_action_neutral);
            button.setAlpha(1f);
        }

        if (label != null) {
            label.setTextColor(0xFF9E8DB5);
        }
    }

    private void applyEmpathyActionVisual(
            LinearLayout button,
            TextView label,
            ImageView icon,
            boolean hasEmpathy,
            boolean isProcessing
    ) {
        int activeColor = 0xFF2FAF9A;
        int inactiveColor = 0xFF7C6B96;

        if (button != null) {
            button.setBackgroundResource(
                    hasEmpathy
                            ? R.drawable.bg_post_action_empathy_active
                            : R.drawable.bg_post_action_neutral
            );
            button.setAlpha(isProcessing ? 0.55f : 1f);
        }

        if (label != null) {
            label.setTextColor(hasEmpathy ? activeColor : inactiveColor);
        }

        if (icon != null) {
            icon.setColorFilter(
                    hasEmpathy ? activeColor : inactiveColor,
                    PorterDuff.Mode.SRC_IN
            );
        }
    }

    private void bindExpandablePostContent(
            @NonNull CommunityPostModel post,
            TextView txtPostContent,
            TextView txtPostExpandToggle
    ) {
        if (txtPostContent == null || txtPostExpandToggle == null) return;

        String postId = safeText(post.getPost_id(), "");
        String content = safeText(post.getContent(), "");

        txtPostContent.setText(content);

        boolean isExpanded = expandedPostIds.contains(postId);

        txtPostContent.setMaxLines(COLLAPSED_POST_MAX_LINES);
        txtPostContent.setEllipsize(TextUtils.TruncateAt.END);
        txtPostExpandToggle.setVisibility(View.GONE);

        txtPostContent.post(() -> {
            if (txtPostContent.getLayout() == null) return;

            int lineCount = txtPostContent.getLayout().getLineCount();
            int lastVisibleLine = Math.max(0, Math.min(COLLAPSED_POST_MAX_LINES - 1, lineCount - 1));

            boolean shouldShowToggle =
                    lineCount > COLLAPSED_POST_MAX_LINES
                            || txtPostContent.getLayout().getEllipsisCount(lastVisibleLine) > 0;

            if (!shouldShowToggle) {
                txtPostExpandToggle.setVisibility(View.GONE);
                txtPostContent.setMaxLines(Integer.MAX_VALUE);
                txtPostContent.setEllipsize(null);
                return;
            }

            applyExpandedPostUi(txtPostContent, txtPostExpandToggle, isExpanded);
        });

        txtPostExpandToggle.setOnClickListener(v -> {
            boolean currentlyExpanded = expandedPostIds.contains(postId);

            if (currentlyExpanded) {
                expandedPostIds.remove(postId);
            } else {
                expandedPostIds.add(postId);
            }

            applyExpandedPostUi(
                    txtPostContent,
                    txtPostExpandToggle,
                    expandedPostIds.contains(postId)
            );
        });
    }

    private void applyExpandedPostUi(
            @NonNull TextView txtPostContent,
            @NonNull TextView txtPostExpandToggle,
            boolean isExpanded
    ) {
        if (isExpanded) {
            txtPostContent.setMaxLines(Integer.MAX_VALUE);
            txtPostContent.setEllipsize(null);
            txtPostExpandToggle.setText("Thu gọn");
        } else {
            txtPostContent.setMaxLines(COLLAPSED_POST_MAX_LINES);
            txtPostContent.setEllipsize(TextUtils.TruncateAt.END);
            txtPostExpandToggle.setText("Xem thêm");
        }

        txtPostExpandToggle.setVisibility(View.VISIBLE);
    }

    private void loadCommunityDashboardStats() {
        if (communityRepository == null) return;

        communityRepository.loadCommunityDashboardStats(new CommunityRepository.LoadCommunityDashboardStatsListener() {
            @Override
            public void onSuccess(@NonNull CommunityRepository.CommunityDashboardStats stats) {
                if (txtOnlineCount != null) {
                    txtOnlineCount.setText(String.valueOf(stats.getActiveTodayUserCount()));
                }

                if (badgeCommunityChat != null) {
                    int unreadLikeCount = stats.getUnreadChatRoomCount();
                    if (unreadLikeCount > 0) {
                        badgeCommunityChat.setVisibility(View.VISIBLE);
                        badgeCommunityChat.setText(unreadLikeCount > 9 ? "9+" : String.valueOf(unreadLikeCount));
                    } else {
                        badgeCommunityChat.setVisibility(View.GONE);
                    }
                }

                if (txtStatValue1 != null) {
                    txtStatValue1.setText(String.valueOf(stats.getSearchingCount()));
                }

                if (txtStatValue2 != null) {
                    txtStatValue2.setText(String.valueOf(stats.getActiveMoodRoomCount()));
                }

                if (txtStatValue3 != null) {
                    txtStatValue3.setText(formatAverageMatchTime(stats.getAverageMatchSeconds()));
                }
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                if (txtOnlineCount != null) {
                    txtOnlineCount.setText("--");
                }

                if (badgeCommunityChat != null) {
                    badgeCommunityChat.setVisibility(View.GONE);
                }

                if (txtStatValue1 != null) {
                    txtStatValue1.setText("--");
                }

                if (txtStatValue2 != null) {
                    txtStatValue2.setText("--");
                }

                if (txtStatValue3 != null) {
                    txtStatValue3.setText("--");
                }
            }
        });
    }

    @NonNull
    private String formatAverageMatchTime(int averageMatchSeconds) {
        if (averageMatchSeconds <= 0) {
            return "--";
        }

        if (averageMatchSeconds < 60) {
            return averageMatchSeconds + "s";
        }

        int minutes = averageMatchSeconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }

        int hours = minutes / 60;
        return hours + "h";
    }

    @NonNull
    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void startCommunityAnimations() {
        startBlobFloat(findViewById(R.id.blobRoseTop), -10f, 8f, 1.04f, 8000, 0);
        startBlobFloat(findViewById(R.id.blobVioletMid), 8f, -10f, 1.05f, 8800, 700);
        startBlobFloat(findViewById(R.id.blobCyanMid), -8f, 8f, 1.03f, 8400, 1200);

        startPulseDot(findViewById(R.id.viewOnlineDot), 1.0f, 1.25f, 1900);

        startSubtleFloat(findViewById(R.id.layoutOnlineBadge), 4f, 3600, 0);
        startSubtleFloat(findViewById(R.id.btnCommunityChat), 4f, 3600, 500);
        startSubtleFloat(findViewById(R.id.layoutSafetyNotice), 3f, 4200, 900);

        startPulseFadeExpand(findViewById(R.id.viewMoodHeroRingOuter), 0.82f, 1.18f, 0.20f, 0f, 1800, 0);
        startPulseFadeExpand(findViewById(R.id.viewMoodHeroRingInner), 0.90f, 1.12f, 0.28f, 0f, 1350, 120);

        startFloatRotateScale(findViewById(R.id.imgSparkle1), 3f, 8f, 1.08f, 2800, 0);
        startFloatRotateScale(findViewById(R.id.imgSparkle2), 4f, -10f, 1.10f, 3000, 250);
        startFloatRotateScale(findViewById(R.id.imgSparkle3), 5f, 12f, 1.12f, 3200, 500);

        startSubtleFloat(findViewById(R.id.btnShareYourFeeling), 3f, 3600, 300);
    }

    private void startBlobFloat(View view, float dxDp, float dyDp, float scaleTo, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator x = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, dp(dxDp), 0f);
        ObjectAnimator y = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, dp(dyDp), 0f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, scaleTo, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, scaleTo, 1f);

        x.setDuration(duration);
        y.setDuration(duration);
        sx.setDuration(duration);
        sy.setDuration(duration);

        x.setStartDelay(delay);
        y.setStartDelay(delay);
        sx.setStartDelay(delay);
        sy.setStartDelay(delay);

        x.setRepeatCount(ValueAnimator.INFINITE);
        y.setRepeatCount(ValueAnimator.INFINITE);
        sx.setRepeatCount(ValueAnimator.INFINITE);
        sy.setRepeatCount(ValueAnimator.INFINITE);

        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        x.setInterpolator(interpolator);
        y.setInterpolator(interpolator);
        sx.setInterpolator(interpolator);
        sy.setInterpolator(interpolator);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(x, y, sx, sy);
        set.start();
    }

    private void startSubtleFloat(View view, float distanceDp, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator y = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -dp(distanceDp), 0f);
        y.setDuration(duration);
        y.setStartDelay(delay);
        y.setRepeatCount(ValueAnimator.INFINITE);
        y.setInterpolator(new AccelerateDecelerateInterpolator());
        y.start();
    }

    private void startPulseDot(View view, float fromScale, float toScale, long duration) {
        if (view == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, fromScale, toScale, fromScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, fromScale, toScale, fromScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.65f, 1f);

        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        scaleX.setInterpolator(interpolator);
        scaleY.setInterpolator(interpolator);
        alpha.setInterpolator(interpolator);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.start();
    }

    private void startPulseFadeExpand(
            View view,
            float startScale,
            float endScale,
            float startAlpha,
            float endAlpha,
            long duration,
            long delay
    ) {
        if (view == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, 1.06f, endScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, 1.06f, endScale);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, startAlpha, 0.55f, endAlpha);

        scaleX.setDuration(duration);
        scaleY.setDuration(duration);
        alpha.setDuration(duration);

        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        scaleX.setInterpolator(interpolator);
        scaleY.setInterpolator(interpolator);
        alpha.setInterpolator(interpolator);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.start();
    }

    private void startFloatRotateScale(View view, float floatDp, float rotateDeg, float scaleTo, long duration, long delay) {
        if (view == null) return;

        ObjectAnimator y = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -dp(floatDp), 0f);
        ObjectAnimator r = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, rotateDeg, 0f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, scaleTo, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, scaleTo, 1f);

        y.setDuration(duration);
        r.setDuration(duration);
        sx.setDuration(duration);
        sy.setDuration(duration);

        y.setStartDelay(delay);
        r.setStartDelay(delay);
        sx.setStartDelay(delay);
        sy.setStartDelay(delay);

        y.setRepeatCount(ValueAnimator.INFINITE);
        r.setRepeatCount(ValueAnimator.INFINITE);
        sx.setRepeatCount(ValueAnimator.INFINITE);
        sy.setRepeatCount(ValueAnimator.INFINITE);

        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        y.setInterpolator(interpolator);
        r.setInterpolator(interpolator);
        sx.setInterpolator(interpolator);
        sy.setInterpolator(interpolator);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(y, r, sx, sy);
        set.start();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private float dp(float value) {
        return getResources().getDisplayMetrics().density * value;
    }
}