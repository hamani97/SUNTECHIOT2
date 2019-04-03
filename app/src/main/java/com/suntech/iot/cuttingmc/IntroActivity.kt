package com.suntech.iot.cuttingmc

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal

class IntroActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        AppGlobal.instance.setContext(this)

        Log.e("settings", "Server IP = " + AppGlobal.instance.get_server_ip())
        Log.e("settings", "Mac addr = " + AppGlobal.instance.get_mac_address())
        Log.e("settings", "IP addr " + AppGlobal.instance.get_local_ip())
        Log.e("settings", "factory = " + AppGlobal.instance.get_factory())
        Log.e("settings", "room = " + AppGlobal.instance.get_room())
        Log.e("settings", "line = " + AppGlobal.instance.get_line())

        Handler().postDelayed({
            moveToNext()
        }, 600)
    }

    private fun moveToNext() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}