package com.suntech.iot.cuttingmc

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseFragment
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.DBHelperForComponent
import com.suntech.iot.cuttingmc.db.DBHelperForDownTime
import com.suntech.iot.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_3.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import kotlin.math.floor

class CountViewFragment : BaseFragment() {

    private var is_loop: Boolean = false

    private var _color_list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _list_for_db: ArrayList<HashMap<String, String>> = arrayListOf()

//    private var _total_target = 0

    private var _list_for_wos_adapter: ListWosAdapter? = null
    private var _list_for_wos: java.util.ArrayList<java.util.HashMap<String, String>> = arrayListOf()

    private var _selected_component_pos = -1

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            computeCycleTime()
            viewWosData()
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop = true
        fetchColorData()     // Get Color
//        computeCycleTime()
//        updateView()
        startHandler()
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(_need_to_refresh)
        is_loop = false
    }

    override fun onSelected() {
        activity.tv_title?.visibility = View.VISIBLE

        if ((activity as MainActivity).countViewType == 1) {
            ll_total_count.visibility = View.VISIBLE
            ll_component_count.visibility = View.GONE

            tv_wos_name.text = AppGlobal.instance.get_wos_name() + " NO :  "    // 하단 WOS name 변경

        } else {
            ll_total_count.visibility = View.GONE
            ll_component_count.visibility = View.VISIBLE

            tv_count_wos_name.text = AppGlobal.instance.get_wos_name()  // 표 안의 WOS name 변경

            fetchFilterWos()    // 기존 선택된 WOS 가 있으면 로드해서 화면에 표시한다.
        }

        if (AppGlobal.instance.get_view_with_component()) {
            ll_charts.visibility = View.VISIBLE
            ll_total_charts.visibility = View.GONE
        } else {
            ll_charts.visibility = View.GONE
            ll_total_charts.visibility = View.VISIBLE
            initOEEGraph()
        }

        // Worker info
        val no = AppGlobal.instance.get_worker_no()
        val name = AppGlobal.instance.get_worker_name()
        if (no == "" || name == "") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
//            (activity as MainActivity).changeFragment(0)
        }
        viewWosData()
//        computeCycleTime()
    }

    override fun initViews() {
        super.initViews()

        val version = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
        val verArr = version.split(".")
        tv_app_version2?.text = "Pv" + verArr[verArr.size-2] + "." + verArr[verArr.size-1]

        _list_for_wos_adapter = ListWosAdapter(activity, _list_for_wos)
        lv_wos_info2.adapter = _list_for_wos_adapter

        // Total count view
        tv_count_view_target.text = "0"
        tv_count_view_actual.text = "0"
        tv_count_view_ratio.text = "0%"
//        tv_count_view_time.text = "0H"

        // Component count view
        tv_component_view_target.text = "0"
        tv_component_view_actual.text = "0"
        tv_component_view_ratio.text = "0%"

        if (AppGlobal.instance.get_compo_sort_key() == "BALANCE") {
            tv_btn_size2.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite2))
            tv_btn_balance2.setTextColor(ContextCompat.getColor(activity, R.color.colorButtonOrange))
        } else {
            tv_btn_size2.setTextColor(ContextCompat.getColor(activity, R.color.colorButtonOrange))
            tv_btn_balance2.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite2))
        }

        tv_btn_size2.setOnClickListener {
            AppGlobal.instance.set_compo_sort_key("SIZE")
            tv_btn_size2.setTextColor(ContextCompat.getColor(activity, R.color.colorButtonOrange))
            tv_btn_balance2.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite2))
            outputWosList()
        }
        tv_btn_balance2.setOnClickListener {
            AppGlobal.instance.set_compo_sort_key("BALANCE")
            tv_btn_size2.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite2))
            tv_btn_balance2.setTextColor(ContextCompat.getColor(activity, R.color.colorButtonOrange))
            outputWosList()
        }

        // Total count view
