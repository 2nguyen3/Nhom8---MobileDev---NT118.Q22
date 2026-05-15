import agoraTokenBundle from "./agoraTokenBundle.cjs";

const resolvedBundle =
  agoraTokenBundle?.buildRtcTokenWithUserAccount
    ? agoraTokenBundle
    : agoraTokenBundle?.default?.buildRtcTokenWithUserAccount
      ? agoraTokenBundle.default
      : null;

export function buildRtcTokenWithUserAccount(appId, appCertificate, channelName, account) {
  if (!resolvedBundle || typeof resolvedBundle.buildRtcTokenWithUserAccount !== "function") {
    throw new Error(
      "Bundled token builder not found. bundle keys = " +
      Object.keys(agoraTokenBundle || {}).join(",")
    );
  }

  return resolvedBundle.buildRtcTokenWithUserAccount(
    appId,
    appCertificate,
    channelName,
    account
  );
}
