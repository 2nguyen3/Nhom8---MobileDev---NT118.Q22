const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(
      JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON)
    ),
  });
}

const db = admin.firestore();

function safeText(value, fallback = "") {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

function truncatePreview(text, max = 120) {
  const value = safeText(text, "");
  return value.length <= max ? value : value.substring(0, max - 1) + "…";
}

module.exports = async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }

  try {
    const authHeader = req.headers.authorization || "";
    const idToken = authHeader.startsWith("Bearer ")
      ? authHeader.substring(7)
      : "";

    if (!idToken) {
      return res.status(401).json({ error: "Missing Firebase ID token" });
    }

    const decoded = await admin.auth().verifyIdToken(idToken);
    const senderUid = decoded.uid;

    const { roomId, messageId, messageText } = req.body || {};

    if (!roomId || !messageText) {
      return res.status(400).json({ error: "roomId and messageText are required" });
    }

    const roomRef = db.collection("chat_rooms").doc(roomId);
    const roomSnap = await roomRef.get();

    if (!roomSnap.exists) {
      return res.status(404).json({ error: "Room not found" });
    }

    const room = roomSnap.data() || {};
    const memberIds = Array.isArray(room.member_ids) ? room.member_ids : [];

    if (!memberIds.includes(senderUid)) {
      return res.status(403).json({ error: "Sender is not a room member" });
    }

    if (safeText(room.status, "ACTIVE") !== "ACTIVE") {
      return res.status(200).json({ sent: false, reason: "room_not_active" });
    }

    const receiverUid = memberIds.find((uid) => uid !== senderUid);
    if (!receiverUid) {
      return res.status(200).json({ sent: false, reason: "receiver_not_found" });
    }

    const mutedByMap = room.muted_by_map || {};
    if (mutedByMap[receiverUid] === true) {
      return res.status(200).json({ sent: false, reason: "muted" });
    }

    const memberNames = Array.isArray(room.member_names) ? room.member_names : [];
    const senderIndex = memberIds.indexOf(senderUid);
    const senderName =
      senderIndex >= 0 && senderIndex < memberNames.length
        ? safeText(memberNames[senderIndex], "Người bạn ẩn danh")
        : "Người bạn ẩn danh";

    const receiverAccountSnap = await db.collection("accounts").doc(receiverUid).get();
    if (!receiverAccountSnap.exists) {
      return res.status(200).json({ sent: false, reason: "receiver_account_missing" });
    }

    const receiverToken = safeText(receiverAccountSnap.get("fcm_token"), "");
    if (!receiverToken) {
      return res.status(200).json({ sent: false, reason: "receiver_token_missing" });
    }

    const preview = truncatePreview(messageText);

    const payload = {
      token: receiverToken,
      notification: {
        title: senderName,
        body: preview
      },
      data: {
        type: "COMMUNITY_CHAT",
        room_id: roomId,
        match_id: safeText(room.related_id, ""),
        matched_user_id: senderUid,
        sender_id: senderUid,
        sender_name: senderName,
        preview_text: preview,
        message_id: safeText(messageId, ""),
        mood_tag: safeText(room.match_mood_tag, "stress")
      },
      android: {
        priority: "high",
        notification: {
          channelId: "heami_chat_messages",
          sound: "default",
          tag: roomId
        }
      }
    };

    const result = await admin.messaging().send(payload);

    return res.status(200).json({
      sent: true,
      messageId: result
    });
  } catch (error) {
    console.error("chat-push error:", error);
    return res.status(500).json({
      error: "internal_error",
      message: error.message || "Unknown error"
    });
  }
};