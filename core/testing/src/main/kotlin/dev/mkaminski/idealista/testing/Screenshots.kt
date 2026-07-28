package dev.mkaminski.idealista.testing

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import org.junit.Assume
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Renders the app's real views to PNGs without a device.
 *
 * There is no emulator in this project's development container, so the README screenshots are
 * rasterised by Robolectric's **native graphics** backend instead: the same layouts, the same
 * theme, the same adapters and the same string resources, drawn to a `Bitmap` offscreen. What this
 * does *not* exercise is a fragment's state-to-view mapping — screenshots bind the adapters
 * directly. `docs/TESTING.md` says so rather than implying these came off a phone.
 *
 * Generation is opt-in ([assumeEnabled]) because it writes into `docs/`. `./gradlew screenshots`
 * sets the flag and pre-fetches the photos; an ordinary `testDebugUnitTest` skips these tests, so
 * CI neither rewrites committed files nor depends on a CDN.
 */
object Screenshots {

    private const val ENABLED_PROPERTY = "idealista.screenshots"

    /** Skips the test unless generation was explicitly asked for. */
    fun assumeEnabled() {
        Assume.assumeTrue(
            "Screenshot generation is opt-in — run ./gradlew screenshots",
            System.getProperty(ENABLED_PROPERTY) == "true",
        )
    }

    private val outputDir: File
        get() = File(System.getProperty("$ENABLED_PROPERTY.dir") ?: "build/screenshots")
            .also { it.mkdirs() }

    /** Populated by the `screenshots` Gradle task before the tests run — never fetched from here. */
    private val photoCache: File
        get() = File(System.getProperty("$ENABLED_PROPERTY.photos") ?: "build/screenshot-photos")

    /**
     * Points Coil at locally cached copies of the real photo URLs, keyed **by URL**.
     *
     * Keyed rather than one shared image on purpose: every ad then shows its own photo, so a
     * screenshot would visibly expose the ad-1-everywhere bug ADR-0005 exists to prevent.
     */
    fun installPhotoLoader(context: Context) {
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(context)
                // Robolectric has one thread and no frame loop, so a request handed to a background
                // dispatcher never lands before the bitmap is drawn. Pinning every stage to an
                // unconfined context makes the load resolve inside the calling turn.
                .interceptorCoroutineContext(Dispatchers.Unconfined)
                .fetcherCoroutineContext(Dispatchers.Unconfined)
                .decoderCoroutineContext(Dispatchers.Unconfined)
                .components {
                    add(
                        Interceptor { chain ->
                            val url = chain.request.data as? String
                                ?: return@Interceptor chain.proceed()
                            SuccessResult(image = photoFor(url).asImage(), request = chain.request)
                        },
                    )
                }
                .build()
        }
    }

    private fun photoFor(url: String): Bitmap {
        val cached = File(photoCache, url.substringAfterLast('/'))
        // A swatch rather than a blank frame when the photo is missing: the point of the image in a
        // screenshot is that a reviewer can judge the layout, and an empty box judges it wrongly.
        return cached.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
            ?: swatch(url.hashCode())
    }

    private fun swatch(seed: Int): Bitmap =
        Bitmap.createBitmap(591, 420, Bitmap.Config.ARGB_8888).apply {
            val s = Math.floorMod(seed, 60)
            eraseColor(Color.rgb(110 + s, 125 + s, 135 + s))
        }

    /**
     * Attaches [view] to [activity], lays it out at [width] × [height] and writes it as a PNG.
     *
     * Attaching is not optional: Coil suspends a request until its target view is attached to a
     * window and has a size, so a detached hierarchy renders nothing but placeholders — which is
     * exactly how the first version of this harness quietly produced photo-less screenshots.
     */
    fun capture(
        activity: Activity,
        view: View,
        name: String,
        width: Int = 1080,
        height: Int = 2100,
    ) {
        activity.setContentView(view, ViewGroup.LayoutParams(width, height))
        val root: View = activity.findViewById(android.R.id.content)

        // Twice, with an idle between: a RecyclerView binds its rows during the *first* layout, and
        // that is when the image requests are issued. Drawing straight after would capture only
        // placeholders.
        repeat(2) {
            root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, width, height)
            ShadowLooper.idleMainLooper()
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        root.draw(Canvas(bitmap))

        File(outputDir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
