package dev.mkaminski.idealista

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.mkaminski.idealista.feature.list.AdListFragment

/**
 * Single activity host. The list screen is attached directly for now; the Navigation graph and the
 * detail destination land in step 5 of docs/PLAN.md.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, AdListFragment())
                .commit()
        }
    }
}
