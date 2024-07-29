package com.example.ideathonpoc.ui.screens

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ideathonpoc.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadFragment("HomeFragment")
        bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)!!
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    loadFragment("HomeFragment")
                    true
                }
                R.id.settings -> {
                    loadFragment("AboutFragment")
                    true
                }
                else -> false
            }
        }

    }

    private fun loadFragment(fragmentTag: String) {
        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
            ?: when (fragmentTag) {
                "HomeFragment" -> HomeFragment()
                "AboutFragment" -> AboutFragment()
                else -> throw IllegalArgumentException("Unknown fragment tag: $fragmentTag")
            }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment, fragmentTag)
            .addToBackStack(null)  // Add to back stack for proper back navigation
            .commit()
    }
}

