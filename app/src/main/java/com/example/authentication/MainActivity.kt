package com.example.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.authentication.databinding.ActivityMainBinding
import com.example.authentication.ui.history.HistoryFragment
import com.example.authentication.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.contentContainer.id, HomeFragment.newInstance())
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.contentContainer.id, HomeFragment.newInstance())
                        .commit()
                    true
                }
                R.id.nav_history -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.contentContainer.id, HistoryFragment.newInstance())
                        .commit()
                    true
                }
                else -> false
            }
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddMedicationActivity::class.java))
        }
    }
}
