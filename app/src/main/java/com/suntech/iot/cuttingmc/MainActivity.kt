package com.suntech.iot.cuttingmc

import android.content.*
import android.graphics.Color
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
import com.google.gson.JsonSyntaxException
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.base.BaseFragment
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.common.Constants
import com.suntech.iot.cuttingmc.db.*
import com.suntech.iot.cuttingmc.popup.*
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

    var _availability_rate = 0F
    var _quality_rate = 0F
    var _performance_rate = 0F
    var _oee_rate = 0F

//    val _stitch_db = DBHelperForCount(this)     // Count 정보
    val _target_db = DBHelperForTarget(this)    // 날짜의 Shift별 정보, Target 수량 정보 저장
    val _report_db = DBHelperForReport(this)    // 날짜의 Shift별 한시간 간격의 Actual 수량 저장

    private var _doubleBackToExitPressedOnce = false
    private var _last_count_received_time = DateTime()

    var _is_call = false

    var watching_count = 0      // 디버깅 창용 변수

    // 부팅시 자동실행
//    companion object {
//        class BootReceiver : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                if ("android.intent.action.BOOT_COMPLETED".equals(intent.action)) {
//                    var it = Intent(context, IntroActivity::class.java)
//                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    context.startService(it)
//                }
//            }
//        }
//    }

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

        // USB state
        btn_usb_state?.isSelected = false

        // 시작시 work_idx 값이 없으면 초기화 한다.
        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            endTodayWork()
        }

        mHandler = MyHandler(this)

//        AppGlobal.instance.set_last_received("")

        // button click event
        if (AppGlobal.instance.get_long_touch()) {
            btn_home.setOnLongClickListener { changeFragment(0); true }
            btn_push_to_app.setOnLongClickListener { startActivity(Intent(this, PushActivity::class.java)); true }
            btn_actual_count_edit.setOnLongClickListener { startActivity(Intent(this, ActualCountEditActivity::class.java)); true }
            btn_downtime.setOnLongClickListener { startDowntimeActivity(); true }
//            btn_downtime.setOnLongClickListener { Toast.makeText(this, "Not yet supported.", Toast.LENGTH_SHORT).show(); true }
//            btn_defective_info.setOnLongClickListener { startActivity(Intent(this, DefectiveActivity::class.java)); true }
            btn_production_report.setOnLongClickListener { startActivity(Intent(this, ProductionReportActivity::class.java)); true }
        } else {
            btn_home.setOnClickListener { changeFragment(0) }
            btn_push_to_app.setOnClickListener { startActivity(Intent(this, PushActivity::class.java)) }
            btn_actual_count_edit.setOnClickListener { startActivity(Intent(this, ActualCountEditActivity::class.java)) }
            btn_downtime.setOnClickListener { startDowntimeActivity() }
//            btn_downtime.setOnClickListener { Toast.makeText(this, "Not yet supported.", Toast.LENGTH_SHORT).show() }
//            btn_defective_info.setOnClickListener { startActivity(Intent(this, DefectiveActivity::class.java)) }
            btn_production_report.setOnClickListener { startActivity(Intent(this, ProductionReportActivity::class.java)) }
        }

        // 디버깅용 창
        top_logo.setOnClickListener {
            if (watching_count >= 4) {
                watching_count = 0
                startActivity(Intent(this, WatchingActivity::class.java))
            }
            watching_count++
//            Log.e("watching", "" + watching_count)
            Handler().postDelayed({ watching_count = 0 }, 2000)
        }

        // fragment & swipe
        val adapter = TabAdapter(supportFragmentManager)
        adapter.addFragment(HomeFragment(), "")
        adapter.addFragment(CountViewFragment(), "")
        vp_fragments.adapter = adapter
        adapter.notifyDataSetChanged()

        vp_fragments.setPagingEnabled(false)
        vp_fragments.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(state: Int) {
                (adapter.getItem(state) as BaseFragment).onSelected()
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(position: Int) {}
        })

        // 지난 DownTime이 있으면 삭제한다.
        RemoveDownTimeData()

        // 30분마다 실행되기 때문에 처음 앱 실행시 한번 실행해 준다.
        updateCurrentWorkTarget()

//        val db = DBHelperForDownTime(this)
//        db.delete()
//        AppGlobal.instance.set_last_received("")

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

        // Actual 값을 좌측에 표시
        tv_report_count?.text = "" + AppGlobal.instance.get_current_shift_actual_cnt()

        // USB state
        btn_usb_state?.isSelected = AppGlobal.instance._usb_state

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
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
        Handler().postDelayed({ _doubleBackToExitPressedOnce = false }, 2000)
    }

    private fun updateView() {
        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false
    }

    fun changeFragment(pos:Int) {
        vp_fragments.setCurrentItem(pos, true)
    }

    // 시작시 호출
    // 이후 10분에 한번씩 호출
    // 서버에 작업시간, 다운타임 기본시간, 색상값을 호출
    private fun fetchRequiredData() {
        if (AppGlobal.instance.get_server_ip().trim() != "") {
            fetchWorkData2()         // 작업시간
            fetchServerTarget()     // 목표수량
            fetchDownTimeType()
            fetchColorData()
        }
    }

    private fun fetchManualShift(): JSONObject? {
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
     *  당일 작업 Shift 별 목표수량 가져오기
     */
    private fun fetchServerTarget() {
        val today = DateTime().toString("yyyy-MM-dd")
        val mac = AppGlobal.instance.getMACAddress()
        val uri = "/getlist1.php"
        var params = listOf("code" to "target",
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  "1",
            "date" to today,
            "mac_addr" to mac
        )
        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                val daytargetsum = result.getString("daytargetsum")
                AppGlobal.instance.set_target_server_shift("1", daytargetsum)
            } else {
                Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            }
        })
        params = listOf("code" to "target",
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  "2",
            "date" to today,
            "mac_addr" to mac
        )
        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                val daytargetsum = result.getString("daytargetsum")
                AppGlobal.instance.set_target_server_shift("2", daytargetsum)
            } else {
                Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            }
        })
        params = listOf("code" to "target",
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  "3",
            "date" to today,
            "mac_addr" to mac
        )
        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if (code == "00") {
                val daytargetsum = result.getString("daytargetsum")
                AppGlobal.instance.set_target_server_shift("3", daytargetsum)
            } else {
                Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            }
        })
    }
