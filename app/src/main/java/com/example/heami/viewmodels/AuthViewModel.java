package com.example.heami.viewmodels;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.heami.data.models.AccountModel;
import com.example.heami.data.models.UserModel;
import com.example.heami.data.models.UserSettingsModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted", "deprecation"})
public class AuthViewModel extends ViewModel {

    private static final String TAG = "AuthError";
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<String> authStatus = new MutableLiveData<>();
    public LiveData<String> getAuthStatus() { return authStatus; }

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    private String mVerificationId;
    private String pendingPhone, pendingPass, pendingNickname;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    private boolean isPhoneNumber(String input) {
        return input != null && input.matches("^[0-9]{9,11}$");
    }

    private String formatInput(String input) {
        if (isPhoneNumber(input)) {
            return input.trim() + "@heami.com";
        }
        return input != null ? input.trim() : "";
    }

    private String formatPhoneForFirebase(String phone) {
        String p = phone != null ? phone.trim().replace(" ", "") : "";
        if (p.startsWith("0")) return "+84" + p.substring(1);
        return p;
    }

    private String getVietnameseErrorMessage(Exception exception) {
        if (exception == null) return "Đã xảy ra sự cố không xác định!";
        Log.e(TAG, "Firebase Exception: ", exception);
        if (exception instanceof FirebaseAuthUserCollisionException) return "Tài khoản này đã được sử dụng!";
        if (exception instanceof FirebaseAuthInvalidUserException) return "Tài khoản này không tồn tại!";
        if (exception instanceof FirebaseAuthInvalidCredentialsException) return "Thông tin đăng nhập không chính xác!";
        if (exception instanceof FirebaseNetworkException) return "Lỗi kết nối mạng, vui lòng thử lại!";
        return "Đã xảy ra sự cố hệ thống, vui lòng thử lại sau.";
    }

    private Task<Void> initUserData(String uid, String email, String nickname) {
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();

        AccountModel account = new AccountModel(uid, email, "USER", "ACTIVE", now);
        account.setLast_sign_in_at(now);
        account.setActive_session_id(UUID.randomUUID().toString());
        batch.set(db.collection("accounts").document(uid), account);

        UserModel user = new UserModel(uid, nickname, null);
        batch.set(db.collection("users").document(uid), user);

        UserSettingsModel settings = new UserSettingsModel("LIGHT", true);
        
        batch.set(db.collection("users").document(uid).collection("settings").document("default"), settings);

        return batch.commit();
    }

    public void cancelLoading() {
        isLoading.setValue(false);
    }

    // Email/password dùng cho Firebase Auth của tài khoản Doctor (nội bộ, không phải login credentials)
    private static final String DOCTOR_FIREBASE_EMAIL = "doctor_doc001@heami.vn";
    private static final String DOCTOR_FIREBASE_PASS  = "Heami@Doc001";

