package dev.mkaminski.idealista

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.feature.detail.AdDetailFragment
import dev.mkaminski.idealista.feature.list.AdListFragment

/**
 * Single-activity host. Screen-to-screen wiring lives here so no feature module depends on another
 * (ADR-0002): the list hands back a propertyCode, the activity decides what to open with it.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            showList()
        } else {
            reattachCallbacks()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }

    private fun showList() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, AdListFragment().withNavigation(), TAG_LIST)
            .commit()
    }

    private fun openDetail(propertyCode: String) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.mainContainer,
                AdDetailFragment.newInstance(propertyCode).withNavigation(),
                TAG_DETAIL,
            )
            .addToBackStack(TAG_DETAIL)
            .commit()
    }

    /** Fragments survive rotation, so their navigation callbacks must be re-attached. */
    private fun reattachCallbacks() {
        (supportFragmentManager.findFragmentByTag(TAG_LIST) as? AdListFragment)?.withNavigation()
        (supportFragmentManager.findFragmentByTag(TAG_DETAIL) as? AdDetailFragment)?.withNavigation()
    }

    private fun AdListFragment.withNavigation() = apply {
        onAdSelected = { propertyCode -> openDetail(propertyCode) }
    }

    private fun AdDetailFragment.withNavigation() = apply {
        onNavigateUp = { onBackPressedDispatcher.onBackPressed() }
    }

    private companion object {
        const val TAG_LIST = "list"
        const val TAG_DETAIL = "detail"
    }
}
