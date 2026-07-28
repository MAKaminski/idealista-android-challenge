package dev.mkaminski.idealista.data.translate

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import dev.mkaminski.idealista.data.di.IoDispatcher
import dev.mkaminski.idealista.model.AppLanguage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * On-device translation through ML Kit.
 *
 * On-device rather than a cloud translation API because a cloud one needs a key, and a submission
 * should carry no secrets — the same reason the map uses OpenStreetMap tiles. The trade is a model
 * download (~30 MB per language) the first time a language is used, after which it works offline.
 *
 * Every failure path returns `null`, which callers render as "show the original". A listing the
 * user can still read in Spanish beats an error message where the description should be.
 */
@Singleton
class MlKitAdTextTranslator @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AdTextTranslator {

    // Translating a screen means several strings into the same language; a fresh engine per string
    // would re-check the model every time.
    private val cache = ConcurrentHashMap<String, String>()

    override suspend fun translate(text: String, target: AppLanguage): String? {
        if (text.isBlank() || target == AdTextTranslator.SOURCE) return null
        val mlKitTarget = target.toMlKitLanguage() ?: return null
        val key = "${target.tag}:${text.hashCode()}"
        cache[key]?.let { return it }

        return withContext(ioDispatcher) {
            val translator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.SPANISH)
                    .setTargetLanguage(mlKitTarget)
                    .build(),
            )
            try {
                translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitOrNull()
                translator.translate(text).awaitOrNull()?.also { cache[key] = it }
            } catch (_: Exception) {
                // Offline on first use, storage full, model withdrawn — all mean "show the original".
                null
            } finally {
                translator.close()
            }
        }
    }

    /** ML Kit names languages with its own constants; only the ones the app ships are mapped. */
    private fun AppLanguage.toMlKitLanguage(): String? = when (this) {
        AppLanguage.ENGLISH -> TranslateLanguage.ENGLISH
        AppLanguage.FRENCH -> TranslateLanguage.FRENCH
        AppLanguage.PORTUGUESE -> TranslateLanguage.PORTUGUESE
        AppLanguage.ITALIAN -> TranslateLanguage.ITALIAN
        AppLanguage.CHINESE -> TranslateLanguage.CHINESE
        AppLanguage.SPANISH -> null
    }
}

/** Bridges a Play-services `Task` into a coroutine without adding kotlinx-coroutines-play-services. */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        continuation.resume(if (task.isSuccessful) task.result else null)
    }
}
