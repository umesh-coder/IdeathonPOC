package com.example.ideathonpoc.ui.screens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ideathonpoc.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var bottomNav : BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        loadFragment("HomeFragment")
        bottomNav = findViewById(R.id.bottomNav) as BottomNavigationView
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    loadFragment("HomeFragment")
                    true
                }
                R.id.message -> {
                    loadFragment("ResultFragment")
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
                "ResultFragment" -> ResultFragment()
                "AboutFragment" -> AboutFragment()
                else -> throw IllegalArgumentException("Unknown fragment tag: $fragmentTag")
            }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment, fragmentTag)
            .addToBackStack(null)  // Add to back stack for proper back navigation
            .commit()
    }
}