//        btn_start.setOnClickListener {
//            //            (activity as MainActivity).saveRowData("barcode", value)
//            Toast.makeText(activity, "Not yet available", Toast.LENGTH_SHORT).show()
//        }

        // End Work button
        btn_exit.setOnClickListener {
            Toast.makeText(activity, "Not yet available", Toast.LENGTH_SHORT).show()
//            val work_idx = "" + AppGlobal.instance.get_work_idx()
//            if (work_idx == "") {
//                Toast.makeText(activity, getString(R.string.msg_not_start_work), Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//            val alertDialogBuilder = AlertDialog.Builder(activity)
//            alertDialogBuilder.setTitle(getString(R.string.notice))
//            alertDialogBuilder
//                .setMessage(getString(R.string.msg_exit_shift))
//                .setCancelable(false)
//                .setPositiveButton(getString(R.string.confirm), DialogInterface.OnClickListener { dialog, id ->
//                    (activity as MainActivity).changeFragment(0)
//                    (activity as MainActivity).endWork()
//                })
//                .setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialog, id ->
//                    dialog.cancel()
//                })
//            val alertDialog = alertDialogBuilder.create()
//            alertDialog.show()
        }

        btn_init_actual.setOnClickListener {
            val work_idx = "" + AppGlobal.instance.get_work_idx()
            if (work_idx == "") {
                Toast.makeText(activity, getString(R.string.msg_not_start_work), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val alertDialogBuilder = AlertDialog.Builder(activity)
            alertDialogBuilder.setTitle(getString(R.string.notice))
            alertDialogBuilder
                .setMessage("Reset Actual?")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.confirm), DialogInterface.OnClickListener { dialog, id ->
                    AppGlobal.instance.set_current_shift_actual_cnt(0)
                    updateView()
                })
                .setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                })
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
        // button click
        tv_btn_wos_count.setOnClickListener {
            if (AppGlobal.instance.get_view_with_component() == false) {
                (activity as MainActivity).ToastOut(activity, R.string.msg_component_not_available, true)
                return@setOnClickListener
            }
            (activity as MainActivity).countViewType = 2
            onSelected()
//            ll_total_count.visibility = View.GONE
//            ll_component_count.visibility = View.VISIBLE
        }
        ll_btn_wos_count.setOnClickListener {
            if (AppGlobal.instance.get_view_with_component() == false) {
                (activity as MainActivity).ToastOut(activity, R.string.msg_component_not_available, true)
                return@setOnClickListener
            }
            (activity as MainActivity).countViewType = 2
            onSelected()
//            ll_total_count.visibility = View.GONE
//            ll_component_count.visibility = View.VISIBLE
        }

        // Component count view buttons
        btn_total_count_view.setOnClickListener {
            (activity as MainActivity).countViewType = 1
            onSelected()
//            ll_total_count.visibility = View.VISIBLE
//            ll_component_count.visibility = View.GONE
        }
        btn_select_component.setOnClickListener {
            if (AppGlobal.instance.get_view_with_component() == false) {
                (activity as MainActivity).ToastOut(activity, R.string.msg_component_not_available, true)
                return@setOnClickListener
            }
            val intent = Intent(activity, ComponentInfoActivity::class.java)
            getBaseActivity().startActivity(intent, { r, c, m, d ->
                if (r && d != null) {
                    (activity as MainActivity).countViewType = 2
                    (activity as MainActivity).changeFragment(1)

                    val wosno = d!!["wosno"]!!
                    val styleno = d["styleno"]!!.toString()
                    val model = d["model"]!!.toString()
                    val component = d["component"]!!.toString()
                    val size = d["size"]!!.toString()
                    val target = d["target"]!!.toString()
                    val actual = d["actual"]!!.toString()

//                        val styleno = d["ct"]!!.toInt()
//                        val pieces_info = AppGlobal.instance.get_pieces_info()
                    viewWosData()
                    fetchFilterWos()

                    (activity as MainActivity).startComponent(wosno, styleno, model, component, size, target, actual)
//                        (activity as MainActivity).startNewProduct(idx, pieces_info, cycle_time, model, article, material_way, component)
                }
            })
        }
        viewWosData()
//        updateView()      // onResume() 에서 함
//        fetchColorData()    // Get Color
        fetchFilterWos()    // 기존 선택된 WOS 가 있으면 로드해서 화면에 표시한다.
    }

    fun viewWosData() {
        // WOS INFO
        // 하단 bottom
        tv_wosno.text = AppGlobal.instance.get_compo_wos()
        tv_model.text = AppGlobal.instance.get_compo_model()
        tv_component.text = AppGlobal.instance.get_compo_component()
        tv_style_no.text = AppGlobal.instance.get_compo_style()

        // 우측 쪽창
        tv_count_view_csize.text = AppGlobal.instance.get_compo_size()
        tv_count_view_clayer.text = AppGlobal.instance.get_compo_layer()
        tv_count_view_ctarget.text = "" + AppGlobal.instance.get_compo_target()
    }

    // 해당 시간에만 카운트 값을 변경하기 위한 변수
    // 타이밍 값을 미리 계산해 놓는다.
//    var _current_cycle_time = 86400     // 1일

    // Total target을 표시할 사이클 타임을 계산한다.
//    private fun computeCycleTime() {
//        force_count = true
//        val target = AppGlobal.instance.get_current_shift_target_cnt()
//        if (target == null || target == "") {
//            // 작업 시간이 아니므로 값을 초기화 한다.
//            _current_cycle_time = 15
//            _total_target = 0
//            return
//        }
//
//        val total_target = target.toInt()
//        val target_type = AppGlobal.instance.get_target_type()
//
//        if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
//            val shift_total_time = AppGlobal.instance.get_current_shift_total_time()
//            _current_cycle_time = if (total_target > 0) (shift_total_time / total_target) else 0
//            if (_current_cycle_time < 5) _current_cycle_time = 5        // 너무 자주 리프레시 되는걸 막기위함
//
//        } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
//            _current_cycle_time = 86400
//
//        } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
//            _current_cycle_time = 86400
//        }
//    }
//    private fun computeCycleTime() {
//
//        force_count = true
//
//        var target_type = AppGlobal.instance.get_target_type()
//
//        if (target_type=="server_per_hourly" || target_type=="server_per_accumulate" || target_type=="server_per_day_total") {
//            fetchServerTarget()
//
//        } else if (target_type=="device_per_hourly" || target_type=="device_per_accumulate" || target_type=="device_per_day_total") {
//            var total_target = 0
//
//            var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
//            if (item != null) {
//                when (item["shift_idx"]) {
//                    "1" -> total_target = AppGlobal.instance.get_target_manual_shift("1").toInt()
//                    "2" -> total_target = AppGlobal.instance.get_target_manual_shift("2").toInt()
//                    "3" -> total_target = AppGlobal.instance.get_target_manual_shift("3").toInt()
//                }
//            } else {
//                // 작업 시간이 아니므로 값을 초기화 한다.
//                _current_cycle_time = 15
//                _total_target = 0
//                return
//            }
//
//            if (target_type=="device_per_accumulate") {
//                val shift_total_time = AppGlobal.instance.get_current_shift_total_time()
//                val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()
//
//                _current_cycle_time = if (total_target > 0) (shift_total_time / total_target) else 0
//                if (_current_cycle_time < 5) _current_cycle_time = 5        // 너무 자주 리프레시 되는걸 막기위함
//
//            } else if (target_type=="device_per_hourly") {
//                _current_cycle_time = 86400
//
//            } else if (target_type=="device_per_day_total") {
//                _current_cycle_time = 86400
//            }
//        }
//    }

