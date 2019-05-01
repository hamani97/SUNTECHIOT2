package com.suntech.iot.cuttingmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.DBHelperForTarget
import com.suntech.iot.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.activity_production_report.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.joda.time.DateTime
import java.util.*

class ProductionReportActivity : BaseActivity() {

    var _current_time = DateTime()

    var _target_db = DBHelperForTarget(this)

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private var list_adapter2: ListAdapter? = null
    private var _list2: ArrayList<HashMap<String, String>> = arrayListOf()

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

    private fun onlineCheck() {
        if (AppGlobal.instance._server_state) btn_server_state.isSelected = true
        else btn_server_state.isSelected = false

        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false
    }

    private fun outputBlank() {
        _list.removeAll(_list)
        _list2.removeAll(_list2)

        val time_row = hashMapOf(
            "type" to "HEAD",
            "name" to "SHIFT 1",
            "target" to "Target : 0",
            "actual" to "",
            "accumulate" to "",
            "rate" to ""
        )
        _list.add(time_row)

        val time_row2 = hashMapOf(
            "type" to "HEAD",
            "name" to "SHIFT 2",
            "target" to "Target : 0",
            "actual" to "",
            "accumulate" to "",
            "rate" to ""
        )
        _list2.add(time_row2)

        val time_row3 = hashMapOf(
            "type" to "DATA",
            "name" to "",
            "target" to "0",
            "actual" to "",
            "accumulate" to "",
            "rate" to ""
        )
        _list.add(time_row3)
        _list2.add(time_row3)

        list_adapter = ListAdapter(this, _list)
        lv_reports.adapter = list_adapter

        list_adapter2 = ListAdapter(this, _list2)
        lv_reports2.adapter = list_adapter2
    }

    private fun updateView() {
        onlineCheck()

        var current_dt = _current_time.toString("yyyy-MM-dd")
        var current_tommorow_dt = _current_time.plusDays(1).toString("yyyy-MM-dd")

        val target_data = _target_db.gets(current_dt)

        if ((target_data?.size ?: 0) == 0) {
            outputBlank()
            return
        }

        _list.removeAll(_list)
        _list2.removeAll(_list2)

        var index = 0

        for (i in 0..((target_data?.size ?: 0)-1)) {
            val item = target_data?.get(i)

            if (item != null) {
                val target_txt = item.get("target").toString()

                val time_row = hashMapOf(
                    "type" to "HEAD",
                    "name" to item.get("shift_name").toString(),
                    "target" to "Target : " + target_txt,
                    "actual" to "",
                    "accumulate" to "",
                    "rate" to ""
                )

                if (index < 11) {
                    _list.add(time_row)
                } else {
                    _list2.add(time_row)
                }
                index += 2

                val now_millis = DateTime().millis

                val work_etime = item.get("work_etime").toString()
                val finish_millis = OEEUtil.parseDateTime(work_etime).millis

                val work_stime = item.get("work_stime").toString()
                var work_time_dt = OEEUtil.parseDateTime(work_stime)    // 2019-04-05 06:01:00

                val target = item.get("target").toString().toInt()
                var accumulate = 0

                var blank_yn = false

                for (j in 0..23) {
                    if (work_time_dt.millis >= finish_millis) break
                    if (now_millis < work_time_dt.millis) blank_yn = true

                    val stime = work_time_dt.toString("HH")
                    work_time_dt = work_time_dt.plusHours(1)
                    val etime = work_time_dt.toString("HH")

                    val actual = j * 8
                    accumulate += actual
                    val rate = if (target != 0) (accumulate.toFloat() / target.toFloat() * 100).toInt().toString() + "%" else ""

                    // 아직 해당 시간이 안됐으면 공백으로 표시
                    val time_row = hashMapOf(
                        "type" to "DATA",
                        "name" to stime + " - " + etime,
                        "target" to target_txt,
                        "actual" to if (blank_yn) "" else actual.toString(),
                        "accumulate" to if (blank_yn) "" else accumulate.toString(),
                        "rate" to if (blank_yn) "" else rate
                    )

                    if (index < 12) {
                        _list.add(time_row)
                    } else {
                        _list2.add(time_row)
                    }
                    index++
                }
            }
        }

        list_adapter = ListAdapter(this, _list)
        lv_reports.adapter = list_adapter

        list_adapter2 = ListAdapter(this, _list2)
        lv_reports2.adapter = list_adapter2
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

    class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_item_report, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            if (_list[position]["type"]=="HEAD") {
                vh.ll_report_head.visibility = View.VISIBLE
                vh.ll_report_data.visibility = View.GONE

                vh.tv_shift_name.text = _list[position]["name"]
                vh.tv_shift_target.text = _list[position]["target"]

            } else {
                vh.ll_report_head.visibility = View.GONE
                vh.ll_report_data.visibility = View.VISIBLE

                vh.tv_report_item_time.text = _list[position]["name"]
                vh.tv_report_item_target.text = _list[position]["actual"]
                vh.tv_report_item_product.text = _list[position]["accumulate"]
                vh.tv_report_item_rate.text = _list[position]["rate"]
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val ll_report_head: LinearLayout
            val ll_report_data: LinearLayout
            val tv_shift_name: TextView
            val tv_shift_target: TextView
            val tv_report_item_time: TextView
            val tv_report_item_target: TextView
            val tv_report_item_product: TextView
            val tv_report_item_rate: TextView

            init {
                this.ll_report_head = row?.findViewById<LinearLayout>(R.id.ll_report_head) as LinearLayout
                this.ll_report_data = row?.findViewById<LinearLayout>(R.id.ll_report_data) as LinearLayout
                this.tv_shift_name = row?.findViewById<TextView>(R.id.tv_shift_name) as TextView
                this.tv_shift_target = row?.findViewById<TextView>(R.id.tv_shift_target) as TextView
                this.tv_report_item_time = row?.findViewById<TextView>(R.id.tv_report_item_time) as TextView
                this.tv_report_item_target = row?.findViewById<TextView>(R.id.tv_report_item_target) as TextView
                this.tv_report_item_product = row?.findViewById<TextView>(R.id.tv_report_item_product) as TextView
                this.tv_report_item_rate = row?.findViewById<TextView>(R.id.tv_report_item_rate) as TextView
            }
        }
    }
}