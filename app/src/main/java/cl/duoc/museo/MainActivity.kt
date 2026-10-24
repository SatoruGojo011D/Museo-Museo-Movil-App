package cl.duoc.museo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import cl.duoc.museo.data.model.Role
import cl.duoc.museo.databinding.ActivityMainBinding
import cl.duoc.museo.utils.SessionManager
import cl.duoc.museo.utils.ViewAnimations

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        // Show BottomNavigationView only on top-level destinations
        val topLevelDestinations = setOf(
            R.id.dashboardFragment,
            R.id.mapFragment,
            R.id.catalogFragment,
            R.id.notificationsFragment,
            R.id.profileFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateNavigationForRole()
            ViewAnimations.installTapFeedback(binding.root)
            ViewAnimations.animateScreenEntry(binding.navHostFragment)
            if (topLevelDestinations.contains(destination.id)) {
                binding.bottomNavigationView.visibility = View.VISIBLE
            } else {
                binding.bottomNavigationView.visibility = View.GONE
            }
        }

    }

    private fun updateNavigationForRole() {
        val role = SessionManager(this).getUserRole()
        binding.bottomNavigationView.menu
            .findItem(R.id.catalogFragment)
            .isVisible = role != Role.RESEARCHER
    }
}