//    private fun countTarget() {
////        val now_time = DateTime()
////        val current_shift_time = AppGlobal.instance.get_current_shift_time()
////        val work_stime = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))
////        val work_etime = OEEUtil.parseDateTime(current_shift_time?.getString("work_etime"))
//
////        var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
////        if (item == null) {
////            activity.tv_title.setText("No shift")
////        } else {
////            activity.tv_title.setText(item["shift_name"].toString() + "   " + item["available_stime"].toString() + " - " + item["available_etime"].toString())
////        }
//
//        var target_type = AppGlobal.instance.get_target_type()
//
//        if (target_type=="server_per_hourly" || target_type=="server_per_accumulate" || target_type=="server_per_day_total") {
//            fetchServerTarget()
//
//        } else if (target_type=="device_per_hourly" || target_type=="device_per_accumulate" || target_type=="device_per_day_total") {
//            _total_target = 0
//
//            var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
//            if (item != null) {
//                when (item["shift_idx"]) {
//                    "1" -> _total_target = AppGlobal.instance.get_target_manual_shift("1").toInt()
//                    "2" -> _total_target = AppGlobal.instance.get_target_manual_shift("2").toInt()
//                    "3" -> _total_target = AppGlobal.instance.get_target_manual_shift("3").toInt()
//                }
//            }
//
//            if (target_type=="device_per_hourly") {
//
//            } else if (target_type=="device_per_accumulate") {
//                val shift_total_time = AppGlobal.instance.get_current_shift_total_time()
//                val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()
//                val cycle_time = if (_total_target > 0) (shift_total_time / _total_target) else shift_now_time
//                val target = if (cycle_time > 0) (shift_now_time / cycle_time).toInt()+1 else 1
//                _total_target = if (target > _total_target) _total_target else target
//
//Log.e("countTarget", "shift_total_time="+shift_total_time)      // 휴식 시간을 뺀 총 근무시간 (초)
//Log.e("countTarget", "shift_now_time="+shift_now_time)          // 현재 작업이 진행된 시간 (초)
//Log.e("countTarget", "cycle_time="+cycle_time)
//
//            } else if (target_type=="device_per_day_total") {
//                Log.e("shift time", "target="+_total_target)
//            }
//            updateView()
//        }
//    }

    // 무조건 계산해야 할경우 true
//    var force_count = true

//    private fun countTarget() {
//        if (_current_cycle_time >= 86400 && force_count == false) return
//
//        val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()
//        if (shift_now_time <= 0 && force_count == false) return
//
//        if (shift_now_time % _current_cycle_time == 0 || force_count) {
////            Log.e("countTarget", "Count refresh start ===========> shift_now_time = " + shift_now_time)
////            Log.e("test -----", "shift_now_time % _current_cycle_time = " + shift_now_time % _current_cycle_time)
////            Log.e("test -----", "force_count = " + force_count)
//            force_count = false
//
//            var target = AppGlobal.instance.get_current_shift_target_cnt()
//            if (target == null || target == "") target = "0"
//
//            var total_target = target.toInt()
//
//            val target_type = AppGlobal.instance.get_target_type()
//
//            if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
//                val target = (shift_now_time / _current_cycle_time).toInt() + 1
//                _total_target = if (target > total_target) total_target else target
//
//            } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
//                val shift_total_time = AppGlobal.instance.get_current_shift_total_time()    // 현시프트의 총 시간
//                val target_per_hour = total_target.toFloat() / shift_total_time.toFloat() * 3600    // 시간당 만들어야 할 갯수
//                val target = ((shift_now_time / 3600).toInt() * target_per_hour + target_per_hour).toInt()    // 현 시간에 만들어야 할 갯수
//                _total_target = if (target > total_target) total_target else target
//
//                Log.e("test -----", "target_per_hour = " + target_per_hour + ", _total_target = " + _total_target + ", _current_cycle_time = " + _current_cycle_time)
//
//            } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
//                _total_target = total_target
//            }


