package com.example.heami.viewmodels;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.heami.models.UserModel;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
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

import java.util.ArrayList;
import java.util.List;
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

    // Hàm chuyển đổi lỗi sang tiếng Việt cho UI, đồng thời ghi log chi tiết cho Dev
    private String getVietnameseErrorMessage(Exception exception) {
        if (exception == null) return "Đã xảy ra sự cố không xác định!";

        // Ghi log lỗi chi tiết vào Logcat
        Log.e(TAG, "Firebase Exception: ", exception);

        if (exception instanceof FirebaseAuthUserCollisionException) return "Tài khoản này đã được sử dụng!";
        if (exception instanceof FirebaseAuthInvalidUserException) return "Tài khoản này không tồn tại!";
        if (exception instanceof FirebaseAuthInvalidCredentialsException) return "Thông tin đăng nhập không chính xác!";
        if (exception instanceof FirebaseNetworkException) return "Lỗi kết nối mạng, vui lòng thử lại!";

        return "Đã xảy ra sự cố hệ thống, vui lòng thử lại sau.";
    }

    private void saveUserToFirestore(String uid, String account, String nickname) {
        UserModel newUser = new UserModel(uid, account, nickname, "USER", System.currentTimeMillis());
        db.collection("users").document(uid).set(newUser)
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lưu Firestore: " + e.getMessage()));
    }

    public void cancelLoading() {
        isLoading.setValue(false);
    }

    public void login(String account, String pass) {
        isLoading.setValue(true);
        String finalAccount = formatInput(account);

        auth.signInWithEmailAndPassword(finalAccount, pass).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null && (isPhoneNumber(account) || user.isEmailVerified())) {
                    authStatus.setValue("SUCCESS:Đăng nhập thành công!");
                } else {
                    auth.signOut();
                    authStatus.setValue("ERROR:Vui lòng kiểm tra hộp thư để xác thực email!");
                }
            } else {
                authStatus.setValue("ERROR:" + getVietnameseErrorMessage(task.getException()));
            }
        });
    }

    public void register(String account, String pass, String nickname, boolean isTermsAccepted, Activity activity) {
        isLoading.setValue(true);
        String authEmail = formatInput(account);

        // Kiểm tra trên hệ thống xác thực Auth
        auth.fetchSignInMethodsForEmail(authEmail).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful() && authTask.getResult() != null &&
                    authTask.getResult().getSignInMethods() != null &&
                    !authTask.getResult().getSignInMethods().isEmpty()) {

                isLoading.setValue(false);
                authStatus.setValue("ERROR:Tài khoản này đã được đăng ký!");
                return;
            }

            if (!authTask.isSuccessful()) {
                Log.e(TAG, "Lỗi fetchSignInMethods: ", authTask.getException());
            }

            // Kiểm tra trong cơ sở dữ liệu Firestore
            List<String> checkValues = new ArrayList<>();
            checkValues.add(account);
            if (isPhoneNumber(account)) {
                checkValues.add(formatPhoneForFirebase(account));
                checkValues.add(authEmail);
            }

            db.collection("users").whereIn("email", checkValues).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    isLoading.setValue(false);
                    authStatus.setValue("ERROR:Thông tin này đã tồn tại!");
                } else {
                    if (!task.isSuccessful()) Log.e(TAG, "Lỗi Firestore email check: ", task.getException());

                    db.collection("users").whereIn("account", checkValues).get().addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful() && task2.getResult() != null && !task2.getResult().isEmpty()) {
                            isLoading.setValue(false);
                            authStatus.setValue("ERROR:Số điện thoại/Email đã được sử dụng!");
                        } else {
                            if (!task2.isSuccessful()) Log.e(TAG, "Lỗi Firestore account check: ", task2.getException());

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
                    saveUserToFirestore(user.getUid(), email, nickname);
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nickname).build();

                    user.updateProfile(profileUpdates).addOnCompleteListener(uTask ->
                            user.sendEmailVerification().addOnCompleteListener(vTask -> {
                                isLoading.setValue(false);
                                authStatus.setValue("SUCCESS_REGISTER_EMAIL:Đăng ký thành công! Hãy kiểm tra email.");
                            })
                    );
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
            authStatus.setValue("ERROR:Phiên xác thực đã hết hạn.");
            return;
        }
        isLoading.setValue(true);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        verifyAndCreateAccount(credential);
    }

    private void verifyAndCreateAccount(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    AuthCredential emailCred = EmailAuthProvider.getCredential(formatInput(pendingPhone), pendingPass);
                    user.linkWithCredential(emailCred).addOnCompleteListener(linkTask -> {
                        if (linkTask.isSuccessful()) {
                            saveUserToFirestore(user.getUid(), pendingPhone, pendingNickname);
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(pendingNickname).build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                isLoading.setValue(false);
                                authStatus.setValue("SUCCESS_REGISTER_PHONE:Xác thực thành công!");
                            });
                        } else {
                            isLoading.setValue(false);
                            Log.e(TAG, "Lỗi link credential: ", linkTask.getException());
                            authStatus.setValue("ERROR:Lỗi thiết lập mật khẩu cho tài khoản.");
                        }
                    });
                }
            } else {
                isLoading.setValue(false);
                authStatus.setValue("ERROR:Mã OTP không chính xác!");
            }
        });
    }

    public void signInWithGoogle(String idToken) {
        isLoading.setValue(true);
        com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (task.getResult().getAdditionalUserInfo() != null && task.getResult().getAdditionalUserInfo().isNewUser() && user != null) {
                    saveUserToFirestore(user.getUid(), user.getEmail(), user.getDisplayName());
                }
                authStatus.setValue("SUCCESS:Đăng nhập thành công!");
            } else {
                authStatus.setValue("ERROR:" + getVietnameseErrorMessage(task.getException()));
            }
        });
    }

    public void resetPassword(String account) {
        isLoading.setValue(true);
        auth.sendPasswordResetEmail(account).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                authStatus.setValue("SUCCESS_RESET:Đã gửi link khôi phục mật khẩu.");
            } else {
                authStatus.setValue("ERROR:" + getVietnameseErrorMessage(task.getException()));
            }
        });
    }
}