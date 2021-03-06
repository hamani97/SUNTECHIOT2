package com.suntech.iot.sewing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.suntech.iot.sewing.ComponentInfoActivity
import com.suntech.iot.sewing.MainActivity
import com.suntech.iot.sewing.R
import com.suntech.iot.sewing.base.BaseFragment
import com.suntech.iot.sewing.common.AppGlobal
import com.suntech.iot.sewing.db.DBHelperForComponent
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_3.*
import kotlinx.android.synthetic.main.layout_top_menu.*
import org.joda.time.DateTime
import kotlin.math.ceil

class CountViewFragment : BaseFragment() {

    private var is_loop: Boolean = false

    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0

    private var _list_for_wos_adapter: ListWosAdapter? = null
    private var _list_for_wos: java.util.ArrayList<java.util.HashMap<String, String>> = arrayListOf()

    private var _selected_component_pos = -1

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            computeCycleTime()
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
        computeCycleTime()
        fetchColorData()     // Get Color
        updateView()
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

            // 선택된 제품 표시 (TRIM or STITCH)
            if (AppGlobal.instance.get_count_type() == "trim") {
                tv_kind_name.text = "TRIM  :  "
                tv_kind_qty.text = "" + (activity as MainActivity).trim_qty
                tv_kind_pairs.text = "" + (activity as MainActivity).trim_pairs

//                tv_trim_qty.text = "TRIM  :  " + AppGlobal.instance.get_trim_qty()
//                tv_trim_qty.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
//                tv_stitch_qty.text = "STITCH  :  0"
//                tv_stitch_qty.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))

                // bottom view
                tv_trim_qty_bottom.text = AppGlobal.instance.get_trim_qty()
                tv_trim_pairs_bottom.text = AppGlobal.instance.get_trim_pairs()

                tv_trim_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
                tv_trim_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))

                tv_stitch_qty_bottom.text = "0"
                tv_stitch_delay_time_bottom.text = "0"
                tv_stitch_pairs_bottom.text = "0"

                tv_stitch_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))
                tv_stitch_delay_time_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))
                tv_stitch_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))

            } else if (AppGlobal.instance.get_count_type() == "stitch") {
                tv_kind_name.text = "STITCH  :  "
                tv_kind_qty.text = "" + (activity as MainActivity).stitch_qty
                tv_kind_pairs.text = "" + (activity as MainActivity).stitch_pairs

//                tv_stitch_qty.text = "STITCH  :  " + AppGlobal.instance.get_stitch_qty_start() + "~" + AppGlobal.instance.get_stitch_qty_end()
//                tv_stitch_qty.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
//                tv_trim_qty.text = "TRIM  :  0"
//                tv_trim_qty.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))

                // bottom view
                tv_stitch_qty_bottom.text = AppGlobal.instance.get_stitch_qty_start() + " ~ " + AppGlobal.instance.get_stitch_qty_end()
                tv_stitch_delay_time_bottom.text = AppGlobal.instance.get_stitch_delay_time()
                tv_stitch_pairs_bottom.text = AppGlobal.instance.get_stitch_pairs()

                tv_stitch_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
                tv_stitch_delay_time_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))
                tv_stitch_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorOrange))

                tv_trim_qty_bottom.text = "0"
                tv_trim_pairs_bottom.text= "0"

                tv_trim_qty_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))
                tv_trim_pairs_bottom.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite))
            }

        } else {
            ll_total_count.visibility = View.GONE
            ll_component_count.visibility = View.VISIBLE

            tv_count_wos_name.text = AppGlobal.instance.get_wos_name()      // 표 안의 WOS name

            fetchFilterWos()    // 기존 선택된 WOS 가 있으면 로드해서 화면에 표시한다.
        }

        // Worker info
        val no = AppGlobal.instance.get_worker_no()
        val name = AppGlobal.instance.get_worker_name()
        if (no == "" || name == "") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
