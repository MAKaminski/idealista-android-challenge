package dev.mkaminski.idealista.designsystem

import android.app.Activity
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * External links are the one place the app hands control to another process, so the two things that
 * matter are asserted here: the right URI leaves, and a device with nothing to handle it does not
 * crash.
 */
@RunWith(RobolectricTestRunner::class)
class ExternalLinksTest {

    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    @Test
    fun `a listing url leaves as a view intent for that url`() {
        ExternalLinks.openInBrowser(activity, "https://www.idealista.com/inmueble/3/")

        val started = shadowOf(activity).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("https://www.idealista.com/inmueble/3/", started.data.toString())
    }

    @Test
    fun `a geo uri is handed to whichever app claims the scheme`() {
        ExternalLinks.openExternally(activity, "geo:40.42,-3.70?q=40.42,-3.70(Calle%20de%20Ferraz)")

        val started = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("geo", started.data?.scheme)
    }

    /**
     * The regression that matters: a device with no handler must show a message, not throw.
     * Robolectric's package manager rejects unmatched intents, which is exactly that device.
     */
    @Test
    fun `a uri no app can handle does not crash the caller`() {
        shadowOf(activity.packageManager).setShouldShowActivityChooser(false)
        shadowOf(activity.packageManager).setResolveInfosForIntent(
            Intent(Intent.ACTION_VIEW),
            emptyList(),
        )

        ExternalLinks.openExternally(activity, "idealista-nonexistent-scheme://nowhere")
    }
}
