package com.example.oauthlite

import android.app.Activity
import android.content.Intent
import android.os.Bundle

internal class RedirectReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forward = Intent(this, AuthManagementActivity::class.java).apply {
            data = intent.data
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(forward)
        finish()
    }
}
