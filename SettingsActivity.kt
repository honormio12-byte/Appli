package com.sitotv.iptv.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sitotv.iptv.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.tvVersion.text = "SitoTV v1.0.0 — Android TV / Fire Stick"
    }
}
