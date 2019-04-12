package com.suntech.iot.cuttingmc

import android.content.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.widget.Toast
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.base.BaseFragment
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.common.Constants
import com.suntech.iot.cuttingmc.db.DBHelperForComponent
import com.suntech.iot.cuttingmc.db.SimpleDatabaseHelper
import com.suntech.iot.cuttingmc.popup.ActualCountEditActivity
import com.suntech.iot.cuttingmc.popup.PushActivity
import com.suntech.iot.cuttingmc.service.UsbService
import com.suntech.iot.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_side_menu.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : BaseActivity() {

    var countViewType = 1       // Count view 화면값 1=Total count, 2=Component count

    private var _doubleBackToExitPressedOnce = false
    private var _last_count_received_time = DateTime()

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
            if (action.equals(Constants.BR_ADD_COUNT)) {
                handleData("{\"cmd\" : \"count\", \"value\" : 1}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppGlobal.instance.setContext(this)

        var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
        if (item == null) {
            tv_title.setText("No shift")
        } else {
            tv_title.setText(item["shift_name"].toString() + "   " + item["available_stime"].toString() + " - " + item["available_etime"].toString())
        }

        mHandler = MyHandler(this)

        // button click event
        if (AppGlobal.instance.get_long_touch()) {
            btn_home.setOnLongClickListener { changeFragment(0); true }
            btn_push_to_app.setOnLongClickListener { startActivity(Intent(this, PushActivity::class.java));true }
            btn_actual_count_edit.setOnLongClickListener { startActivity(Intent(this, ActualCountEditActivity::class.java)); true }
        } else {
            btn_home.setOnClickListener { changeFragment(0) }
            btn_push_to_app.setOnClickListener { startActivity(Intent(this, PushActivity::class.java)) }
            btn_actual_count_edit.setOnClickListener { startActivity(Intent(this, ActualCountEditActivity::class.java)) }
        }

        // fragment & swipe
        val adapter = TabAdapter(supportFragmentManager)
        adapter.addFragment(HomeFragment(), "")
        adapter.addFragment(CountViewFragment(), "")
        vp_fragments.adapter = adapter
        adapter.notifyDataSetChanged()
        vp_fragments.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(state: Int) {
                (adapter.getItem(state) as BaseFragment).onSelected()
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(position: Int) {}
        })
        start_timer()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    public override fun onResume() {
        super.onResume()

        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(mUsbReceiver, filter)

        startService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        registerReceiver(_broadcastReceiver, IntentFilter(Constants.BR_ADD_COUNT))

        updateView()
        fetchRequiredData()
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(mUsbReceiver)
        unbindService(usbConnection)
        unregisterReceiver(_broadcastReceiver)
    }

    override fun onBackPressed() {
        if (vp_fragments.currentItem != 0) {
            changeFragment(0)
            return
        }
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this._doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        Handler().postDelayed({ _doubleBackToExitPressedOnce = false }, 2000)
    }

    private fun updateView() {
        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false
    }

    fun changeFragment(pos:Int) {
//        if (pos==1) c
//        else tv_title.visibility = View.GONE
        vp_fragments.setCurrentItem(pos, true)
    }

    private fun fetchRequiredData() {
        if (AppGlobal.instance.get_server_ip().trim() != "") {
            fetchWorkData()
            fetchDownTimeType()
            fetchColorData()
        }
    }

    private fun fetchMaualShift(): JSONObject? {
        // manual 데이터가 있으면 가져온다.
        val manual = AppGlobal.instance.get_work_time_manual()
        if (manual != null && manual.length()>0) {
            val available_stime = manual.getString("available_stime") ?: ""
            val available_etime = manual.getString("available_etime") ?: ""
            var planned1_stime = manual.getString("planned1_stime") ?: ""
            var planned1_etime = manual.getString("planned1_etime") ?: ""

            if (available_stime != "" && available_etime != "") {
                if (planned1_stime == "" || planned1_etime == "") {
                    planned1_stime = ""
                    planned1_etime = ""
                }
                var shift3 = JSONObject()
                shift3.put("idx", "0")
//                shift3.put("date", dt.toString("yyyy-MM-dd"))
                shift3.put("available_stime", available_stime)
                shift3.put("available_etime", available_etime)
                shift3.put("planned1_stime", planned1_stime)
                shift3.put("planned1_etime", planned1_etime)
                shift3.put("planned2_stime", "")
                shift3.put("planned2_etime", "")
                shift3.put("planned3_stime", "")
                shift3.put("planned3_etime", "")
                shift3.put("over_time", "0")
//                shift3.put("line_idx", "0")
//                shift3.put("line_name", "")
                shift3.put("shift_idx", "3")
                shift3.put("shift_name", "SHIFT 3")
                return shift3
            }
        }
        return null
    }

    /*
     *  당일 작업시간 가져오기. 새벽이 지난 시간은 1일을 더한다.
     *  전일 작업이 끝나지 않았을수 있기 때문에 전일 데이터도 가져온다.
     */
    private fun fetchWorkData() {
        var dt = DateTime()
        val shift3: JSONObject? = fetchMaualShift()      // manual 데이터가 있으면 가져온다.

        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "work_time",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "date" to dt.toString("yyyy-MM-dd"))
Log.e("params", "" + params)

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list1 = result.getJSONArray("item")
                if (shift3 != null) {
                    var today_shift = shift3
                    if (list1.length()>0) {
                        val item = list1.getJSONObject(0)
                        today_shift.put("date", item["date"])
                        today_shift.put("line_idx", item["line_idx"])
                        today_shift.put("line_name", item["line_name"])
                    } else {
                        today_shift.put("date", dt.toString("yyyy-MM-dd"))
                        today_shift.put("line_idx", "0")
                        today_shift.put("line_name", "Manual")
                    }
                    list1.put(today_shift)
                }
//                Log.e("today work list-1", "" + list1.toString())
                list1 = handleWorkData(list1)
//                Log.e("today work list-2", "" + list1.toString())

                AppGlobal.instance.set_today_work_time(list1)
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })

        // 전날짜 데이터 가져오기
        var prev_params = listOf(
            "code" to "work_time",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "date" to dt.minusDays(1).toString("yyyy-MM-dd"))

        request(this, uri, false, prev_params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list2 = result.getJSONArray("item")
                if (shift3 != null) {
                    var yester_shift = shift3
                    if (list2.length()>0) {
                        val item = list2.getJSONObject(0)
                        yester_shift.put("date", item["date"])
                        yester_shift.put("line_idx", item["line_idx"])
                        yester_shift.put("line_name", item["line_name"])
                    } else {
                        yester_shift.put("date", dt.minusDays(1).toString("yyyy-MM-dd"))
                        yester_shift.put("line_idx", "0")
                        yester_shift.put("line_name", "Manual")
                    }
                    list2.put(yester_shift)
                }
//                Log.e("yester work list-1", "" + list2.toString())
                list2 = handleWorkData(list2)
//                Log.e("yester work list-2", "" + list2.toString())

                AppGlobal.instance.set_prev_work_time(list2)
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    /*
     *  downtime check time
     *  select_yn = 'Y' 것만 가져온다.
     *  etc_yn = 'Y' 이면 second 값, 'N' 이면 name 값이 리턴된다. (1800)
     */
    private fun fetchDownTimeType() {
        val uri = "/getlist1.php"
        var params = listOf("code" to "check_time")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var value = result.getString("value")
                AppGlobal.instance.set_downtime_sec(value)
                val s = value.toInt()
                if (s > 0) {
                }
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    /*
     *  칼라코드 가져오기
     *  color_name = 'yellow'
     *  color_cole = 'FFBC34'
     */
    private fun fetchColorData() {
        val uri = "/getlist1.php"
        var params = listOf("code" to "color")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list = result.getJSONArray("item")
                AppGlobal.instance.set_color_code(list)
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    /*
     *  작업 시간을 검사한다.
     *  첫 작업 시간보다 작은 시간이 보일경우 하루가 지난것이므로 1일을 더한다.
     */
    private fun handleWorkData(list:JSONArray) :JSONArray {
        var shift_stime = DateTime()
        for (i in 0..(list.length() - 1)) {
            var item = list.getJSONObject(i)

            val over_time = item["over_time"]   // 0
            val date = item["date"].toString()  // 2019-04-05
            if (i==0) { // 첫시간 기준
                shift_stime = OEEUtil.parseDateTime(date + " " + item["available_stime"] + ":00")   // 2019-04-05 06:01:00  (available_stime = 06:01)
            }

            var work_stime = OEEUtil.parseDateTime(date + " " + item["available_stime"] + ":00")    // 2019-04-05 06:01:00
            var work_etime = OEEUtil.parseDateTime(date + " " + item["available_etime"] + ":00")    // 2019-04-05 14:00:00
            work_etime = work_etime.plusHours(over_time.toString().toInt())

            val planned1_stime_txt = date + " " + if (item["planned1_stime"] == "") "00:00:00" else item["planned1_stime"].toString() + ":00"   // 2019-04-05 11:30:00
            val planned1_etime_txt = date + " " + if (item["planned1_etime"] == "") "00:00:00" else item["planned1_etime"].toString() + ":00"   // 2019-04-05 13:00:00
            val planned2_stime_txt = date + " " + if (item["planned2_stime"] == "") "00:00:00" else item["planned2_stime"].toString() + ":00"   // 2019-04-05 00:00:00
            val planned2_etime_txt = date + " " + if (item["planned2_etime"] == "") "00:00:00" else item["planned2_etime"].toString() + ":00"   // 2019-04-05 00:00:00

            var planned1_stime_dt = OEEUtil.parseDateTime(planned1_stime_txt)
            var planned1_etime_dt = OEEUtil.parseDateTime(planned1_etime_txt)
            var planned2_stime_dt = OEEUtil.parseDateTime(planned2_stime_txt)
            var planned2_etime_dt = OEEUtil.parseDateTime(planned2_etime_txt)

            // 첫 시작시간 보다 작은 값이면 하루가 지난 날짜임
            // 종료 시간이 시작 시간보다 작은 경우도 하루가 지난 날짜로 처리
            if (shift_stime.secondOfDay > work_stime.secondOfDay) work_stime = work_stime.plusDays(1)
            if (shift_stime.secondOfDay > work_etime.secondOfDay || work_stime.secondOfDay > work_etime.secondOfDay) work_etime = work_etime.plusDays(1)
            if (shift_stime.secondOfDay > planned1_stime_dt.secondOfDay) planned1_stime_dt = planned1_stime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned1_etime_dt.secondOfDay || planned1_stime_dt.secondOfDay > planned1_etime_dt.secondOfDay) planned1_etime_dt = planned1_etime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned2_stime_dt.secondOfDay) planned2_stime_dt = planned2_stime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned2_etime_dt.secondOfDay || planned2_stime_dt.secondOfDay > planned2_etime_dt.secondOfDay) planned2_etime_dt = planned2_etime_dt.plusDays(1)

            item.put("work_stime", work_stime.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("work_etime", work_etime.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned1_stime_dt", planned1_stime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned1_etime_dt", planned1_etime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned2_stime_dt", planned2_stime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned2_etime_dt", planned2_etime_dt.toString("yyyy-MM-dd HH:mm:ss"))
//            Log.e("new list", ""+item.toString())
        }
        return list
    }


    private fun sendPing() {
        tv_ms.text = "-" + " ms"
        if (AppGlobal.instance.get_server_ip() == "") return

        val currentTimeMillisStart = System.currentTimeMillis()
        val uri = "/ping.php"

        request(this, uri, false, false, null, { result ->
            val currentTimeMillisEnd = System.currentTimeMillis()
            val millis = currentTimeMillisEnd - currentTimeMillisStart

            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                btn_server_state.isSelected = true
                AppGlobal.instance._server_state = true
                tv_ms.text = "" + millis + " ms"

                val br_intent = Intent("need.refresh.server.state")
                br_intent.putExtra("state", "Y")
                this.sendBroadcast(br_intent)
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }, {
            btn_server_state.isSelected = false
            val br_intent = Intent("need.refresh.server.state")
            br_intent.putExtra("state", "N")
            this.sendBroadcast(br_intent)
        })
    }

    private fun sendTarget(target:String) {
        if (AppGlobal.instance.get_server_ip()=="") return

        val work_idx = "" + AppGlobal.instance.get_product_idx()
        if (work_idx == "") return

        var db = SimpleDatabaseHelper(this)
        val row = db.get(work_idx)
        val seq = row!!["seq"].toString().toInt()

        val uri = "/targetdata.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "target" to target,
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "seq" to seq)

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun endWork() {

    }

//    private fun updateCurrentWorkTarget() {
//        val idx = AppGlobal.instance.get_design_info_idx()
//        if (idx == "") return
//
//        val work_idx = "" + AppGlobal.instance.get_product_idx()
//        if (work_idx == "") return
//
//        var db = SimpleDatabaseHelper(this)
//        val row = db.get(work_idx)
//        if (row == null) return
//
//        val actual = row["actual"].toString().toInt()
//
//        val elapsedTime = AppGlobal.instance.get_current_product_accumulated_time()
//        val elapsedTime_no_constraint = AppGlobal.instance.get_current_product_accumulated_time(true)
//        val cycle_time = AppGlobal.instance.get_cycle_time()
//        var target = (ceil(elapsedTime.toFloat() / cycle_time.toFloat())).toInt()
//        var target_no_contraint = (ceil(elapsedTime_no_constraint.toFloat() / cycle_time.toFloat())).toInt()
//
//        //Log.e("test", "elapsedTime = " + elapsedTime)
//        //Log.e("test", "cycle_time = " + cycle_time)
//        //Log.e("test", "target = " + target)
//        db.updateWorkTarget(work_idx, target, target_no_contraint)
//
//        if (Constants.DEMO_VERSION) {
//            val current_shift_time = AppGlobal.instance.get_current_shift_time()
//            var work_stime = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))
//            var work_etime = OEEUtil.parseDateTime(current_shift_time?.getString("work_etime"))
//
//            var start_dt = OEEUtil.parseDateTime(row["start_dt"].toString())
//            var end_dt = work_etime
//            val list = db.gets()
//
//            if (list == null || list.size <= 1) start_dt = work_stime
//            val t = AppGlobal.instance.compute_work_time(start_dt, end_dt, false, false)
//
//            target = ( t / cycle_time )
//        }
//        // actual 이 0이면 서버로 보내지 않음
//        if (actual > 0) sendTarget(target.toString())
//    }

    /////// 쓰레드
//    private val _downtime_timer = Timer()
    private val _timer_task1 = Timer()          // 서버 접속 체크 ping test.
    private val _timer_task2 = Timer()          // 작업시간, 디자인, 다운타입, 칼라 Data 가져오기 (workdata, designdata, downtimetype, color)

    private fun start_timer() {

//        val downtime_task = object : TimerTask() {
//            override fun run() {
//                runOnUiThread {
//                    checkDownTime()
//                    checkExit()
//                }
//            }
//        }
//        _downtime_timer.schedule(downtime_task, 500, 1000)

        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendPing()
//                    updateCurrentWorkTarget()
                }
            }
        }
        _timer_task1.schedule(task1, 2000, 10000)

        val task2 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    fetchRequiredData()
                }
            }
        }
        _timer_task2.schedule(task2, 600000, 600000)
    }
    private fun cancel_timer () {
//        _downtime_timer.cancel()
        _timer_task1.cancel()
        _timer_task2.cancel()
    }

    ////////// USB
    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED // USB PERMISSION GRANTED
                -> Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED // USB PERMISSION NOT GRANTED
                -> Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_NO_USB // NO USB CONNECTED
                -> Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_DISCONNECTED // USB DISCONNECTED
                -> Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show()
                UsbService.ACTION_USB_NOT_SUPPORTED // USB NOT SUPPORTED
                -> Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var usbService: UsbService? = null
    private var mHandler: MyHandler? = null

    private val usbConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            usbService = (arg1 as UsbService.UsbBinder).service
            usbService!!.setHandler(mHandler)
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }
    private fun startService(service: Class<*>, serviceConnection: ServiceConnection, extras: Bundle?) {
        if (!UsbService.SERVICE_CONNECTED) {
            val startService = Intent(this, service)
            if (extras != null && !extras.isEmpty) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    startService.putExtra(key, extra)
                }
            }
            startService(startService)
        }
        val bindingIntent = Intent(this, service)
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    private class MyHandler(activity: MainActivity) : Handler() {
        private val mActivity: WeakReference<MainActivity>
        init {
            mActivity = WeakReference(activity)
        }
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                    val data = msg.obj as String
                    mActivity.get()?.handleData(data)
                }
                UsbService.CTS_CHANGE -> Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show()
                UsbService.DSR_CHANGE -> Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show()
            }
        }
    }

    private var recvBuffer = ""
    fun handleData (data:String) {
        if (data.indexOf("{") >= 0)  recvBuffer = ""

        recvBuffer += data

        val pos_end = recvBuffer.indexOf("}")
        if (pos_end < 0) return

        if (isJSONValid(recvBuffer)) {

            val parser = JsonParser()
            val element = parser.parse(recvBuffer)
            val cmd = element.asJsonObject.get("cmd").asString
            val value = element.asJsonObject.get("value")

            Toast.makeText(this, element.toString(), Toast.LENGTH_SHORT).show()
            Log.w("test", "usb = " + recvBuffer)

            saveRowData(cmd, value)
        } else {
            Log.e("test", "usb parsing error! = " + recvBuffer)
        }
    }
    private fun isJSONValid(test: String): Boolean {
        try {
            JSONObject(test)
        } catch (ex: JSONException) {
            try {
                JSONArray(test)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }
    private fun saveRowData(cmd:String, value: JsonElement) {
        var db = DBHelperForComponent(this)
//        var db = SimpleDatabaseHelper(this)

        if (cmd=="count") {

            if (AppGlobal.instance.get_sound_at_count()) AppGlobal.instance.playSound(this)

            val layer = AppGlobal.instance.get_compo_layer()

            if (layer == "") {
                Toast.makeText(this, getString(R.string.msg_layer_not_selected), Toast.LENGTH_SHORT).show()
                return
            }

            val layer_value = AppGlobal.instance.get_layer_pairs(layer)

            if (layer_value == "" || layer_value == "") {
                Toast.makeText(this, getString(R.string.msg_layer_not_selected), Toast.LENGTH_SHORT).show()
                return
            }

            var inc_count = 1

            if (layer_value == "0.5") {
                val accumulated_count = AppGlobal.instance.get_accumulated_count() + 1
                if (accumulated_count <= 1) {
                    AppGlobal.instance.set_accumulated_count(1)
                    return
                } else {
                    AppGlobal.instance.set_accumulated_count(0)
                }
                inc_count = 1
            } else {
                inc_count = layer_value.toInt()
            }

            // total count
            var cnt = AppGlobal.instance.get_current_shift_actual_cnt() + inc_count
            AppGlobal.instance.set_current_shift_actual_cnt(cnt)

            // component total count
            val work_idx = AppGlobal.instance.get_work_idx()
            if (work_idx == "") return

            val row = db.get(work_idx)
            if (row != null) {
                val actual = (row!!["actual"].toString().toInt() + inc_count)
                db.updateWorkActual(work_idx, actual)
            }


            _last_count_received_time = DateTime()

            sendCountData(value.toString())

//            _stitch_db.add(work_idx, value.toString())
        }
    }

    fun startComponent(wosno:String, styleno:String, model:String, size:String, target:String, actual:String) {

        var db = DBHelperForComponent(this)

        val work_info = AppGlobal.instance.get_current_shift_time()
        val shift_idx = work_info?.getString("shift_idx") ?: ""
        val shift_name = work_info?.getString("shift_name") ?: ""

        val row = db.get(wosno, size)

        if (row == null) {
            db.add(wosno, shift_idx, shift_name, styleno, model, size, target.toInt(), 0)
            val row2 = db.get(wosno, size)
            if (row2 == null) {
                Log.e("work_idx", "none")
                AppGlobal.instance.set_work_idx("")
            } else {
                AppGlobal.instance.set_work_idx(row2["work_idx"].toString())
                Log.e("work_idx", row2["work_idx"].toString())
            }
        } else {
            AppGlobal.instance.set_work_idx(row["work_idx"].toString())
            Log.e("work_idx", row["work_idx"].toString())
        }
        val br_intent = Intent("need.refresh")
        this.sendBroadcast(br_intent)
    }

    fun startNewProduct(didx:String, piece_info:Int, cycle_time:Int, model:String, article:String, material_way:String, component:String) {

    }

    private fun sendCountData(count:String) {

    }

    private class TabAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        override fun getCount(): Int { return mFragments.size }
        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }
        override fun getItem(position: Int): Fragment {
            return mFragments.get(position)
        }
        override fun getItemPosition(`object`: Any?): Int {
            return PagerAdapter.POSITION_NONE
        }
        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitles.get(position)
        }
    }
}