//    private fun fetchServerTarget() {
////        val work_idx = AppGlobal.instance.get_product_idx()
////        var db = SimpleDatabaseHelper(activity)
////        val row = db.get(work_idx)
//        var total_target = 0
//
//        val uri = "/getlist1.php"
//        var params = listOf("code" to "target",
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
//            "date" to DateTime().toString("yyyy-MM-dd"),
//            "mac_addr" to AppGlobal.instance.getMACAddress()
//        )
//
//        request(this, uri, false, params, { result ->
//            var code = result.getString("code")
//            var msg = result.getString("msg")
//            if (code == "00") {
//Log.e("server target", ""+result.toString())
//                var target = result.getString("target")
//                var targetsum = result.getString("targetsum")
//                var daytargetsum = result.getString("daytargetsum")
//                total_target = targetsum.toInt()
//
//                var target_type = AppGlobal.instance.get_target_type()
//                if (target_type=="server_per_hourly") total_target = target.toInt()
//                else if (target_type=="server_per_accumulate") total_target = targetsum.toInt()
//                else if (target_type=="server_per_day_total") total_target = daytargetsum.toInt()
//
//            } else {
//                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//            }
//        })
//    }

    /*
     *  당일 작업시간 가져오기. 새벽이 지난 시간은 1일을 더한다.
     *  전일 작업이 끝나지 않았을수 있기 때문에 전일 데이터도 가져온다.
     */
    private fun fetchWorkData2() {

        var dt = DateTime()
        val shift3: JSONObject? = fetchManualShift()      // manual 데이터가 있으면 가져온다.

        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "work_time2",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "today" to dt.toString("yyyy-MM-dd"),
            "yesterday" to dt.minusDays(1).toString("yyyy-MM-dd"))  // 전일 데이터

//        OEEUtil.LogWrite(params.toString(), "Shift worktime Request params")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var list1 = result.getJSONArray("item1")
                var list2 = result.getJSONArray("item2")
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
                list1 = handleWorkData(list1)
                list2 = handleWorkData(list2)
                AppGlobal.instance.set_today_work_time(list1)
                AppGlobal.instance.set_prev_work_time(list2)

                // Log 확인
//                OEEUtil.LogWrite(list1.toString(), "Today Shift info")
//                OEEUtil.LogWrite(list2.toString(), "Yester Shift info")

                compute_work_shift()

            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }
    private fun fetchWorkData() {
        // 당일과 전일 데이터를 모두 불러왔는지 체크하기 위한 변수 (2가 되면 모두 읽어옴)
        var _load_work_data_cnt = 0

        var dt = DateTime()
        val shift3: JSONObject? = fetchManualShift()      // manual 데이터가 있으면 가져온다.

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
                list1 = handleWorkData(list1)
                AppGlobal.instance.set_today_work_time(list1)
//Log.e("today shift", list1.toString())
                _load_work_data_cnt++
                if (_load_work_data_cnt >= 2) compute_work_shift()
            } else {
                ToastOut(this, msg, true)
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
                list2 = handleWorkData(list2)
                AppGlobal.instance.set_prev_work_time(list2)
//Log.e("yester shift", list2.toString())
                _load_work_data_cnt++
                if (_load_work_data_cnt >= 2) compute_work_shift()
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }


    /*
     *  Shift 전환을 위한 변수를 미리 세팅한다.
     *  현재 Shift의 idx, 종료시간과 다음 Shift의 시작 시간을 미리 구해놓는다. (매초마다 검사를 하기 때문에 최대한 작업을 단순화하기 위함)
     */
    private var is_loop :Boolean = false        // 처리 중일때 중복 처리를 하지 않기 위함
    var _current_shift_etime_millis = 0L        // 현재 Shift 의 종료 시간 저장
    var _next_shift_stime_millis = 0L           // 다음 Shift 의 시작 시간 저장 (위의 종료 시간이 0L 일때만 세팅된다.)
    var _last_working = false                   // 금일 마지막 작업인지 확인용

    private fun compute_work_shift() {
        if (is_loop) return
        is_loop = true

        val list = AppGlobal.instance.get_current_work_time()
//Log.e("current work time", list.toString())

        // 현재 쉬프트의 종료 시간을 구한다. 자동 종료를 위해
        // 종료 시간이 있으면 다음 시작 시간을 구할 필요없음. 종료되면 이 로직이 실행되므로 자동으로 구해지기 때문..
        if (list.length() > 0) {

            // DB에 Shift 정보를 저장한다.
            // Production report 때문에 그날의 정보를 모두 저장해야 함.
            var target_type = AppGlobal.instance.get_target_type()

            for (i in 0..(list.length() - 1)) {

                val item = list.getJSONObject(i)
                var target = if (target_type.indexOf("server") >= 0)
                        AppGlobal.instance.get_target_server_shift(item["shift_idx"].toString())
                    else AppGlobal.instance.get_target_manual_shift(item["shift_idx"].toString())
                if (target == null || target == "") target = "0"

                val row = _target_db.get(item["date"].toString(), item["shift_idx"].toString())

                if (row == null) { // insert
//                    Log.e("db info", "===> " + item["date"].toString() + " : " + item["shift_idx"].toString() + " : null")
                    _target_db.add(item["date"].toString(), item["shift_idx"].toString(), item["shift_name"].toString(), target, item["work_stime"].toString(), item["work_etime"].toString())
                } else {           // update
//                    Log.e("db info", "===> " + item["date"].toString() + " : " + item["shift_idx"].toString() + " : " + row.toString())
                    _target_db.update(row["idx"].toString(), item["shift_name"].toString(), target, item["work_stime"].toString(), item["work_etime"].toString())
                }
            }

            val now_millis = DateTime().millis

            for (i in 0..(list.length() - 1)) {
                val item = list.getJSONObject(i)
                var shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString()).millis
                var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString()).millis

                if (shift_stime <= now_millis && now_millis < shift_etime) {
                    // 타이틀 변경
                    tv_title.setText(item["shift_name"].toString() + "   " +
                            OEEUtil.parseDateTime(item["work_stime"].toString()).toString("HH:mm") + " - " +
                            OEEUtil.parseDateTime(item["work_etime"].toString()).toString("HH:mm"))

                    // 이전 Shift와 현재 Shift가 다르다면 Actual 초기화
                    val shift_info = item["date"].toString() + item["shift_idx"].toString()
                    if (shift_info != AppGlobal.instance.get_last_shift_info()) {
                        AppGlobal.instance.set_current_shift_actual_cnt(0)      // 토탈 Actual 초기화
                        AppGlobal.instance.set_last_shift_info(shift_info)      // 현재 Shift 정보 저장
                    }

                    _current_shift_etime_millis = shift_etime
                    _next_shift_stime_millis = 0L

                    // 마지막 레코드라면 그날의 마지막 작업이므로 마지막을 위한 플래그 세팅
                    if (i == list.length()-1) {
                        _last_working = true
                    } else {
                        _last_working = false
                    }

                    Log.e("compute_work_shift", "shift_idx=" + item["shift_idx"].toString() + ", shift_name=" + item["shift_name"].toString() +
                            ", work time=" + item["work_stime"].toString() + "~" + item["work_etime"].toString() + " ===> Current shift end millis = " + _current_shift_etime_millis)

                    val br_intent = Intent("need.refresh")
                    this.sendBroadcast(br_intent)

                    is_loop = false
                    return
                }
            }
        }

        // 루프를 빠져나왔다는 것은 현재 작업중인 Shift 가 없다는 의미이므로 다음 Shift 의 시작 시간을 구한다.
        // 만약 해당일의 모든 Shift 가 끝났으며 다음 시작 시간은 0L 로 저장한다.
        // 다음날 Shift 시작 정보는 10분마다 로딩하므로 구할 필요없음

        tv_title.setText("No shift")

        AppGlobal.instance.set_current_shift_actual_cnt(0)      // 토탈 Actual 초기화

