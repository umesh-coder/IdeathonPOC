package com.example.ideathonpoc.ui.screens

import android.os.Bundle
import android.content.Intent
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ideathonpoc.R
import com.example.ideathonpoc.ui.adapters.OnboardingAdapter

class OnboardingTutorial : AppCompatActivity() {
    private lateinit var mSLideViewPager: ViewPager
    private lateinit var mDotLayout: LinearLayout
    private lateinit var backbtn: Button
    private lateinit var nextbtn: Button
    private lateinit var skipbtn: Button

    private lateinit var dots: Array<TextView?>
    private lateinit var viewPagerAdapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(200)
        installSplashScreen()
        setContentView(R.layout.activity_onboarding_tutorial)

        backbtn = findViewById(R.id.backbtn)
        nextbtn = findViewById(R.id.nextbtn)
        skipbtn = findViewById(R.id.skipButton)

        backbtn.setOnClickListener {
            if (getitem(0) > 0) {
                mSLideViewPager.currentItem = getitem(-1)
            }
        }

        nextbtn.setOnClickListener {
            if (getitem(0) < 3) {
                mSLideViewPager.currentItem = getitem(1)
            } else {
                val i = Intent(this@OnboardingTutorial, MainActivity::class.java)
                startActivity(i)
                finish()
            }
        }

        skipbtn.setOnClickListener {
            val i = Intent(this@OnboardingTutorial, MainActivity::class.java)
            startActivity(i)
            finish()
        }

        mSLideViewPager = findViewById(R.id.slideViewPager)
        mDotLayout = findViewById(R.id.indicator_layout)

        viewPagerAdapter = OnboardingAdapter(this)
        mSLideViewPager.adapter = viewPagerAdapter

        setUpindicator(0)
        mSLideViewPager.addOnPageChangeListener(viewListener)

    }

    private fun setUpindicator(position: Int) {
        dots = arrayOfNulls(4)
        mDotLayout.removeAllViews()

        for (i in dots.indices) {
            dots[i] = TextView(this).apply {
                text = Html.fromHtml("&#8226", Html.FROM_HTML_MODE_LEGACY)
                textSize = 35f
                setTextColor(resources.getColor(R.color.inactive, applicationContext.theme))
            }
            mDotLayout.addView(dots[i])
        }

        dots[position]?.setTextColor(resources.getColor(R.color.active, applicationContext.theme))
    }

    private val viewListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

        override fun onPageSelected(position: Int) {
            setUpindicator(position)

            backbtn.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    private fun getitem(i: Int): Int {
        return mSLideViewPager.currentItem + i
    }
}