//            if (target_type=="server_per_hourly" || target_type=="server_per_accumulate" || target_type=="server_per_day_total") {
//                fetchServerTarget()
//
//            } else if (target_type=="device_per_hourly" || target_type=="device_per_accumulate" || target_type=="device_per_day_total") {
//                var total_target = 0
//
//                var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
//                if (item != null) {
//                    when (item["shift_idx"]) {
//                        "1" -> total_target = AppGlobal.instance.get_target_manual_shift("1").toInt()
//                        "2" -> total_target = AppGlobal.instance.get_target_manual_shift("2").toInt()
//                        "3" -> total_target = AppGlobal.instance.get_target_manual_shift("3").toInt()
//                    }
//                }
//
//                if (target_type=="device_per_accumulate") {
////                    val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()
//                    val target = (shift_now_time / _current_cycle_time).toInt() + 1
//                    _total_target = if (target > total_target) total_target else target
//
//                } else if (target_type=="device_per_hourly") {
//                    val shift_total_time = AppGlobal.instance.get_current_shift_total_time()    // 현시프트의 총 시간
//                    val target_per_hour = total_target.toFloat() / shift_total_time.toFloat() * 3600    // 시간당 만들어야 할 갯수
//                    val target = ((shift_now_time / 3600).toInt() * target_per_hour + target_per_hour).toInt()    // 현 시간에 만들어야 할 갯수
//                    _total_target = if (target > total_target) total_target else target
//
//                    Log.e("test -----", "target_per_hour = " + target_per_hour + ", _total_target = " + _total_target + ", _current_cycle_time = " + _current_cycle_time)
//
//                } else if (target_type=="device_per_day_total") {
//                    _total_target = total_target
//                }
//            }
//            Log.e("countTarget", "Count refresh end ===========> shift_now_time = " + shift_now_time)
//        }
//    }

    // 값에 변화가 생길때만 화면을 리프레쉬 하기 위한 변수
    var _current_target_count = -1
    var _current_actual_count = -1
    var _current_compo_target_count = -1
    var _current_compo_actual_count = -1

    private fun updateView() {

        // 기본 출력
        if ((activity as MainActivity).countViewType == 1) {
            tv_current_time?.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        } else if ((activity as MainActivity).countViewType == 2) {
            tv_component_time?.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        }

        // 현재 시프트
        val shift_time = AppGlobal.instance.get_current_shift_time()
        if (shift_time == null) {
            resetScreen()
            initOEEGraph()
            return
        }

//        val shift_idx = shift_time["shift_idx"].toString()

        val work_idx = AppGlobal.instance.get_work_idx()

        if (work_idx=="") {
            resetScreen()
            initOEEGraph()
            return
        }

        val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입
        val target_type_6 = target_type.substring(0, 6)
        val start_at_target = AppGlobal.instance.get_start_at_target()  // 타겟의 시작을 0부터 할지 1부터 할지
        val shift_target = AppGlobal.instance.get_current_shift_target()    // 시프트의 총 타겟

        // 서버에서 받아온 타겟값이 정상인지 체크
        if (target_type_6 == "server") {
            if (shift_target == 0) {
                if ((DateTime().millis / 1000) % 10 == 0L) {    // 10초마다 출력
                    (activity as MainActivity).ToastOut(activity, R.string.msg_no_target_value_from_server)
                }
                resetScreen()
                return
            }
        }

        // 현재 시프트의 기본 정보
        val work_stime = shift_time["work_stime"].toString()
        val work_etime = shift_time["work_etime"].toString()
        val shift_stime = OEEUtil.parseDateTime(work_stime)
        val shift_etime = OEEUtil.parseDateTime(work_etime)

        // 설정되어 있는 휴식 시간
        val _planned1_stime = OEEUtil.parseDateTime(shift_time["planned1_stime_dt"].toString())
        val _planned1_etime = OEEUtil.parseDateTime(shift_time["planned1_etime_dt"].toString())
        val _planned2_stime = OEEUtil.parseDateTime(shift_time["planned2_stime_dt"].toString())
        val _planned2_etime = OEEUtil.parseDateTime(shift_time["planned2_etime_dt"].toString())


        var total_target = 0                                                            // 현시점까지 타겟
//        val total_target = AppGlobal.instance.get_current_shift_target()                // 현시점까지 타겟
        val total_actual = AppGlobal.instance.get_current_shift_actual_cnt()            // 현시점까지 액추얼

        val now = DateTime()        // 현재
        val now_millis = now.millis

        if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
            val one_item_sec = AppGlobal.instance.get_current_maketime_per_piece()
//            Log.e("one_item_sec", "one_item_sec=" + one_item_sec)
            if (one_item_sec != 0F) {
                val d1 = AppGlobal.instance.compute_time(shift_stime, now, _planned1_stime, _planned1_etime)
                val d2 = AppGlobal.instance.compute_time(shift_stime, now, _planned2_stime, _planned2_etime)

//                val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()
//                val target = (shift_now_time / one_item_sec).toInt() + start_at_target
//                total_target = if (target > shift_target) shift_target else target

                val work_time = ((now.millis - shift_stime.millis) / 1000) - d1 - d2 - start_at_target
//                Log.e("one_item_sec", "work_time=" + ((now.millis - shift_stime.millis) / 1000).toString() + ", d1=" + d1 + ", d2=" + d2)

                total_target = (work_time / one_item_sec).toInt() + start_at_target
            }
        } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
            total_target = shift_target
        } else {
            total_target = shift_target
        }

        if (AppGlobal.instance.get_target_stop_when_downtime()) {
            // Downtime
            val down_db = DBHelperForDownTime(activity)
            val down_list = down_db.gets()
            var down_target = 0
            down_list?.forEach { item ->
                down_target += item["target"].toString().toInt()
            }
            total_target -= down_target
        }


//        countTarget()

        // Total count view 화면 정보 표시