//        AppGlobal.instance.set_current_shift_idx("-1")
//        AppGlobal.instance.set_current_shift_name("No-shift")

        _current_shift_etime_millis = 0L
        _next_shift_stime_millis = 0L

        // 종료 시간이 없다는 것은 작업 시간이 아니라는 의미이므로 다음 시작 시간을 구한다.
        if (list.length() > 0) {
            val now_millis = DateTime().millis
            for (i in 0..(list.length() - 1)) {
                val item = list.getJSONObject(i)
                var shift_stime = OEEUtil.parseDateTime(item["work_stime"].toString()).millis

                if (shift_stime > now_millis) {
                    _next_shift_stime_millis = shift_stime
                    break
                }
            }
        }
        Log.e("compute_work_shift", "shift_idx=-1, shift_name=No-shift ===> Next shift start millis = " + _next_shift_stime_millis)

        val br_intent = Intent("need.refresh")
        this.sendBroadcast(br_intent)

        is_loop = false
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
            if (code == "00") {
                var value = result.getString("value")
                AppGlobal.instance.set_downtime_sec(value)
//                val s = value.toInt()
//                if (s > 0) {
//                }
            } else {
                ToastOut(this, result.getString("msg"), true)
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
            if (code == "00") {
                AppGlobal.instance.set_color_code(result.getJSONArray("item"))
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
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
            if (code == "00") {
                btn_server_state.isSelected = true
                AppGlobal.instance._server_state = true
                tv_ms.text = "" + millis + " ms"

                val br_intent = Intent("need.refresh.server.state")
                br_intent.putExtra("state", "Y")
                this.sendBroadcast(br_intent)
            } else {
                ToastOut(this, result.getString("msg"))
            }
        }, {
            btn_server_state.isSelected = false
            val br_intent = Intent("need.refresh.server.state")
            br_intent.putExtra("state", "N")
            this.sendBroadcast(br_intent)
        })
    }

    // 10초마다 현재 target을 서버에 저장
    // 30분 마다로 바뀜
    // 작업 시간이 아닐경우는 Pass
    private fun updateCurrentWorkTarget() {
        var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
        if (item != null) {
            var _total_target = 0
            var target_type = AppGlobal.instance.get_target_type()
            if (target_type.indexOf("server") >= 0) {
                when (item["shift_idx"]) {
                    "1" -> _total_target = AppGlobal.instance.get_target_server_shift("1").toInt()
                    "2" -> _total_target = AppGlobal.instance.get_target_server_shift("2").toInt()
                    "3" -> _total_target = AppGlobal.instance.get_target_server_shift("3").toInt()
                }
            } else if (target_type.indexOf("device") >= 0) {
                when (item["shift_idx"]) {
                    "1" -> _total_target = AppGlobal.instance.get_target_manual_shift("1").toInt()
                    "2" -> _total_target = AppGlobal.instance.get_target_manual_shift("2").toInt()
                    "3" -> _total_target = AppGlobal.instance.get_target_manual_shift("3").toInt()
                }
            }
            Log.e("updateCurrentWorkTarget", "target_type=" + target_type + ", _total_target=" + _total_target)
            if (_total_target > 0) {
                // 구서버용
//                val uri = "/sendtarget.php"
//                var params = listOf(
//                    "mac_addr" to AppGlobal.instance.getMACAddress(),
//                    "date" to item["date"].toString(),
//                    "shift_idx" to  item["shift_idx"],     // AppGlobal.instance.get_current_shift_idx()
//                    "target_count" to _total_target)
//                request(this, uri, true,false, params, { result ->
//                    var code = result.getString("code")
//                    var msg = result.getString("msg")
//                    if(code != "00"){
//                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//                    }
//                })

                // 신서버용
                val uri = "/Starget.php"
                var params = listOf(
                    "mac_addr" to AppGlobal.instance.getMACAddress(),
                    "target" to _total_target,
                    "shift_idx" to  item["shift_idx"]
                )

                Log.e("Starget request", "= " + params.toString())
                request(this, uri, true,false, params, { result ->
                    var code = result.getString("code")
                    Log.e("Starget result", "= " + result.getString("msg").toString())
                    if(code != "00"){
                        ToastOut(this, result.getString("msg"), true)
                    }
                })
            }
        }
    }

    fun endWork() {
        AppGlobal.instance.set_work_idx("")
        AppGlobal.instance.set_worker_no("")
        AppGlobal.instance.set_worker_name("")
//        AppGlobal.instance.set_design_info_idx("")
//        AppGlobal.instance.set_model("")
//        AppGlobal.instance.set_article("")
//        AppGlobal.instance.set_material_way("")
//        AppGlobal.instance.set_component("")

        AppGlobal.instance.set_current_shift_actual_cnt(0)

        // 다운타임이 있으면 완료로 처리
        val downtime_idx = AppGlobal.instance.get_downtime_idx()
        if (downtime_idx != "") sendEndDownTimeForce()

        var db = SimpleDatabaseHelperBackup(this)
        db.delete()

        var db2 = DBHelperForDownTime(this)
        db2.delete()

        var db3 = DBHelperForCount(this)
        db3.delete()

        // Component DB는 그날의 shift 가 다 끝나야 초기화한다.
        val today = AppGlobal.instance.get_today_work_time()
        if (today.length() > 0) {
            val shift = today.getJSONObject(today.length() - 1)
            var shift_etime = OEEUtil.parseDateTime(shift["work_etime"].toString())
            val now = DateTime()
            if (shift_etime.millis <= now.millis) {
                var db1 = DBHelperForComponent(this)
                db1.delete()

//                AppGlobal.instance.set_compo_wos("")
//                AppGlobal.instance.set_compo_model("")
//                AppGlobal.instance.set_compo_style("")
//                AppGlobal.instance.set_compo_component("")
                AppGlobal.instance.set_compo_size("")
                AppGlobal.instance.set_compo_target(0)
            }
        }
//Log.e("today", today.toString())
//        var db1 = DBHelperForComponent(this)
//        db1.delete()

        Toast.makeText(this, getString(R.string.msg_exit_automatically), Toast.LENGTH_SHORT).show()
    }

