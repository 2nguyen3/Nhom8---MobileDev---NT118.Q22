import { buildRtcTokenWithUserAccount } from "./agoraTokenShim.js";

import {
  getConsultationDocument,
  getStringField,
  getTimestampField,
  hasDocumentField,
  updateConsultationFields,
  makeTimestampUpdate
} from "./firestore.js";

function isCallFormat(rawFormatType) {
  const normalized = String(rawFormatType || "").trim().toLowerCase();
  return (
    normalized.includes("gọi") ||
    normalized.includes("video") ||
    normalized.includes("call")
  );
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/") {
      return Response.json({
        success: true,
        message: "Heami call worker is running",
        secrets: {
          hasAgoraAppId: !!env.AGORA_APP_ID,
          hasAgoraCertificate: !!env.AGORA_APP_CERTIFICATE,
          hasFirebaseProjectId: !!env.FIREBASE_PROJECT_ID,
          hasFirebaseClientEmail: !!env.FIREBASE_CLIENT_EMAIL,
          hasFirebasePrivateKey: !!env.FIREBASE_PRIVATE_KEY
        }
      });
    }

    if (request.method === "GET" && url.pathname === "/debug/consultation") {
      try {
        const sessionId = String(url.searchParams.get("session_id") || "").trim();

        if (!sessionId) {
          return Response.json(
            {
              success: false,
              message: "Thiếu query session_id"
            },
            { status: 400 }
          );
        }

        const doc = await getConsultationDocument(env, sessionId);

        if (!doc) {
          return Response.json(
            {
              success: false,
              message: "Không tìm thấy consultation"
            },
            { status: 404 }
          );
        }

        return Response.json({
          success: true,
          data: {
            documentName: doc.name || "",
            sessionId: sessionId,
            userId: getStringField(doc, "user_id"),
            doctorId: getStringField(doc, "doctor_id"),
            formatType: getStringField(doc, "format_type"),
            status: getStringField(doc, "status"),
            roomId: getStringField(doc, "room_id"),
            callChannelId: getStringField(doc, "call_channel_id"),
            callStatus: getStringField(doc, "call_status"),
            callStartedBy: getStringField(doc, "call_started_by"),
            hasCallStartedAt: hasDocumentField(doc, "call_started_at"),
            hasCallEndedAt: hasDocumentField(doc, "call_ended_at"),
            startTime: getTimestampField(doc, "start_time"),
            endTime: getTimestampField(doc, "end_time")
          }
        });
      } catch (error) {
        return Response.json(
          {
            success: false,
            message: error?.message || "Không đọc được consultation debug"
          },
          { status: 500 }
        );
      }
    }

