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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted", "deprecation"})
public class AuthViewModel extends ViewModel {

    private static final String TAG = "AuthError";

    private static final String DOCTOR_LOGIN_ACCOUNT = "doctor";
    private static final String DOCTOR_LOGIN_PASS = "1234";
    private static final String DOCTOR_ACCOUNT_DOC_ID = "doc_001";
    private static final String DOCTOR_FIREBASE_EMAIL = "doctor_doc001@heami.vn";
    private static final String DOCTOR_FIREBASE_PASS = "Heami@Doc001";

    private static final String ADMIN_LOGIN_ACCOUNT = "admin";
    private static final String ADMIN_LOGIN_PASS = "1234";
    private static final String ADMIN_FIREBASE_EMAIL = "admin_root@heami.vn";
    private static final String ADMIN_FIREBASE_PASS = "Heami@Admin001";

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final MutableLiveData<String> authStatus = new MutableLiveData<>();
    public LiveData<String> getAuthStatus() {
        return authStatus;
    }

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

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

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean handleBlockedOrPendingStatus(String status) {
        String normalized = normalizeStatus(status);

        if ("BANNED".equals(normalized)) {
            auth.signOut();
            isLoading.setValue(false);
            authStatus.setValue("ERROR:Tài khoản này đã bị khóa. Vui lòng liên hệ quản trị viên.");
            return true;
        }

        if ("PENDING_VERIFY".equals(normalized) || "PENDING".equals(normalized)) {
            auth.signOut();
            isLoading.setValue(false);
            authStatus.setValue("ERROR:Tài khoản này chưa được kích hoạt.");
            return true;
        }

        return false;
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

    private void signInDoctorFirebaseAuth(@NonNull Runnable onSuccess) {
        auth.signInWithEmailAndPassword(DOCTOR_FIREBASE_EMAIL, DOCTOR_FIREBASE_PASS)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthViewModel", "Doctor Firebase sign-in OK uid=" + auth.getUid());
                        onSuccess.run();
                    } else {
                        Log.d("AuthViewModel", "Doctor sign-in failed, trying create account...");
                        auth.createUserWithEmailAndPassword(DOCTOR_FIREBASE_EMAIL, DOCTOR_FIREBASE_PASS)
                                .addOnCompleteListener(createTask -> {
                                    if (createTask.isSuccessful()) {
                                        Log.d("AuthViewModel", "Doctor Firebase account created uid=" + auth.getUid());
                                        onSuccess.run();
                                    } else {
                                        Exception e = createTask.getException();
                                        Log.e("AuthViewModel", "Doctor internal auth failed", e);
                                        auth.signOut();
                                        isLoading.setValue(false);
                                        authStatus.setValue("ERROR:Không thể đăng nhập tài khoản nội bộ của bác sĩ.");
                                    }
                                });
                    }
                });
    }

    private void signInAdminFirebaseAuth(@NonNull Runnable onSuccess) {
        auth.signInWithEmailAndPassword(ADMIN_FIREBASE_EMAIL, ADMIN_FIREBASE_PASS)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AuthViewModel", "Admin Firebase sign-in OK uid=" + auth.getUid());
                        onSuccess.run();
                    } else {
                        Log.d("AuthViewModel", "Admin sign-in failed, trying create account...");
                        auth.createUserWithEmailAndPassword(ADMIN_FIREBASE_EMAIL, ADMIN_FIREBASE_PASS)
                                .addOnCompleteListener(createTask -> {
                                    if (createTask.isSuccessful()) {
                                        Log.d("AuthViewModel", "Admin Firebase account created uid=" + auth.getUid());
                                        onSuccess.run();
                                    } else {
                                        Exception e = createTask.getException();
                                        Log.e("AuthViewModel", "Admin internal auth failed", e);
                                        auth.signOut();
                                        isLoading.setValue(false);
                                        authStatus.setValue("ERROR:Không thể đăng nhập tài khoản nội bộ của admin.");
                                    }
                                });
                    }
                });
    }

    private Task<Void> ensureAdminData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("Admin chưa đăng nhập Firebase Auth")
            );
        }

        String uid = currentUser.getUid();
        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();

        Map<String, Object> adminAccount = new HashMap<>();
        adminAccount.put("account_id", uid);
        adminAccount.put("email", ADMIN_LOGIN_ACCOUNT);
        adminAccount.put("role", "ADMIN");
        adminAccount.put("password", ADMIN_LOGIN_PASS);
        adminAccount.put("status", "ACTIVE");
        adminAccount.put("created_at", now);
        adminAccount.put("last_sign_in_at", now);
        adminAccount.put("active_session_id", UUID.randomUUID().toString());

        Map<String, Object> permissions = new HashMap<>();
        permissions.put("manage_accounts", true);
        permissions.put("moderate_community", true);
        permissions.put("view_analytics", true);

        Map<String, Object> adminProfile = new HashMap<>();
        adminProfile.put("admin_id", uid);
        adminProfile.put("full_name", "Heami Admin");
        adminProfile.put("email", ADMIN_LOGIN_ACCOUNT);
        adminProfile.put("avatar_url", "");
        adminProfile.put("status", "ACTIVE");
        adminProfile.put("permissions", permissions);
        adminProfile.put("created_at", now);
        adminProfile.put("updated_at", now);
        adminProfile.put("last_sign_in_at", now);

        batch.set(db.collection("accounts").document(uid), adminAccount);
        batch.set(db.collection("admins").document(uid), adminProfile);

        return batch.commit();
    }

    private void validateDoctorAccountAfterInternalSignIn(String inputPassword, boolean requirePasswordCheck) {
        db.collection("accounts")
                .document(DOCTOR_ACCOUNT_DOC_ID)
                .get()
                .addOnCompleteListener(task -> {
                    if (!(task.isSuccessful() && task.getResult() != null && task.getResult().exists())) {
                        auth.signOut();
                        isLoading.setValue(false);
                        authStatus.setValue("ERROR:Không tìm thấy tài khoản bác sĩ.");
                        return;
                    }

                    DocumentSnapshot doc = task.getResult();

                    String role = safeTrim(doc.getString("role"));
                    if (!"DOCTOR".equalsIgnoreCase(role)) {
                        auth.signOut();
                        isLoading.setValue(false);
                        authStatus.setValue("ERROR:Tài khoản bác sĩ không hợp lệ.");
                        return;
                    }

                    String dbStatus = doc.getString("status");
                    if (handleBlockedOrPendingStatus(dbStatus)) {
                        return;
                    }

                    if (requirePasswordCheck) {
                        String dbPassword = safeTrim(doc.getString("password"));
                        if (!safeTrim(inputPassword).equals(dbPassword)) {
                            auth.signOut();
                            isLoading.setValue(false);
                            authStatus.setValue("ERROR:Mật khẩu bác sĩ không chính xác!");
                            return;
                        }
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("last_sign_in_at", Timestamp.now());
                    updates.put("active_session_id", UUID.randomUUID().toString());

                    db.collection("accounts")
                            .document(DOCTOR_ACCOUNT_DOC_ID)
                            .update(updates)
                            .addOnCompleteListener(updateTask -> {
                                isLoading.setValue(false);
                                authStatus.setValue("SUCCESS_DOCTOR:Đăng nhập thành công!");
                            });
                })
                .addOnFailureListener(e -> {
                    auth.signOut();
                    isLoading.setValue(false);
                    authStatus.setValue("ERROR:Không thể kiểm tra tài khoản bác sĩ.");
                });
    }

    public void login(String account, String pass) {
        isLoading.setValue(true);

        String normalizedAccount = account != null ? account.trim() : "";
        String normalizedPass = pass != null ? pass.trim() : "";

        if (ADMIN_LOGIN_ACCOUNT.equalsIgnoreCase(normalizedAccount)) {
            if (!ADMIN_LOGIN_PASS.equals(normalizedPass)) {
                isLoading.setValue(false);
                authStatus.setValue("ERROR:Mật khẩu admin không chính xác!");
                return;
            }

            signInAdminFirebaseAuth(() -> {
                ensureAdminData()
                        .addOnSuccessListener(unused -> {
                            isLoading.setValue(false);
                            authStatus.setValue("SUCCESS_ADMIN:Đăng nhập admin thành công!");
                        })
                        .addOnFailureListener(e -> {
                            isLoading.setValue(false);
                            authStatus.setValue("ERROR:Không thể khởi tạo dữ liệu admin!");
                        });
            });
            return;
        }

        if (DOCTOR_LOGIN_ACCOUNT.equalsIgnoreCase(normalizedAccount)) {
            signInDoctorFirebaseAuth(() ->
                    validateDoctorAccountAfterInternalSignIn(normalizedPass, true)
            );
            return;
        }

        loginNormalUser(account, pass);
    }

    private void loginNormalUser(String account, String pass) {
        String finalAccount = formatInput(account);

        auth.signInWithEmailAndPassword(finalAccount, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null && (isPhoneNumber(account) || user.isEmailVerified())) {
                    db.collection("accounts").document(user.getUid()).get().addOnCompleteListener(accTask -> {
                        if (accTask.isSuccessful() && accTask.getResult() != null && accTask.getResult().exists()) {
                            String dbStatus = accTask.getResult().getString("status");
                            if (handleBlockedOrPendingStatus(dbStatus)) {
                                return;
                            }

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("last_sign_in_at", Timestamp.now());
                            updates.put("active_session_id", UUID.randomUUID().toString());

                            db.collection("accounts").document(user.getUid())
                                    .update(updates)
                                    .addOnCompleteListener(updateTask -> checkUserProfile(user.getUid()));
                        } else {
                            auth.signOut();
                            isLoading.setValue(false);
                            authStatus.setValue("ERROR:Không tìm thấy hồ sơ tài khoản.");
                        }
                    });
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
                String status = accTask.getResult().getString("status");
                if (handleBlockedOrPendingStatus(status)) {
                    return;
                }

                String role = accTask.getResult().getString("role");

                if ("ADMIN".equals(role)) {
                    isLoading.setValue(false);
                    authStatus.setValue("SUCCESS_ADMIN:Đăng nhập thành công!");
                    return;
                }

                if ("DOCTOR".equals(role)) {
                    isLoading.setValue(false);
                    authStatus.setValue("SUCCESS_DOCTOR:Đăng nhập thành công!");
                    return;
                }
            } else {
                auth.signOut();
                isLoading.setValue(false);
                authStatus.setValue("ERROR:Không tìm thấy hồ sơ tài khoản.");
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

    public void checkLegacyDoctorAccess() {
        isLoading.setValue(true);
        signInDoctorFirebaseAuth(() ->
                validateDoctorAccountAfterInternalSignIn(null, false)
        );
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
                            initUserData(user.getUid(), user.getEmail(), user.getDisplayName())
                                    .addOnCompleteListener(t -> checkUserProfile(user.getUid()));
                        } else {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("last_sign_in_at", Timestamp.now());
                            updates.put("active_session_id", UUID.randomUUID().toString());

                            db.collection("accounts").document(user.getUid())
                                    .update(updates)
                                    .addOnCompleteListener(updateTask -> checkUserProfile(user.getUid()));
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