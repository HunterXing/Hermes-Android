package com.qingyu.hermescompanion.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.i18n.uiText
import kotlinx.coroutines.delay

/** Actions apply only to the answer body, never the separate reasoning field. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReplyActions(text: String, reading: Boolean, preparing: Boolean, onReadAloud: () -> Unit) {
    val context = LocalContext.current
    var copied by remember(text) { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1_500); copied = false } }
    val preparingLabel = uiText(R.string.reply_preparing_audio, "正在准备语音")
    FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Hermes", text))
                copied = true
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("reply_copy"),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(painterResource(R.drawable.hermes_refined_common_copy_outline), null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (copied) uiText(R.string.reply_copied, "已复制") else uiText(R.string.reply_copy, "复制"))
        }
        TextButton(
            onClick = onReadAloud,
            modifier = Modifier.heightIn(min = 48.dp).testTag("reply_read_aloud")
                .semantics { if (preparing) stateDescription = preparingLabel },
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            if (preparing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 1.5.dp)
            else Icon(painterResource(if (reading) R.drawable.hermes_refined_task_stop_filled else R.drawable.hermes_reply_speaker), null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (reading) uiText(R.string.reply_stop_reading, "停止朗读") else uiText(R.string.reply_read_aloud, "朗读"))
        }
    }
}
