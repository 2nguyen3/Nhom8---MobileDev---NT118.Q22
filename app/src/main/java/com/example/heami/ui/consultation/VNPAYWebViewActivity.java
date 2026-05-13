package com.example.heami.ui.consultation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity chuyên dụng cho thanh toán VNPAY qua WebView.
 * Kết quả được trả về qua ActivityResultLauncher trong BookingFlowActivity.
 */
public class VNPAYWebViewActivity extends AppCompatActivity {

    public static final String EXTRA_PAYMENT_URL    = "payment_url";
    public static final String RESULT_RESPONSE_CODE  = "vnp_ResponseCode";
    public static final String RESULT_TRANSACTION_NO = "vnp_TransactionNo";

    private WebView webView;
    private boolean isHandled = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {

            // ✅ Primary interceptor — fired before URL loads
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                android.util.Log.d("VNPAYWebView", "shouldOverride: " + url);
                if (interceptReturnUrl(url)) return true;
                return super.shouldOverrideUrlLoading(view, request);
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                android.util.Log.d("VNPAYWebView", "shouldOverride(deprecated): " + url);
                if (interceptReturnUrl(url)) return true;
                return super.shouldOverrideUrlLoading(view, url);
            }

            // ✅ Backup interceptor — catches any URL that slips through
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                android.util.Log.d("VNPAYWebView", "onPageStarted: " + url);
                if (interceptReturnUrl(url)) return;
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // Cho phép kết nối SSL của VNPAY sandbox
                handler.proceed();
            }
        });

        // Back button: đi lại trong WebView hoặc hủy thanh toán
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        });

        String paymentUrl = getIntent().getStringExtra(EXTRA_PAYMENT_URL);
        if (paymentUrl != null) {
            android.util.Log.d("VNPAYWebView", "Loading: " + paymentUrl);
            webView.loadUrl(paymentUrl);
        } else {
            // Có thể được mở qua deep link (heami://vnpay_return?...)
            if (getIntent() != null && getIntent().getData() != null) {
                interceptReturnUrl(getIntent().getData().toString());
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    /**
     * Được gọi khi Activity đã tồn tại (launchMode=singleTop) và nhận Intent mới.
     * Xử lý trường hợp Android hệ thống mở heami://vnpay_return như một deep link.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getData() != null) {
            String url = intent.getData().toString();
            android.util.Log.d("VNPAYWebView", "onNewIntent deep link: " + url);
            interceptReturnUrl(url);
        }
    }

    /**
     * Kiểm tra URL trả về từ VNPAY.
     * Nếu đúng là return URL → set result và kết thúc Activity.
     * @return true nếu đây là return URL và đã được xử lý
     */
    private boolean interceptReturnUrl(String url) {
        if (url == null || isHandled) return false;

        // Kiểm tra case-insensitive để đề phòng VNPAY thay đổi case
        String urlLower = url.toLowerCase();
        if (urlLower.startsWith("heami://vnpay_return")) {
            isHandled = true;
            try {
                android.util.Log.d("VNPAYWebView", "Redirecting explicitly to BookingFlowActivity with URL: " + url);
                Intent intent = new Intent(this, BookingFlowActivity.class);
                intent.setData(Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("VNPAYWebView", "Error launching BookingFlowActivity: " + e.getMessage());
            }

            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.destroy();
        }
        super.onDestroy();
    }
}
