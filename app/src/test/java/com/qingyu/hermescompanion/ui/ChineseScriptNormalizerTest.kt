package com.qingyu.hermescompanion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ChineseScriptNormalizerTest {
    @Test
    fun simplifiedModeUsesTraditionalToSimplifiedTransform() {
        val result = normalizeVoiceTranscript("測試語音", "simplified") { transformId, text ->
            assertEquals("Traditional-Simplified", transformId)
            assertEquals("測試語音", text)
            "测试语音"
        }

        assertEquals("测试语音", result)
    }

    @Test
    fun traditionalModeUsesReverseTransform() {
        assertEquals("Simplified-Traditional", chineseScriptTransliteratorId("traditional"))
    }

    @Test
    fun originalModeKeepsProviderText() {
        val result = normalizeVoiceTranscript("測試語音", "original") { _, _ ->
            fail("保持原文时不应执行转换")
            ""
        }

        assertEquals("測試語音", result)
    }

    @Test
    fun recognitionLocaleFollowsSelectedChineseScript() {
        assertEquals("zh-HK", voiceRecognitionLanguage("zh-HK", "simplified"))
        assertEquals("zh-TW", voiceRecognitionLanguage("zh-CN", "traditional"))
        assertEquals("en-US", voiceRecognitionLanguage("en-US", "simplified"))
    }

    @Test fun englishTranscriptIsPreservedWithoutChineseConversion() {
        val original = "I'll check the files.\nThen reply in English."
        for (script in listOf("simplified", "traditional", "original")) {
            assertEquals(original, normalizeVoiceTranscript(original, script) { _, _ ->
                fail("English must not be passed through Chinese conversion")
                ""
            })
        }
    }

    @Test fun defaultRecognitionUsesPhoneLanguageInsteadOfMandarin() {
        val old = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.UK)
            assertEquals("en-GB", voiceRecognitionLanguage("system", "simplified"))
            assertEquals("en-GB", voiceRecognitionLanguage("system", "traditional"))
        } finally { java.util.Locale.setDefault(old) }
    }
}