//        val total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

        var refresh_graph = false

        if (_current_target_count != total_target || _current_actual_count != total_actual) {
            _current_target_count = total_target
            _current_actual_count = total_actual
            refresh_graph = true

            var ratio = 0
            var ratio_txt = "N/A"

            if (total_target > 0) {
                ratio = (total_actual.toFloat() / total_target.toFloat() * 100).toInt()
                if (ratio > 999) ratio = 999
                ratio_txt = "" + ratio + "%"
            }

            tv_count_view_target?.text = "" + total_target
            tv_count_view_actual?.text = "" + total_actual
            tv_count_view_ratio?.text = ratio_txt

//            var maxEnumber = 0
            var color_code = "ffffff"

//            for (i in 0..(_list.size - 1)) {
//                val snumber = _list[i]["snumber"]?.toInt() ?: 0
//                val enumber = _list[i]["enumber"]?.toInt() ?: 0
////                if (maxEnumber < enumber) maxEnumber = enumber
//                if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
//            }
            for (i in (_color_list.size - 1) downTo 0 step 1) {
                val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                if (snumber <= ratio) {
                    color_code = _color_list[i]["color_code"].toString()
                    break
                }
            }
            tv_count_view_target?.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_actual?.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_ratio?.setTextColor(Color.parseColor("#" + color_code))
        }

        if (!refresh_graph) {
            if ((activity as MainActivity)._availability_rate == 0F) refresh_graph = true
        }

        // 콤포넌트 사용안하면 OEE 그래프를 그린다.
        // refresh_graph 값이 true 일때만 그린다.
        if (!AppGlobal.instance.get_view_with_component() && refresh_graph) {
            // 시프트 시작/끝
            val shift_stime_millis = shift_stime.millis
            val shift_etime_millis = shift_etime.millis

            // 휴식시간
            val planned1_stime_millis = _planned1_stime.millis
            val planned1_etime_millis = _planned1_etime.millis
            val planned2_stime_millis = _planned2_stime.millis
            val planned2_etime_millis = _planned2_stime.millis

            val planned1_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned1_stime_millis, planned1_etime_millis)
            val planned2_time = AppGlobal.instance.compute_time_millis(shift_stime_millis, now_millis, planned2_stime_millis, planned2_etime_millis)

            // 현재까지의 작업시간
            val work_time = ((now_millis - shift_stime_millis) / 1000) - planned1_time - planned2_time

            // Downtime
            var down_time = 0
            var down_target = 0

            var down_db = DBHelperForDownTime(activity)
            val down_list = down_db.gets()
            down_list?.forEach { item ->
                down_time += item["real_millis"].toString().toInt()
                down_target += item["target"].toString().toInt()
            }

            // Availability Check
            // availity = (현시점까지 작업시간 - 다운타임 시간) / 현시점까지 작업시간(초)
            val availability = (work_time-down_time).toFloat() / work_time
            val availability_rate = floor(availability * 1000) / 10

            if ((activity as MainActivity)._availability_rate != availability_rate) {
                (activity as MainActivity)._availability_rate = availability_rate

                Log.e("refreshOEEGraph", "oee graph redraw : work_time = " + work_time + ", down_time = " + down_time + ", planned1_time = " + planned1_time + ", planned2_time = " + planned2_time)
                Log.e("refreshOEEGraph", "oee graph redraw : availability = " + (availability * 100) + "%")

                val availability_int = floor(availability_rate).toInt()
                var availability_color_code = "ff0000"

                for (i in 0..(_color_list.size - 1)) {
                    val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                    if (snumber <= availability_int) availability_color_code = _color_list[i]["color_code"].toString()
                }
                tv_availability_rate.text = "" + availability_int + "%"
                availability_progress.progress = availability_int
                availability_progress.progressStartColor = Color.parseColor("#" + availability_color_code)
                availability_progress.progressEndColor = Color.parseColor("#" + availability_color_code)
            }

            // Performance Check
            // performance = 현재까지의 Actual / (현시점까지 작업시간 - 다운타임 시간)의 타겟
            val performance = if (AppGlobal.instance.get_target_stop_when_downtime()) {
                if (total_target > 0) total_actual.toFloat() / total_target else 0F     // 이미 down_target을 빼고 온 값이므로 또 빼지 않기 위함
            } else {
                if (total_target-down_target > 0) total_actual.toFloat() / (total_target-down_target) else 0F
            }

            val performance_rate = floor(performance * 1000) / 10

            if ((activity as MainActivity)._performance_rate != performance_rate) {

                // 100% 넘어가면 푸시발송
                if (performance_rate >= 100.0f) {
                    if ((activity as MainActivity)._performance_rate < 100.0f) {
                        Log.e("refreshOEEGraph", "Best performance Push send...")
                        (activity as MainActivity).sendPush("SYS: PERFORMANCE")
                    }
                }
                (activity as MainActivity)._performance_rate = performance_rate

                Log.e("refreshOEEGraph", "oee graph redraw : performance = " + (performance * 100) + "%")

                val performance_int = floor(performance_rate).toInt()
                var performance_color_code = "ff0000"

                for (i in 0..(_color_list.size - 1)) {
                    val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                    if (snumber <= performance_int) performance_color_code = _color_list[i]["color_code"].toString()
                }
                tv_performance_rate.text = "" + performance_int + "%"
                performance_progress.progress = performance_int
                performance_progress.progressStartColor = Color.parseColor("#" + performance_color_code)
                performance_progress.progressEndColor = Color.parseColor("#" + performance_color_code)
            }

            // Quality Check
            // qulity = (현시점의 actual - defective) / Actual
