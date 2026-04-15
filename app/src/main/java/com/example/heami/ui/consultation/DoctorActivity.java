package com.example.heami.ui.consultation;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heami.R;
import com.example.heami.data.models.DoctorModel;
import com.example.heami.ui.main.BottomNavManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DoctorActivity extends AppCompatActivity {

    private RecyclerView rvDoctors;
    private DoctorAdapter adapter;
    private EditText edtSearch;
    private ImageView btnSearchClear;
    private TextView txtReadyNotice;
    private LinearLayout layoutEmptyState, layoutErrorState;
    private NestedScrollView scrollDoctor;
    private Button btnLoadMore, btnRetry;

    private List<DoctorModel> allDoctors = new ArrayList<>();
    private List<DoctorModel> filteredDoctors = new ArrayList<>();
    private List<DoctorModel> paginatedDoctors = new ArrayList<>();
    private FirebaseFirestore db;
    
    private int selectedSortOptionId = -1;
    private String currentCategoryId = "all";
    private String currentSearchQuery = "";
    private boolean isFetchError = false;

    // Phân trang
    private final int PAGE_SIZE = 7; 
    private int currentDisplayCount = PAGE_SIZE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        db = FirebaseFirestore.getInstance();
        BottomNavManager.setup(this, BottomNavManager.TAB_DOCTOR);

        initViews();
        setupRecyclerView();
        setupDoctorFilters();
        setupSearchLogic();
        
        btnLoadMore.setOnClickListener(v -> {
            currentDisplayCount += PAGE_SIZE;
            updatePaginatedList();
        });

        fetchDoctorsFromFirestore();
        
        findViewById(R.id.btnDoctorOptions).setOnClickListener(v -> showFilterDialog());
    }

    public void scrollToTopAndRefresh() {
        if (scrollDoctor != null) {
            scrollDoctor.smoothScrollTo(0, 0);
        }
        // Làm mới dữ liệu
        fetchDoctorsFromFirestore();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void initViews() {
        rvDoctors = findViewById(R.id.rvDoctors);
        edtSearch = findViewById(R.id.edtDoctorSearch);
        btnSearchClear = findViewById(R.id.btnDoctorSearchClear);
        txtReadyNotice = findViewById(R.id.txtDoctorReadyNotice);
        layoutEmptyState = findViewById(R.id.layoutDoctorEmptyState);
        layoutErrorState = findViewById(R.id.layoutDoctorErrorState);
        scrollDoctor = findViewById(R.id.scrollDoctor);
        btnLoadMore = findViewById(R.id.btnDoctorLoadMore);
        btnRetry = findViewById(R.id.btnDoctorRetry);
        
        if (btnSearchClear != null) {
            btnSearchClear.setOnClickListener(v -> {
                edtSearch.setText("");
            });
        }
        
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                isFetchError = false;
                if (layoutErrorState != null) layoutErrorState.setVisibility(View.GONE);
                fetchDoctorsFromFirestore();
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new DoctorAdapter(paginatedDoctors, this);
        rvDoctors.setAdapter(adapter);
    }

    private void fetchDoctorsFromFirestore() {
        db.collection("doctors")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isFetchError = false;
                    allDoctors.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        allDoctors.add(document.toObject(DoctorModel.class));
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    isFetchError = true;
                    updatePaginatedList();
                });
    }

    private void setupDoctorFilters() {
        findViewById(R.id.filterDoctorAll).setOnClickListener(v -> {
            currentCategoryId = "all";
            resetPagination();
            applyFilters();
        });
        findViewById(R.id.filterDoctorClinical).setOnClickListener(v -> {
            currentCategoryId = "clinical";
            resetPagination();
            applyFilters();
        });
        findViewById(R.id.filterDoctorPsychiatry).setOnClickListener(v -> {
            currentCategoryId = "psychiatry";
            resetPagination();
            applyFilters();
        });
        findViewById(R.id.filterDoctorTherapy).setOnClickListener(v -> {
            currentCategoryId = "therapy";
            resetPagination();
            applyFilters();
        });
        findViewById(R.id.filterDoctorPositive).setOnClickListener(v -> {
            currentCategoryId = "positive";
            resetPagination();
            applyFilters();
        });
        findViewById(R.id.filterDoctorCare).setOnClickListener(v -> {
            currentCategoryId = "care";
            resetPagination();
            applyFilters();
        });
    }

    private void setupSearchLogic() {
        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().toLowerCase().trim();
                    
                    if (btnSearchClear != null) {
                        btnSearchClear.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    
                    resetPagination();
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            edtSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                    edtSearch.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(edtSearch.getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            });

            edtSearch.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            });
        }
    }

    private void resetPagination() {
        currentDisplayCount = PAGE_SIZE;
    }

    private void applyFilters() {
        updateDoctorFilterTabs(currentCategoryId);
        
        filteredDoctors.clear();
        for (DoctorModel doctor : allDoctors) {
            boolean matchesCategory = currentCategoryId.equals("all") || 
                                     (doctor.getCategory_id() != null && doctor.getCategory_id().equals(currentCategoryId));
            
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String fullName = doctor.getFull_name() != null ? doctor.getFull_name().toLowerCase() : "";
                matchesSearch = fullName.contains(currentSearchQuery);
                
                if (!matchesSearch && doctor.getSpecialization() != null) {
                    for (String spec : doctor.getSpecialization()) {
                        if (spec != null && spec.toLowerCase().contains(currentSearchQuery)) {
                            matchesSearch = true;
                            break;
                        }
                    }
                }
            }

            if (matchesCategory && matchesSearch) {
                filteredDoctors.add(doctor);
            }
        }
        
        if (selectedSortOptionId != -1) {
            applySortingAndFiltering(selectedSortOptionId);
        }
        
        updatePaginatedList();
        updateReadyNotice();
    }

    private void updatePaginatedList() {
        paginatedDoctors.clear();
        int totalFiltered = filteredDoctors.size();

        // Ẩn tất cả các trạng thái trước
        if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
        if (layoutErrorState != null) layoutErrorState.setVisibility(View.GONE);
        if (rvDoctors != null) rvDoctors.setVisibility(View.GONE);
        btnLoadMore.setVisibility(View.GONE);

        if (isFetchError) {
            if (layoutErrorState != null) layoutErrorState.setVisibility(View.VISIBLE);
        } else if (totalFiltered == 0) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            if (rvDoctors != null) rvDoctors.setVisibility(View.VISIBLE);

            int countToDisplay = Math.min(currentDisplayCount, totalFiltered);
            for (int i = 0; i < countToDisplay; i++) {
                paginatedDoctors.add(filteredDoctors.get(i));
            }

            if (currentDisplayCount < totalFiltered) {
                btnLoadMore.setVisibility(View.VISIBLE);
            }
        }
        
        if (scrollDoctor != null) scrollDoctor.setVisibility(View.VISIBLE);
        adapter.updateList(paginatedDoctors);
    }

    private void updateReadyNotice() {
        if (txtReadyNotice == null) return;
        int onlineCount = 0;
        for (DoctorModel doctor : allDoctors) {
            if (doctor.isIs_online()) onlineCount++;
        }
        
        if (onlineCount > 0) {
            txtReadyNotice.setText(onlineCount + " chuyên gia đang trực tuyến");
        } else {
            txtReadyNotice.setText("Hiện không có chuyên gia nào trực tuyến");
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showFilterDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_doctor_filter);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.TOP);
            window.setWindowAnimations(R.style.FilterDialogAnimation);
            window.setDimAmount(0.6f);
        }

        final View root = dialog.findViewById(R.id.layoutFilterRoot);
        View handleBar = dialog.findViewById(R.id.handleBarContainer);

        if (handleBar != null && root != null) {
            handleBar.setOnTouchListener(new View.OnTouchListener() {
                private float initialY;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialY = root.getTranslationY();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float diff = event.getRawY() - initialTouchY;
                            if (diff < 0) { 
                                root.setTranslationY(initialY + diff);
                                float alpha = 1.0f + (diff / 600f);
                                root.setAlpha(Math.max(0.1f, alpha));
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (root.getTranslationY() < -150) {
                                dialog.dismiss();
                            } else {
                                root.animate().translationY(0).alpha(1.0f).setDuration(200).start();
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        setupDialogLogic(dialog);
        dialog.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            applyFilters();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void setupDialogLogic(Dialog dialog) {
        View lOnline = dialog.findViewById(R.id.layoutSortOnline);
        View lRating = dialog.findViewById(R.id.layoutSortRating);
        View lPriceLow = dialog.findViewById(R.id.layoutSortPriceLow);
        View lPriceHigh = dialog.findViewById(R.id.layoutSortPriceHigh);
        View lExp = dialog.findViewById(R.id.layoutSortExp);

        updateDialogUI(dialog);

        View.OnClickListener listener = v -> {
            int clickedId = v.getId();
            if (selectedSortOptionId == clickedId) {
                selectedSortOptionId = -1;
            } else {
                selectedSortOptionId = clickedId;
            }
            updateDialogUI(dialog);
        };

        if (lOnline != null) lOnline.setOnClickListener(listener);
        if (lRating != null) lRating.setOnClickListener(listener);
        if (lPriceLow != null) lPriceLow.setOnClickListener(listener);
        if (lPriceHigh != null) lPriceHigh.setOnClickListener(listener);
        if (lExp != null) lExp.setOnClickListener(listener);
    }

    private void updateDialogUI(Dialog dialog) {
        View l0 = dialog.findViewById(R.id.layoutSortOnline);
        View l1 = dialog.findViewById(R.id.layoutSortRating);
        View l2 = dialog.findViewById(R.id.layoutSortPriceLow);
        View l3 = dialog.findViewById(R.id.layoutSortPriceHigh);
        View l4 = dialog.findViewById(R.id.layoutSortExp);

        if (l0 != null) l0.setSelected(selectedSortOptionId == R.id.layoutSortOnline);
        if (l1 != null) l1.setSelected(selectedSortOptionId == R.id.layoutSortRating);
        if (l2 != null) l2.setSelected(selectedSortOptionId == R.id.layoutSortPriceLow);
        if (l3 != null) l3.setSelected(selectedSortOptionId == R.id.layoutSortPriceHigh);
        if (l4 != null) l4.setSelected(selectedSortOptionId == R.id.layoutSortExp);
    }

    private void applySortingAndFiltering(int optionId) {
        if (optionId == R.id.layoutSortOnline) {
            // Lọc những bác sĩ online
            List<DoctorModel> onlyOnline = new ArrayList<>();
            for (DoctorModel d : filteredDoctors) {
                if (d.isIs_online()) onlyOnline.add(d);
            }
            filteredDoctors.clear();
            filteredDoctors.addAll(onlyOnline);
        } else if (optionId == R.id.layoutSortRating) {
            Collections.sort(filteredDoctors, (d1, d2) -> Double.compare(d2.getRating_avg(), d1.getRating_avg()));
        } else if (optionId == R.id.layoutSortPriceLow) {
            Collections.sort(filteredDoctors, (d1, d2) -> Double.compare(d1.getMin_price(), d2.getMin_price()));
        } else if (optionId == R.id.layoutSortPriceHigh) {
            Collections.sort(filteredDoctors, (d1, d2) -> Double.compare(d2.getMin_price(), d1.getMin_price()));
        } else if (optionId == R.id.layoutSortExp) {
            Collections.sort(filteredDoctors, (d1, d2) -> Integer.compare(d2.getExperience_years(), d1.getExperience_years()));
        }
    }

    private void updateDoctorFilterTabs(String activeFilter) {
        updateSingleDoctorFilterTab(R.id.filterDoctorAll, activeFilter.equals("all"));
        updateSingleDoctorFilterTab(R.id.filterDoctorClinical, activeFilter.equals("clinical"));
        updateSingleDoctorFilterTab(R.id.filterDoctorPsychiatry, activeFilter.equals("psychiatry"));
        updateSingleDoctorFilterTab(R.id.filterDoctorTherapy, activeFilter.equals("therapy"));
        updateSingleDoctorFilterTab(R.id.filterDoctorPositive, activeFilter.equals("positive"));
        updateSingleDoctorFilterTab(R.id.filterDoctorCare, activeFilter.equals("care"));
    }

    private void updateSingleDoctorFilterTab(int viewId, boolean isActive) {
        TextView tab = findViewById(viewId);
        if (tab == null) return;
        tab.setSelected(isActive);
        if (isActive) {
            tab.setBackgroundResource(R.drawable.bg_doctor_filter_active);
            tab.setTextColor(0xFFE86FA0);
        } else {
            tab.setBackgroundResource(R.drawable.bg_doctor_filter);
            tab.setTextColor(0xFF8A9AAA);
        }
    }
}
