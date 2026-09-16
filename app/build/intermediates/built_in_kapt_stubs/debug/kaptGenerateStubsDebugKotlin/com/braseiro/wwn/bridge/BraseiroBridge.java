package com.braseiro.wwn.bridge;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b9\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001BW\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\t\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\r\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000f\u00a2\u0006\u0004\b\u0010\u0010\u0011J\b\u0010\u0014\u001a\u00020\u0015H\u0007J\u0018\u0010\u0016\u001a\u00020\u00152\u0006\u0010\u0017\u001a\u00020\u00152\u0006\u0010\u0018\u001a\u00020\u0015H\u0007J\u0010\u0010\u0019\u001a\u00020\u00152\u0006\u0010\u0018\u001a\u00020\u0015H\u0007J\u0010\u0010\u001a\u001a\u00020\u00152\u0006\u0010\u0018\u001a\u00020\u0015H\u0007J\u0010\u0010\u001b\u001a\u00020\u00152\u0006\u0010\u001c\u001a\u00020\u001dH\u0007J \u0010\u001e\u001a\u00020\u00152\u0006\u0010\u0018\u001a\u00020\u00152\u0006\u0010\u001f\u001a\u00020\u00152\u0006\u0010 \u001a\u00020\u0015H\u0007J\b\u0010!\u001a\u00020\u0015H\u0007J\u0010\u0010\"\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u0010$\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u0010%\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\b\u0010&\u001a\u00020\u0015H\u0007J\b\u0010\'\u001a\u00020\u0015H\u0007J\u0010\u0010(\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u0010)\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u0010*\u001a\u00020\u00152\u0006\u0010+\u001a\u00020\u0015H\u0007J\u0010\u0010,\u001a\u00020\u00152\u0006\u0010-\u001a\u00020\u0015H\u0007J\b\u0010.\u001a\u00020\u0015H\u0007J\u0010\u0010/\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u00100\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\b\u00101\u001a\u00020\u0015H\u0007J\b\u00102\u001a\u00020\u0015H\u0007J\u0010\u00103\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u00104\u001a\u00020\u00152\u0006\u00105\u001a\u00020\u0015H\u0007J\u0010\u00106\u001a\u00020\u00152\u0006\u00107\u001a\u00020\u001dH\u0007J\b\u00108\u001a\u00020\u0015H\u0007J\u0010\u00109\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\b\u0010:\u001a\u00020\u0015H\u0007J\u0018\u0010;\u001a\u00020\u00152\u0006\u0010<\u001a\u00020\u00152\u0006\u0010=\u001a\u00020\u0015H\u0007J\b\u0010>\u001a\u00020\u0015H\u0007J\u0018\u0010?\u001a\u00020\u00152\u0006\u0010@\u001a\u00020\u00152\u0006\u0010A\u001a\u00020\u0015H\u0007J\b\u0010B\u001a\u00020\u0015H\u0007J\u0018\u0010C\u001a\u00020\u00152\u0006\u0010@\u001a\u00020\u00152\u0006\u0010A\u001a\u00020\u0015H\u0007J\b\u0010D\u001a\u00020\u0015H\u0007J\b\u0010E\u001a\u00020\u0015H\u0007J\u0010\u0010F\u001a\u00020\u00152\u0006\u0010G\u001a\u00020\u0015H\u0007J\b\u0010H\u001a\u00020\u0015H\u0007J\u0010\u0010I\u001a\u00020\u00152\u0006\u0010G\u001a\u00020\u0015H\u0007J\u0010\u0010J\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u0010K\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u0010L\u001a\u00020\u00152\u0006\u0010#\u001a\u00020\u0015H\u0007J\u0010\u0010M\u001a\u00020\u00152\u0006\u0010N\u001a\u00020\u0015H\u0007J\u0018\u0010O\u001a\u00020\u00152\u0006\u0010G\u001a\u00020\u00152\u0006\u0010P\u001a\u00020\u0015H\u0007J\u0010\u0010Q\u001a\u00020\u00152\u0006\u0010G\u001a\u00020\u0015H\u0007J\u0010\u0010R\u001a\u00020\u00152\u0006\u0010G\u001a\u00020\u0015H\u0007J\u0018\u0010S\u001a\u00020\u00152\u0006\u0010T\u001a\u00020\u00152\u0006\u0010U\u001a\u00020\u001dH\u0007J\u0010\u0010V\u001a\u00020W2\u0006\u0010X\u001a\u00020YH\u0002J\u0010\u0010Z\u001a\u00020[2\u0006\u0010=\u001a\u00020\u0015H\u0002J\u0012\u0010\\\u001a\u00020[2\b\u0010]\u001a\u0004\u0018\u00010\u0001H\u0002J\u0010\u0010^\u001a\u00020Y2\u0006\u0010_\u001a\u00020`H\u0002J\u0010\u0010a\u001a\u00020Y2\u0006\u0010b\u001a\u00020cH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0004\u001a\u0004\u0018\u00010\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\b\u001a\u0004\u0018\u00010\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006d"}, d2 = {"Lcom/braseiro/wwn/bridge/BraseiroBridge;", "", "repository", "Lcom/braseiro/wwn/data/CampaignRepository;", "fileActions", "Lcom/braseiro/wwn/bridge/NativeFileActions;", "geminiRuntime", "Lcom/braseiro/wwn/bridge/GeminiRuntime;", "ragRuntime", "Lcom/braseiro/wwn/bridge/RagRuntime;", "ttsRuntime", "Lcom/braseiro/wwn/bridge/NativeTtsRuntime;", "audioRuntime", "Lcom/braseiro/wwn/bridge/NativeAudioRuntime;", "diceActions", "Lcom/braseiro/wwn/bridge/NativeDiceActions;", "<init>", "(Lcom/braseiro/wwn/data/CampaignRepository;Lcom/braseiro/wwn/bridge/NativeFileActions;Lcom/braseiro/wwn/bridge/GeminiRuntime;Lcom/braseiro/wwn/bridge/RagRuntime;Lcom/braseiro/wwn/bridge/NativeTtsRuntime;Lcom/braseiro/wwn/bridge/NativeAudioRuntime;Lcom/braseiro/wwn/bridge/NativeDiceActions;)V", "CREDENTIAL_KEY", "Lkotlin/text/Regex;", "geminiStatus", "", "setGeminiApiKey", "apiKey", "model", "setGeminiModel", "setGeminiImageModel", "setGeminiVisionEnabled", "enabled", "", "setGeminiTtsConfig", "voice", "provider", "clearGeminiApiKey", "geminiGenerateText", "requestJson", "geminiGenerateTextAsync", "geminiGenerateImageAsync", "ragStatus", "listRagDocuments", "ragSearch", "ragSearchAsync", "removeRagDocument", "documentId", "openRulesPdf", "authorityProfile", "ttsStatus", "ttsConfigure", "ttsSpeak", "ttsStop", "audioStatus", "audioConfigure", "audioApplyScenePlan", "planJson", "audioSetNarrationActive", "active", "audioStopAll", "startPhysicalDice", "openSceneImage", "shareText", "title", "text", "openCharacterJson", "saveCharacterJson", "json", "suggestedName", "openBackupJson", "saveBackupJson", "capabilities", "getActiveCampaignId", "setActiveCampaignId", "campaignId", "listCampaigns", "loadCampaign", "createCampaign", "commit", "appendNarrative", "getEvent", "eventId", "latestNarrative", "mechanicalEventId", "listEvents", "listSnapshots", "restorePortableBackup", "backupJson", "overwrite", "parsePortableRestorePayload", "Lcom/braseiro/wwn/data/PortableRestorePayload;", "backup", "Lorg/json/JSONObject;", "rejectCredentialJsonString", "", "rejectCredentialKeys", "value", "eventJson", "event", "Lcom/braseiro/wwn/data/CampaignEventEntity;", "receiptJson", "receipt", "Lcom/braseiro/wwn/data/CommitReceipt;", "app"})
public final class BraseiroBridge {
    @org.jetbrains.annotations.NotNull()
    private final com.braseiro.wwn.data.CampaignRepository repository = null;
    @org.jetbrains.annotations.Nullable()
    private final com.braseiro.wwn.bridge.NativeFileActions fileActions = null;
    @org.jetbrains.annotations.Nullable()
    private final com.braseiro.wwn.bridge.GeminiRuntime geminiRuntime = null;
    @org.jetbrains.annotations.Nullable()
    private final com.braseiro.wwn.bridge.RagRuntime ragRuntime = null;
    @org.jetbrains.annotations.Nullable()
    private final com.braseiro.wwn.bridge.NativeTtsRuntime ttsRuntime = null;
    @org.jetbrains.annotations.Nullable()
    private final com.braseiro.wwn.bridge.NativeAudioRuntime audioRuntime = null;
    @org.jetbrains.annotations.Nullable()
    private final com.braseiro.wwn.bridge.NativeDiceActions diceActions = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.text.Regex CREDENTIAL_KEY = null;
    