//    private fun checkExit() {
//        val work_idx = ""+AppGlobal.instance.get_work_idx()
//        if (work_idx=="") return
//
//        val last_work_dt = OEEUtil.parseDate(AppGlobal.instance.get_current_work_day())
//Log.e("checkExit", "last_work_dt="+last_work_dt.toString())
//        val current = AppGlobal.instance.get_current_work_time()
//        if (current.length()==0) return
//
//        val shift = current.getJSONObject(0)
//        var shift_stime = OEEUtil.parseDateTime(shift["work_stime"].toString())
//        var d = Days.daysBetween(last_work_dt.toLocalDate(), shift_stime.toLocalDate()).getDays()
//Log.e("checkExit", "shift stime="+shift_stime.toString())
//Log.e("checkExit", "d="+d.toString())
//        if (d != 0) {
////            endWork()
//        }
//    }


    fun endTodayWork() {
        tv_report_count?.text = "0"                              // 좌측 Report 버튼의 Actual 값도 0으로 초기화

        AppGlobal.instance.set_last_received("")                // 다운타임 검사용 변수도 초기화
        AppGlobal.instance.set_downtime_idx("")

        AppGlobal.instance.set_work_idx("")
        AppGlobal.instance.set_worker_no("")
        AppGlobal.instance.set_worker_name("")
        AppGlobal.instance.set_compo_size("")
        AppGlobal.instance.set_compo_target(0)

        AppGlobal.instance.set_current_shift_actual_cnt(0)

        var db = SimpleDatabaseHelperBackup(this)
        db.delete()

        var db2 = DBHelperForDownTime(this)
        db2.delete()

        var db3 = DBHelperForCount(this)
        db3.delete()

        // 지난 데이터를 확인하고 계속 작업할 수 있도록 하기 위해
        // 삭제 하지 않음. 19-10-22
//        var db4 = DBHelperForComponent(this)
//        db4.delete()

        ToastOut(this, R.string.msg_exit_automatically)
    }

    /*
     *  Shift 전환을 위한 실시간 검사. 매초마다 실행됨
     *  현재 작업중인 Shift 가 있으면 종료되는 시간을 검사해서 종료 시간이 되었다면 다음 쉬프트를 계산한다. (_current_shift_etime_millis)
     *  현재 작업중인 Shift 가 없으면 일하는 시간이 아니므로 다음 시작 시간을 검사하고, 시작 시간이라면 Shift의 종료시간을 계산한다. (_next_shift_stime_millis)
     */
    fun checkCurrentShiftEndTime() {
        // _current_shift_etime_millis 가 0이 아니면 진행중이라는 의미이므로 Shift가 종료되었는지 검사한다.
        if (_current_shift_etime_millis != 0L) {
            if (_current_shift_etime_millis <= DateTime().millis) {     // 현재 시간보다 작으면 Shift 끝난거임.
                Log.e("checkCurrentShiftEnd", "end time . finish shift work =============================> need reload")
//                AppGlobal.instance.set_current_shift_actual_cnt(0)      // 토탈 Actual 초기화
//                AppGlobal.instance.set_last_received("")                // 다운타임 검사용 변수도 초기화
//                tv_report_count?.text = "0"                              // 좌측 Report 버튼의 Actual 값도 0으로 초기화

                // 마지막 작업이 끝났으면 완전 초기화
//                if (_last_working == true) {
//                    endTodayWork()
//                    _last_working = false
//                }
                endTodayWork()
                compute_work_shift()
            }

        } else {        // 여기로 빠졌다면 앞의 Shift는 종료되었으므로 다음 Shift 가 시작되었는지 검사한다.
            if (_next_shift_stime_millis != 0L) {                       // 다음 Shift가 있다면...
                if (_next_shift_stime_millis <= DateTime().millis) {    // 현재 시간보다 작으면 Shift가 진행중인 것임
                    Log.e("checkCurrentShiftEnd", "start time . start shift work =============================> need reload")
                    compute_work_shift()
                }
            }
        }
    }

    /////// 쓰레드
    private val _downtime_timer = Timer()
    private val _timer_task1 = Timer()          // 서버 접속 체크 Ping test. Shift의 Target 정보
    private val _timer_task2 = Timer()          // 작업시간, 다운타입, 칼라 Data 가져오기 (workdata, designdata, downtimetype, color)
    private val _timer_task4 = Timer()          // 30분마다. 서버로 타겟값 전송

    private fun start_timer() {

        // 매초
        val downtime_task = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    checkCurrentShiftEndTime()
                    checkDownTime()
//                    checkExit()
                }
            }
        }
        _downtime_timer.schedule(downtime_task, 500, 1000)

        // 30초마다
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    sendPing()
                    RemoveDownTimeData()    // Shift가 지난 다운타임 데이터를 삭제한다.
