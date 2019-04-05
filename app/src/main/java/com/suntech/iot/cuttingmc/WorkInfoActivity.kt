package com.suntech.iot.cuttingmc

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.util.OEEUtil
import kotlinx.android.synthetic.main.activity_work_info.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import org.json.JSONArray

class WorkInfoActivity : BaseActivity() {

    private var tab_pos : Int = 1

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    var _selected_index = -1

    private var _list_json: JSONArray? = null

    private var list_for_operator_adapter: ListOperatorAdapter? = null
    private var _list_for_operator: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _filtered_list_for_operator: ArrayList<HashMap<String, String>> = arrayListOf()

    private var list_for_last_worker_adapter: ListOperatorAdapter? = null
    private var _list_for_last_worker: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_info)
        initView()
        fetchShiftData()
        fetchOperatorData()
    }

    private fun initView() {

        tv_title.text = "WORK INFO"

        list_adapter = ListAdapter(this, _list)
        lv_available_info.adapter = list_adapter

        _selected_index = AppGlobal.instance.get_current_shift_time_idx()

        list_for_operator_adapter = ListOperatorAdapter(this, _filtered_list_for_operator)
        lv_operator_info.adapter = list_for_operator_adapter

        lv_operator_info.setOnItemClickListener { adapterView, view, i, l ->
            list_for_operator_adapter?.select(i)
            list_for_operator_adapter?.notifyDataSetChanged()
        }

        list_for_last_worker_adapter = ListOperatorAdapter(this, _list_for_last_worker)
        lv_last_worker.adapter = list_for_last_worker_adapter

        lv_last_worker.setOnItemClickListener { adapterView, view, i, l ->
            et_searcg_text.setText("")

            list_for_last_worker_adapter?.select(i)
            list_for_last_worker_adapter?.notifyDataSetChanged()

            var list = AppGlobal.instance.get_last_workers()
            val worker = list.getJSONObject(list.length() - 1 - i)

            for (j in 0..(_list_for_operator.size-1)) {
                val item = _list_for_operator[j]
                val number = item["number"] ?: ""
                if (number == worker.getString("number")) {
                    list_for_operator_adapter?.select(j)
                    list_for_operator_adapter?.notifyDataSetChanged()
                    lv_operator_info.smoothScrollToPosition(j)
                    break
                }
            }
        }

        et_searcg_text.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s != "") {
                    filterOperatorData()
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        // Tab button click
        btn_work_info_server.setOnClickListener { tabChange(1) }
        btn_work_info_manual.setOnClickListener { tabChange(2) }

        btn_setting_cancel.setOnClickListener { finish() }
    }

    private fun fetchShiftData() {
        val list = AppGlobal.instance.get_current_work_time()
        _list_json = list

        if (list == null) return

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var map = hashMapOf(
                "idx" to item.getString("idx"),
                "date" to item.getString("date"),
                "work_stime" to item.getString("work_stime"),
                "work_etime" to item.getString("work_etime"),
                "available_stime" to item.getString("available_stime"),
                "available_etime" to item.getString("available_etime"),
                "planned1_stime" to item.getString("planned1_stime"),
                "planned1_etime" to item.getString("planned1_etime"),
                "planned2_stime" to item.getString("planned2_stime"),
                "planned2_etime" to item.getString("planned2_etime"),
                "planned3_stime" to item.getString("planned3_stime"),
                "planned3_etime" to item.getString("planned3_etime"),
                "over_time" to item.getString("over_time"),
                "line_idx" to item.getString("line_idx"),
                "line_name" to item.getString("line_name"),
                "shift_idx" to item.getString("shift_idx"),
                "shift_name" to item.getString("shift_name")
            )
            _list.add(map)
        }
        list_adapter?.notifyDataSetChanged()
    }

    private fun filterOperatorData() {
        _filtered_list_for_operator.removeAll(_filtered_list_for_operator)
        list_for_operator_adapter?.select(-1)

        val filter_text = et_searcg_text.text.toString()

        for (i in 0..(_list_for_operator.size-1)) {

            val item = _list_for_operator[i]
            val number = item["number"] ?: ""
            val name = item["name"] ?: ""

            val b = number.toUpperCase().contains(filter_text.toUpperCase())
            val c = name.toUpperCase().contains(filter_text.toUpperCase())
            if (filter_text=="" || b || c) {
                _filtered_list_for_operator.add(item)
            }
        }
        list_for_operator_adapter?.notifyDataSetChanged()
    }

    private fun fetchOperatorData() {
        val uri = "/getlist1.php"
        var params = listOf(
            "code" to "worker",
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list = result.getJSONArray("item")
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "number" to item.getString("number"),
                        "name" to item.getString("name")
                    )
                    _list_for_operator.add(map)
                }
                filterOperatorData()
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
        filterOperatorData()
    }

    private class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

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
                view = this._inflator.inflate(R.layout.list_available_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            var work_stime = OEEUtil.parseDateTime(_list[position]["work_stime"].toString())
            var work_etime = OEEUtil.parseDateTime(_list[position]["work_etime"].toString())

            vh.tv_item_shift.text = _list[position]["shift_name"]
            vh.tv_item_work_time.text = work_stime.toString("HH:mm") + "~" + work_etime.toString("HH:mm")
            vh.tv_item_planned_time1.text = _list[position]["planned1_stime"] + "~" + _list[position]["planned1_etime"]
            vh.tv_item_planned_time2.text = _list[position]["planned2_stime"] + "~" + _list[position]["planned2_etime"]

            if ((_context as WorkInfoActivity)._selected_index==position) {
                vh.tv_item_shift.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_work_time.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_planned_time1.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_planned_time2.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
            } else {
                vh.tv_item_shift.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_work_time.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_planned_time1.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_planned_time2.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_shift: TextView
            val tv_item_work_time: TextView
            val tv_item_planned_time1: TextView
            val tv_item_planned_time2: TextView

            init {
                this.tv_item_shift = row?.findViewById<TextView>(R.id.tv_item_shift) as TextView
                this.tv_item_work_time = row?.findViewById<TextView>(R.id.tv_item_work_time) as TextView
                this.tv_item_planned_time1 = row?.findViewById<TextView>(R.id.tv_item_planned_time1) as TextView
                this.tv_item_planned_time2 = row?.findViewById<TextView>(R.id.tv_item_planned_time2) as TextView
            }
        }
    }

    private class ListOperatorAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? =null
        private var _selected_index = -1

        init {
            this._inflator = LayoutInflater.from(context)
            this._list = list
            this._context = context
        }

        fun select(index:Int) {_selected_index=index}
        fun getSelected(): Int { return _selected_index }

        override fun getCount(): Int { return _list.size }
        override fun getItem(position: Int): Any { return _list[position] }
        override fun getItemId(position: Int): Long { return position.toLong() }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: ViewHolder
            if (convertView == null) {
                view = this._inflator.inflate(R.layout.list_operator_info, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_employee_number.text = _list[position]["number"]
            vh.tv_item_name.text = _list[position]["name"]

            if (_selected_index==position) {
                vh.tv_item_employee_number.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
                vh.tv_item_name.setTextColor(ContextCompat.getColor(_context, R.color.list_item_highlight_text_color))
            } else {
                vh.tv_item_employee_number.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
                vh.tv_item_name.setTextColor(ContextCompat.getColor(_context, R.color.list_item_text_color))
            }

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_employee_number: TextView
            val tv_item_name: TextView

            init {
                this.tv_item_employee_number = row?.findViewById<TextView>(R.id.tv_item_employee_number) as TextView
                this.tv_item_name = row?.findViewById<TextView>(R.id.tv_item_name) as TextView
            }
        }
    }

    private fun tabChange(v : Int) {
        if (tab_pos == v) return
        tab_pos = v
        when (tab_pos) {
            1 -> {
                btn_work_info_server.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_work_info_server.setBackgroundResource(R.color.colorButtonBlue)
                btn_work_info_manual.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_work_info_manual.setBackgroundResource(R.color.colorButtonDefault)
                layout_work_info_server.visibility = View.VISIBLE
                layout_work_info_manual.visibility = View.GONE
            }
            2 -> {
                btn_work_info_server.setTextColor(ContextCompat.getColor(this, R.color.colorGray))
                btn_work_info_server.setBackgroundResource(R.color.colorButtonDefault)
                btn_work_info_manual.setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                btn_work_info_manual.setBackgroundResource(R.color.colorButtonBlue)
                layout_work_info_server.visibility = View.GONE
                layout_work_info_manual.visibility = View.VISIBLE
            }
        }
    }
}