    public BraseiroBridge(@org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.data.CampaignRepository repository, @org.jetbrains.annotations.Nullable()
    com.braseiro.wwn.bridge.NativeFileActions fileActions, @org.jetbrains.annotations.Nullable()
    com.braseiro.wwn.bridge.GeminiRuntime geminiRuntime, @org.jetbrains.annotations.Nullable()
    com.braseiro.wwn.bridge.RagRuntime ragRuntime, @org.jetbrains.annotations.Nullable()
    com.braseiro.wwn.bridge.NativeTtsRuntime ttsRuntime, @org.jetbrains.annotations.Nullable()
    com.braseiro.wwn.bridge.NativeAudioRuntime audioRuntime, @org.jetbrains.annotations.Nullable()
    com.braseiro.wwn.bridge.NativeDiceActions diceActions) {
        super();
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String geminiStatus() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String setGeminiApiKey(@org.jetbrains.annotations.NotNull()
    java.lang.String apiKey, @org.jetbrains.annotations.NotNull()
    java.lang.String model) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String setGeminiModel(@org.jetbrains.annotations.NotNull()
    java.lang.String model) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String setGeminiImageModel(@org.jetbrains.annotations.NotNull()
    java.lang.String model) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String setGeminiVisionEnabled(boolean enabled) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String setGeminiTtsConfig(@org.jetbrains.annotations.NotNull()
    java.lang.String model, @org.jetbrains.annotations.NotNull()
    java.lang.String voice, @org.jetbrains.annotations.NotNull()
    java.lang.String provider) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String clearGeminiApiKey() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String geminiGenerateText(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String geminiGenerateTextAsync(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String geminiGenerateImageAsync(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ragStatus() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String listRagDocuments() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ragSearch(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ragSearchAsync(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String removeRagDocument(@org.jetbrains.annotations.NotNull()
    java.lang.String documentId) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String openRulesPdf(@org.jetbrains.annotations.NotNull()
    java.lang.String authorityProfile) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ttsStatus() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ttsConfigure(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ttsSpeak(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ttsStop() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String audioStatus() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String audioConfigure(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String audioApplyScenePlan(@org.jetbrains.annotations.NotNull()
    java.lang.String planJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String audioSetNarrationActive(boolean active) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String audioStopAll() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String startPhysicalDice(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String openSceneImage() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String shareText(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String openCharacterJson() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String saveCharacterJson(@org.jetbrains.annotations.NotNull()
    java.lang.String json, @org.jetbrains.annotations.NotNull()
    java.lang.String suggestedName) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String openBackupJson() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String saveBackupJson(@org.jetbrains.annotations.NotNull()
    java.lang.String json, @org.jetbrains.annotations.NotNull()
    java.lang.String suggestedName) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String capabilities() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getActiveCampaignId() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String setActiveCampaignId(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String listCampaigns() {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String loadCampaign(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String createCampaign(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String commit(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String appendNarrative(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getEvent(@org.jetbrains.annotations.NotNull()
    java.lang.String eventId) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String latestNarrative(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId, @org.jetbrains.annotations.NotNull()
    java.lang.String mechanicalEventId) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String listEvents(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String listSnapshots(@org.jetbrains.annotations.NotNull()
    java.lang.String campaignId) {
        return null;
    }
    
    @android.webkit.JavascriptInterface()
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String restorePortableBackup(@org.jetbrains.annotations.NotNull()
    java.lang.String backupJson, boolean overwrite) {
        return null;
    }
    
    private final com.braseiro.wwn.data.PortableRestorePayload parsePortableRestorePayload(org.json.JSONObject backup) {
        return null;
    }
    
    private final void rejectCredentialJsonString(java.lang.String text) {
    }
    
    private final void rejectCredentialKeys(java.lang.Object value) {
    }
    
    private final org.json.JSONObject eventJson(com.braseiro.wwn.data.CampaignEventEntity event) {
        return null;
    }
    
    private final org.json.JSONObject receiptJson(com.braseiro.wwn.data.CommitReceipt receipt) {
        return null;
    }
}