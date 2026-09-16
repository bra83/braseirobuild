package com.braseiro.wwn.bridge;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000e\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\u0018\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0006H&J\b\u0010\b\u001a\u00020\u0003H&J\u0018\u0010\t\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0006H&J\u0010\u0010\n\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\u0006H&J\b\u0010\f\u001a\u00020\u0003H&J\u0018\u0010\r\u001a\u00020\u00032\u0006\u0010\u000e\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\u0006H&J\u0010\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u0006H&J\u0010\u0010\u0012\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u0006H&J\u0010\u0010\u0013\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\u0006H&\u00a8\u0006\u0014\u00c0\u0006\u0003"}, d2 = {"Lcom/braseiro/wwn/bridge/NativeFileActions;", "", "requestOpenCharacterJson", "", "requestSaveCharacterJson", "json", "", "suggestedName", "requestOpenBackupJson", "requestSaveBackupJson", "requestOpenRulesPdf", "authorityProfile", "requestOpenSceneImage", "requestShareText", "title", "text", "requestGeminiTextAsync", "requestJson", "requestGeminiImageAsync", "requestRagSearchAsync", "app"})
public abstract interface NativeFileActions {
    
    public abstract void requestOpenCharacterJson();
    
    public abstract void requestSaveCharacterJson(@org.jetbrains.annotations.NotNull()
    java.lang.String json, @org.jetbrains.annotations.NotNull()
    java.lang.String suggestedName);
    
    public abstract void requestOpenBackupJson();
    
    public abstract void requestSaveBackupJson(@org.jetbrains.annotations.NotNull()
    java.lang.String json, @org.jetbrains.annotations.NotNull()
    java.lang.String suggestedName);
    
    public abstract void requestOpenRulesPdf(@org.jetbrains.annotations.NotNull()
    java.lang.String authorityProfile);
    
    public abstract void requestOpenSceneImage();
    
    public abstract void requestShareText(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String text);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.lang.String requestGeminiTextAsync(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.lang.String requestGeminiImageAsync(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson);
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.lang.String requestRagSearchAsync(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson);
}