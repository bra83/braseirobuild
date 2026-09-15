package com.braseiro.wwn.bridge

interface NativeFileActions {
    fun requestOpenCharacterJson()
    fun requestSaveCharacterJson(json: String, suggestedName: String)
    fun requestOpenBackupJson()
    fun requestSaveBackupJson(json: String, suggestedName: String)
    fun requestOpenRulesPdf(authorityProfile: String)
    fun requestOpenSceneImage()
    fun requestShareText(title: String, text: String)
    fun requestGeminiTextAsync(requestJson: String): String
    fun requestGeminiImageAsync(requestJson: String): String
    fun requestRagSearchAsync(requestJson: String): String
}
