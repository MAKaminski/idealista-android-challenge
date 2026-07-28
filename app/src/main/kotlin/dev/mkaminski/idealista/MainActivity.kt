package dev.mkaminski.idealista

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.feature.detail.AdDetailFragment
import dev.mkaminski.idealista.feature.favorites.FavoritesFragment
import dev.mkaminski.idealista.feature.list.AdListFragment

/**
 * Single-activity host. Screen-to-screen wiring lives here so no feature module depends on another
 * (ADR-0002): the list hands back a propertyCode, the activity decides what to open with it.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        bottomNav = findViewById(R.id.bottomNav)
        // Edge-to-edge draws behind the system bars, so the gesture bar's space has to be given
        // back explicitly — otherwise the last tab row sits under it.
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = bars.bottom)
            windowInsets
        }
        // AGP 9 makes R fields non-final, so menu ids cannot be `when` branches (ADR-0001).
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_favorites) {
                showRoot(FavoritesFragment(), TAG_FAVORITES)
            } else {
                showRoot(AdListFragment().withNavigation(), TAG_LIST)
            }
            true
        }

        if (savedInstanceState == null) {
            showRoot(AdListFragment().withNavigation(), TAG_LIST)
        } else {
            reattachCallbacks()
        }

        supportFragmentManager.addOnBackStackChangedListener { updateChromeForBackStack() }

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

    /** Root destinations replace each other; only detail goes on the back stack. */
    private fun showRoot(fragment: Fragment, tag: String) {
        supportFragmentManager.popBackStack(TAG_DETAIL, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, fragment, tag)
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

    /** The detail screen is full-bleed; the tab bar would fight its collapsing toolbar. */
    private fun updateChromeForBackStack() {
        val onDetail = supportFragmentManager.backStackEntryCount > 0
        bottomNav.visibility = if (onDetail) View.GONE else View.VISIBLE
    }

    /** Fragments survive rotation, so their navigation callbacks must be re-attached. */
    private fun reattachCallbacks() {
        (supportFragmentManager.findFragmentByTag(TAG_LIST) as? AdListFragment)?.withNavigation()
        (supportFragmentManager.findFragmentByTag(TAG_DETAIL) as? AdDetailFragment)?.withNavigation()
        updateChromeForBackStack()
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
        const val TAG_FAVORITES = "favorites"
    }
}