//            (activity as MainActivity).changeFragment(0)
        }
        computeCycleTime()
    }

    override fun initViews() {
        super.initViews()

        _list_for_wos_adapter = ListWosAdapter(activity, _list_for_wos)
        lv_wos_info2.adapter = _list_for_wos_adapter

        // Init widget
        // Total count view
        tv_count_view_target.text = "0"
        tv_count_view_actual.text = "0"
        tv_count_view_ratio.text = "0%"

        // Component count view
        tv_component_view_target.text = "0"
        tv_component_view_actual.text = "0"
        tv_component_view_ratio.text = "0%"

        // Server charts
        oee_progress.progress = 0
        availability_progress.progress = 0
        performance_progress.progress = 0
        quality_progress.progress = 0
        tv_oee_rate.text = "0%"
        tv_availability_rate.text = "0%"
        tv_performance_rate.text = "0%"
        tv_quality_rate.text = "0%"

        if (AppGlobal.instance.get_compo_sort_key() == "BALANCE") {
            tv_btn_size2.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite2))
            tv_btn_balance2.setTextColor(ContextCompat.getColor(activity, R.color.colorButtonOrange))
        } else {
            tv_btn_size2.setTextColor(ContextCompat.getColor(activity, R.color.colorButtonOrange))
            tv_btn_balance2.setTextColor(ContextCompat.getColor(activity, R.color.colorWhite2))
        }

        // Click event
        // Button click in Count view
        btn_go_repair_mode.setOnClickListener {
            (activity as MainActivity).countViewMode = 2
            ll_count_mode.visibility = View.GONE
            ll_repair_mode.visibility = View.VISIBLE
        }
        btn_go_count_mode.setOnClickListener {
            (activity as MainActivity).countViewMode = 1
            ll_count_mode.visibility = View.VISIBLE
            ll_repair_mode.visibility = View.GONE
        }
        tv_btn_wos_count.setOnClickListener {
            (activity as MainActivity).countViewType = 2
            onSelected()
        }
        ll_btn_wos_count.setOnClickListener {
            (activity as MainActivity).countViewType = 2
            onSelected()
        }

        // Button click in Component count view
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
        btn_total_count_view.setOnClickListener {
            (activity as MainActivity).countViewType = 1
            onSelected()
        }
        btn_select_component.setOnClickListener {
            val intent = Intent(activity, ComponentInfoActivity::class.java)
            getBaseActivity().startActivity(intent, { r, c, m, d ->
                if (r && d != null) {
                    (activity as MainActivity).countViewType = 2
                    (activity as MainActivity).changeFragment(1)

                    val wosno = d!!["wosno"]!!
                    val styleno = d["styleno"]!!.toString()
                    val model = d["model"]!!.toString()
                    val size = d["size"]!!.toString()
                    val target = d["target"]!!.toString()
                    val actual = d["actual"]!!.toString()

//                        val styleno = d["ct"]!!.toInt()
//                        val pieces_info = AppGlobal.instance.get_pieces_info()
                    viewWosData()
                    fetchFilterWos()

                    (activity as MainActivity).startComponent(wosno, styleno, model, size, target, actual)
////                        (activity as MainActivity).startNewProduct(idx, pieces_info, cycle_time, model, article, material_way, component)
                }
            })
        }
        viewWosData()
        fetchColorData()    // Get Color
        fetchFilterWos()    // 기존 선택된 WOS 가 있으면 로드해서 화면에 표시한다.
    }

    fun viewWosData() {
        // WOS INFO
//        tv_wosno.text = AppGlobal.instance.get_compo_wos()
//        tv_model.text = AppGlobal.instance.get_compo_model()
//        tv_component.text = AppGlobal.instance.get_compo_component()
//        tv_style_no.text = AppGlobal.instance.get_compo_style()

        // 우측 쪽창
        tv_count_view_csize.text = AppGlobal.instance.get_compo_size()
        tv_count_view_clayer.text = AppGlobal.instance.get_compo_layer()
        tv_count_view_ctarget.text = "" + AppGlobal.instance.get_compo_target()
    }

    // 해당 시간에만 카운트 값을 변경하기 위한 변수
    // 타이밍 값을 미리 계산해 놓는다.
    var _current_cycle_time = 86400     // 1일

    // Total target을 표시할 사이클 타임을 계산한다.
    private fun computeCycleTime() {
        force_count = true
        val target = AppGlobal.instance.get_current_shift_target_cnt()
        if (target == null || target == "") {
            // 작업 시간이 아니므로 값을 초기화 한다.
            _current_cycle_time = 15
            _total_target = 0
            return
        }

        val total_target = target.toInt()
        val target_type = AppGlobal.instance.get_target_type()

        if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
            val shift_total_time = AppGlobal.instance.get_current_shift_total_time()
            _current_cycle_time = if (total_target > 0) (shift_total_time / total_target) else 0
            if (_current_cycle_time < 5) _current_cycle_time = 5        // 너무 자주 리프레시 되는걸 막기위함

        } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
            _current_cycle_time = 86400

        } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
            _current_cycle_time = 86400
        }
    }

    // 무조건 계산해야 할경우 true
    var force_count = true

    private fun countTarget() {
        if (_current_cycle_time >= 86400 && force_count == false) return

        val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()
        if (shift_now_time <= 0 && force_count == false) return

        if (shift_now_time % _current_cycle_time == 0 || force_count) {
            force_count = false

            var target = AppGlobal.instance.get_current_shift_target_cnt()
            if (target == null || target == "") target = "0"

            var total_target = target.toInt()

            val target_type = AppGlobal.instance.get_target_type()

            if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
                val target = (shift_now_time / _current_cycle_time).toInt() + 1
                _total_target = if (target > total_target) total_target else target

            } else if (target_type=="device_per_hourly" || target_type=="server_per_hourly") {
                val shift_total_time = AppGlobal.instance.get_current_shift_total_time()    // 현시프트의 총 시간
                val target_per_hour = total_target.toFloat() / shift_total_time.toFloat() * 3600    // 시간당 만들어야 할 갯수
                val target = ((shift_now_time / 3600).toInt() * target_per_hour + target_per_hour).toInt()    // 현 시간에 만들어야 할 갯수
                _total_target = if (target > total_target) total_target else target

                Log.e("test -----", "target_per_hour = " + target_per_hour + ", _total_target = " + _total_target + ", _current_cycle_time = " + _current_cycle_time)

            } else if (target_type=="device_per_day_total" || target_type=="server_per_day_total") {
                _total_target = total_target
            }
        }
    }

    // 값에 변화가 생길때만 화면을 리프레쉬 하기 위한 변수
    var _current_target_count = -1
    var _current_actual_count = -1
    var _current_compo_target_count = -1
    var _current_compo_actual_count = -1

    private fun updateView() {
        // 콤포넌트 기능을 사용하는지 체크
        if ((activity as MainActivity).countViewType == 1) {
            if (AppGlobal.instance.get_with_component()) {
                ll_charts.visibility = View.VISIBLE
                ll_server_charts.visibility = View.GONE
            } else {
                ll_charts.visibility = View.GONE
                ll_server_charts.visibility = View.VISIBLE
                drawChartView2()
            }
            if (AppGlobal.instance.get_count_type() == "trim") {
                val pairs = AppGlobal.instance.get_trim_pairs()
                var pairs_str = ""
                when (pairs) {
                    "1/2" -> pairs_str = "/2"
                    "1/4" -> pairs_str = "/4"
                    "1/8" -> pairs_str = "/8"
                }
                tv_kind_qty.text = "" + (activity as MainActivity).trim_qty
                tv_kind_pairs.text = "" + (activity as MainActivity).trim_pairs + pairs_str
            } else if (AppGlobal.instance.get_count_type() == "stitch") {
                val pairs = AppGlobal.instance.get_stitch_pairs()
                var pairs_str = ""
                when (pairs) {
                    "1/2" -> pairs_str = "/2"
                    "1/4" -> pairs_str = "/4"
                    "1/8" -> pairs_str = "/8"
                }
                tv_kind_qty.text = "" + (activity as MainActivity).stitch_qty
                tv_kind_pairs.text = "" + (activity as MainActivity).stitch_pairs + pairs_str
            }
        }

        countTarget()

        // Total count view 화면 정보 표시
        val total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

        // 값에 변화가 있을때만 갱신
        if (_current_target_count != _total_target || _current_actual_count != total_actual) {
            _current_target_count = _total_target
            _current_actual_count = total_actual
            var ratio = 0
            var ratio_txt = "N/A"

            if (_total_target > 0) {
                ratio = (total_actual.toFloat() / _total_target.toFloat() * 100).toInt()
                if (ratio > 999) ratio = 999
                ratio_txt = "" + ratio + "%"
            }

            tv_count_view_target.text = "" + _total_target
            tv_count_view_actual.text = "" + total_actual
            tv_count_view_ratio.text = ratio_txt

            var maxEnumber = 0
            var color_code = "ffffff"

            for (i in 0..(_list.size - 1)) {
                val snumber = _list[i]["snumber"]?.toInt() ?: 0
                val enumber = _list[i]["enumber"]?.toInt() ?: 0
                if (maxEnumber < enumber) maxEnumber = enumber
                if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
            }
            tv_count_view_target.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_actual.setTextColor(Color.parseColor("#" + color_code))
            tv_count_view_ratio.setTextColor(Color.parseColor("#" + color_code))
        }

        // Component 사용안함이면 시간만 표시하고 리턴한다.
        if (AppGlobal.instance.get_with_component() == false) {
            tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
            return
        }

        // Component count 정보 표시
        var db = DBHelperForComponent(activity)
        val work_idx = AppGlobal.instance.get_work_idx()

        if ((activity as MainActivity).countViewType == 1) {
            tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

            if (work_idx=="") {
                tv_count_view_ctarget.text = "0"
                tv_count_view_cactual.text = "0"
                tv_count_view_crate.text = "N/A"
                line_progress1.progress = 0
                _current_compo_target_count = -1
                _current_compo_actual_count = -1
            } else {
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

                    tv_count_view_cactual.text = "" + actual
                    tv_count_view_crate.text = ratio_txt
                    line_progress1.progress = ratio

                    var maxEnumber = 0
                    var color_code = "ff0000"

                    for (i in 0..(_list.size - 1)) {
                        val snumber = _list[i]["snumber"]?.toInt() ?: 0
                        val enumber = _list[i]["enumber"]?.toInt() ?: 0
                        color_code = _list[i]["color_code"].toString()
                        if (maxEnumber < enumber) maxEnumber = enumber
                        if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
                    }
                    line_progress1.progressStartColor = Color.parseColor("#" + color_code)
                    line_progress1.progressEndColor = Color.parseColor("#" + color_code)
                }
            }

        } else if ((activity as MainActivity).countViewType == 2) {
            tv_component_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

            if (work_idx=="") {
                tv_component_view_target.text = "0"
                tv_component_view_actual.text = "0"
                tv_component_view_ratio.text = "0%"

                _current_compo_target_count = -1
                _current_compo_actual_count = -1

                _selected_component_pos = -1
                _list_for_wos.removeAll(_list_for_wos)
                _list_for_wos_adapter?.select(_selected_component_pos)
                _list_for_wos_adapter?.notifyDataSetChanged()

            } else {
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

                    tv_component_view_target.text = "" + target
                    tv_component_view_actual.text = "" + actual
                    tv_component_view_ratio.text = ratio_txt

                    var maxEnumber = 0
                    var color_code = "ffffff"

                    for (i in 0..(_list.size - 1)) {
                        val snumber = _list[i]["snumber"]?.toInt() ?: 0
                        val enumber = _list[i]["enumber"]?.toInt() ?: 0
                        if (maxEnumber < enumber) maxEnumber = enumber
                        if (snumber <= ratio && enumber >= ratio) color_code = _list[i]["color_code"].toString()
                    }
                    tv_component_view_target.setTextColor(Color.parseColor("#" + color_code))
                    tv_component_view_actual.setTextColor(Color.parseColor("#" + color_code))
                    tv_component_view_ratio.setTextColor(Color.parseColor("#" + color_code))

                    // 리스트에서 첫번째 항목이 선택되어 있으면 같이 업데이트 한다.
                    if (_selected_component_pos >= 0) {
                        var item = _list_for_wos.get(_selected_component_pos)
                        _list_for_wos[_selected_component_pos]["target"] = "" + target
                        _list_for_wos[_selected_component_pos]["actual"] = "" + actual
                        _list_for_wos[_selected_component_pos]["balance"] = "" + (target - actual).toString()
                        _list_for_wos_adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    // 값에 변화가 생길때만 화면을 리프레쉬 하기 위한 변수
    var _availability = ""
    var _performance = ""
    var _quality = ""

    private fun drawChartView2() {
        var availability = AppGlobal.instance.get_availability()
        var performance = AppGlobal.instance.get_performance()
        var quality = AppGlobal.instance.get_quality()

        if (availability=="") availability = "0"
        if (performance=="") performance = "0"
        if (quality=="") quality = "0"

        // 값에 변화가 있을때만 갱신
        if (_availability != availability || _performance != performance || _quality != quality) {
            _availability = availability
            _performance = performance
            _quality = quality

            Log.e("drawChartView2", "oee graph redraw")

            var oee = availability.toFloat() * performance.toFloat() * quality.toFloat() / 10000.0f
            var oee2 = String.format("%.1f", oee)
            oee2 = oee2.replace(",", ".")//??

            tv_oee_rate.text = oee2 + "%"
            tv_availability_rate.text = availability + "%"
            tv_performance_rate.text = performance + "%"
            tv_quality_rate.text = quality + "%"

            val oee_int = oee.toInt()
            val availability_int = ceil(availability.toFloat()).toInt()
            val performance_int = ceil(performance.toFloat()).toInt()
            val quality_int = ceil(quality.toFloat()).toInt()

            oee_progress.progress = oee_int
            availability_progress.progress = availability_int
            performance_progress.progress = performance_int
            quality_progress.progress = quality_int

            var oee_color_code = "ff0000"
            var availability_color_code = "ff0000"
            var performance_color_code = "ff0000"
            var quality_color_code = "ff0000"

            for (i in 0..(_list.size - 1)) {
                val snumber = _list[i]["snumber"]?.toInt() ?: 0
                val enumber = _list[i]["enumber"]?.toInt() ?: 0
                if (snumber <= oee_int && enumber >= oee_int) oee_color_code = _list[i]["color_code"].toString()
                if (snumber <= availability_int && enumber >= availability_int) availability_color_code = _list[i]["color_code"].toString()
                if (snumber <= performance_int && enumber >= performance_int) performance_color_code = _list[i]["color_code"].toString()
                if (snumber <= quality_int && enumber >= quality_int) quality_color_code = _list[i]["color_code"].toString()
            }

            oee_progress.progressStartColor = Color.parseColor("#" + oee_color_code)
            oee_progress.progressEndColor = Color.parseColor("#" + oee_color_code)

            availability_progress.progressStartColor = Color.parseColor("#" + availability_color_code)
            availability_progress.progressEndColor = Color.parseColor("#" + availability_color_code)

            performance_progress.progressStartColor = Color.parseColor("#" + performance_color_code)
            performance_progress.progressEndColor = Color.parseColor("#" + performance_color_code)

            quality_progress.progressStartColor = Color.parseColor("#" + quality_color_code)
            quality_progress.progressEndColor = Color.parseColor("#" + quality_color_code)
        }
    }

    var handle_cnt = 0
    fun startHandler() {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
                checkBlink()
                if (handle_cnt++ > 15) {
                    handle_cnt = 0
                    computeCycleTime()
                }
                startHandler()
            }
        }, 1000)
    }

    var blink_cnt = 0
    private fun checkBlink() {
        if (AppGlobal.instance.get_with_component() == false) return

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
    }

    // Get Color code
    private fun fetchColorData() {
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
            _list.add(map)
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

        if (size == "") {
            _list_for_wos.addAll(sortedList)
        } else {
            // 선택된 항목을 맨앞으로 뺀다.
            for (i in 0..(sortedList.size - 1)) {
                val item = sortedList.get(i)
                if (wosno == item["wosno"] && size == item["size"]) {
                    _list_for_wos.add(item)
                    _selected_component_pos = 0
                    break
                }
            }
            for (i in 0..(sortedList.size - 1)) {
                val item = sortedList.get(i)
                if (wosno != item["wosno"] || size != item["size"]) {
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

                    val row = db.get(item.getString("wosno"), item.getString("size"))
                    if (row != null) actual = row["actual"].toString()

                    val balance = item.getString("target").toInt() - actual.toInt()

                    var map = hashMapOf(
                        "wosno" to item.getString("wosno"),
                        "styleno" to item.getString("styleno"),
                        "model" to item.getString("model"),
                        "size" to item.getString("size"),
                        "target" to item.getString("target"),
                        "actual" to actual,
                        "balance" to balance.toString()
                    )
                    _list_for_wos.add(map)
                }
                outputWosList()

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

            vh.tv_item_wosno.text = _list[position]["wosno"]
            vh.tv_item_model.text = _list[position]["model"]
            vh.tv_item_size.text = _list[position]["size"]
            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = _list[position]["actual"]
            vh.tv_item_balance.text = balance.toString()

            if (_selected_index == position) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
            } else if (balance <= 0) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_complete_text_color))
            } else {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
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
            val tv_item_size: TextView
            val tv_item_target: TextView
            val tv_item_actual: TextView
            val tv_item_balance: TextView

            init {
                this.tv_item_wosno = row?.findViewById<TextView>(R.id.tv_item_wosno) as TextView
                this.tv_item_model = row?.findViewById<TextView>(R.id.tv_item_model) as TextView
                this.tv_item_size = row?.findViewById<TextView>(R.id.tv_item_size) as TextView
                this.tv_item_target = row?.findViewById<TextView>(R.id.tv_item_target) as TextView
                this.tv_item_actual = row?.findViewById<TextView>(R.id.tv_item_actual) as TextView
                this.tv_item_balance = row?.findViewById<TextView>(R.id.tv_item_balance) as TextView
            }
        }
    }
}