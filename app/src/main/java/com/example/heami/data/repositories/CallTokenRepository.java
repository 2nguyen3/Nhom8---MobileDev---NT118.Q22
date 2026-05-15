package com.example.heami.data.repositories;

import androidx.annotation.NonNull;

import com.example.heami.data.models.CallTokenResponseModel;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Dns;

public class CallTokenRepository {

    public interface FetchCallTokenListener {
        void onSuccess(@NonNull CallTokenResponseModel.Data data);
        void onFailure(@NonNull String errorMessage);
    }

    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .dns(hostname -> {
                try {
                    List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname);
                    List<InetAddress> ipv4 = new ArrayList<>();
                    List<InetAddress> ipv6 = new ArrayList<>();

                    for (InetAddress address : addresses) {
                        if (address instanceof Inet4Address) {
                            ipv4.add(address);
                        } else {
                            ipv6.add(address);
                        }
                    }

                    List<InetAddress> result = new ArrayList<>();
                    result.addAll(ipv4);
                    result.addAll(ipv6);

                    if (result.isEmpty()) {
                        throw new UnknownHostException("No addresses for " + hostname);
                    }

                    return result;
                } catch (Exception e) {
                    throw new UnknownHostException(e.getMessage());
                }
            })
            .build();

    private static final String BASE_URL = "https://heami-call-worker.heami-call.workers.dev";
    private static final String RTC_TOKEN_URL = BASE_URL + "/rtc/agora/token";

    public void fetchAgoraToken(
            @NonNull String sessionId,
            @NonNull String role,
            @NonNull String uid,
            @NonNull FetchCallTokenListener listener
    ) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("session_id", sessionId);
            bodyJson.put("role", role);
            bodyJson.put("uid", uid);

            RequestBody requestBody = RequestBody.create(
                    bodyJson.toString(),
                    JSON
            );

            Request request = new Request.Builder()
                    .url(RTC_TOKEN_URL)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    listener.onFailure(
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Không thể gọi API lấy token Agora"
                    );
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String raw = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        listener.onFailure(
                                raw.isEmpty()
                                        ? "Backend trả về lỗi khi lấy token"
                                        : raw
                        );
                        return;
                    }

                    try {
                        JSONObject root = new JSONObject(raw);
                        boolean success = root.optBoolean("success", false);

                        if (!success) {
                            listener.onFailure(root.optString("message", "Không lấy được token Agora"));
                            return;
                        }

                        JSONObject dataObj = root.optJSONObject("data");
                        if (dataObj == null) {
                            listener.onFailure("Response token không có data");
                            return;
                        }

                        CallTokenResponseModel.Data data = new CallTokenResponseModel.Data();
                        data.setAppId(dataObj.optString("appId", ""));
                        data.setChannelName(dataObj.optString("channelName", ""));
                        data.setToken(dataObj.optString("token", ""));
                        data.setUid(dataObj.optString("uid", ""));
                        data.setSessionId(dataObj.optString("sessionId", ""));
                        data.setCallChannelId(dataObj.optString("callChannelId", ""));
                        data.setCallStatus(dataObj.optString("callStatus", ""));

                        if (data.getAppId().isEmpty()
                                || data.getChannelName().isEmpty()
                                || data.getToken().isEmpty()) {
                            listener.onFailure("Dữ liệu token trả về chưa đầy đủ");
                            return;
                        }

                        listener.onSuccess(data);

                    } catch (Exception parseError) {
                        listener.onFailure(
                                parseError.getMessage() != null
                                        ? parseError.getMessage()
                                        : "Không parse được response token"
                        );
                    }
                }
            });

        } catch (Exception e) {
            listener.onFailure(
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Không tạo được request lấy token"
            );
        }
    }
}