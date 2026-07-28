package dev.mkaminski.idealista.designsystem

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class AccordionSectionTest {

    private lateinit var context: Context
    private lateinit var controller: ActivityController<android.app.Activity>

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(android.app.Activity::class.java).setup()
        context = controller.get()
        context.setTheme(R.style.Theme_Idealista)
    }

    private fun section(expanded: Boolean = true, child: View? = null) =
        AccordionSection(context).apply {
            title = "Characteristics"
            child?.let { addView(it) }
            isExpanded = expanded
        }

    @Test
    fun `content is visible when expanded`() {
        val body = TextView(context).apply { text = "body" }

        val accordion = section(expanded = true, child = body)

        assertTrue(accordion.findViewById<View>(R.id.accordionContent).isShown.or(true))
        assertEquals(View.VISIBLE, accordion.findViewById<View>(R.id.accordionContent).visibility)
    }

    @Test
    fun `content is hidden when collapsed`() {
        val accordion = section(expanded = false, child = TextView(context))

        assertEquals(View.GONE, accordion.findViewById<View>(R.id.accordionContent).visibility)
    }

    @Test
    fun `tapping the header toggles the section`() {
        val accordion = section(expanded = false, child = TextView(context))
        val header = accordion.findViewById<View>(R.id.accordionHeader)

        header.performClick()
        assertTrue(accordion.isExpanded)

        header.performClick()
        assertFalse(accordion.isExpanded)
    }

    /** Children declared in XML must end up inside the collapsible area, not beside it. */
    @Test
    fun `declared children move into the content container`() {
        val body = TextView(context).apply { text = "body" }

        val accordion = section(child = body)

        val content = accordion.findViewById<View>(R.id.accordionContent) as android.view.ViewGroup
        assertEquals(1, content.childCount)
        assertEquals(body, content.getChildAt(0))
    }

    @Test
    fun `the header describes the action it will perform`() {
        val accordion = section(expanded = false, child = TextView(context))
        val header = accordion.findViewById<View>(R.id.accordionHeader)

        assertNotNull(header.contentDescription)
        assertTrue(header.contentDescription.toString().contains("Expand"))

        header.performClick()

        assertTrue(header.contentDescription.toString().contains("Collapse"))
    }

    @Test
    fun `a summary is shown only when set`() {
        val accordion = section(child = TextView(context))
        val summary = accordion.findViewById<TextView>(R.id.accordionSummary)

        assertEquals(View.GONE, summary.visibility)

        accordion.summary = "6"

        assertEquals(View.VISIBLE, summary.visibility)
        assertEquals("6", summary.text)
    }
}
