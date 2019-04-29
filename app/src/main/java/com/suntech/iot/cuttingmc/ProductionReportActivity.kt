package com.suntech.iot.cuttingmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.activity_production_report.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.joda.time.DateTime
import java.util.*

class ProductionReportActivity : BaseActivity() {

    var _current_time = DateTime()

    private val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)){
                    btn_wifi_state.isSelected = true
                } else {
                    btn_wifi_state.isSelected = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_production_report)
        initView()
        start_timer()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        updateView()
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun updateView() {
        if (AppGlobal.instance._server_state) btn_server_state.isSelected = true
        else btn_server_state.isSelected = false

        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false
    }

    private fun initView() {
        tv_title.text = "PRODUCTION REPORT"

        tv_current_date.text = _current_time.toString("yyyy-MM-dd")

        ib_arrow_l.setOnClickListener {
            _current_time = _current_time.plusDays(-1)
            tv_current_date.text = _current_time.toString("yyyy-MM-dd")
            updateView()
        }
        ib_arrow_r.setOnClickListener {
            _current_time = _current_time.plusDays(+1)
            tv_current_date.text = _current_time.toString("yyyy-MM-dd")
            updateView()
        }
        btn_production_report_exit.setOnClickListener { finish() }
    }

    private fun sendPing() {
        if (AppGlobal.instance.get_server_ip() == "") return
        val uri = "/ping.php"
        request(this, uri, false, false, null, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                btn_server_state.isSelected = true
                AppGlobal.instance._server_state = true
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }, {
            btn_server_state.isSelected = false
        })
    }

    /////// 쓰레드
    private val _timer_task1 = Timer()          // 서버 접속 체크 ping test.

    private fun start_timer() {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendPing()
                }
            }
        }
        _timer_task1.schedule(task1, 5000, 10000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
    }
}