    /**
     * Đăng nhập vào Firebase Auth bằng tài khoản email/password nội bộ của Doctor.
     * Nếu tài khoản chưa tồn tại → tự động tạo trước, rồi sign in.
     * Giúp đảm bảo Firestore Rules (require auth) luôn được thỏa mãn.
     */
    private void signInDoctorFirebaseAuth(Runnable onSuccess) {
        auth.signInWithEmailAndPassword(DOCTOR_FIREBASE_EMAIL, DOCTOR_FIREBASE_PASS)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    android.util.Log.d("AuthViewModel", "Doctor Firebase sign-in OK uid=" + auth.getUid());
                    onSuccess.run();
                } else {
                    // Tài khoản chưa tồn tại → tạo mới
                    android.util.Log.d("AuthViewModel", "Doctor sign-in failed, creating account...");
                    auth.createUserWithEmailAndPassword(DOCTOR_FIREBASE_EMAIL, DOCTOR_FIREBASE_PASS)
                        .addOnCompleteListener(createTask -> {
                            if (createTask.isSuccessful()) {
                                android.util.Log.d("AuthViewModel", "Doctor Firebase account created, uid=" + auth.getUid());
                                onSuccess.run();
                            } else {
                                // Fallback cuối: thử ẩn danh
                                android.util.Log.e("AuthViewModel", "Create failed: " + createTask.getException());
                                auth.signInAnonymously().addOnCompleteListener(anonTask -> {
                                    android.util.Log.d("AuthViewModel", "Anon fallback uid=" + auth.getUid());
                                    onSuccess.run();
                                });
                            }
                        });
                }
            });
    }

    public void login(String account, String pass) {
        isLoading.setValue(true);

        db.collection("accounts")
            .whereEqualTo("email", account)
            .whereEqualTo("role", "DOCTOR")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                    String dbPassword = doc.getString("password");
                    if (pass != null && pass.equals(dbPassword)) {
                        // Đăng nhập Firebase Auth bằng tài khoản email cố định của Doctor
                        signInDoctorFirebaseAuth(() -> {
                            isLoading.setValue(false);
                            authStatus.setValue("SUCCESS_DOCTOR:Đăng nhập thành công!");
                        });
                    } else {
                        isLoading.setValue(false);
                        authStatus.setValue("ERROR: Mật khẩu bác sĩ không chính xác!");
                    }
                } else {
                    if ("doctor".equals(account) && "1234".equals(pass)) {
                        Map<String, Object> doctorAcc = new HashMap<>();
                        doctorAcc.put("account_id", "doc_001");
                        doctorAcc.put("email", "doctor");
                        doctorAcc.put("role", "DOCTOR");
                        doctorAcc.put("password", "1234");
                        doctorAcc.put("status", "ACTIVE");
                        doctorAcc.put("created_at", Timestamp.now());
                        doctorAcc.put("last_sign_in_at", Timestamp.now());
                        doctorAcc.put("active_session_id", UUID.randomUUID().toString());

                        Map<String, Object> doctorInfo = new HashMap<>();
                        doctorInfo.put("doctor_id", "doc_001");
                        doctorInfo.put("category_id", "clinical");
                        doctorInfo.put("min_price", 450000);
                        doctorInfo.put("is_online", true);
                        doctorInfo.put("full_name", "ThS. BS. Nguyễn Hoài Thu");
                        doctorInfo.put("location", "Hà Nội");
                        doctorInfo.put("avatar_url", "https://res.cloudinary.com/dqnyi6ubx/image/upload/v1776499589/doc_001.jpg");
                        doctorInfo.put("rating_avg", 4.9);
                        doctorInfo.put("review_count", 128);

                        java.util.List<String> specs = new java.util.ArrayList<>();
                        specs.add("Trầm cảm");
                        specs.add("Rối loạn lo âu");
                        specs.add("Stress công việc");
                        doctorInfo.put("specialization", specs);

                        doctorInfo.put("degree", "Thạc sĩ");
                        doctorInfo.put("experience_years", 8);
                        doctorInfo.put("total_sessions", 520);
                        doctorInfo.put("bio", "Thạc sĩ Hoài Thu có kinh nghiệm chuyên sâu trong việc trị liệu nhận thức hành vi (CBT). Bà đã giúp nhiều người trẻ vượt qua áp lực đồng trang lứa và cân bằng cuộc sống công việc - gia đình.");

                        WriteBatch batch = db.batch();
                        batch.set(db.collection("accounts").document("doc_001"), doctorAcc);
                        batch.set(db.collection("doctors").document("doc_001"), doctorInfo, SetOptions.merge());

                        batch.commit().addOnCompleteListener(commitTask -> {
                            signInDoctorFirebaseAuth(() -> {
                                isLoading.setValue(false);
                                authStatus.setValue("SUCCESS_DOCTOR:Đăng nhập thành công!");
                            });
                        });
                    } else {
                        loginNormalUser(account, pass);
                    }
                }
            });
    }


    private void loginNormalUser(String account, String pass) {
        String finalAccount = formatInput(account);

        auth.signInWithEmailAndPassword(finalAccount, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null && (isPhoneNumber(account) || user.isEmailVerified())) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("last_sign_in_at", Timestamp.now());
                    updates.put("active_session_id", UUID.randomUUID().toString());

                    db.collection("accounts").document(user.getUid())
                            .update(updates);
                    
                    checkUserProfile(user.getUid());
                } else {
                    isLoading.setValue(false);
                    auth.signOut();
                    authStatus.setValue("ERROR:Vui lòng kiểm tra hộp thư để xác thực email!");
                }
            } else {
                isLoading.setValue(false);
                authStatus.setValue("ERROR:" + getVietnameseErrorMessage(task.getException()));
            }
        });
    }

    public void checkUserProfile(String uid) {
        isLoading.setValue(true);
        db.collection("accounts").document(uid).get().addOnCompleteListener(accTask -> {
            if (accTask.isSuccessful() && accTask.getResult() != null && accTask.getResult().exists()) {
                String role = accTask.getResult().getString("role");
                if ("DOCTOR".equals(role)) {
                    isLoading.setValue(false);
                    authStatus.setValue("SUCCESS_DOCTOR:Đăng nhập thành công!");
                    return;
                }
            }

            // Nếu uid là doc_001 (Doctor mặc định), bỏ qua bước check user profile của người dùng
            if ("doc_001".equals(uid)) {
                isLoading.setValue(false);
                authStatus.setValue("SUCCESS_DOCTOR:Đăng nhập thành công!");
                return;
            }

            db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                isLoading.setValue(false);
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    String avatarUrl = task.getResult().getString("avatar_url");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        authStatus.setValue("SUCCESS_HOME:Đăng nhập thành công!");
                    } else {
                        authStatus.setValue("SUCCESS_SETUP:Vui lòng hoàn tất hồ sơ!");
                    }
                } else {
                    authStatus.setValue("SUCCESS_SETUP:Vui lòng hoàn tất hồ sơ!");
                }
            });
        });
    }

    public void register(String account, String pass, String nickname, boolean isTermsAccepted, Activity activity) {
        isLoading.setValue(true);
        String authEmail = formatInput(account);

        auth.fetchSignInMethodsForEmail(authEmail).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful() && authTask.getResult() != null &&
                    authTask.getResult().getSignInMethods() != null &&
                    !authTask.getResult().getSignInMethods().isEmpty()) {

                isLoading.setValue(false);
                authStatus.setValue("ERROR:Tài khoản này đã được đăng ký!");
                return;
            }

            db.collection("accounts").whereEqualTo("email", account).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    isLoading.setValue(false);
                    authStatus.setValue("ERROR:Email/Số điện thoại đã được sử dụng!");
                } else {
                    if (isPhoneNumber(account)) {
                        if (account.equals(pendingPhone) && mVerificationId != null) {
                            isLoading.setValue(false);
                            authStatus.setValue("OTP_SENT:" + System.currentTimeMillis());
                        } else {
                            sendOTP(account, pass, nickname, activity);
                        }
                    } else {
                        createEmailAccount(account, pass, nickname);
                    }
                }
            });
        });
    }

    private void createEmailAccount(String email, String pass, String nickname) {
        String finalAccount = formatInput(email);
        auth.createUserWithEmailAndPassword(finalAccount, pass).addOnCompleteListener(regTask -> {
            if (regTask.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    initUserData(user.getUid(), email, nickname).addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(nickname).build();

                            user.updateProfile(profileUpdates).addOnCompleteListener(uTask ->
                                    user.sendEmailVerification().addOnCompleteListener(vTask -> {
                                        auth.signOut();
                                        Log.d(TAG, "Auth Status Update: SUCCESS_REGISTER_EMAIL at " + System.currentTimeMillis());
                                        authStatus.setValue("SUCCESS_REGISTER_EMAIL:Đăng ký thành công! Hãy kiểm tra email.");
                                    })
                            );
                        } else {
                            isLoading.setValue(false);
                            authStatus.setValue("ERROR: Lỗi khởi tạo dữ liệu người dùng.");
                        }
                    });
                }
            } else {
                isLoading.setValue(false);
                authStatus.setValue("ERROR:" + getVietnameseErrorMessage(regTask.getException()));
            }
        });
    }

    private void sendOTP(String phone, String pass, String nickname, Activity activity) {
        pendingPhone = phone;
        pendingPass = pass;
        pendingNickname = nickname;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formatPhoneForFirebase(phone))
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        verifyAndCreateAccount(credential);
                    }
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        isLoading.setValue(false);
                        Log.e(TAG, "Gửi OTP thất bại: ", e);
                        authStatus.setValue("ERROR:Không thể gửi mã OTP, vui lòng thử lại sau.");
                    }
                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        isLoading.setValue(false);
                        mVerificationId = verificationId;
                        mResendToken = token;
                        authStatus.setValue("OTP_SENT:Mã OTP đã được gửi!");
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void resendOTP(Activity activity) {
        if (pendingPhone == null || mResendToken == null) {
            authStatus.setValue("ERROR:Vui lòng đăng ký lại từ đầu!");
            return;
        }

        isLoading.setValue(true);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formatPhoneForFirebase(pendingPhone))
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setForceResendingToken(mResendToken)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        verifyAndCreateAccount(credential);
                    }
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        isLoading.setValue(false);
                        Log.e(TAG, "Gửi lại OTP thất bại: ", e);
                        authStatus.setValue("ERROR:Gửi lại mã thất bại!");
                    }
                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        isLoading.setValue(false);
                        mVerificationId = verificationId;
                        mResendToken = token;
                        authStatus.setValue("RESEND_SUCCESS:Đã gửi lại mã OTP!");
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    public void verifyOTP(String code) {
        if (mVerificationId == null) {
            authStatus.setValue("ERROR:Mã xác thực không hợp lệ!");
            return;
        }
        isLoading.setValue(true);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        verifyAndCreateAccount(credential);
    }

    private void verifyAndCreateAccount(PhoneAuthCredential credential) {
        String finalAccount = formatInput(pendingPhone);
        AuthCredential emailCred = EmailAuthProvider.getCredential(finalAccount, pendingPass);

        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    user.linkWithCredential(emailCred).addOnCompleteListener(linkTask -> {
                        initUserData(user.getUid(), pendingPhone, pendingNickname).addOnCompleteListener(dbTask -> {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(pendingNickname).build();
                            user.updateProfile(profileUpdates);
                            authStatus.setValue("SUCCESS_REGISTER_PHONE:Xác thực thành công!");
                        });
                    });
                }
            } else {
                isLoading.setValue(false);
                authStatus.setValue("ERROR:" + getVietnameseErrorMessage(task.getException()));
            }
        });
    }

    public void signInWithGoogle(String idToken) {
        isLoading.setValue(true);
        AuthCredential credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    db.collection("accounts").document(user.getUid()).get().addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful() && !dbTask.getResult().exists()) {
                            initUserData(user.getUid(), user.getEmail(), user.getDisplayName()).addOnCompleteListener(t -> checkUserProfile(user.getUid()));
                        } else {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("last_sign_in_at", Timestamp.now());
                            updates.put("active_session_id", UUID.randomUUID().toString());

                            db.collection("accounts").document(user.getUid())
                                    .update(updates);
                            checkUserProfile(user.getUid());
                        }
                    });
                }
            } else {
                isLoading.setValue(false);
                authStatus.setValue("ERROR:" + getVietnameseErrorMessage(task.getException()));
            }
        });
    }

    public void resetPassword(String email) {
        isLoading.setValue(true);
        String finalEmail = formatInput(email);
        auth.sendPasswordResetEmail(finalEmail).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                authStatus.setValue("SUCCESS_RESET:Yêu cầu đã được gửi! Vui lòng kiểm tra email của bạn.");
            } else {
                authStatus.setValue("ERROR:" + getVietnameseErrorMessage(task.getException()));
            }
        });
    }
}
