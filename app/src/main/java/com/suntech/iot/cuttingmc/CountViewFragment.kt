package com.suntech.iot.cuttingmc

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseFragment
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.fragment_count_view.*
import org.joda.time.DateTime

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
        Log.e("count view", "resume " + (activity as MainActivity).countViewType.toString())
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
        Log.e("count view", "selected " + (activity as MainActivity).countViewType.toString())
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
//                    (activity as MainActivity).endWork()
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

        val now_time = DateTime()
        val current_shift_time = AppGlobal.instance.get_current_shift_time()
        val work_stime = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))
        val work_etime = OEEUtil.parseDateTime(current_shift_time?.getString("work_etime"))

    }

    private fun countTargetComponent() {

    }

    private fun updateView() {

        if ((activity as MainActivity).countViewType == 1) {
            tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        } else {
            tv_component_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        }
    }

    private fun fetchServerTarget() {

    }

    fun startHandler () {
        val handler = Handler()
        handler.postDelayed({
            if (is_loop) {
                updateView()
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