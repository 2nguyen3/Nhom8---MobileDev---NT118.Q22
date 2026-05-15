import { getGoogleAccessToken } from "./googleAuth.js";

function buildDocumentUrl(projectId, collectionName, documentId) {
  return `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${collectionName}/${documentId}`;
}

export async function getConsultationDocument(env, sessionId) {
  if (!env.FIREBASE_PROJECT_ID) {
    throw new Error("Thiếu FIREBASE_PROJECT_ID");
  }

  const accessToken = await getGoogleAccessToken(env);
  const url = buildDocumentUrl(env.FIREBASE_PROJECT_ID, "consultations", sessionId);

  const response = await fetch(url, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  if (response.status === 404) {
    return null;
  }

  const json = await response.json();

  if (!response.ok) {
    throw new Error(json?.error?.message || "Không đọc được consultation từ Firestore");
  }

  return json;
}

export function getStringField(document, fieldName) {
  return document?.fields?.[fieldName]?.stringValue || "";
}

export function getTimestampField(document, fieldName) {
  return document?.fields?.[fieldName]?.timestampValue || "";
}

export function hasDocumentField(document, fieldName) {
  return !!document?.fields?.[fieldName];
}

function toFirestoreFieldValue(value) {
  if (value && typeof value === "object" && value.__type === "timestamp") {
    return {
      timestampValue: value.value
    };
  }

  return {
    stringValue: String(value ?? "")
  };
}

export function makeTimestampUpdate(value = new Date().toISOString()) {
  return {
    __type: "timestamp",
    value
  };
}

export async function updateConsultationFields(env, sessionId, fieldUpdates) {
  if (!env.FIREBASE_PROJECT_ID) {
    throw new Error("Thiếu FIREBASE_PROJECT_ID");
  }

  const accessToken = await getGoogleAccessToken(env);
  const url = new URL(
    buildDocumentUrl(env.FIREBASE_PROJECT_ID, "consultations", sessionId)
  );

  const fields = {};

  for (const [fieldName, value] of Object.entries(fieldUpdates)) {
    url.searchParams.append("updateMask.fieldPaths", fieldName);
    fields[fieldName] = toFirestoreFieldValue(value);
  }

  const response = await fetch(url.toString(), {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "content-type": "application/json"
    },
    body: JSON.stringify({ fields })
  });

  const json = await response.json();

  if (!response.ok) {
    throw new Error(json?.error?.message || "Không cập nhật được consultation");
  }

  return json;
}
