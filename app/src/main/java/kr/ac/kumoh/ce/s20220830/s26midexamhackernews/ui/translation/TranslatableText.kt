package kr.ac.kumoh.ce.s20220830.s26midexamhackernews.ui.translation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translator

val LocalTranslator = staticCompositionLocalOf<Translator> {
    error("Translator is not available")
}

@Composable
fun TranslatableText(text: String) {
    val translator = LocalTranslator.current
    var translated by rememberSaveable(text) { mutableStateOf<String?>(null) }
    var showTranslation by rememberSaveable(text) { mutableStateOf(false) }
    var loading by remember(text) { mutableStateOf(false) }
    var error by remember(text) { mutableStateOf(false) }
    // Ignore callbacks when a lazy-list item has left the composition.
    val active = remember(text) { booleanArrayOf(true) }
    DisposableEffect(active) {
        active[0] = true
        onDispose { active[0] = false }
    }

    Column {
        Text(if (showTranslation) translated ?: text else text)
        if (showTranslation && translated != null) {
            Text("Translated by Google", style = MaterialTheme.typography.labelSmall)
        }
        if (text.isNotBlank()) {
            TextButton(
                enabled = !loading,
                onClick = {
                    when {
                        showTranslation -> showTranslation = false
                        translated != null -> showTranslation = true
                        else -> {
                            error = false
                            loading = true
                            val conditions = DownloadConditions.Builder().requireWifi().build()
                            translator.downloadModelIfNeeded(conditions)
                                .addOnSuccessListener {
                                    if (active[0]) {
                                        translator.translate(text)
                                            .addOnSuccessListener { result ->
                                                if (active[0]) {
                                                    translated = result
                                                    showTranslation = true
                                                    loading = false
                                                }
                                            }
                                            .addOnFailureListener {
                                                if (active[0]) { loading = false; error = true }
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    if (active[0]) { loading = false; error = true }
                                }
                        }
                    }
                }
            ) {
                Text(when {
                    loading -> "번역 중…"
                    showTranslation -> "원문 보기"
                    error -> "다시 번역"
                    else -> "한국어로 번역"
                })
            }
            if (error) {
                Text(
                    "번역하지 못했습니다. 처음 사용할 때는 Wi-Fi 연결이 필요합니다.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
