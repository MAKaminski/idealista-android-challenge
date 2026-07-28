package dev.mkaminski.idealista.designsystem

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Opens an external destination without leaving the app's visual context.
 *
 * Custom Tabs keeps the app's colours and the back stack; a plain `ACTION_VIEW` is the fallback for
 * `geo:` URIs and for devices with no Custom Tabs provider. Nothing here throws at the caller — a
 * missing handler becomes a message rather than a crash.
 */
object ExternalLinks {

    fun openInBrowser(context: Context, url: String) {
        val uri = Uri.parse(url)
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .build()
                .launchUrl(context, uri)
        }.onFailure { openWithViewIntent(context, uri) }
    }

    /** `geo:` and other non-http schemes: hand straight to whatever app claims them. */
    fun openExternally(context: Context, uri: String) = openWithViewIntent(context, Uri.parse(uri))

    private fun openWithViewIntent(context: Context, uri: Uri) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.link_no_app, Toast.LENGTH_SHORT).show()
        }
    }
}
