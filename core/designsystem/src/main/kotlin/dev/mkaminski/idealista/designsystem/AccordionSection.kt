package dev.mkaminski.idealista.designsystem

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager

/**
 * A collapsible section: a tappable header with a title, an optional summary and a chevron, over a
 * content area that holds whatever children the layout declares.
 *
 * Long screens read as a wall of text otherwise. Sections collapse so the parts a reader does not
 * care about take one line each, and the expanded/collapsed choice survives rotation.
 *
 * ```xml
 * <dev.mkaminski.idealista.designsystem.AccordionSection
 *     app:accordionTitle="@string/detail_description"
 *     app:accordionExpanded="false">
 *     <TextView … />
 * </dev.mkaminski.idealista.designsystem.AccordionSection>
 * ```
 */
class AccordionSection @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val header: LinearLayout
    private val titleView: TextView
    private val summaryView: TextView
    private val chevron: ImageView
    private val content: LinearLayout

    /** False until the template is in place, so the template's own views are not re-routed. */
    private var templateReady = false

    var title: CharSequence?
        get() = titleView.text
        set(value) {
            titleView.text = value
            updateAccessibility()
        }

    /** Shown greyed next to the title — a count or a one-word hint of what is inside. */
    var summary: CharSequence?
        get() = summaryView.text
        set(value) {
            summaryView.text = value
            summaryView.isVisible = !value.isNullOrBlank()
        }

    var isExpanded: Boolean = true
        set(value) {
            field = value
            content.isVisible = value
            chevron.rotation = if (value) 180f else 0f
            updateAccessibility()
        }

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_accordion_section, this, true)
        header = findViewById(R.id.accordionHeader)
        titleView = findViewById(R.id.accordionTitle)
        summaryView = findViewById(R.id.accordionSummary)
        chevron = findViewById(R.id.accordionChevron)
        content = findViewById(R.id.accordionContent)
        templateReady = true

        context.obtainStyledAttributes(attrs, R.styleable.AccordionSection).use { typed ->
            title = typed.getString(R.styleable.AccordionSection_accordionTitle)
            isExpanded = typed.getBoolean(R.styleable.AccordionSection_accordionExpanded, true)
        }
        summary = null

        header.setOnClickListener { toggle() }
    }

    /**
     * Anything added after the template — whether declared in XML or added in code — belongs inside
     * the collapsible area, not beside the header.
     */
    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (templateReady) {
            content.addView(child, index, params)
        } else {
            super.addView(child, index, params)
        }
    }

    fun toggle() {
        TransitionManager.beginDelayedTransition(
            parent as? ViewGroup ?: this,
            AutoTransition().apply { duration = 180 },
        )
        isExpanded = !isExpanded
    }

    private fun updateAccessibility() {
        val name = titleView.text ?: return
        header.contentDescription = context.getString(
            if (isExpanded) R.string.accordion_collapse else R.string.accordion_expand,
            name,
        )
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable(STATE_SUPER, super.onSaveInstanceState())
        putBoolean(STATE_EXPANDED, isExpanded)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            isExpanded = state.getBoolean(STATE_EXPANDED, true)
            @Suppress("DEPRECATION")
            super.onRestoreInstanceState(state.getParcelable(STATE_SUPER))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /** Child views share this view's id, so let them keep their own saved state. */
    override fun dispatchSaveInstanceState(container: android.util.SparseArray<Parcelable>) =
        dispatchFreezeSelfOnly(container)

    override fun dispatchRestoreInstanceState(container: android.util.SparseArray<Parcelable>) =
        dispatchThawSelfOnly(container)

    private companion object {
        const val STATE_SUPER = "super"
        const val STATE_EXPANDED = "expanded"
    }
}

private inline fun <T> android.content.res.TypedArray.use(block: (android.content.res.TypedArray) -> T): T =
    try {
        block(this)
    } finally {
        recycle()
    }
