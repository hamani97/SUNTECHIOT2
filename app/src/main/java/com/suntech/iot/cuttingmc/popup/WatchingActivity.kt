package com.suntech.iot.cuttingmc.popup

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import com.suntech.iot.cuttingmc.R
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.DBHelperForComponent
import com.suntech.iot.cuttingmc.db.DBHelperForDownTime
import com.suntech.iot.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.activity_watching.*
import org.joda.time.DateTime
import java.util.*

class WatchingActivity : BaseActivity() {

    private var _list_design: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _list_down: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watching)
        initView()
        start_timer()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel_timer()
    }

    private fun initView() {
        tv_watching.movementMethod = ScrollingMovementMethod()

        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }
    }

    private fun updateView() {
        var value = "Conpenent Data : \n\n"

        var db = DBHelperForComponent(this)
        _list_design = db.gets() ?: _list_design

        for (i in 0..(_list_design.size - 1)) {
            val item = _list_design[i]
            value = value + item?.toString() + "\n"
        }

        value += "\n"
        value += "Downtime Data : \n\n"

        var _db = DBHelperForDownTime(this)
        _list_down = _db.gets() ?: _list_down

        // Downtime
        var down_time = 0
        var down_target = 0

        _list_down?.forEach { item ->
            value = value + item?.toString() + "\n"

            down_time += item["real_millis"].toString().toInt()
            down_target += item["target"].toString().toInt()
        }

        val shift_time = AppGlobal.instance.get_current_shift_time()

        if (shift_time != null) {

            val now = DateTime()
            val now_millis = now.millis

            // 현시점까지 타겟
            var total_target = 0

            // 현재까지의 Actual
            var total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

            val shift_target = AppGlobal.instance.get_current_shift_target()    // 시프트의 총 타겟
            val start_at_target = AppGlobal.instance.get_start_at_target()  // 타겟의 시작을 0부터 할지 1부터 할지

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

            val target_type = AppGlobal.instance.get_target_type()          // setting menu 메뉴에서 선택한 타입
            if (target_type=="device_per_accumulate" || target_type=="server_per_accumulate") {
                val one_item_sec = AppGlobal.instance.get_current_maketime_per_piece()
                if (one_item_sec != 0F) {
                    val d1 = AppGlobal.instance.compute_time(shift_stime, now, _planned1_stime, _planned1_etime)
                    val d2 = AppGlobal.instance.compute_time(shift_stime, now, _planned2_stime, _planned2_etime)

                    val work_time = ((now.millis - shift_stime.millis) / 1000) - d1 - d2 - start_at_target

                    total_target = (work_time / one_item_sec).toInt() + start_at_target
                }
            } else {
                total_target = shift_target
            }

            if (AppGlobal.instance.get_target_stop_when_downtime()) {
                // Downtime
                val down_list = _db.gets()
                var down_target = 0
                down_list?.forEach { item ->
                    down_target += item["target"].toString().toInt()
                }
                total_target -= down_target
            }

//            for (i in 0..((_list_design?.size ?: 1) - 1)) {
//
//                val item = _list_design?.get(i)
//                val target2 = item?.get("target").toString().toInt()
//
//                total_target += target2   // 현재 계산된 카운트를 더한다.
//            }

            value += "\n"
            value += "[ OEE Graph ] \n\n"


            // 시프트 시작/끝
            val shift_stime_millis = shift_stime.millis
            val shift_etime_millis = shift_etime.millis

            // 휴식시간
            val planned1_stime_millis = _planned1_stime.millis
            val planned1_etime_millis = _planned1_etime.millis
            val planned2_stime_millis = _planned2_stime.millis
            val planned2_etime_millis = _planned2_etime.millis

            val planned1_time = AppGlobal.instance.compute_time_millis(
                shift_stime_millis,
                now_millis,
                planned1_stime_millis,
                planned1_etime_millis
            )
            val planned2_time = AppGlobal.instance.compute_time_millis(
                shift_stime_millis,
                now_millis,
                planned2_stime_millis,
                planned2_etime_millis
            )

            // 현재까지의 작업시간
            val work_time = ((now_millis - shift_stime_millis) / 1000) - planned1_time - planned2_time


            // Availability Check
            val availability = (work_time - down_time).toFloat() / work_time

            value += "Availibility = ($work_time - $down_time) / $work_time = $availability\n"


            val performance = if (total_target - down_target > 0) total_actual.toFloat() / (total_target - down_target) else 0F

            value += "Performance = $total_actual / ($total_target - $down_target) = $performance\n"


            // Quality Check
//            var defective_count = db.sum_defective_count()
//            if (defective_count==null || defective_count<0) defective_count = 0
            val defective_count = 0

            val quality = if (total_actual != 0) (total_actual - defective_count).toFloat() / total_actual else 0F

            value += "Quality = ($total_actual - $defective_count) / $total_actual = $quality\n"

            value += "\n"
            value += "\n"

            value += "- Availibility = (현시점까지 작업시간 - 다운타임 시간) / 현시점까지 작업시간(초)\n"
            value += "- Performance = 현재까지의 Actual / (현시점까지 타겟 - 다운타임 시간동안 타겟) ===> 원래는 (현시점까지 작업시간-다운타임 시간)의 타겟임\n"
            value += "- Quality = (현시점의 actual - defective) / Actual\n"

        }

        tv_watching.setText(value)
    }

    private val _timer_task1 = Timer()

    private fun start_timer () {
        val task1 = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateView()
                }
            }
        }
        _timer_task1.schedule(task1, 1000, 10000)
    }
    private fun cancel_timer () {
        _timer_task1.cancel()
    }
}
