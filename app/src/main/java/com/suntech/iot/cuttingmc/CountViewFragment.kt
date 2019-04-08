package com.suntech.iot.cuttingmc

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseFragment
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.SimpleDatabaseHelper
import com.suntech.iot.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_2.*
import org.joda.time.DateTime
import org.json.JSONObject

class CountViewFragment : BaseFragment() {

    private var is_loop :Boolean = false

    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _list_for_db: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0

//    private var _list_for_wos_adapter: ListWosAdapter? = null
//    private var _list_for_wos: java.util.ArrayList<java.util.HashMap<String, String>> = arrayListOf()

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)

//        _list_for_wos_adapter = ListWosAdapter(activity, _list_for_wos)
//        lv_wos_info2.adapter = _list_for_wos_adapter
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop=true
        updateView()
//        fetchWosAll()
        startHandler()
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(_need_to_refresh)
        is_loop=false
    }

    override fun onSelected() {
        if ((activity as MainActivity).countViewType == 1) {
            ll_total_count.visibility = View.VISIBLE
            ll_component_count.visibility = View.GONE
        } else {
            ll_total_count.visibility = View.GONE
            ll_component_count.visibility = View.VISIBLE
        }

        // Worker info
        val no = AppGlobal.instance.get_worker_no()
        val name = AppGlobal.instance.get_worker_name()
        if (no== "" || name == "") {
            Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            (activity as MainActivity).changeFragment(0)
        }

        updateView()
        fetchColorData()     // Get Color

        if ((activity as MainActivity).countViewType == 1) {
            countTarget()
        } else {
            countTargetComponent()
        }
    }

    override fun initViews() {
        super.initViews()

        // Total count view
        tv_count_view_target.text = "0"
        tv_count_view_actual.text = "0"
        tv_count_view_ratio.text = "0%"
        tv_count_view_time.text = "0H"

        // Component count view
        tv_component_view_target.text = "0"
        tv_component_view_actual.text = "0"
        tv_component_view_ratio.text = "0%"

        // Total count view
        btn_start.setOnClickListener {
            //            (activity as MainActivity).saveRowData("barcode", value)
        }
        btn_exit.setOnClickListener {
            val work_idx = ""+ AppGlobal.instance.get_product_idx()
            if (work_idx == "") {
                Toast.makeText(activity, getString(R.string.msg_not_start_work), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val alertDialogBuilder = AlertDialog.Builder(activity)
            alertDialogBuilder.setTitle(getString(R.string.notice))
            alertDialogBuilder
                .setMessage(getString(R.string.msg_exit_shift))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.confirm), DialogInterface.OnClickListener { dialog, id ->
                    (activity as MainActivity).changeFragment(0)
                    (activity as MainActivity).endWork()
                })
                .setNegativeButton(getString(R.string.cancel), DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                } )
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        // Component count view
        btn_total_count_view.setOnClickListener {
            (activity as MainActivity).countViewType = 1
            ll_total_count.visibility = View.VISIBLE
            ll_component_count.visibility = View.GONE
        }
        btn_select_component.setOnClickListener {
            val intent = Intent(activity, ComponentInfoActivity::class.java)
            startActivity(intent)
        }

        updateView()
        fetchColorData()     // Get Color

        if ((activity as MainActivity).countViewType == 1) {
            countTarget()
        } else {
            countTargetComponent()
        }
    }

    private fun countTarget() {

//        val now_time = DateTime()
//        val current_shift_time = AppGlobal.instance.get_current_shift_time()
//        val work_stime = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))
//        val work_etime = OEEUtil.parseDateTime(current_shift_time?.getString("work_etime"))

        var target_type = AppGlobal.instance.get_target_type()
        var target_type_6 = target_type.substring(0, 6)

        if (target_type_6 == "server") {
            fetchServerTarget()

        } else if (target_type_6 == "device") {
            _total_target = 0

            var item: JSONObject? = AppGlobal.instance.get_current_shift_time()
            if (item != null) {
                when (item["shift_idx"]) {
                    "1" -> _total_target = AppGlobal.instance.get_target_manual_shift("1").toInt()
                    "2" -> _total_target = AppGlobal.instance.get_target_manual_shift("2").toInt()
                    "3" -> _total_target = AppGlobal.instance.get_target_manual_shift("3").toInt()
                }
            }

            if (target_type=="device_per_hourly") {

            } else if (target_type=="device_per_accumulate") {
                val shift_total_time = AppGlobal.instance.get_current_shift_total_time()
                val shift_now_time = AppGlobal.instance.get_current_shift_accumulated_time()
                var cycle_time = if (_total_target > 0) (shift_total_time / _total_target) else shift_now_time
                _total_target = if (cycle_time > 0) (shift_now_time / cycle_time).toInt() else 0
            } else if (target_type=="device_per_day_total") {
                Log.e("shift time", "target="+_total_target)
            }
            updateView()
        }
    }

    private fun countTargetComponent() {

    }

    private fun updateView() {

        if ((activity as MainActivity).countViewType == 1) {
            tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

            // Total count view
            tv_design_idx.text = AppGlobal.instance.get_design_info_idx()
//            tv_pieces.text = AppGlobal.instance.get_pieces_info().toString()
//            tv_cycle_time.text = AppGlobal.instance.get_cycle_time().toString()
//
//            tv_article.text = AppGlobal.instance.get_article()
//            tv_model.text = AppGlobal.instance.get_model()
//            tv_material.text = AppGlobal.instance.get_material_way()
//            tv_component.text = AppGlobal.instance.get_component()

            val elapsedTime = AppGlobal.instance.get_current_shift_accumulated_time()

            val h = (elapsedTime / 3600)
//            val m = ((elapsedTime - (h*3600)) / 60)
//            val s = ((elapsedTime - (h*3600)) - m*60 )

            tv_count_view_time.text = "" + h + "H"
//            tv_count_view_time_ms.text = "" + m  + "M " + s + "S"


            var total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

            var ratio_txt = ""
            var ratio = 0
            if (_total_target>0) {
                ratio = (total_actual.toFloat() / _total_target.toFloat() * 100).toInt()
                if (ratio > 999) ratio = 999
                ratio_txt = "" + ratio + "%"
            } else {
                ratio_txt = "N/A"
            }

            tv_count_view_target.text = "" +_total_target
            tv_count_view_actual.text = "" + total_actual
            tv_count_view_ratio.text = ratio_txt
            tv_count_view_time.text = "" + h + "H"

            var maxEnumber = 0
            var color_code = "ffffff"
            for (i in 0..(_list.size - 1)) {
                val row = _list[i]
                val snumber = row["snumber"]?.toInt() ?: 0
                val enumber = row["enumber"]?.toInt() ?: 0
                color_code = row["color_code"].toString()
                if (maxEnumber < enumber) maxEnumber = enumber
                if (snumber <= ratio && enumber >= ratio) {
                    tv_count_view_target.setTextColor(Color.parseColor("#"+color_code))
                    tv_count_view_actual.setTextColor(Color.parseColor("#"+color_code))
                    tv_count_view_ratio.setTextColor(Color.parseColor("#"+color_code))
                    tv_count_view_time.setTextColor(Color.parseColor("#"+color_code))
                }
            }
            if (maxEnumber < ratio) {
                tv_count_view_target.setTextColor(Color.parseColor("#"+color_code))
                tv_count_view_actual.setTextColor(Color.parseColor("#"+color_code))
                tv_count_view_ratio.setTextColor(Color.parseColor("#"+color_code))
                tv_count_view_time.setTextColor(Color.parseColor("#"+color_code))
            }

        } else {
            tv_component_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

//            val elapsedTime = AppGlobal.instance.get_current_shift_accumulated_time()

//            val h = (elapsedTime / 3600)
//            val m = ((elapsedTime - (h*3600)) / 60)
//            val s = ((elapsedTime - (h*3600)) - m*60 )
        }
    }

    private fun fetchServerTarget() {
        val work_idx = AppGlobal.instance.get_product_idx()
        var db = SimpleDatabaseHelper(activity)
        val row = db.get(work_idx)

        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "target",
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "date" to DateTime().toString("yyyy-MM-dd"),
            "mac_addr" to AppGlobal.instance.getMACAddress()
        )

        getBaseActivity().request(activity, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){
                var target_type = AppGlobal.instance.get_target_type()

                if (target_type=="server_per_hourly") _total_target = result.getString("target").toInt()
                else if (target_type=="server_per_accumulate") _total_target = result.getString("targetsum").toInt()
                else if (target_type=="server_per_day_total") _total_target = result.getString("daytargetsum").toInt()
                else _total_target = result.getString("targetsum").toInt()

                updateView()
            }else{
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    var handle_cnt = 0
    fun startHandler () {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
                if (handle_cnt++ > 15) {
                    handle_cnt = 0
                    countTarget()
                }
                startHandler()
            }
        }, 1000)
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
}