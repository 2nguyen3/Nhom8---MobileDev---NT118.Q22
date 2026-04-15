package com.example.heami.ui.consultation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.heami.R;
import com.example.heami.utils.FavoriteManager;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DoctorDetailActivity extends AppCompatActivity {

    private ImageButton btnBackDoctorDetail;
    private ImageButton btnDoctorDetailFavorite;

    private ImageView imgDoctorDetailAvatar;

    private TextView txtDoctorDetailName;
    private TextView txtDoctorDetailDegree;
    private TextView txtDoctorDetailSpecialty;
    private TextView txtDoctorDetailLocationText;
    private TextView txtDoctorDetailAvailable;

    private TextView txtDoctorStatRating;
    private TextView txtDoctorStatSessions;
    private TextView txtDoctorStatExperience;

    private TextView txtDoctorIntro;
    private TextView txtDoctorReviewTitle;

    private LinearLayout cardPackage15;
    private LinearLayout cardPackage30;
    private LinearLayout cardPackage7Days;

    private RadioButton radioPackage15;
    private RadioButton radioPackage30;
    private RadioButton radioPackage7Days;

    private TextView txtPricePackage15;
    private TextView txtPricePackage30;
    private TextView txtPricePackage7Days;

    private LinearLayout cardDoctorFormatCall;
    private LinearLayout cardDoctorFormatChat;

    private FrameLayout layoutFormatCallCheck;
    private FrameLayout layoutFormatChatCheck;
    
    private FrameLayout iconBoxFormatCall;
    private FrameLayout iconBoxFormatChat;

    private ImageView imgFormatCall;
    private ImageView imgFormatChat;

    private TextView txtFormatCallTitle;
    private TextView txtFormatCallSubtitle;
    private TextView txtFormatChatTitle;
    private TextView txtFormatChatSubtitle;

    private TextView txtDoctorBottomSummaryType;
    private TextView txtDoctorBottomSummaryPrice;
    private TextView txtDoctorBottomBookingStatus;
    private LinearLayout btnDoctorCheckout;

    private LinearLayout cardSelectAppointment;
    private TextView txtBookingStatus;
    private TextView txtBookingSubtitle;
    private MaterialButton btnChangeAppointment;
    private ImageView imgBookingArrow;

    private int selectedPackageType = 30;
    private String selectedFormatType = "call";
    private String doctorId; // ID định danh bác sĩ
    private Calendar selectedDate = null;
    private String selectedTime = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_detail);

        doctorId = getIntent().getStringExtra("doctor_id");

        bindViews();
        setupActions();
        bindDoctorDataFromIntent();
        updateFavoriteUI();
    }

    private void bindViews() {
        btnBackDoctorDetail = findViewById(R.id.btnBackDoctorDetail);
        btnDoctorDetailFavorite = findViewById(R.id.btnDoctorDetailFavorite);
        imgDoctorDetailAvatar = findViewById(R.id.imgDoctorDetailAvatar);
        txtDoctorDetailName = findViewById(R.id.txtDoctorDetailName);
        txtDoctorDetailDegree = findViewById(R.id.txtDoctorDetailDegree);
        txtDoctorDetailSpecialty = findViewById(R.id.txtDoctorDetailSpecialty);
        txtDoctorDetailLocationText = findViewById(R.id.txtDoctorDetailLocationText);
        txtDoctorDetailAvailable = findViewById(R.id.txtDoctorDetailAvailable);
        txtDoctorStatRating = findViewById(R.id.txtDoctorStatRating);
        txtDoctorStatSessions = findViewById(R.id.txtDoctorStatSessions);
        txtDoctorStatExperience = findViewById(R.id.txtDoctorStatExperience);
        txtDoctorIntro = findViewById(R.id.txtDoctorIntro);
        txtDoctorReviewTitle = findViewById(R.id.txtDoctorReviewTitle);
        cardPackage15 = findViewById(R.id.cardPackage15);
        cardPackage30 = findViewById(R.id.cardPackage30);
        cardPackage7Days = findViewById(R.id.cardPackage7Days);
        radioPackage15 = findViewById(R.id.radioPackage15);
        radioPackage30 = findViewById(R.id.radioPackage30);
        radioPackage7Days = findViewById(R.id.radioPackage7Days);

        txtPricePackage15 = findViewById(R.id.txtPricePackage15);
        txtPricePackage30 = findViewById(R.id.txtPricePackage30);
        txtPricePackage7Days = findViewById(R.id.txtPricePackage7Days);

        cardDoctorFormatCall = findViewById(R.id.cardDoctorFormatCall);
        cardDoctorFormatChat = findViewById(R.id.cardDoctorFormatChat);
        layoutFormatCallCheck = findViewById(R.id.layoutFormatCallCheck);
        layoutFormatChatCheck = findViewById(R.id.layoutFormatChatCheck);
        iconBoxFormatCall = findViewById(R.id.iconBoxFormatCall);
        iconBoxFormatChat = findViewById(R.id.iconBoxFormatChat);
        imgFormatCall = findViewById(R.id.imgFormatCall);
        imgFormatChat = findViewById(R.id.imgFormatChat);
        txtFormatCallTitle = findViewById(R.id.txtFormatCallTitle);
        txtFormatCallSubtitle = findViewById(R.id.txtFormatCallSubtitle);
        txtFormatChatTitle = findViewById(R.id.txtFormatChatTitle);
        txtFormatChatSubtitle = findViewById(R.id.txtFormatChatSubtitle);
        txtDoctorBottomSummaryType = findViewById(R.id.txtDoctorBottomSummaryType);
        txtDoctorBottomSummaryPrice = findViewById(R.id.txtDoctorBottomSummaryPrice);
        txtDoctorBottomBookingStatus = findViewById(R.id.txtDoctorBottomBookingStatus);
        btnDoctorCheckout = findViewById(R.id.btnDoctorCheckout);

        cardSelectAppointment = findViewById(R.id.cardSelectAppointment);
        txtBookingStatus = findViewById(R.id.txtBookingStatus);
        btnChangeAppointment = findViewById(R.id.btnChangeAppointment);
        imgBookingArrow = findViewById(R.id.imgBookingArrow);
    }

    private void setupActions() {
        if (btnBackDoctorDetail != null) {
            btnBackDoctorDetail.setOnClickListener(v -> finish());
        }

        // Logic Yêu thích: Lưu vào máy qua FavoriteManager
        if (btnDoctorDetailFavorite != null) {
            btnDoctorDetailFavorite.setOnClickListener(v -> {
                if (doctorId == null) return;
                FavoriteManager.getInstance(this).toggleFavorite(doctorId);
                updateFavoriteUI();
                v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() -> 
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                ).start();
            });
        }

        // Setup click cho toàn bộ vùng card package
        if (cardPackage15 != null) {
            cardPackage15.setOnClickListener(v -> selectPackage(15));
        }
        if (cardPackage30 != null) {
            cardPackage30.setOnClickListener(v -> selectPackage(30));
        }
        if (cardPackage7Days != null) {
            cardPackage7Days.setOnClickListener(v -> selectPackage(7));
        }

        // Đồng bộ click trực tiếp vào RadioButton
        View.OnClickListener radioClickListener = v -> {
            if (v == radioPackage15) selectPackage(15);
            else if (v == radioPackage30) selectPackage(30);
            else if (v == radioPackage7Days) selectPackage(7);
        };
        
        if (radioPackage15 != null) radioPackage15.setOnClickListener(radioClickListener);
        if (radioPackage30 != null) radioPackage30.setOnClickListener(radioClickListener);
        if (radioPackage7Days != null) radioPackage7Days.setOnClickListener(radioClickListener);

        if (cardDoctorFormatCall != null) cardDoctorFormatCall.setOnClickListener(v -> selectFormat("call"));
        if (cardDoctorFormatChat != null) cardDoctorFormatChat.setOnClickListener(v -> selectFormat("chat"));

        if (cardSelectAppointment != null) {
            cardSelectAppointment.setOnClickListener(v -> showBookingDialog());
        }
        
        if (btnChangeAppointment != null) {
            btnChangeAppointment.setOnClickListener(v -> showBookingDialog());
        }
        
        if (btnDoctorCheckout != null) {
            btnDoctorCheckout.setOnClickListener(v -> {
                if (selectedDate == null || selectedTime == null) {
                    Toast.makeText(this, "Vui lòng chọn lịch hẹn trước", Toast.LENGTH_SHORT).show();
                    showBookingDialog();
                } else {
                    // Mở màn hình BookingFlowActivity
                    Intent intent = new Intent(DoctorDetailActivity.this, BookingFlowActivity.class);
                    
                    // Truyền thêm dữ liệu nếu cần
                    intent.putExtra("doctor_id", doctorId);
                    intent.putExtra("doctor_name", txtDoctorDetailName.getText().toString());
                    intent.putExtra("price", txtDoctorBottomSummaryPrice.getText().toString());
                    
                    startActivity(intent);
                }
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showBookingDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_booking_calendar, null);
        bottomSheetDialog.setContentView(view);

        // Reset temporary selection
        final Calendar[] tempSelectedDate = {selectedDate};
        final String[] tempSelectedTime = {selectedTime};

        setupBottomSheetBehavior(bottomSheetDialog, view);

        LinearLayout dateContainer = view.findViewById(R.id.layoutDateContainer);
        FlexboxLayout timeContainer = view.findViewById(R.id.flexTimeContainer);
        View btnConfirm = view.findViewById(R.id.btnConfirmBooking);

        setupDynamicCalendar(dateContainer, timeContainer, btnConfirm, tempSelectedDate, tempSelectedTime);

        // Nếu đã có ngày chọn trước đó, hiển thị giờ
        if (tempSelectedDate[0] != null) {
            setupTimeSlots(timeContainer, btnConfirm, tempSelectedDate, tempSelectedTime);
        }

        view.findViewById(R.id.btnCancelBooking).setOnClickListener(v -> bottomSheetDialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (tempSelectedDate[0] != null && tempSelectedTime[0] != null) {
                selectedDate = tempSelectedDate[0];
                selectedTime = tempSelectedTime[0];
                updateConfirmedBookingUI();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    private void updateConfirmedBookingUI() {
        if (selectedDate != null && selectedTime != null) {
            SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
            
            String dayName = dayNameFormat.format(selectedDate.getTime());
            String dateStr = dateFormat.format(selectedDate.getTime());
            
            txtBookingStatus.setText(dayName + ", " + dateStr + " - " + selectedTime);
            txtBookingStatus.setTextColor(Color.parseColor("#2D1B47"));
            
            imgBookingArrow.setVisibility(View.GONE);
            btnChangeAppointment.setVisibility(View.VISIBLE);
            
            txtDoctorBottomBookingStatus.setText(selectedTime + " · " + dateFormat.format(selectedDate.getTime()));
            txtDoctorBottomBookingStatus.setTextColor(Color.parseColor("#E86FA0"));
            
            // Cập nhật màu nút thanh toán
            btnDoctorCheckout.setBackgroundResource(R.drawable.bg_doctor_checkout_cta);
            
            // Đổi màu text và icon trong nút thanh toán sang trắng cho nổi bật
            TextView txtCheckout = findViewById(R.id.txtDoctorCheckout);
            ImageView imgLeft = findViewById(R.id.imgCheckoutLeft);
            ImageView imgRight = findViewById(R.id.imgCheckoutRight);
            
            if (txtCheckout != null) txtCheckout.setTextColor(Color.WHITE);
            if (imgLeft != null) imgLeft.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            if (imgRight != null) imgRight.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
    }

    private void setupDynamicCalendar(LinearLayout dateContainer, FlexboxLayout timeContainer, View btnConfirm, final Calendar[] tempDate, final String[] tempTime) {
        dateContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));

        for (int i = 0; i < 14; i++) {
            final Calendar itemDate = (Calendar) calendar.clone();
            View dateView = inflater.inflate(R.layout.item_booking_date, dateContainer, false);

            TextView txtDayOfWeek = dateView.findViewById(R.id.txtDayOfWeek);
            TextView txtDayNumber = dateView.findViewById(R.id.txtDayNumber);
            TextView txtMonthYear = dateView.findViewById(R.id.txtMonthYear);

            if (i == 0) {
                txtDayOfWeek.setText("Hôm nay");
            } else {
                txtDayOfWeek.setText(dayFormat.format(itemDate.getTime()));
            }

            String dateMonth = itemDate.get(Calendar.DAY_OF_MONTH) + "/" + (itemDate.get(Calendar.MONTH) + 1);
            txtDayNumber.setText(dateMonth);
            txtDayNumber.setTextSize(18);

            txtMonthYear.setText(String.valueOf(itemDate.get(Calendar.YEAR)));

            dateView.setTag(itemDate.getTimeInMillis());
            
            // Highlight nếu đã chọn
            boolean isInitiallySelected = tempDate[0] != null && 
                    itemDate.get(Calendar.DAY_OF_YEAR) == tempDate[0].get(Calendar.DAY_OF_YEAR);
            updateDateItemUI(dateView, isInitiallySelected);

            dateView.setOnClickListener(v -> {
                tempDate[0] = itemDate;
                tempTime[0] = null; // Reset time when date changes
                updateDateSelectionUI(dateContainer, tempDate[0]);
                setupTimeSlots(timeContainer, btnConfirm, tempDate, tempTime);
                btnConfirm.setEnabled(false);
                btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
            });

            dateContainer.addView(dateView);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void setupTimeSlots(FlexboxLayout container, View btnConfirm, final Calendar[] tempDate, final String[] tempTime) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        
        String[] hours = {"08", "09", "10", "11", "12", "13", "14", "15", "16"};
        String[] minutes = {"00", "30"};

        for (String hour : hours) {
            for (String min : minutes) {
                String time = hour + ":" + min;
                
                View timeView = inflater.inflate(R.layout.item_booking_time, container, false);
                TextView txtTime = (TextView) timeView;
                txtTime.setText(time);
                
                // Giả lập một số khung giờ đã được đặt (Demo)
                boolean isBooked = (time.equals("09:00") || time.equals("10:30") || time.equals("14:00"));
                
                boolean isSelected = time.equals(tempTime[0]);
                updateTimeItemUI(txtTime, isSelected, isBooked);
                
                if (!isBooked) {
                    txtTime.setOnClickListener(v -> {
                        tempTime[0] = time;
                        updateTimeSelectionUI(container, tempTime[0]);
                        btnConfirm.setEnabled(true);
                        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                    });
                } else {
                    txtTime.setOnClickListener(v -> {
                        Toast.makeText(this, "Khung giờ này đã có người đặt", Toast.LENGTH_SHORT).show();
                    });
                }
                
                container.addView(timeView);
            }
        }
    }

    private void updateDateSelectionUI(LinearLayout container, Calendar selected) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            long tagTime = (long) child.getTag();
            Calendar childCal = Calendar.getInstance();
            childCal.setTimeInMillis(tagTime);

            boolean isSelected = selected != null &&
                    childCal.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR);
            updateDateItemUI(child, isSelected);
        }
    }

    private void updateDateItemUI(View view, boolean isSelected) {
        view.setBackgroundResource(isSelected ? R.drawable.bg_doctor_package_card_selected : R.drawable.bg_doctor_package_card);
        TextView tvDay = view.findViewById(R.id.txtDayOfWeek);
        TextView tvMonth = view.findViewById(R.id.txtMonthYear);
        int color = isSelected ? Color.parseColor("#E86FA0") : Color.parseColor("#9E8AAA");
        tvDay.setTextColor(color);
        tvMonth.setTextColor(color);
    }

    private void updateTimeSelectionUI(FlexboxLayout container, String selectedTime) {
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView child = (TextView) container.getChildAt(i);
            String time = child.getText().toString();
            boolean isBooked = (time.equals("09:00") || time.equals("10:30") || time.equals("14:00"));
            boolean isSelected = time.equals(selectedTime);
            updateTimeItemUI(child, isSelected, isBooked);
        }
    }

    private void updateTimeItemUI(TextView view, boolean isSelected, boolean isBooked) {
        if (isBooked) {
            view.setBackgroundResource(R.drawable.bg_doctor_calendar_card);
            view.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
            view.setTextColor(Color.parseColor("#CBD5E1"));
        } else {
            view.setBackgroundResource(isSelected ? R.drawable.bg_doctor_package_card_selected : R.drawable.bg_doctor_package_card);
            view.setBackgroundTintList(null);
            view.setTextColor(isSelected ? Color.parseColor("#E86FA0") : Color.parseColor("#9E8AAA"));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupBottomSheetBehavior(BottomSheetDialog dialog, View view) {
        View parent = (View) view.getParent();
        if (parent != null) {
            BottomSheetBehavior behavior = BottomSheetBehavior.from(parent);
            int heightInPx = (int) (600 * getResources().getDisplayMetrics().density);
            parent.getLayoutParams().height = heightInPx;
            behavior.setPeekHeight(heightInPx);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setDraggable(false);

            View handleBar = view.findViewById(R.id.handleBarContainer);
            View root = view.findViewById(R.id.layoutBookingRoot);

            if (handleBar != null && root != null) {
                handleBar.setOnTouchListener(new View.OnTouchListener() {
                    private float initialTouchY;
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                initialTouchY = event.getRawY();
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                float diff = event.getRawY() - initialTouchY;
                                if (diff > 0) {
                                    root.setTranslationY(diff);
                                    root.setAlpha(1.0f - (diff / 1500f));
                                }
                                return true;
                            case MotionEvent.ACTION_UP:
                                if (root.getTranslationY() > 300) dialog.dismiss();
                                else root.animate().translationY(0).alpha(1.0f).setDuration(200).start();
                                return true;
                        }
                        return false;
                    }
                });
            }
        }
    }

    private void updateFavoriteUI() {
        if (btnDoctorDetailFavorite != null && doctorId != null) {
            boolean isFav = FavoriteManager.getInstance(this).isFavorite(doctorId);
            btnDoctorDetailFavorite.setImageResource(isFav ? R.drawable.ic_doctor_favorite_filled : R.drawable.ic_doctor_favorite);
        }
    }

    private void bindDoctorDataFromIntent() {
        String name = getIntent().getStringExtra("doctor_name");
        String degree = getIntent().getStringExtra("doctor_degree");
        String specialty = getIntent().getStringExtra("doctor_specialty");
        String location = getIntent().getStringExtra("doctor_location");
        String rating = getIntent().getStringExtra("doctor_rating");
        String sessions = getIntent().getStringExtra("doctor_sessions");
        String experience = getIntent().getStringExtra("doctor_experience");
        String intro = getIntent().getStringExtra("doctor_intro");
        String avatarUrl = getIntent().getStringExtra("doctor_avatar");
        int imageResFallback = getIntent().getIntExtra("doctor_image", R.drawable.img_doctor_1);

        if (txtDoctorDetailName != null) txtDoctorDetailName.setText(name);
        if (txtDoctorDetailDegree != null) txtDoctorDetailDegree.setText(degree);
        if (txtDoctorDetailSpecialty != null) txtDoctorDetailSpecialty.setText(specialty);
        if (txtDoctorDetailLocationText != null) txtDoctorDetailLocationText.setText(location);
        if (txtDoctorStatRating != null) txtDoctorStatRating.setText(rating);
        if (txtDoctorStatSessions != null) txtDoctorStatSessions.setText(sessions);
        if (txtDoctorStatExperience != null) txtDoctorStatExperience.setText(experience);
        if (txtDoctorIntro != null) txtDoctorIntro.setText(intro);
        
        if (imgDoctorDetailAvatar != null) {
            Glide.with(this).load(avatarUrl).placeholder(imageResFallback).error(imageResFallback).into(imgDoctorDetailAvatar);
        }
        
        updatePackageRadios();
        updatePackageCardStates();
        updateFormatCardStates();
        updateBottomSummary();
    }

    private void selectPackage(int packageType) {
        selectedPackageType = packageType;
        updatePackageRadios();
        updatePackageCardStates();
        updateBottomSummary();
    }

    private void selectFormat(String formatType) {
        selectedFormatType = formatType;
        updateFormatCardStates();
        updateBottomSummary();
    }

    private void updatePackageRadios() {
        if (radioPackage15 != null) {
            radioPackage15.setChecked(selectedPackageType == 15);
            radioPackage15.setBackgroundResource(selectedPackageType == 15 ? R.drawable.bg_doctor_radio_checked : R.drawable.bg_doctor_radio_unchecked);
        }
        if (radioPackage30 != null) {
            radioPackage30.setChecked(selectedPackageType == 30);
            radioPackage30.setBackgroundResource(selectedPackageType == 30 ? R.drawable.bg_doctor_radio_checked : R.drawable.bg_doctor_radio_unchecked);
        }
        if (radioPackage7Days != null) {
            radioPackage7Days.setChecked(selectedPackageType == 7);
            radioPackage7Days.setBackgroundResource(selectedPackageType == 7 ? R.drawable.bg_doctor_radio_checked : R.drawable.bg_doctor_radio_unchecked);
        }
    }

    private void updatePackageCardStates() {
        int activeColor = Color.parseColor("#E86FA0");

        if (txtPricePackage15 != null) {
            txtPricePackage15.setTextColor(selectedPackageType == 15 ? activeColor : Color.BLACK);
        }
        if (txtPricePackage30 != null) {
            txtPricePackage30.setTextColor(selectedPackageType == 30 ? activeColor : Color.BLACK);
        }
        if (txtPricePackage7Days != null) {
            txtPricePackage7Days.setTextColor(selectedPackageType == 7 ? activeColor : Color.BLACK);
        }

        if (cardPackage15 != null) cardPackage15.setBackgroundResource(selectedPackageType == 15 ? R.drawable.bg_doctor_package_card_selected : R.drawable.bg_doctor_package_card);
        if (cardPackage30 != null) cardPackage30.setBackgroundResource(selectedPackageType == 30 ? R.drawable.bg_doctor_package_card_selected : R.drawable.bg_doctor_package_card);
        if (cardPackage7Days != null) cardPackage7Days.setBackgroundResource(selectedPackageType == 7 ? R.drawable.bg_doctor_package_card_selected : R.drawable.bg_doctor_package_card);
    }

    private void updateFormatCardStates() {
        boolean isCallSelected = "call".equals(selectedFormatType);
        
        if (cardDoctorFormatCall != null) cardDoctorFormatCall.setBackgroundResource(isCallSelected ? R.drawable.bg_doctor_format_selected : R.drawable.bg_doctor_format_card);
        if (cardDoctorFormatChat != null) cardDoctorFormatChat.setBackgroundResource(!isCallSelected ? R.drawable.bg_doctor_format_selected : R.drawable.bg_doctor_format_card);
        
        if (iconBoxFormatCall != null) iconBoxFormatCall.setBackgroundResource(isCallSelected ? R.drawable.bg_doctor_format_icon_box_selected : R.drawable.bg_doctor_format_icon_box);
        if (iconBoxFormatChat != null) iconBoxFormatChat.setBackgroundResource(!isCallSelected ? R.drawable.bg_doctor_format_icon_box_selected : R.drawable.bg_doctor_format_icon_box);

        int activeIconColor = Color.parseColor("#00BFA5");
        int inactiveIconColor = Color.parseColor("#9E8AAA");
        
        if (imgFormatCall != null) imgFormatCall.setImageTintList(ColorStateList.valueOf(isCallSelected ? activeIconColor : inactiveIconColor));
        if (imgFormatChat != null) imgFormatChat.setImageTintList(ColorStateList.valueOf(!isCallSelected ? activeIconColor : inactiveIconColor));

        int selectedTextColor = Color.parseColor("#2D1B47");
        int unselectedTitleColor = Color.parseColor("#9E8AAA");
        int unselectedSubColor = Color.parseColor("#C0AED0");

        if (txtFormatCallTitle != null) txtFormatCallTitle.setTextColor(isCallSelected ? selectedTextColor : unselectedTitleColor);
        if (txtFormatCallSubtitle != null) txtFormatCallSubtitle.setTextColor(isCallSelected ? Color.parseColor("#00BFA5") : unselectedSubColor);
        
        if (txtFormatChatTitle != null) txtFormatChatTitle.setTextColor(!isCallSelected ? selectedTextColor : unselectedTitleColor);
        if (txtFormatChatSubtitle != null) txtFormatChatSubtitle.setTextColor(!isCallSelected ? Color.parseColor("#00BFA5") : unselectedSubColor);

        if (layoutFormatCallCheck != null) layoutFormatCallCheck.setVisibility(isCallSelected ? View.VISIBLE : View.INVISIBLE);
        if (layoutFormatChatCheck != null) layoutFormatChatCheck.setVisibility(!isCallSelected ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateBottomSummary() {
        String packageLabel = selectedPackageType == 7 ? "Gói 7 ngày" : selectedPackageType + " phút";
        String formatLabel = "chat".equals(selectedFormatType) ? "Chat trong 24h" : "Đặt lịch gọi";
        
        String price = "0đ";
        if (selectedPackageType == 15) price = "125.000đ";
        else if (selectedPackageType == 30) price = "250.000đ";
        else if (selectedPackageType == 7) price = "1.125.000đ";

        if (txtDoctorBottomSummaryType != null) txtDoctorBottomSummaryType.setText(packageLabel + " · " + formatLabel);
        if (txtDoctorBottomSummaryPrice != null) {
            txtDoctorBottomSummaryPrice.setText(price);
            txtDoctorBottomSummaryPrice.setTextColor(Color.parseColor("#E86FA0"));
        }
    }
}
