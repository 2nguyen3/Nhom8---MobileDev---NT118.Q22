package com.example.heami.ui.community;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heami.R;
import com.example.heami.data.models.CommunityPostModel;
import com.example.heami.data.repositories.CommunityRepository;
import com.example.heami.ui.main.BottomNavManager;
import com.google.firebase.Timestamp;

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

    private String currentFilter = "all";
    private boolean isLoadingPosts = false;

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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null && intent.getBooleanExtra("refresh_community_feed", false)) {
            loadCommunityPosts();
            intent.removeExtra("refresh_community_feed");
            intent.removeExtra("created_post_id");
        }
    }

    private void bindViews() {
        layoutPostsContainer = findViewById(R.id.layoutPostsContainer);
        txtPostsCount = findViewById(R.id.txtPostsCount);
        btnCommunityChat = findViewById(R.id.btnCommunityChat);
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

        LinearLayout btnPostHugAction = postView.findViewById(R.id.btnPostHugAction);
        LinearLayout btnPostCommentAction = postView.findViewById(R.id.btnPostCommentAction);
        LinearLayout btnPostEmpathyAction = postView.findViewById(R.id.btnPostEmpathyAction);
        ImageButton btnPostReport = postView.findViewById(R.id.btnPostReport);

        String postId = safeText(post.getPost_id(), "");
        String mood = safeText(post.getMood_tag(), "stress");

        int myHugCount = myHugCountMap.containsKey(postId) ? myHugCountMap.get(postId) : 0;
        boolean isProcessingHug = processingHugPostIds.contains(postId);

        boolean hasEmpathy = myEmpathyStateMap.containsKey(postId) && Boolean.TRUE.equals(myEmpathyStateMap.get(postId));
        boolean isProcessingEmpathy = processingEmpathyPostIds.contains(postId);

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

        if (txtPostContent != null) {
            txtPostContent.setText(safeText(post.getContent(), ""));
        }

        if (txtPostHugCount != null) {
            txtPostHugCount.setText(buildHugLabel(post.getLike_count(), myHugCount));
            txtPostHugCount.setTextColor(myHugCount > 0 ? 0xFF7F5AF0 : 0xFFB0A0C0);
        }

        if (txtPostCommentCount != null) {
            txtPostCommentCount.setText(String.valueOf(post.getComment_count()));
        }

        if (txtPostEmpathyLabel != null) {
            txtPostEmpathyLabel.setText(buildEmpathyLabel(post.getEmpathy_count(), hasEmpathy));
            txtPostEmpathyLabel.setTextColor(hasEmpathy ? 0xFF2FAF9A : 0xFF4BBDAD);
        }

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
            btnPostReport.setOnClickListener(v -> Toast.makeText(
                    CommunityActivity.this,
                    "Chức năng Báo cáo sẽ được hoàn thiện sau",
                    Toast.LENGTH_SHORT
            ).show());
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

    @NonNull
    private String buildHugLabel(int totalHugCount, int myHugCount) {
        if (myHugCount > 0) {
            return "Ôm " + totalHugCount + " · Bạn " + myHugCount;
        }
        return "Ôm " + totalHugCount;
    }

    @NonNull
    private String buildEmpathyLabel(int totalEmpathyCount, boolean hasEmpathy) {
        if (hasEmpathy) {
            return "Đã đồng cảm · " + totalEmpathyCount;
        }
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