if (request.method === "POST" && url.pathname === "/rtc/agora/token") {
  try {
    const body = await request.json();
    const sessionId = String(body?.session_id || "").trim();
    const role = String(body?.role || "").trim().toUpperCase();
    const uid = String(body?.uid || "").trim();

    if (!sessionId || !role || !uid) {
      return Response.json(
        {
          success: false,
          message: "Thiếu session_id hoặc role hoặc uid"
        },
        { status: 400 }
      );
    }

    if (!env.AGORA_APP_ID || !env.AGORA_APP_CERTIFICATE) {
      return Response.json(
        {
          success: false,
          message: "Thiếu secret Agora trên Worker"
        },
        { status: 500 }
      );
    }

    if (!env.FIREBASE_PROJECT_ID || !env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
      return Response.json(
        {
          success: false,
          message: "Thiếu secret Firebase trên Worker"
        },
        { status: 500 }
      );
    }

    const consultationDoc = await getConsultationDocument(env, sessionId);

    if (!consultationDoc) {
      return Response.json(
        {
          success: false,
          message: "Không tìm thấy consultation"
        },
        { status: 404 }
      );
    }

const consultationUserId = getStringField(consultationDoc, "user_id");
const consultationDoctorId = getStringField(consultationDoc, "doctor_id");
const formatType = getStringField(consultationDoc, "format_type");
const consultationStatus = getStringField(consultationDoc, "status").toUpperCase();
const existingCallChannelId = getStringField(consultationDoc, "call_channel_id");
const existingCallStartedBy = getStringField(consultationDoc, "call_started_by");
const hasCallStartedAt = hasDocumentField(consultationDoc, "call_started_at");

    if (!isCallFormat(formatType)) {
      return Response.json(
        {
          success: false,
          message: "Phiên tư vấn này không hỗ trợ gọi video"
        },
        { status: 409 }
      );
    }

    if (consultationStatus === "COMPLETED") {
      return Response.json(
        {
          success: false,
          message: "Phiên tư vấn đã hoàn tất"
        },
        { status: 410 }
      );
    }

    if (consultationStatus === "CANCELLED" || consultationStatus === "CANCELED") {
      return Response.json(
        {
          success: false,
          message: "Phiên tư vấn đã bị hủy"
        },
        { status: 410 }
      );
    }

    if (role === "USER" && uid !== consultationUserId) {
      return Response.json(
        {
          success: false,
          message: "Bạn không có quyền truy cập consultation này"
        },
        { status: 403 }
      );
    }

    if (role === "DOCTOR" && uid !== consultationDoctorId) {
      return Response.json(
        {
          success: false,
          message: "Bạn không có quyền truy cập consultation này"
        },
        { status: 403 }
      );
    }

    if (role !== "USER" && role !== "DOCTOR") {
      return Response.json(
        {
          success: false,
          message: "role không hợp lệ"
        },
        { status: 400 }
      );
    }

const finalCallChannelId = existingCallChannelId || `consult_${sessionId}`;

const fieldUpdates = {
  call_channel_id: finalCallChannelId,
  call_status: "CONNECTING"
};

if (!existingCallStartedBy) {
  fieldUpdates.call_started_by = uid;
}

if (!hasCallStartedAt) {
  fieldUpdates.call_started_at = makeTimestampUpdate();
}

await updateConsultationFields(env, sessionId, fieldUpdates);

let realToken = "";

try {
  realToken = buildRtcTokenWithUserAccount(
    env.AGORA_APP_ID,
    env.AGORA_APP_CERTIFICATE,
    finalCallChannelId,
    uid
  );

  console.log("REAL_TOKEN_DEBUG", {
    appIdLength: (env.AGORA_APP_ID || "").length,
    certLength: (env.AGORA_APP_CERTIFICATE || "").length,
    channelName: finalCallChannelId,
    uid: uid,
    tokenType: typeof realToken,
    tokenLength: typeof realToken === "string" ? realToken.length : -1,
    tokenPreview: typeof realToken === "string" ? realToken.slice(0, 24) : null
  });
} catch (error) {
  console.error("REAL_TOKEN_BUILD_ERROR", {
    message: error?.message,
    stack: error?.stack,
    name: error?.name
  });

  return Response.json(
    {
      success: false,
      message: error?.message || "Build token thật bị lỗi"
    },
    { status: 500 }
  );
}

if (!realToken || typeof realToken !== "string" || realToken.trim() === "") {
  return Response.json(
    {
      success: false,
      message: "Không tạo được Agora token thật",
      debug: {
        appIdLength: (env.AGORA_APP_ID || "").length,
        certLength: (env.AGORA_APP_CERTIFICATE || "").length,
        channelName: finalCallChannelId,
        uid: uid,
        tokenType: typeof realToken,
        tokenLength: typeof realToken === "string" ? realToken.length : -1
      }
    },
    { status: 500 }
  );
}

return Response.json({
  success: true,
  data: {
    appId: env.AGORA_APP_ID,
    channelName: finalCallChannelId,
    token: realToken,
    uid: uid,
    sessionId: sessionId,
    callChannelId: finalCallChannelId,
    callStatus: "CONNECTING"
  }
});
  } catch (error) {
    return Response.json(
      {
        success: false,
        message: error?.message || "Không xử lý được request token"
      },
      { status: 500 }
    );
  }
}

    return Response.json(
      {
        success: false,
        message: "Not found"
      },
      { status: 404 }
    );
  }
};