//                    updateCurrentWorkTarget() // 30분으로 이동
                }
            }
        }
        _timer_task1.schedule(task1, 3000, 30000)

        // 10분마다
        val task2 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    fetchRequiredData()
                }
            }
        }
        _timer_task2.schedule(task2, 600000, 600000)

        // 30분마다
        val task4 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateCurrentWorkTarget()
                }
            }
        }
        _timer_task4.schedule(task4, 600000, 1800000)
    }
    private fun cancel_timer () {
        _downtime_timer.cancel()
        _timer_task1.cancel()
        _timer_task2.cancel()
        _timer_task4.cancel()
    }

    ////////// USB
    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED // USB PERMISSION GRANTED
                -> ToastOut(context, "USB Ready")
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED // USB PERMISSION NOT GRANTED
                -> ToastOut(context, "USB Permission not granted")
                UsbService.ACTION_NO_USB // NO USB CONNECTED
                -> ToastOut(context, "No USB connected")
                UsbService.ACTION_USB_DISCONNECTED // USB DISCONNECTED
                -> ToastOut(context, "USB disconnected")
                UsbService.ACTION_USB_NOT_SUPPORTED // USB NOT SUPPORTED
                -> ToastOut(context, "USB device not supported")
            }
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED // USB PERMISSION GRANTED
                -> {
                    btn_usb_state.isSelected = true
                    AppGlobal.instance._usb_state = true
                    tv_usb.setTextColor(Color.parseColor("#f8ad13"))
                }
                else -> {
                    btn_usb_state.isSelected = false
                    AppGlobal.instance._usb_state = false
                    tv_usb.setTextColor(Color.parseColor("#EEEEEE"))
                }
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
            try {
                val parser = JsonParser()
                val element = parser.parse(recvBuffer)
                val cmd = element.asJsonObject.get("cmd").asString
                val value = element.asJsonObject.get("value")

                ToastOut(this, element.toString())
                Log.w("test", "usb = " + recvBuffer)

                saveRowData(cmd, value)
            } catch(e: JsonSyntaxException) {
//                OEEUtil.LogWrite(e.toString(), "USB Input Error")
            }
        } else {
            ToastOut(this, "usb parsing error! = " + recvBuffer)
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
    private fun saveRowData(cmd: String, value: JsonElement) {

        if (cmd=="count") {
            var db = DBHelperForComponent(this)
//          var db = SimpleDatabaseHelper(this)

            if (AppGlobal.instance.get_sound_at_count()) AppGlobal.instance.playSound(this)

            // 작업 시간인지 확인
            val cur_shift: JSONObject ?= AppGlobal.instance.get_current_shift_time()
            if (cur_shift == null) {
                Toast.makeText(this, getString(R.string.msg_not_start_work), Toast.LENGTH_SHORT).show()
                return
            }


            // 휴식시간인지 확인. 휴식 시간이면 Actual Count 가능한지 체크
            if (!AppGlobal.instance.get_planned_count_process()) {
                // 설정되어 있는 휴식 시간
                val planned1_stime = OEEUtil.parseDateTime(cur_shift["planned1_stime_dt"].toString())
                val planned1_etime = OEEUtil.parseDateTime(cur_shift["planned1_etime_dt"].toString())
                val planned2_stime = OEEUtil.parseDateTime(cur_shift["planned2_stime_dt"].toString())
                val planned2_etime = OEEUtil.parseDateTime(cur_shift["planned2_etime_dt"].toString())
                val now_millis = DateTime().millis

                // 워크 타임안에 있으면서 휴식 시간 안에 있다면,
                if ((planned1_stime.millis < now_millis && planned1_etime.millis > now_millis ) ||
                    (planned2_stime.millis < now_millis && planned2_etime.millis > now_millis )) {
                    ToastOut(this, R.string.msg_cannot_work_planned_time, true)
                    return
                }
            }


            val shift_idx = cur_shift["shift_idx"]      // 현재 작업중인 Shift
            var inc_count = 1

            // 콤포넌트 선택되어야만 실행되는 경우
            if (AppGlobal.instance.get_view_with_component()) {

                // 선택한 Component 제품이 있는지 확인
                val work_idx = AppGlobal.instance.get_work_idx()
                if (work_idx == "") {
                    Toast.makeText(this, getString(R.string.msg_select_component), Toast.LENGTH_SHORT).show(); return
                }

                // Pairs 선택 확인
                val layer_value = AppGlobal.instance.get_compo_pairs()
                if (layer_value == "") {
                    Toast.makeText(this, getString(R.string.msg_layer_not_selected), Toast.LENGTH_SHORT).show(); return
                }

                // Operator 선택 확인
                if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                    Toast.makeText(this, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show(); return
                }

                if (layer_value == "0.5") {
                    if ((AppGlobal.instance.get_accumulated_count()+1) <= 1) {
                        AppGlobal.instance.set_accumulated_count(1); return
                    } else {
                        AppGlobal.instance.set_accumulated_count(0)
                    }
                } else {
                    inc_count = layer_value.toInt()
                }

                // component total count
                val row = db.get(work_idx)
                if (row != null) {
                    val actual = (row!!["actual"].toString().toInt() + inc_count)
                    db.updateWorkActual(work_idx, actual)
                }
            }

            // total count
            var cnt = AppGlobal.instance.get_current_shift_actual_cnt() + inc_count
            AppGlobal.instance.set_current_shift_actual_cnt(cnt)

//            tv_report_count.text = "" + AppGlobal.instance.get_current_shift_actual_cnt()
            tv_report_count.text = "" + cnt

            _last_count_received_time = DateTime()      // downtime 시간 초기화 (구버전용)

            AppGlobal.instance.set_last_received(DateTime().toString("yyyy-MM-dd HH:mm:ss"))

            sendEndDownTimeForce()      // 처리안된 Downtime 강제 완료

            sendCountData(value.toString(), inc_count)  // 서버에 카운트 정보 전송

//            _stitch_db.add(work_idx, value.toString())

            //val now = DateTime()
            val now = cur_shift["date"]
            val date = now.toString()
            val houly = DateTime().toString("HH")

            val rep = _report_db.get(date, houly, shift_idx.toString())
            if (rep == null) {
                _report_db.add(date, houly, shift_idx.toString(), inc_count)
            } else {
                val idx = rep!!["idx"].toString()
                val actual = rep!!["actual"].toString().toInt() + inc_count
                _report_db.updateActual(idx, actual)
            }
        }
    }

    fun startComponent(wosno:String, styleno:String, model:String, component:String, size:String, target:String, actual:String) {

        var db = DBHelperForComponent(this)

        val work_info = AppGlobal.instance.get_current_shift_time()
        val shift_idx = work_info?.getString("shift_idx") ?: ""
        val shift_name = work_info?.getString("shift_name") ?: ""

        val row = db.get(wosno, size, component)

        if (row == null) {
            val s = db.gets_all_wos()
            val seq = (s?.size ?: 0) + 1

            db.add(wosno, shift_idx, shift_name, styleno, model, component, size, target.toInt(), 0, 0, seq)
            val row2 = db.get(wosno, size, component)
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

        // 작업시작할때 현재 쉬프트의 날짜를 기록해놓음
        val current = AppGlobal.instance.get_current_work_time()
        if (current.length() > 0) {
            val shift = current.getJSONObject(0)
            var shift_stime = OEEUtil.parseDateTime(shift["work_stime"].toString())
            AppGlobal.instance.set_current_work_day(shift_stime.toString("yyyy-MM-dd"))
        }

        // downtime sec 초기화
        // 새로 선택한 상품이 있으므로 이 값을 초기화 한다. 기존에 없던 부분
        //_last_count_received_time = DateTime()

        // 현재 shift의 첫생산인데 지각인경우 downtime 처리
//        val list = db.gets()
//        if (list?.size == 1) {
//            val item = AppGlobal.instance.get_current_shift_time()
//            if (item == null) return
//
//            var work_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
//            val now = DateTime()
//            if (now.millis - work_stime.millis > Constants.DOWNTIME_FIRST) {
//                sendStartDownTime(work_stime)
//                startDowntimeActivity()
//            }
//        }
    }

    fun startNewProduct(didx:String, piece_info:Int, cycle_time:Int, model:String, article:String, material_way:String, component:String) {

    }

    // downtime 발생시 푸시 발송
    fun sendPush(push_text: String, add_text: String = "", progress: Boolean=false) {
        val uri = "/pushcall.php"
        var params = listOf(
            "code" to "push_text_list",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "machine_no" to AppGlobal.instance.get_mc_no1(),
            "mc_model" to AppGlobal.instance.get_mc_model(),
            "seq" to "0",
            "text" to push_text,
            "add_text" to add_text)

        request(this, uri, progress, params, { result ->
            var code = result.getString("code")
            if(code != "00"){
//                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun sendStartDownTime(dt:DateTime) {

        if (AppGlobal.instance.get_server_ip() == "") return

        val work_idx = "" + AppGlobal.instance.get_product_idx()
        if (work_idx=="") return

        if (_is_call) return
        _is_call = true
/*
        var db = SimpleDatabaseHelper(this)
        val row = db.get(work_idx)
        val seq = row!!["seq"].toString().toInt() + 1
*/
        var down_db = DBHelperForDownTime(this)
        val count = down_db.counts_for_notcompleted()
        if (count > 0) return

        val list = down_db.gets()

        val uri = "/downtimedata.php"
        var params = listOf(
            "code" to "start",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
//            "didx" to AppGlobal.instance.get_design_info_idx(),
            "didx" to "1001",
            "sdate" to dt.toString("yyyy-MM-dd"),
            "stime" to dt.toString("HH:mm:ss"),
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "seq" to (list?.size ?: 0) + 1)

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                var idx = result.getString("idx")
                AppGlobal.instance.set_downtime_idx(idx)

//                val didx = AppGlobal.instance.get_design_info_idx()
                val didx = "1001"
                val work_info = AppGlobal.instance.get_current_shift_time()
                val shift_idx = work_info?.getString("shift_idx") ?: ""
                val shift_name = work_info?.getString("shift_name") ?: ""

                val start_dt = dt.toString("yyyy-MM-dd HH:mm:ss")
                down_db.add(idx, work_idx, didx, shift_idx, shift_name, start_dt)

                startDowntimeInputActivity(idx, start_dt)

                Log.e("checkDownTime", "idx=" + idx + ", start_dt=" + start_dt)

                sendPush("SYS: DOWNTIME")

            } else {
                ToastOut(this, result.getString("msg"), true)
            }
            _is_call = false
        },{
            _is_call = false
        })
    }

    private fun sendEndDownTimeForce() {
        if (AppGlobal.instance.get_server_ip() == "") return
        if (AppGlobal.instance.get_downtime_idx() == "") return

        var db = DBHelperForDownTime(this)

        val idx = AppGlobal.instance.get_downtime_idx()
        val item = db.get(idx)
        if (item == null) return

        val now = DateTime()
        val now_millis = now.millis
        val down_start_millis = OEEUtil.parseDateTime(item["start_dt"].toString()).millis

        var planned1_time = 0
        var planned2_time = 0

        val shift_time = AppGlobal.instance.get_current_shift_time()

        if (shift_time != null) {
            val planned1_stime_millis = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString()).millis
            val planned1_etime_millis = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString()).millis
            val planned2_stime_millis = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString()).millis
            val planned2_etime_millis = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString()).millis

            planned1_time = AppGlobal.instance.compute_time_millis(down_start_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
            planned2_time = AppGlobal.instance.compute_time_millis(down_start_millis, now_millis, planned2_stime_millis, planned2_etime_millis)
        }
        val down_time = ((now_millis - down_start_millis) / 1000).toInt()
        val real_down_time = down_time - planned1_time - planned2_time

        val ct = AppGlobal.instance.get_cycle_time()
        val target = if (ct > 0) real_down_time / ct else 0

        val downtime = "99"
        val uri = "/downtimedata.php"
        var params = listOf(
            "code" to "end",
            "idx" to AppGlobal.instance.get_downtime_idx(),
            "downtime" to downtime,
            "edate" to DateTime().toString("yyyy-MM-dd"),
            "etime" to DateTime().toString("HH:mm:ss"))

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")
            if (code == "00") {
                val idx = AppGlobal.instance.get_downtime_idx()
                AppGlobal.instance.set_downtime_idx("")

//                db.updateEnd(idx, "ignored")
                db.updateEnd(idx, "ignored", now.toString("yyyy-MM-dd HH:mm:ss"), down_time, real_down_time, target)

                // 기존 다운타임 화면이 열려있으면 닫고
                val br_intent = Intent("start.downtime")
                this.sendBroadcast(br_intent)

                // 카운트뷰로 이동
//                if (vp_fragments.currentItem != 1) changeFragment(1)

            } else if (code == "99") {
                // ?
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    // 작업 시간일때, 작업 시작 시간보다 작은 시간의 DownTime 을 삭제한다.(=지난 Shift의 DownTime)
    fun RemoveDownTimeData() {
        val item = AppGlobal.instance.get_current_shift_time()
        if (item != null) {
            val db = DBHelperForDownTime(this)
            db.deleteOldData(item["work_stime"].toString())
        }
    }

    private var _is_down_loop: Boolean = false

    // 신버전
    private fun checkDownTime() {
        if (_is_down_loop) return
        _is_down_loop = true

        var db = DBHelperForDownTime(this)
        val count = db.counts_for_notcompleted()
        if (count > 0) {
            AppGlobal.instance.set_last_received(DateTime().toString("yyyy-MM-dd HH:mm:ss"))
            _is_down_loop = false
            return
        }

        val work_idx = AppGlobal.instance.get_product_idx()
        if (work_idx == "") {
            _is_down_loop = false
            return
        }

        val downtime_time = AppGlobal.instance.get_downtime_sec()
        if (downtime_time == "") {
            ToastOut(this, R.string.msg_no_downtime)
            _is_down_loop = false
            return
        }
        val downtime_time_sec = downtime_time.toInt()

        val item = AppGlobal.instance.get_current_shift_time()
        if (item == null) {
            _is_down_loop = false
            return
        }

        val work_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
        val work_stime_millis = work_stime.millis
        val work_etime_millis = OEEUtil.parseDateTime(item["work_etime"].toString()).millis
        val planned1_stime_millis = OEEUtil.parseDateTime(item["planned1_stime_dt"].toString()).millis
        val planned1_etime_millis = OEEUtil.parseDateTime(item["planned1_etime_dt"].toString()).millis
        val planned2_stime_millis = OEEUtil.parseDateTime(item["planned2_stime_dt"].toString()).millis
        val planned2_etime_millis = OEEUtil.parseDateTime(item["planned2_etime_dt"].toString()).millis

        val now = DateTime()
        val now_millis = now.millis

        var last_received_time = work_stime    // downtime 값이 "" 이면 처음이므로 Shift 시작 시간으로 저장

        var chk = AppGlobal.instance.get_last_received()
        if (chk != "") {
            if (OEEUtil.parseDateTime(chk).millis < work_stime_millis) {    // downtime 시작 시간이 Shift의 시작 시간보다 작다면 초기화
                chk = item["work_stime"].toString()
                AppGlobal.instance.set_last_received(chk)
            }
            last_received_time = OEEUtil.parseDateTime(chk)
        }

        val last_received_time_millis = last_received_time.millis

        // 워크 타임안에 있는 경우
        if (work_stime_millis < now_millis && work_etime_millis > now_millis) {

            // 휴식 시간이 아닐때
            if (!(planned1_stime_millis < now_millis && planned1_etime_millis > now_millis ) &&
                !(planned2_stime_millis < now_millis && planned2_etime_millis > now_millis ) && downtime_time_sec > 0) {

                // 다운타임 안의 휴식시간
                val d1 = AppGlobal.instance.compute_time_millis(last_received_time_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
                val d2 = AppGlobal.instance.compute_time_millis(last_received_time_millis, now_millis, planned2_stime_millis, planned2_etime_millis)

                val cur_down_time = ((now_millis - last_received_time_millis) / 1000) - d1 - d2     // 휴식시간을 뺀 실제 다운타임

                // 지정된 downtime 이 지났으면 downtime을 발생시킨다.
                if (cur_down_time > downtime_time_sec) {
                    sendStartDownTime(last_received_time)
                }
            }

        } else {
            // 워크 타임이 아니면 downtime 시작 시간 초기화
            AppGlobal.instance.set_last_received("")
        }

        // 위의 로직으로 변경됨 (다운타임 시간에서 휴식 시간을 빼고 계산)
        // 워크 타임안에 있으면서 휴식 시간이 아니고,
        // 지정된 downtime 이 지났으면 downtime을 발생시킨다.
//        if (work_stime.millis < now_millis && work_etime.millis > now_millis &&
//            !(planned1_stime_dt.millis < now_millis && planned1_etime_dt.millis > now_millis ) &&
//            !(planned2_stime_dt.millis < now_millis && planned2_etime_dt.millis > now_millis ) &&
//            downtime_time_sec > 0 && now_millis - last_received_time.millis > downtime_time_sec * 1000) {
////            Log.e("downtime chk", "over time")
////            sendStartDownTime(OEEUtil.parseDateTime(downtime_chk))
//            sendStartDownTime(last_received_time)
////            startDowntimeActivity()
//        }

        // 위의 로직으로 변경됨 (다운타임 시간에서 휴식 시간을 빼고 계산)
        // 워크 타임이 아니면 downtime 시작 시간을 현재 시간으로 초기화
//        if (work_stime.millis > now_millis || work_etime.millis < now_millis) {
//            AppGlobal.instance.set_last_received("")
//            // downtime 시간 초기화 하기 전에 "" 값이면 초기화를 하지 않는다. (Shift 시작시 지각인지 체크하기 위함)
//            if (AppGlobal.instance.get_last_received() != "") {
//                AppGlobal.instance.set_last_received("")
////                AppGlobal.instance.set_last_received(now.toString("yyyy-MM-dd HH:mm:ss"))
//            }
//        }

//        var last_count_received = work_stime    // downtime 값이 "" 이면 처음이므로 Shift 시작 시간으로 저장하기 위함

//        var downtime_chk = AppGlobal.instance.get_last_received()
//
//        // 저장된 downtime 시간이 현재 Shift의 시작 시간보다 작다면 초기화 시킨다.
//        if (OEEUtil.parseDateTime(downtime_chk).millis < work_stime.millis) {
//            downtime_chk = work_stime.toString("yyyy-MM-dd HH:mm:ss")
//            AppGlobal.instance.set_last_received(downtime_chk)
//        }
//
//        if (downtime_chk != "") {
////            Log.e("DownTime value check", "time : "+downtime_chk)
//            last_count_received = OEEUtil.parseDateTime(downtime_chk)
//        }

        // 워크 타임안에 있으면서 휴식 시간이 아니고,
        // 지정된 downtime 이 지났으면 downtime을 발생시킨다.
//        if (work_stime.millis < now_millis && work_etime.millis > now_millis &&
//            !(planned1_stime_dt.millis < now_millis && planned1_etime_dt.millis > now_millis ) &&
//            !(planned2_stime_dt.millis < now_millis && planned2_etime_dt.millis > now_millis ) &&
//            downtime_time_sec > 0 && now_millis - last_count_received.millis > downtime_time_sec*1000) {
////            Log.e("downtime chk", "over time")
////            sendStartDownTime(OEEUtil.parseDateTime(downtime_chk))
//            sendStartDownTime(last_count_received)
//            startDowntimeActivity()
//        }

        // 워크 타임이 아니면 downtime 시작 시간을 현재 시간으로 초기화
//        if (work_stime.millis > now_millis || work_etime.millis < now_millis) {
//            // downtime 시간 초기화 하기 전에 "" 값이면 초기화를 하지 않는다.
//            // 처음 Shift 시작시 지각인지 체크하기 위함.
//            if (AppGlobal.instance.get_last_received() != "") {
//                AppGlobal.instance.set_last_received(now.toString("yyyy-MM-dd HH:mm:ss"))
////            Log.e("downtime chk", "now work")
//            }
//        }

        // 휴식 시간이면 downtime 시작 시간을 현재 시간으로 초기화
//        if ((planned1_stime_dt.millis < now_millis && planned1_etime_dt.millis > now_millis ) ||
//            (planned2_stime_dt.millis < now_millis && planned2_etime_dt.millis > now_millis )) {
//            // downtime 시간 초기화 하기 전에 "" 값이면 초기화를 하지 않는다.
//            // 처음 Shift 시작시 지각인지 체크하기 위함.
//            if (AppGlobal.instance.get_last_received() != "") {
//                AppGlobal.instance.set_last_received(now.toString("yyyy-MM-dd HH:mm:ss"))
////            Log.e("downtime chk", "planned time")
//            }
//        }
        _is_down_loop = false
    }

    // 구버전
//    private fun checkDownTime2() {
//        var db = DBHelperForDownTime(this)
//        val count = db.counts_for_notcompleted()
////Log.e("iot count",""+count)
//
//        if (count > 0) {
//            _last_count_received_time = DateTime()
//            return
//        }
////        val idx = AppGlobal.instance.get_design_info_idx()
////        if (idx == "") return
////        val work_idx = "" + AppGlobal.instance.get_product_idx()
////        if (work_idx == "") return
//
//        val work_idx = AppGlobal.instance.get_work_idx()
////Log.e("iot work_idx","work_idx : "+work_idx)
//        if (work_idx == "") return
//
//        val now = DateTime()
//        val downtime_time = AppGlobal.instance.get_downtime_sec()
//
//        if (downtime_time == "") {
//            ToastOut(this, R.string.msg_no_downtime)
////Log.e("downtime_time",""+downtime_time)
//            return
//        }
//
//        val item = AppGlobal.instance.get_current_shift_time()
//        if (item==null) return
//
//        var work_stime = OEEUtil.parseDateTime(item["work_stime"].toString())
//        var work_etime = OEEUtil.parseDateTime(item["work_etime"].toString())
//        var planned1_stime_dt = OEEUtil.parseDateTime(item["planned1_stime_dt"].toString())
//        var planned1_etime_dt = OEEUtil.parseDateTime(item["planned1_etime_dt"].toString())
//        var planned2_stime_dt = OEEUtil.parseDateTime(item["planned2_stime_dt"].toString())
//        var planned2_etime_dt = OEEUtil.parseDateTime(item["planned2_etime_dt"].toString())
//
//        val downtime_time_sec = downtime_time.toInt()
////Log.e("downtime_time_sec",""+downtime_time_sec)
////Log.e("downtime_time_sec","now.millis - _last_count_received_time.millis : " + (now.millis - _last_count_received_time.millis))
////Log.e("downtime_time_sec","downtime_time_sec*1000 : " + downtime_time_sec*1000)
//        // 워크 타임안에 있으면서 휴식 시간이 아니고,
//        // 지정된 downtime 이 지났으면 downtime을 발생시킨다.
//        if (work_stime.millis < now.millis && work_etime.millis > now.millis &&
//                !(planned1_stime_dt.millis < now.millis && planned1_etime_dt.millis > now.millis ) &&
//                !(planned2_stime_dt.millis < now.millis && planned2_etime_dt.millis > now.millis ) &&
//                downtime_time_sec > 0 && now.millis - _last_count_received_time.millis > downtime_time_sec*1000) {
//            sendStartDownTime(_last_count_received_time)
//            startDowntimeActivity()
//        }
//
//        // 워크 타임이 아니거나 휴식 시간 안에 있으면 downtime 시작 시간을 현재 시간으로 초기화
//        if (work_stime.millis > now.millis || work_etime.millis < now.millis ||
//                (planned1_stime_dt.millis < now.millis && planned1_etime_dt.millis > now.millis ) ||
//                (planned2_stime_dt.millis < now.millis && planned2_etime_dt.millis > now.millis )) {
//            _last_count_received_time = DateTime()
//        }
//    }

    private fun startDowntimeActivity () {
        val br_intent = Intent("start.downtime")
        this.sendBroadcast(br_intent)
        val intent = Intent(this, DownTimeActivity::class.java)
        startActivity(intent)
    }

    private fun startDowntimeInputActivity(idx: String = "", start_dt: String = "") {
//        val br_intent = Intent("start.downtime")
//        this.sendBroadcast(br_intent)
        if (idx == "" || start_dt == "") return

        val intent = Intent(this, DownTimeInputActivity::class.java)
        intent.putExtra("idx", idx)
        intent.putExtra("start_dt", start_dt)
        startActivity(intent)
    }

    private fun sendCountData(count:String, inc_count:Int) {
        if (AppGlobal.instance.get_server_ip()=="") return

        val work_idx = AppGlobal.instance.get_work_idx()
        if (work_idx == "") return

        var shift_idx = AppGlobal.instance.get_current_shift_idx()
        if (shift_idx == "") shift_idx = "0"

        val actual = AppGlobal.instance.get_current_shift_actual_cnt()

        var db = DBHelperForComponent(this)
        val row = db.get(work_idx)
//        val actual = row!!["actual"].toString().toInt()
        val seq = row!!["seq"].toString().toInt()
//        val seq = "1"

        // 구서버용
        // 패턴과 동일한 API 호출하도록 수정해 달라고 요청함. 2020-01-05.
//        val uri = "/senddata1.php"
//        var params = listOf(
//            "mac_addr" to AppGlobal.instance.getMACAddress(),
//            "didx" to "1001",
//            "count" to inc_count.toString(),
//            "total_count" to actual,
//            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//            "factory_idx" to AppGlobal.instance.get_room_idx(),
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  shift_idx,
//            "seq" to seq,
//            "wos" to AppGlobal.instance.get_compo_wos(),
//            "comp" to AppGlobal.instance.get_compo_component_idx(),
//            "size" to AppGlobal.instance.get_compo_size(),
//            "max_rpm" to "",
//            "avr_rpm" to "")
//Log.e("params", params.toString())

        val count_defective = db.sum_defective_count()      // 현재 디펙티브 값

        // 신서버용
        val uri = "/Scount.php"
        var params = listOf(
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to "1001",
            "count" to inc_count.toString(),
            "total_count" to actual,
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  shift_idx,
            "seq" to seq,
            "wos" to AppGlobal.instance.get_compo_wos(),
            "comp" to AppGlobal.instance.get_compo_component_idx(),
            "size" to AppGlobal.instance.get_compo_size(),
            "max_rpm" to "",
            "avr_rpm" to "",
            "runtime" to "1",
            "actualO" to "1",
            "ctO" to "1",
            "defective" to count_defective.toString(),
            "worker" to AppGlobal.instance.get_worker_no())

        request(this, uri, true,false, params, { result ->
            var code = result.getString("code")
            Log.e("Scount result", "= " + result.getString("msg").toString())
            if(code != "00") {
                Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            }
        })
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