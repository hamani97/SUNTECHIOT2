package com.suntech.iot.cuttingmc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.util.UtilString.addPairText
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.joda.time.DateTime
import java.util.*

class SettingActivity : BaseActivity() {

    private var usb_state = false

    private var tab_pos: Int = 1
    private var _selected_target_type: String = "device"
    private var _selected_blink_color: String = AppGlobal.instance.get_blink_color()

    private var _selected_factory_idx: String = ""
    private var _selected_room_idx: String = ""
    private var _selected_line_idx: String = ""
    private var _selected_mc_no_idx: String = ""
    private var _selected_mc_model_idx: String = ""

    private var _selected_layer_0: String = ""
    private var _selected_layer_1: String = ""
    private var _selected_layer_2: String = ""
    private var _selected_layer_3: String = ""
    private var _selected_layer_4: String = ""
    private var _selected_layer_5: String = ""

    private var is_loop = true

    private var cur_app_version = 0
    private var cur_update_version = 0

    val _broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getAction()
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false))
                    btn_wifi_state.isSelected = true
                else
                    btn_wifi_state.isSelected = false

            } else if (action.equals("need.refresh.server.state")) {
                val state = intent.getStringExtra("state")
                if (state == "Y") {
                    btn_server_state.isSelected = true
                } else btn_server_state.isSelected = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        initView()
        getUpdateVersion()  // update 확인
    }

    fun parentSpaceClick(view: View) {
        var v = this.currentFocus
        if (v != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(_broadcastReceiver, IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
        is_loop = true
        startHandler()
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(_broadcastReceiver)
        is_loop = false
    }

    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                checkUSB()
                startHandler()
            }
        }, 1000)
    }

    private fun checkUSB() {
        if (usb_state != AppGlobal.instance._usb_state) {
            usb_state = AppGlobal.instance._usb_state
            btn_usb_state2.isSelected = usb_state
        }
    }

    private fun initView() {

        tv_title?.setText(R.string.title_setting)

        // system setting
        // set hidden value
        _selected_factory_idx = AppGlobal.instance.get_factory_idx()
        _selected_room_idx = AppGlobal.instance.get_room_idx()
        _selected_line_idx = AppGlobal.instance.get_line_idx()
        _selected_mc_no_idx = AppGlobal.instance.get_mc_no_idx()
        _selected_mc_model_idx = AppGlobal.instance.get_mc_model_idx()

        // widget
        tv_setting_wifi.text = AppGlobal.instance.getWiFiSSID(this)
        tv_setting_ip.text = AppGlobal.instance.get_local_ip()
        tv_setting_mac.text = AppGlobal.instance.getMACAddress()
        tv_setting_factory.text = AppGlobal.instance.get_factory()
        tv_setting_room.text = AppGlobal.instance.get_room()
        tv_setting_line.text = AppGlobal.instance.get_line()
        tv_setting_mc_model.text = AppGlobal.instance.get_mc_model()
        tv_setting_mc_no1.setText(AppGlobal.instance.get_mc_no1())
        et_setting_mc_serial.setText(AppGlobal.instance.get_mc_serial())

        et_setting_server_ip.setText(AppGlobal.instance.get_server_ip())
        et_setting_port.setText(AppGlobal.instance.get_server_port())

        et_setting_wos_name.setText(AppGlobal.instance.get_wos_name())

        sw_long_touch.isChecked = AppGlobal.instance.get_long_touch()
        sw_message_enable.isChecked = AppGlobal.instance.get_message_enable()
        sw_sound_at_count.isChecked = AppGlobal.instance.get_sound_at_count()
//        sw_without_component.isChecked = AppGlobal.instance.get_without_component()
        sw_view_with_component.isChecked = AppGlobal.instance.get_view_with_component()
        sw_screen_blink_effect.isChecked = AppGlobal.instance.get_screen_blink()
        sw_planned_count_process.isChecked = AppGlobal.instance.get_planned_count_process()
        sw_target_stop_when_downtime.isChecked = AppGlobal.instance.get_target_stop_when_downtime()

        val start_target = AppGlobal.instance.get_start_at_target()
        if (start_target==0) sw_start_at_target_1.isChecked = false
        else sw_start_at_target_1.isChecked = true

        // 깜박임 기능. 0일때는 10으로 초기화
        val remain = if (AppGlobal.instance.get_remain_number()==0) "10" else AppGlobal.instance.get_remain_number().toString()
        et_remain_number.setText(remain)

        if (_selected_blink_color == "") _selected_blink_color = "ff0000"
        blinkColorChange(_selected_blink_color)

        blink_color_f8ad13.setOnClickListener {
            blinkColorChange("f8ad13")
        }
        blink_color_ff0000.setOnClickListener {
            blinkColorChange("ff0000")
        }
        blink_color_0079BA.setOnClickListener {
            blinkColorChange("0079BA")
        }
        blink_color_888888.setOnClickListener {
            blinkColorChange("888888")
        }

        // count setting
        // set hidden value
        _selected_layer_0 = AppGlobal.instance.get_layer_pairs("0")     // 1 layer = 0.5 pair
        _selected_layer_1 = AppGlobal.instance.get_layer_pairs("1")     // 2 layer = 1 pair
        _selected_layer_2 = AppGlobal.instance.get_layer_pairs("2")     // 4 layer = 2 pairs
        _selected_layer_3 = AppGlobal.instance.get_layer_pairs("3")     // 6 layer = 3 pairs
        _selected_layer_4 = AppGlobal.instance.get_layer_pairs("4")     // 8 layer = 4 pairs
        _selected_layer_5 = AppGlobal.instance.get_layer_pairs("5")     // 10 layer = 5 pairs

        // widget
        if (_selected_layer_0 != "") tv_layer_0.text = addPairText(_selected_layer_0)
        if (_selected_layer_1 != "") tv_layer_1.text = addPairText(_selected_layer_1)
        if (_selected_layer_2 != "") tv_layer_2.text = addPairText(_selected_layer_2)
        if (_selected_layer_3 != "") tv_layer_3.text = addPairText(_selected_layer_3)
        if (_selected_layer_4 != "") tv_layer_4.text = addPairText(_selected_layer_4)
        if (_selected_layer_5 != "") tv_layer_5.text = addPairText(_selected_layer_5)

        // target setting
        if (AppGlobal.instance.get_target_type() == "") targetTypeChange("device_per_accumulate")
        else targetTypeChange(AppGlobal.instance.get_target_type())

        tv_shift_1.setText(AppGlobal.instance.get_target_manual_shift("1"))
        tv_shift_2.setText(AppGlobal.instance.get_target_manual_shift("2"))
        tv_shift_3.setText(AppGlobal.instance.get_target_manual_shift("3"))


        // Version
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        val verArr = version.split(".")
        tv_app_version?.text = verArr[verArr.size-3] + "." + verArr[verArr.size-2] + "." + verArr[verArr.size-1]

        cur_app_version = verArr[verArr.size-3].toInt() * 1000000 + verArr[verArr.size-2].toInt() * 1000 + verArr[verArr.size-1].toInt()


        // click listener
        // Tab button
        btn_setting_system.setOnClickListener { tabChange(1) }
        btn_setting_count.setOnClickListener { tabChange(2) }
        btn_setting_target.setOnClickListener { tabChange(3) }
        btn_setting_appinfo.setOnClickListener { tabChange(4) }

        // System setting button listener
        tv_setting_factory.setOnClickListener { fetchDataForFactory() }
        tv_setting_room.setOnClickListener { fetchDataForRoom() }
        tv_setting_line.setOnClickListener { fetchDataForLine() }
        tv_setting_mc_model.setOnClickListener { fetchDataForMCModel() }

        // Count setting button listener
        tv_layer_0.setOnClickListener { fetchPairData("0") }
        tv_layer_1.setOnClickListener { fetchPairData("1") }
        tv_layer_2.setOnClickListener { fetchPairData("2") }
        tv_layer_3.setOnClickListener { fetchPairData("3") }
        tv_layer_4.setOnClickListener { fetchPairData("4") }
        tv_layer_5.setOnClickListener { fetchPairData("5") }

        // Target setting button listener
        img_server_accumulate.setOnClickListener { targetTypeChange("server_per_accumulate") }
        tv_server_accumulate.setOnClickListener { targetTypeChange("server_per_accumulate") }
//        img_server_hourly.setOnClickListener { targetTypeChange("server_per_hourly") }
//        tv_server_hourly.setOnClickListener { targetTypeChange("server_per_hourly") }
        img_server_shifttotal.setOnClickListener { targetTypeChange("server_per_day_total") }
        tv_server_shifttotal.setOnClickListener { targetTypeChange("server_per_day_total") }
//        btn_server_accumulate.setOnClickListener { targetTypeChange("server_per_accumulate") }
//        btn_server_hourly.setOnClickListener { targetTypeChange("server_per_hourly") }
//        btn_server_shifttotal.setOnClickListener { targetTypeChange("server_per_day_total") }
//        btn_server_accumulate.setOnClickListener {
//            Toast.makeText(this, "Not yet supported.", Toast.LENGTH_SHORT).show()
//        }
//        btn_server_hourly.setOnClickListener {
//            Toast.makeText(this, "Not yet supported.", Toast.LENGTH_SHORT).show()
//        }
//        btn_server_shifttotal.setOnClickListener {
//            Toast.makeText(this, "Not yet supported.", Toast.LENGTH_SHORT).show()
//        }
        img_device_accumulate.setOnClickListener { targetTypeChange("device_per_accumulate") }
        tv_device_accumulate.setOnClickListener { targetTypeChange("device_per_accumulate") }
//        img_device_hourly.setOnClickListener { targetTypeChange("device_per_hourly") }
//        tv_device_hourly.setOnClickListener { targetTypeChange("device_per_hourly") }
        img_device_shifttotal.setOnClickListener { targetTypeChange("device_per_day_total") }
        tv_device_shifttotal.setOnClickListener { targetTypeChange("device_per_day_total") }
//        btn_manual_accumulate.setOnClickListener { targetTypeChange("device_per_accumulate") }
//        btn_manual_hourly.setOnClickListener { targetTypeChange("device_per_hourly") }
//        btn_manual_shifttotal.setOnClickListener { targetTypeChange("device_per_day_total") }

        // check server button
        btn_setting_check_server.setOnClickListener {
            checkServer(true)
            var new_ip = et_setting_server_ip.text.toString()
            var old_ip = AppGlobal.instance.get_server_ip()
            if (!new_ip.equals(old_ip)) {
                tv_setting_factory.text = ""
                tv_setting_room.text = ""
                tv_setting_line.text = ""
                tv_setting_mc_model.text = ""
            }
        }

        // Update Start click
        btn_update_start.setOnClickListener {
            startActivity(Intent(this, DownloadActivity::class.java))
        }

        // Save button click
        btn_setting_confirm.setOnClickListener {
            saveSettingData()
        }

        // Cancel button click
        btn_setting_cancel.setOnClickListener { finish() }

        if (AppGlobal.instance.isOnline(this)) btn_wifi_state.isSelected = true
        else btn_wifi_state.isSelected = false

        if (AppGlobal.instance._server_state) btn_server_state.isSelected = true
        else btn_server_state.isSelected = false

        // TODO: TEST
        // 10.10.10.90
        // 49.247.205.235
        // 115.68.227.31
        // 183.81.156.206 : inni
        // 36.66.169.221 (8124)
        if (et_setting_server_ip.text.toString() == "") et_setting_server_ip.setText("115.68.227.31")
        if (et_setting_port.text.toString() == "") et_setting_port.setText("80")
    }

    private fun checkServer(show_toast:Boolean = false) {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/ping.php"
        var params = listOf("" to "")

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            if (show_toast) Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            if (code == "00") {
                btn_server_state.isSelected = true
            } else {
                btn_server_state.isSelected = false
            }
        }, {
            btn_server_state.isSelected = false
            if (show_toast) Toast.makeText(this, getString(R.string.msg_connection_fail), Toast.LENGTH_SHORT).show()
        })
    }

    private fun saveSettingData() {
        // check value
        if (_selected_factory_idx == "" || _selected_room_idx == "" || _selected_line_idx == "" || tv_setting_mac.text.toString().trim() == "") {
            tabChange(1)
            ToastOut(this, R.string.msg_require_info, true)
            return
        }
//        if (_selected_layer_1.trim() == "") {
//            Toast.makeText(this, getString(R.string.msg_select_layer1), Toast.LENGTH_SHORT).show()
//            return
//        }

        if (tv_shift_1.text.toString().trim()=="") {
            tv_shift_1.setText("0")
        }
        if (tv_shift_2.text.toString().trim()=="") {
            tv_shift_2.setText("0")
        }
        if (tv_shift_3.text.toString().trim()=="") {
            tv_shift_3.setText("0")
        }
//        if (_selected_target_type.substring(0, 6) == "device") {
//            if (tv_shift_1.text.toString().trim()=="" || tv_shift_2.text.toString().trim()=="" || tv_shift_3.text.toString().trim()=="") {
//                tabChange(3)
//                ToastOut(this, R.string.msg_require_target_quantity, true)
//                return
//            }
//        }
        val remain_num = if (et_remain_number.text.toString()=="") 10 else et_remain_number.text.toString().toInt()
        if (remain_num < 5 || remain_num > 30) {
            tabChange(2)
            ToastOut(this, R.string.msg_remain_out_of_range, true)
            return
        }

        // setting value
        AppGlobal.instance.set_factory_idx(_selected_factory_idx)
        AppGlobal.instance.set_room_idx(_selected_room_idx)
        AppGlobal.instance.set_line_idx(_selected_line_idx)
        AppGlobal.instance.set_mc_no_idx(_selected_mc_no_idx)
        AppGlobal.instance.set_mc_model_idx(_selected_mc_model_idx)

        AppGlobal.instance.set_factory(tv_setting_factory.text.toString())
        AppGlobal.instance.set_room(tv_setting_room.text.toString())
        AppGlobal.instance.set_line(tv_setting_line.text.toString())
        AppGlobal.instance.set_mc_model(tv_setting_mc_model.text.toString())
        AppGlobal.instance.set_mc_no1(tv_setting_mc_no1.text.toString())
        AppGlobal.instance.set_mc_serial(et_setting_mc_serial.text.toString())

        AppGlobal.instance.set_server_ip(et_setting_server_ip.text.toString())
        AppGlobal.instance.set_server_port(et_setting_port.text.toString())
        AppGlobal.instance.set_long_touch(sw_long_touch.isChecked)
        AppGlobal.instance.set_message_enable(sw_message_enable.isChecked)
        AppGlobal.instance.set_sound_at_count(sw_sound_at_count.isChecked)
//        AppGlobal.instance.set_without_component(sw_without_component.isChecked)
        AppGlobal.instance.set_view_with_component(sw_view_with_component.isChecked)
        AppGlobal.instance.set_planned_count_process(sw_planned_count_process.isChecked)
        AppGlobal.instance.set_target_stop_when_downtime(sw_target_stop_when_downtime.isChecked)

        if (sw_start_at_target_1.isChecked) AppGlobal.instance.set_start_at_target(1)
        else AppGlobal.instance.set_start_at_target(0)

        AppGlobal.instance.set_wos_name(et_setting_wos_name.text.toString())

        AppGlobal.instance.set_screen_blink(sw_screen_blink_effect.isChecked)
        AppGlobal.instance.set_remain_number(remain_num)
        AppGlobal.instance.set_blink_color(_selected_blink_color)

        // count layer
        AppGlobal.instance.set_layer_pairs("0", _selected_layer_0)
        AppGlobal.instance.set_layer_pairs("1", _selected_layer_1)
        AppGlobal.instance.set_layer_pairs("2", _selected_layer_2)
        AppGlobal.instance.set_layer_pairs("3", _selected_layer_3)
        AppGlobal.instance.set_layer_pairs("4", _selected_layer_4)
        AppGlobal.instance.set_layer_pairs("5", _selected_layer_5)

        // target type
        AppGlobal.instance.set_target_type(_selected_target_type)
        AppGlobal.instance.set_target_manual_shift("1", tv_shift_1.text.toString())
        AppGlobal.instance.set_target_manual_shift("2", tv_shift_2.text.toString())
        AppGlobal.instance.set_target_manual_shift("3", tv_shift_3.text.toString())

        // 장비 설정값 저장
        val uri = "/setting1.php"
        var params = listOf(
            "code" to "server",
            "factory_parent_idx" to _selected_factory_idx,
            "factory_idx" to _selected_room_idx,
            "line_idx" to _selected_line_idx,
            "shift_idx" to AppGlobal.instance.get_current_shift_idx(),
            "mac_addr" to tv_setting_mac.text,
            "machine_no" to tv_setting_mc_no1.text.toString(),
            "ip_addr" to tv_setting_ip.text,
            "mc_model" to tv_setting_mc_model.text,
            "mc_serial" to et_setting_mc_serial.text.toString()
        )
        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if(code == "00") {
                ToastOut(this, result.getString("msg"))
                sendAppStartTime()      // 앱 시작을 알림. 결과에 상관없이 종료
                finish()
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun getUpdateVersion() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/version.php"
        var params = listOf(
            "code" to "version",
            "mac_addr" to tv_setting_mac.text
        )
        request(this, url, port, uri, false, false,false, params, { result ->
//        request(this, uri, false, params, { result ->
            val code = result.getString("code")
            if(code == "00") {
                val version = result.getString("version")
                val urlName = result.getString("url")       // http://115.68.227.31
                val pathName = result.getString("path")     // /apk/cutting
                val fileName = result.getString("file")     // cutting-debug_1.2.6.apk

                val verArr = version.split(".")

                if (verArr.count() == 3) {
                    cur_update_version =
                        verArr[verArr.size - 3].toInt() * 1000000 +
                        verArr[verArr.size - 2].toInt() * 1000 +
                        verArr[verArr.size - 1].toInt()
                    tv_update_version?.text = version

                    if (cur_app_version < cur_update_version) {
                        btn_update_start.visibility = View.VISIBLE
                    }

                } else {
                    tv_update_version?.text = "parsing error"
                }
            }
        })
    }

    private fun sendAppStartTime() {
        val now = DateTime()
        val uri = "/setting1.php"
        var params = listOf(
            "code" to "time",
            "mac_addr" to AppGlobal.instance.getMACAddress(),
            "start_time" to now.toString("yyyy-MM-dd HH:mm:ss"))
        request(this, uri, true, params, { result ->
            var code = result.getString("code")
        })
    }

    private fun fetchDataForFactory() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf("code" to "factory_parent")

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00"){
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_factory_idx = lists[c]["idx"] ?: ""
                        tv_setting_factory.text = lists[c]["name"] ?: ""
                        tv_setting_room.text = ""
                        tv_setting_line.text = ""
                    }
                })
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun fetchDataForRoom() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "factory",
            "factory_parent_idx" to _selected_factory_idx)

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_room_idx = lists[c]["idx"] ?: ""
                        tv_setting_room.text = lists[c]["name"] ?: ""
                        tv_setting_line.text = ""
                    }
                })
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun fetchDataForLine() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "line",
            "factory_parent_idx" to _selected_factory_idx,
            "factory_idx" to _selected_room_idx)

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_line_idx = lists[c]["idx"] ?: ""
                        tv_setting_line.text = lists[c]["name"] ?: ""
                    }
                })
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun fetchDataForMCModel() {
        val url = "http://"+ et_setting_server_ip.text.toString()
        val port = et_setting_port.text.toString()
        val uri = "/getlist1.php"
        var params = listOf("code" to "machine_model")

        request(this, url, port, uri, false, false,false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map=hashMapOf(
                        "idx" to item.getString("idx"),
                        "name" to item.getString("name")
                    )
                    lists.add(map)
                    arr.add(item.getString("name"))
                }

                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_mc_model_idx = lists[c]["idx"] ?: ""
                        tv_setting_mc_model.text = lists[c]["name"] ?: ""
                    }
                })
            } else {
                ToastOut(this, result.getString("msg"), true)
            }
        })
    }

    private fun fetchPairData(layer_no: String) {
        var arr: ArrayList<String> = arrayListOf<String>()
        var lists : ArrayList<HashMap<String, String>> = arrayListOf()

        arr.add("0.5 pair")
        arr.add("1 pair")
        lists.add(hashMapOf("pair" to "0.5", "desc" to "0.5 pair"))
        lists.add(hashMapOf("pair" to "1", "desc" to "1 pair"))

        for (i in 2..5) {
            var num = i.toString()
            arr.add(num + " pairs")
            lists.add(hashMapOf("pair" to num, "desc" to num + " pairs"))
        }

        arr.add("None")
        lists.add(hashMapOf("pair" to "", "desc" to ""))

        val intent = Intent(this, PopupSelectList::class.java)
        intent.putStringArrayListExtra("list", arr)
        startActivity(intent, { r, c, m, d ->
            if (r) {
                when (layer_no) {
                    "0" -> {
                        tv_layer_0.text = lists[c]["desc"] ?: ""
                        _selected_layer_0 = lists[c]["pair"] ?: ""
                    }
                    "1" -> {
                        tv_layer_1.text = lists[c]["desc"] ?: ""
                        _selected_layer_1 = lists[c]["pair"] ?: ""
                    }
                    "2" -> {
                        tv_layer_2.text = lists[c]["desc"] ?: ""
                        _selected_layer_2 = lists[c]["pair"] ?: ""
                    }
                    "3" -> {
                        tv_layer_3.text = lists[c]["desc"] ?: ""
                        _selected_layer_3 = lists[c]["pair"] ?: ""
                    }
                    "4" -> {
                        tv_layer_4.text = lists[c]["desc"] ?: ""
                        _selected_layer_4 = lists[c]["pair"] ?: ""
                    }
                    "5" -> {
                        tv_layer_5.text = lists[c]["desc"] ?: ""
                        _selected_layer_5 = lists[c]["pair"] ?: ""
                    }
                }
            }
        })
    }

    private fun tabChange(v : Int) {
        if (tab_pos == v) return
        tab_pos = v
        when (tab_pos) {
            1 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_system.setBackgroundResource(R.color.colorButtonBlue)
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_count.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_target.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_appinfo.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_appinfo.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_system.visibility = View.VISIBLE
                layout_setting_count.visibility = View.GONE
                layout_setting_target.visibility = View.GONE
                layout_setting_appinfo.visibility = View.GONE
            }
            2 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_system.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_count.setBackgroundResource(R.color.colorButtonBlue)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_target.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_appinfo.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_appinfo.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_system.visibility = View.GONE
                layout_setting_count.visibility = View.VISIBLE
                layout_setting_target.visibility = View.GONE
                layout_setting_appinfo.visibility = View.GONE
            }
            3 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_system.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_count.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_target.setBackgroundResource(R.color.colorButtonBlue)
                btn_setting_appinfo.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_appinfo.setBackgroundResource(R.color.colorButtonDefault)
                layout_setting_system.visibility = View.GONE
                layout_setting_count.visibility = View.GONE
                layout_setting_target.visibility = View.VISIBLE
                layout_setting_appinfo.visibility = View.GONE
            }
            4 -> {
                btn_setting_system.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_system.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_count.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_count.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_target.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_setting_target.setBackgroundResource(R.color.colorButtonDefault)
                btn_setting_appinfo.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_setting_appinfo.setBackgroundResource(R.color.colorButtonBlue)
                layout_setting_system.visibility = View.GONE
                layout_setting_count.visibility = View.GONE
                layout_setting_target.visibility = View.GONE
                layout_setting_appinfo.visibility = View.VISIBLE
            }
        }
    }

    private fun targetTypeChange(v : String) {
        if (_selected_target_type == v) return
        when (_selected_target_type) {
            "server_per_accumulate" -> {
                img_server_accumulate.isSelected = false
                tv_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
//            "server_per_hourly" -> {
//                img_server_hourly.isSelected = false
//                tv_server_hourly.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
//            }
            "server_per_day_total" -> {
                img_server_shifttotal.isSelected = false
                tv_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
            "device_per_accumulate" -> {
                img_device_accumulate.isSelected = false
                tv_device_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
//            "device_per_hourly" -> {
//                img_device_hourly.isSelected = false
//                tv_device_hourly.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
//            }
            "device_per_day_total" -> {
                img_device_shifttotal.isSelected = false
                tv_device_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            }
//            "server_per_accumulate" -> btn_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
//            "server_per_hourly" -> btn_server_hourly.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
//            "server_per_day_total" -> btn_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
//            "device_per_accumulate" -> btn_manual_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
//            "device_per_hourly" -> btn_manual_hourly.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
//            "device_per_day_total" -> btn_manual_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
        }
        when (_selected_target_type.substring(0, 6)) {
            "server" -> tv_setting_target_type_server.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            "device" -> tv_setting_target_type_manual.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
        }

        _selected_target_type = v

        // Device에 딸려있는 메뉴의 색상 변경
        if (_selected_target_type.substring(0, 6) != "device") {
            tv_target_per_shift.setTextColor(ContextCompat.getColor(this, R.color.colorDarkGray))
            tv_label_shift1.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            tv_label_shift2.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            tv_label_shift3.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
            tv_shift_1.setBackgroundResource(R.color.colorEditor1Readonly)
            tv_shift_2.setBackgroundResource(R.color.colorEditor1Readonly)
            tv_shift_3.setBackgroundResource(R.color.colorEditor1Readonly)
        } else {
            tv_target_per_shift.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            tv_label_shift1.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_label_shift2.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_label_shift3.setTextColor(ContextCompat.getColor(this, R.color.colorWhite2))
            tv_shift_1.setBackgroundResource(R.color.colorEditor1)
            tv_shift_2.setBackgroundResource(R.color.colorEditor1)
            tv_shift_3.setBackgroundResource(R.color.colorEditor1)
        }

        when (_selected_target_type) {
            "server_per_accumulate" -> {
                img_server_accumulate.isSelected = true
                tv_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
//            "server_per_hourly" -> {
//                img_server_hourly.isSelected = true
//                tv_server_hourly.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
//            }
            "server_per_day_total" -> {
                img_server_shifttotal.isSelected = true
                tv_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
            "device_per_accumulate" -> {
                img_device_accumulate.isSelected = true
                tv_device_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
//            "device_per_hourly" -> {
//                img_device_hourly.isSelected = true
//                tv_device_hourly.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
//            }
            "device_per_day_total" -> {
                img_device_shifttotal.isSelected = true
                tv_device_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            }
//            "server_per_accumulate" -> btn_server_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
//            "server_per_hourly" -> btn_server_hourly.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
//            "server_per_day_total" -> btn_server_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
//            "device_per_accumulate" -> btn_manual_accumulate.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
//            "device_per_hourly" -> btn_manual_hourly.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
//            "device_per_day_total" -> btn_manual_shifttotal.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
        }
        when (_selected_target_type.substring(0, 6)) {
            "server" -> tv_setting_target_type_server.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
            "device" -> tv_setting_target_type_manual.setTextColor(ContextCompat.getColor(this, R.color.colorOrange))
        }
    }

    private fun blinkColorChange(v : String) {
        when (_selected_blink_color) {
            "f8ad13" -> blink_color_f8ad13.text = ""
            "ff0000" -> blink_color_ff0000.text = ""
            "0079BA" -> blink_color_0079BA.text = ""
            "888888" -> blink_color_888888.text = ""
        }
        _selected_blink_color = v
        when (_selected_blink_color) {
            "f8ad13" -> blink_color_f8ad13.text = "V"
            "ff0000" -> blink_color_ff0000.text = "V"
            "0079BA" -> blink_color_0079BA.text = "V"
            "888888" -> blink_color_888888.text = "V"
        }
    }
}