//                val db = DBHelperForDesign(activity)
//                var defective_count = db.sum_defective_count()
//                if (defective_count==null || defective_count<0) defective_count = 0
            val defective_count = 0

            val quality = if(total_actual!=0) (total_actual-defective_count).toFloat() / total_actual else 0F
            val quality_rate = floor(quality * 1000) / 10

            if ((activity as MainActivity)._quality_rate != quality_rate) {
                (activity as MainActivity)._quality_rate = quality_rate

                Log.e("refreshOEEGraph", "oee graph redraw : quality = " + (quality*100) + "%")

                val quality_int = floor(quality_rate).toInt()
                var quality_color_code = "ff0000"

                for (i in 0..(_color_list.size - 1)) {
                    val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                    if (snumber <= quality_int) quality_color_code = _color_list[i]["color_code"].toString()
                }
//            OEEUtil.LogWrite("Qual value = "+quality_int+", Qual color = "+quality_color_code, "Color")
                tv_quality_rate.text = "" + quality_int + "%"
                quality_progress.progress = quality_int
                quality_progress.progressStartColor = Color.parseColor("#" + quality_color_code)
                quality_progress.progressEndColor = Color.parseColor("#" + quality_color_code)
            }

            // OEE Check
            var oee = availability_rate * performance_rate * quality_rate / 1000F
            var oee_rate = floor(oee) / 10

            if ((activity as MainActivity)._oee_rate != oee_rate) {
                (activity as MainActivity)._oee_rate = oee_rate

                Log.e("refreshOEEGraph", "oee graph redraw : OEE = " + (oee/10) + "%")

                val oee_int = floor(oee_rate).toInt()
                var oee_color_code = "ff0000"

                for (i in 0..(_color_list.size - 1)) {
                    val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                    if (snumber <= oee_int) oee_color_code = _color_list[i]["color_code"].toString()
                }
//            OEEUtil.LogWrite("OEE value = "+oee_int+", OEE color = "+oee_color_code, "Color")
                tv_oee_rate.text = "" + oee_int + "%"
                oee_progress.progress = oee_int
                oee_progress.progressStartColor = Color.parseColor("#" + oee_color_code)
                oee_progress.progressEndColor = Color.parseColor("#" + oee_color_code)
            }
        }


        // Component count 정보 표시
        var db = DBHelperForComponent(activity)

        if ((activity as MainActivity).countViewType == 1) {

            var ratio = 1
            var ratio_txt = "N/A"

            val item = db.get(work_idx)
            if (item != null && item.toString() != "") {
                val target = item["target"].toString().toInt()
                val actual = (item["actual"].toString().toInt())
                _current_compo_target_count = target
                _current_compo_actual_count = actual

                if (target > 0) {
                    ratio = (actual.toFloat() / target.toFloat() * 100).toInt()
                    ratio_txt = if (ratio > 999) "999%" else "" + ratio + "%"
                    if (ratio > 100) ratio = 100
                }

                tv_count_view_cactual?.text = "" + actual
                tv_count_view_crate?.text = ratio_txt
                line_progress1?.progress = ratio

//                    var maxEnumber = 0
                var color_code = "ff0000"

//                    for (i in 0..(_list.size - 1)) {
//                        val snumber = _list[i]["snumber"]?.toInt() ?: 0
//                        val enumber = _list[i]["enumber"]?.toInt() ?: 0
//                        color_code = _list[i]["color_code"].toString()
////                        if (maxEnumber < enumber) maxEnumber = enumber
//                        if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
//                    }
                for (i in (_color_list.size - 1) downTo 0 step 1) {
                    val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                    if (snumber <= ratio) {
                        color_code = _color_list[i]["color_code"].toString()
                        break
                    }
                }
                line_progress1?.progressStartColor = Color.parseColor("#" + color_code)
                line_progress1?.progressEndColor = Color.parseColor("#" + color_code)
            }

        } else if ((activity as MainActivity).countViewType == 2) {

            var ratio = 0
            var ratio_txt = "N/A"

            var db = DBHelperForComponent(activity)

            // component count view 화면을 보고 있을 경우 처리

            val item = db.get(work_idx)
            if (item != null && item.toString() != "") {
                val target = item["target"].toString().toInt()
                val actual = (item["actual"].toString().toInt())
                _current_compo_target_count = target
                _current_compo_actual_count = actual

                if (target > 0) {
                    ratio = (actual.toFloat() / target.toFloat() * 100).toInt()
                    if (ratio > 999) ratio = 999
                    ratio_txt = "" + ratio + "%"
                }

                tv_component_view_target?.text = "" + target
                tv_component_view_actual?.text = "" + actual
                tv_component_view_ratio?.text = ratio_txt

//                    var maxEnumber = 0
                var color_code = "ffffff"

//                    for (i in 0..(_list.size - 1)) {
//                        val snumber = _list[i]["snumber"]?.toInt() ?: 0
//                        val enumber = _list[i]["enumber"]?.toInt() ?: 0
//                        if (maxEnumber < enumber) maxEnumber = enumber
//                        if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
//                    }
                for (i in (_color_list.size - 1) downTo 0 step 1) {
                    val snumber = _color_list[i]["snumber"]?.toInt() ?: 0
                    if (snumber <= ratio) {
                        color_code = _color_list[i]["color_code"].toString()
                        break
                    }
                }
                tv_component_view_target?.setTextColor(Color.parseColor("#" + color_code))
                tv_component_view_actual?.setTextColor(Color.parseColor("#" + color_code))
                tv_component_view_ratio?.setTextColor(Color.parseColor("#" + color_code))

                // 리스트에서 첫번째 항목이 선택되어 있으면 같이 업데이트 한다.
                if (_selected_component_pos >= 0) {
//                        var item = _list_for_wos.get(_selected_component_pos)
                    _list_for_wos[_selected_component_pos]["target"] = "" + target
                    _list_for_wos[_selected_component_pos]["actual"] = "" + actual
                    _list_for_wos[_selected_component_pos]["balance"] =
                        "" + (target - actual).toString()
                    _list_for_wos_adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun resetScreen() {
        if ((activity as MainActivity).countViewType == 1) {
            tv_count_view_ctarget?.text = "0"
            tv_count_view_cactual?.text = "0"
            tv_count_view_crate?.text = "N/A"
            line_progress1.progress = 0
            _current_compo_target_count = -1
            _current_compo_actual_count = -1
        } else if ((activity as MainActivity).countViewType == 2) {
            tv_component_view_target.text = "0"
            tv_component_view_actual.text = "0"
            tv_component_view_ratio.text = "0%"

            _current_compo_target_count = -1
            _current_compo_actual_count = -1

            _selected_component_pos = -1
            _list_for_wos.removeAll(_list_for_wos)
            _list_for_wos_adapter?.select(_selected_component_pos)
            _list_for_wos_adapter?.notifyDataSetChanged()
        }
    }

    private fun initOEEGraph() {
        (activity as MainActivity)._availability_rate = 0F
        (activity as MainActivity)._quality_rate = 0F
        (activity as MainActivity)._performance_rate = 0F
        (activity as MainActivity)._oee_rate = 0F

        // Server charts
        oee_progress?.progress = 0
        availability_progress?.progress = 0
        performance_progress?.progress = 0
        quality_progress?.progress = 0
        tv_oee_rate?.text = "0%"
        tv_availability_rate?.text = "0%"
        tv_performance_rate?.text = "0%"
        tv_quality_rate?.text = "0%"
    }


//    private fun fetchServerTarget() {
////        val work_idx = AppGlobal.instance.get_work_idx()
////        var db = SimpleDatabaseHelper(activity)
////        val row = db.get(work_idx)
//
//        val uri = "/getlist1.php"
//        var params = listOf(
//            "code" to "target",
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
//            "date" to DateTime().toString("yyyy-MM-dd"),
//            "mac_addr" to AppGlobal.instance.getMACAddress()
//        )
//
//        getBaseActivity().request(activity, uri, false, params, { result ->
//            var code = result.getString("code")
//            var msg = result.getString("msg")
//            if(code == "00"){
//                var target_type = AppGlobal.instance.get_target_type()
//
//                if (target_type=="server_per_hourly") _total_target = result.getString("target").toInt()
//                else if (target_type=="server_per_accumulate") _total_target = result.getString("targetsum").toInt()
//                else if (target_type=="server_per_day_total") _total_target = result.getString("daytargetsum").toInt()
//                else _total_target = result.getString("targetsum").toInt()
//
//                updateView()
//            }else{
//                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
//            }
//        })
//    }

//    var handle_cnt = 0
    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
                checkBlink()
//                if (handle_cnt++ > 15) {
//                    handle_cnt = 0
//                    computeCycleTime()
//                }
                startHandler()
            }
        }, 1000)
    }

    var blink_cnt = 0
    private fun checkBlink() {
        var is_toggle = false
        if (AppGlobal.instance.get_screen_blink()) {
            if (_current_compo_target_count != -1 || _current_compo_actual_count != -1) {
                if (_current_compo_target_count - _current_compo_actual_count <= AppGlobal.instance.get_remain_number()) {
                    blink_cnt = 1 - blink_cnt
                    is_toggle = true
                }
            }
        }
        if (is_toggle && blink_cnt==1) {
            if ((activity as MainActivity).countViewType == 1) {
                ll_btn_wos_count.setBackgroundColor(Color.parseColor("#" + AppGlobal.instance.get_blink_color()))
            } else {
                ll_component_count.setBackgroundColor(Color.parseColor("#" + AppGlobal.instance.get_blink_color()))
            }
        } else {
            if ((activity as MainActivity).countViewType == 1) {
                ll_btn_wos_count.setBackgroundResource(R.color.colorBlack2)
            } else {
                ll_component_count.setBackgroundResource(R.color.colorBackground)
            }
        }
//        if (AppGlobal.instance.get_current_shift_idx().toInt() > 0) {
//            if (AppGlobal.instance.get_screen_blink()) {
//                if (_total_target - AppGlobal.instance.get_current_shift_actual_cnt() <= AppGlobal.instance.get_remain_number()) {
//                    if (blink_cnt == 0) {
//                        blink_cnt = 1
//                        ll_total_count.setBackgroundResource(R.color.colorOrange)
//                    } else {
//                        blink_cnt = 0
//                        ll_total_count.setBackgroundResource(R.color.colorBackground)
//                    }
//                }
//            }
//        }
    }

    // Get Color code
    private fun fetchColorData() {
        _color_list.removeAll(_color_list)
        var list = AppGlobal.instance.get_color_code()
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map=hashMapOf(
                "idx" to item.getString("idx"),
                "snumber" to item.getString("snumber"),
                "enumber" to item.getString("enumber"),
                "color_name" to item.getString("color_name"),
                "color_code" to item.getString("color_code")
            )
            _color_list.add(map)
        }
    }

    private fun outputWosList() {

        // 정렬
        val sort_key = AppGlobal.instance.get_compo_sort_key()
        var sortedList = _list_for_wos.sortedWith(compareBy({ it.get(if (sort_key=="BALANCE") "balance" else "size").toString().toInt() }))

        _list_for_wos.removeAll(_list_for_wos)
        _selected_component_pos = -1

        val wosno = AppGlobal.instance.get_compo_wos()
        val size = AppGlobal.instance.get_compo_size()
        val component = AppGlobal.instance.get_compo_component()

        if (size == "") {
            _list_for_wos.addAll(sortedList)
        } else {
            // 선택된 항목을 맨앞으로 뺀다.
            for (i in 0..(sortedList.size - 1)) {
                val item = sortedList.get(i)
                if (wosno == item["wosno"] && size == item["size"] && component == item["component"]) {
                    _list_for_wos.add(item)
                    _selected_component_pos = 0
                    break
                }
            }
            for (i in 0..(sortedList.size - 1)) {
                val item = sortedList.get(i)
                if (wosno != item["wosno"] || size != item["size"] || component != item["component"]) {
                    _list_for_wos.add(item)
                }
            }
        }
        _list_for_wos_adapter?.select(_selected_component_pos)
        _list_for_wos_adapter?.notifyDataSetChanged()
    }

    private fun fetchFilterWos() {

        _list_for_wos.removeAll(_list_for_wos)
        _selected_component_pos = -1
        _list_for_wos_adapter?.select(-1)
        _list_for_wos_adapter?.notifyDataSetChanged()

        val def_wosno = AppGlobal.instance.get_compo_wos().trim()
        val def_size = AppGlobal.instance.get_compo_size().trim()

        if (def_wosno == "" || def_size == "") return

        var db = DBHelperForComponent(activity)

        val uri = "/wos.php"
        val params = listOf(
                "code" to "wos",
                "wosno" to def_wosno)

        getBaseActivity().request(activity, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                _selected_component_pos = -1
                _list_for_wos.removeAll(_list_for_wos)

                var list = result.getJSONArray("item")
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var actual = "0"

                    val row = db.get(item.getString("wosno"), item.getString("size"), item.getString("c_name"))
                    if (row != null) actual = row["actual"].toString()

                    val balance = item.getString("target").toInt() - actual.toInt()

                    var map = hashMapOf(
                        "wosno" to item.getString("wosno"),
                        "styleno" to item.getString("styleno"),
                        "model" to item.getString("model"),
                        "size" to item.getString("size"),
                        "target" to item.getString("target"),
                        "actual" to actual,
                        "balance" to balance.toString(),
                        "c_idx" to item.getString("c_idx"),
                        "c_code" to item.getString("c_code"),
                        "component" to item.getString("c_name")
                    )
                    _list_for_wos.add(map)
                }
                outputWosList()

//                // 선택된 항목을 맨앞으로 뺀다.
//                for (i in 0..(list.length() - 1)) {
//                    val item = list.getJSONObject(i)
//                    val wosno = item.getString("wosno")
//                    val size = item.getString("size")
//
//                    if (wosno == def_wosno && size == def_size) {
//                        val row = db.get(wosno, size)
//                        var actual = "0"
//                        if (row != null) actual = row["actual"].toString()
//
//                        var map = hashMapOf(
//                            "wosno" to item.getString("wosno"),
//                            "styleno" to item.getString("styleno"),
//                            "model" to item.getString("model"),
//                            "size" to item.getString("size"),
//                            "target" to item.getString("target"),
//                            "actual" to actual
//                        )
//                        _list_for_wos.add(map)
//                        _selected_component_pos = 0
//                        break;
//                    }
//                }
//
//                for (i in 0..(list.length() - 1)) {
//                    val item = list.getJSONObject(i)
//                    val wosno = item.getString("wosno")
//                    val size = item.getString("size")
//
//                    if (wosno != def_wosno || size != def_size) {
//                        val row = db.get(wosno, size)
//                        var actual = "0"
//                        if (row != null) actual = row["actual"].toString()
//
//                        var map = hashMapOf(
//                            "wosno" to item.getString("wosno"),
//                            "styleno" to item.getString("styleno"),
//                            "model" to item.getString("model"),
//                            "size" to item.getString("size"),
//                            "target" to item.getString("target"),
//                            "actual" to actual
//                        )
//                        _list_for_wos.add(map)
//                    }
//                }
//                _list_for_wos_adapter?.select(_selected_component_pos)
//                _list_for_wos_adapter?.notifyDataSetChanged()
            } else {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private class ListWosAdapter(context: Context, list: java.util.ArrayList<java.util.HashMap<String, String>>) : BaseAdapter() {

        private var _list: java.util.ArrayList<java.util.HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null
        private var _selected_index = -1

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        fun select(index:Int) { _selected_index = index }
        fun getSelected(): Int { return _selected_index }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_component_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            val balance = Integer.parseInt(_list[position]["target"]) - Integer.parseInt(_list[position]["actual"])

            val cut = _list[position]["wosno"].toString().substringBeforeLast("-")
            if (cut != null) {
                vh.tv_item_wosno.text = cut
            } else {
                vh.tv_item_wosno.text = _list[position]["wosno"]
            }

//            vh.tv_item_wosno.text = _list[position]["wosno"]
            vh.tv_item_model.text = _list[position]["model"]
            vh.tv_item_component.text = _list[position]["component"]
            vh.tv_item_size.text = _list[position]["size"]
            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = _list[position]["actual"]
            vh.tv_item_balance.text = balance.toString()

            if (_selected_index == position) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_component.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
            } else if (balance <= 0) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_component.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
            } else {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_component.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_wosno: TextView
            val tv_item_model: TextView
            val tv_item_component: TextView
            val tv_item_size: TextView
            val tv_item_target: TextView
            val tv_item_actual: TextView
            val tv_item_balance: TextView

            init {
                this.tv_item_wosno = row?.findViewById<TextView>(R.id.tv_item_wosno) as TextView
                this.tv_item_model = row?.findViewById<TextView>(R.id.tv_item_model) as TextView
                this.tv_item_component = row?.findViewById<TextView>(R.id.tv_item_component) as TextView
                this.tv_item_size = row?.findViewById<TextView>(R.id.tv_item_size) as TextView
                this.tv_item_target = row?.findViewById<TextView>(R.id.tv_item_target) as TextView
                this.tv_item_actual = row?.findViewById<TextView>(R.id.tv_item_actual) as TextView
                this.tv_item_balance = row?.findViewById<TextView>(R.id.tv_item_balance) as TextView
            }
        }
    }
}