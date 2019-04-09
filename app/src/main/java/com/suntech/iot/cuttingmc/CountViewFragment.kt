package com.suntech.iot.cuttingmc

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
import com.suntech.iot.cuttingmc.base.BaseFragment
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.DBHelperForComponent
import com.suntech.iot.cuttingmc.db.SimpleDatabaseHelper
import kotlinx.android.synthetic.main.fragment_count_view.*
import kotlinx.android.synthetic.main.layout_bottom_info_3.*
import org.joda.time.DateTime
import org.json.JSONObject

class CountViewFragment : BaseFragment() {

    private var is_loop :Boolean = false

    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _list_for_db: ArrayList<HashMap<String, String>> = arrayListOf()

    private var _total_target = 0

    private var _list_for_wos_adapter: ListWosAdapter? = null
    private var _list_for_wos: java.util.ArrayList<java.util.HashMap<String, String>> = arrayListOf()

    private var _selected_component_pos = -1

    private val _need_to_refresh = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateView()
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(_need_to_refresh, IntentFilter("need.refresh"))
        is_loop=true
        updateView()
        if ((activity as MainActivity).countViewType == 2) {
            fetchWosAll()
        }
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
            fetchWosAll()
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
        countTarget()
    }

    override fun initViews() {
        super.initViews()

        _list_for_wos_adapter = ListWosAdapter(activity, _list_for_wos)
        lv_wos_info2.adapter = _list_for_wos_adapter

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
            Toast.makeText(activity, "Not yet available\n", Toast.LENGTH_SHORT).show()
        }
        btn_exit.setOnClickListener {
            Toast.makeText(activity, "Not yet available\n", Toast.LENGTH_SHORT).show()
//            val work_idx = ""+ AppGlobal.instance.get_product_idx()
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
//                } )
//            val alertDialog = alertDialogBuilder.create()
//            alertDialog.show()
        }

        // Component count view
        btn_total_count_view.setOnClickListener {
            (activity as MainActivity).countViewType = 1
            ll_total_count.visibility = View.VISIBLE
            ll_component_count.visibility = View.GONE
        }
        btn_select_component.setOnClickListener {
//            val intent = Intent(activity, ComponentInfoActivity::class.java)
//            startActivity(intent)

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

                    (activity as MainActivity).startComponent(wosno, styleno, model, size, target, actual)
//                        (activity as MainActivity).startNewProduct(idx, pieces_info, cycle_time, model, article, material_way, component)
                }
            })
        }

        updateView()
        fetchColorData()     // Get Color
        countTarget()
    }

    private fun countTarget() {

//        val now_time = DateTime()
//        val current_shift_time = AppGlobal.instance.get_current_shift_time()
//        val work_stime = OEEUtil.parseDateTime(current_shift_time?.getString("work_stime"))
//        val work_etime = OEEUtil.parseDateTime(current_shift_time?.getString("work_etime"))

        var target_type = AppGlobal.instance.get_target_type()

        if (target_type=="server_per_hourly" || target_type=="server_per_accumulate" || target_type=="server_per_day_total") {
            fetchServerTarget()

        } else if (target_type=="device_per_hourly" || target_type=="device_per_accumulate" || target_type=="device_per_day_total") {
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

        // WOS INFO
        tv_wosno.text = AppGlobal.instance.get_compo_wos()
        tv_model.text = AppGlobal.instance.get_compo_model()
        tv_component.text = AppGlobal.instance.get_compo_component()
        tv_style_no.text = AppGlobal.instance.get_compo_style()
        tv_size.text = AppGlobal.instance.get_compo_size()
        tv_layer.text = AppGlobal.instance.get_compo_layer()
        tv_target.text = AppGlobal.instance.get_compo_target()

        // component count view 화면 선택일때 처리
        if ((activity as MainActivity).countViewType == 2) {

        }
    }

    private fun updateView() {

        // total count view
        if ((activity as MainActivity).countViewType == 1) {
            tv_current_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")

        // component count view
        } else if ((activity as MainActivity).countViewType == 2) {
            tv_component_time.text = DateTime.now().toString("yyyy-MM-dd HH:mm:ss")
        }

        // total count view 는 선택되지 않았더라도 무조건 처리

        // Total count view
//        tv_design_idx.text = AppGlobal.instance.get_design_info_idx()
//        tv_pieces.text = AppGlobal.instance.get_pieces_info().toString()
//        tv_cycle_time.text = AppGlobal.instance.get_cycle_time().toString()
//
//        tv_article.text = AppGlobal.instance.get_article()
//        tv_model.text = AppGlobal.instance.get_model()
//        tv_material.text = AppGlobal.instance.get_material_way()
//        tv_component.text = AppGlobal.instance.get_component()


        val elapsedTime = AppGlobal.instance.get_current_shift_accumulated_time()

        val h = (elapsedTime / 3600)
//            val m = ((elapsedTime - (h*3600)) / 60)
//            val s = ((elapsedTime - (h*3600)) - m*60 )

        tv_count_view_time.text = "" + h + "H"
//            tv_count_view_time_ms.text = "" + m  + "M " + s + "S"


        var total_actual = AppGlobal.instance.get_current_shift_actual_cnt()

        var ratio_txt = ""
        var ratio = 0
        if (_total_target > 0) {
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


        // component count view 화면을 보고 있을 경우 처리

        if ((activity as MainActivity).countViewType == 2) {

            var db = DBHelperForComponent(activity)

            val work_idx = AppGlobal.instance.get_work_idx()
//            Log.e("get_work_idx", work_idx)
            if (work_idx=="") return

            val item = db.get(work_idx)
            if (item != null && item.toString() != "") {
                val target = item["target"].toString().toInt()
                val actual = (item["actual"].toString().toInt())

                var ratio = 0
                if (target > 0) {
                    ratio = (actual.toFloat() / target.toFloat() * 100).toInt()
                    if (ratio > 999) ratio = 999
                    ratio_txt = "" + ratio + "%"
                } else {
                    ratio_txt = "N/A"
                }

                tv_component_view_target.text = "" + target
                tv_component_view_actual.text = "" + actual
                tv_component_view_ratio.text = ratio_txt

                var maxEnumber = 0
                var color_code = "ffffff"
                for (i in 0..(_list.size - 1)) {
                    val row = _list[i]
                    val snumber = row["snumber"]?.toInt() ?: 0
                    val enumber = row["enumber"]?.toInt() ?: 0
                    color_code = row["color_code"].toString()
                    if (maxEnumber < enumber) maxEnumber = enumber
                    if (snumber <= ratio && enumber >= ratio) {
                        tv_component_view_target.setTextColor(Color.parseColor("#"+color_code))
                        tv_component_view_actual.setTextColor(Color.parseColor("#"+color_code))
                        tv_component_view_ratio.setTextColor(Color.parseColor("#"+color_code))
                    }
                }
                if (maxEnumber < ratio) {
                    tv_component_view_target.setTextColor(Color.parseColor("#"+color_code))
                    tv_component_view_actual.setTextColor(Color.parseColor("#"+color_code))
                    tv_component_view_ratio.setTextColor(Color.parseColor("#"+color_code))
                }

                // 리스트에서 첫번째 항목이 선택되어 있으면 같이 업데이트 한다.
                if (_selected_component_pos >= 0) {
                    var item = _list_for_wos.get(_selected_component_pos)
                    _list_for_wos[_selected_component_pos]["target"] = "" + target
                    _list_for_wos[_selected_component_pos]["actual"] = "" + actual
                    _list_for_wos[_selected_component_pos]["balance"] = "" + (target.toInt() - actual.toInt()).toString()
                    _list_for_wos_adapter?.notifyDataSetChanged()
//                    item["target"] = "" + target
//                    item["actual"] = "" + actual
//                    item["balance"] = "" + (target.toInt() - actual.toInt()).toString()
                }
            }

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

    private fun fetchWosAll() {
        var db = DBHelperForComponent(activity)

        val uri = "/wos.php"
        var params = listOf("code" to "wos")

        val set_wosno = AppGlobal.instance.get_compo_wos().trim()
        val set_size = AppGlobal.instance.get_compo_size().trim()

        if (set_wosno != "") {
            params = listOf(
                "code" to "wos",
                "wosno" to set_wosno
            )
        }

        getBaseActivity().request(activity, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                _selected_component_pos = -1
                _list_for_wos.removeAll(_list_for_wos)

                var list = result.getJSONArray("item")

                // 선택된 항목을 맨앞으로 뺀다.
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    val wosno = item.getString("wosno")
                    val size = item.getString("size")

                    if (wosno == set_wosno && size == set_size) {
                        val row = db.get(wosno, size)
                        var actual = "0"
                        if (row != null) actual = row["actual"].toString()

                        var map = hashMapOf(
                            "wosno" to item.getString("wosno"),
                            "styleno" to item.getString("styleno"),
                            "model" to item.getString("model"),
                            "size" to item.getString("size"),
                            "target" to item.getString("target"),
                            "actual" to actual
                        )
                        _list_for_wos.add(map)
                        _selected_component_pos = 0
                        break;
                    }
                }

                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    val wosno = item.getString("wosno")
                    val size = item.getString("size")

                    if (wosno != set_wosno || size != set_size) {
                        val row = db.get(wosno, size)
                        var actual = "0"
                        if (row != null) actual = row["actual"].toString()

                        var map = hashMapOf(
                            "wosno" to item.getString("wosno"),
                            "styleno" to item.getString("styleno"),
                            "model" to item.getString("model"),
                            "size" to item.getString("size"),
                            "target" to item.getString("target"),
                            "actual" to actual
                        )
                        _list_for_wos.add(map)
                    }
                }
                _list_for_wos_adapter?.select(_selected_component_pos)
                _list_for_wos_adapter?.notifyDataSetChanged()
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