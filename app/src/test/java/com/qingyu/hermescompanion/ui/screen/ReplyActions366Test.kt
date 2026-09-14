package com.qingyu.hermescompanion.ui.screen

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.qingyu.hermescompanion.i18n.*
import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.ui.*
import com.qingyu.hermescompanion.ui.theme.HermesCompanionTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], qualifiers="en-rUS-w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReplyActions366Test {
    @get:Rule val compose = createComposeRule()
    private val context get() = RuntimeEnvironment.getApplication()
    @After fun resetLanguage() { AppLanguage.setMode(context, AppLanguageMode.CHINESE) }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun capture(name: String) = compose.runOnIdle {
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        File("build/ui-validation/366-$name.png").apply { parentFile.mkdirs() }.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test fun actionsCopyOnlyAnswerAndToggleAcrossSkinsAndLanguages() {
        val body = "Here is your answer.\n\n**Next step:** check the files."
        var skin by mutableStateOf(SkinMode.CLEAN)
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var reading by mutableStateOf(false)
        var calls = 0
        compose.setContent {
            HermesCompanionTheme(theme, skin) {
                Surface {
                    Column(Modifier.width(320.dp).padding(12.dp)) {
                        MessageItem(ChatMessage("answer", MessageRole.ASSISTANT, body, reasoning="PRIVATE REASONING"),
                            true, false, {_,_->}, {}, "User", "", "Hermes", "", emptyMap(), 0,
                            reading=reading, preparing=reading, onReadAloud={ calls++; reading=!reading })
                    }
                }
            }
        }
        for (language in listOf(AppLanguageMode.ENGLISH, AppLanguageMode.CHINESE)) {
            compose.runOnIdle { AppLanguage.setMode(context, language) }
            for (mode in SkinMode.entries) {
                compose.runOnIdle { skin=mode; theme=if(mode==SkinMode.GLASS) ThemeMode.DARK else ThemeMode.LIGHT }
                compose.onNodeWithTag("reply_copy").assertIsDisplayed().performClick()
                val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                assertEquals(body,clipboard.primaryClip!!.getItemAt(0).text.toString())
                val before=calls
                compose.onNodeWithTag("reply_read_aloud").assertIsDisplayed().performClick()
                assertEquals(before+1,calls)
                compose.onNodeWithText(if(language==AppLanguageMode.ENGLISH) "Stop reading" else "停止朗读").assertIsDisplayed()
                compose.onNodeWithTag("reply_read_aloud").performClick()
                assertEquals(before+2,calls)
                if(language==AppLanguageMode.ENGLISH) capture("reply-${mode.name.lowercase()}")
            }
        }
    }

    @Test fun actionsWaitForCompletedAssistantAnswer() {
        var message by mutableStateOf(ChatMessage("a", MessageRole.ASSISTANT, "Partial answer", isStreaming=true))
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) {
            Surface { MessageItem(message, false, false, {_,_->}, {}, "User", "", "Hermes", "", emptyMap(), 0) }
        } }
        compose.onNodeWithTag("reply_copy").assertDoesNotExist()
        compose.runOnIdle { message=message.copy(isStreaming=false) }
        compose.onNodeWithTag("reply_copy").assertExists()
        compose.runOnIdle { message=message.copy(role=MessageRole.USER) }
        compose.onNodeWithTag("reply_copy").assertDoesNotExist()
        compose.runOnIdle { message=message.copy(role=MessageRole.ASSISTANT,content="",reasoning="Only reasoning") }
        compose.onNodeWithTag("reply_read_aloud").assertDoesNotExist()
    }

    @Test fun englishRecognitionIsVisibleAndSavedOnConfirmation() {
        AppLanguage.setMode(context, AppLanguageMode.ENGLISH)
        var state by mutableStateOf(AppUiState(route=AppRoute.VOICE_SETTINGS))
        var saved: ServerVoiceSettings? = null
        compose.setContent { HermesCompanionTheme(ThemeMode.LIGHT, SkinMode.CLEAN) {
            Surface(Modifier.fillMaxSize()) {
                VoiceSettingsScreen(state, PaddingValues(), {}, { state=state.copy(voicePreferences=it) },
                    { saved=it;state=state.copy(serverSettings=state.serverSettings.copy(voice=it)) }, {}, {}, {}, {}, {})
            }
        } }
        compose.onNodeWithText("Hermes recognition language").assertIsDisplayed().performClick()
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("OK").performClick()
        assertEquals("en", saved?.stt?.language)
        compose.onNodeWithText("English").assertIsDisplayed()
        capture("voice-language-english")
        compose.onNodeWithText("Phone recognition language").performClick()
        compose.onNodeWithText("English (UK)").performClick()
        compose.onNodeWithText("OK").performClick()
        assertEquals("en-GB", state.voicePreferences.language)
    }
}
