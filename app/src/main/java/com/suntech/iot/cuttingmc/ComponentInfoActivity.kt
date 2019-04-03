package com.suntech.iot.cuttingmc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.activity_component_info.*
import kotlinx.android.synthetic.main.layout_top_menu_2.*
import java.util.ArrayList
import java.util.HashMap

class ComponentInfoActivity : BaseActivity() {

    private var _selected_wos_idx : String = ""
    private var _selected_component_idx : String = ""
    private var _selected_component_code : String = ""
    private var _selected_size_idx : String = ""

    private var _selected_layer_no : String = ""
    private var _selected_pair_info : String = ""

    private var _list_for_wos_adapter: ListWosAdapter? = null
    private var _list_for_wos: ArrayList<HashMap<String, String>> = arrayListOf()

    var _selected_wos_index = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_component_info)
        initView()
        fetchWosAll()
    }

    private fun initView() {
        tv_title.setText("1 Shift 07:00 - 16:00")

        tv_compo_wos.text = AppGlobal.instance.get_compo_wos()
        tv_compo_model.text = AppGlobal.instance.get_compo_model()
        tv_compo_style.text = AppGlobal.instance.get_compo_style()
        tv_compo_component.text = AppGlobal.instance.get_compo_component()
        tv_compo_size.text = AppGlobal.instance.get_compo_size()
        tv_compo_layer.text = AppGlobal.instance.get_compo_layer()
        tv_compo_target.text = AppGlobal.instance.get_compo_target()

        // set hidden value
        _selected_wos_idx = AppGlobal.instance.get_compo_wos_idx()
        _selected_component_idx = AppGlobal.instance.get_compo_component_idx()
        _selected_size_idx = AppGlobal.instance.get_compo_size_idx()

        btn_setting_confirm.setOnClickListener {
            if (tv_compo_wos.text.toString() == "" || tv_compo_model.text.toString() == "" ||
                tv_compo_style.text.toString() == "" || tv_compo_size.text.toString() == "") {
                Toast.makeText(this, getString(R.string.msg_require_info), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveSettingData()
        }
        btn_setting_cancel.setOnClickListener { finish() }

        _list_for_wos_adapter = ListWosAdapter(this, _list_for_wos)
        lv_wos_info.adapter = _list_for_wos_adapter

        lv_wos_info.setOnItemClickListener { adapterView, view, i, l ->
            _selected_wos_index = i
            _list_for_wos_adapter?.select(i)
            _list_for_wos_adapter?.notifyDataSetChanged()
        }

        // button click
        tv_compo_wos.setOnClickListener { fetchWosData() }
        tv_compo_component.setOnClickListener { fetchComponentData() }
        tv_compo_size.setOnClickListener { fetchSizeData() }
        tv_compo_layer.setOnClickListener { fetchLayerData() }
    }

    private fun saveSettingData() {

        AppGlobal.instance.set_compo_wos(tv_compo_wos.text.toString())
        AppGlobal.instance.set_compo_model(tv_compo_model.text.toString())
        AppGlobal.instance.set_compo_style(tv_compo_style.text.toString())
        AppGlobal.instance.set_compo_component(tv_compo_component.text.toString())
        AppGlobal.instance.set_compo_size(tv_compo_size.text.toString())
        AppGlobal.instance.set_compo_layer(tv_compo_layer.text.toString())
        AppGlobal.instance.set_compo_target(tv_compo_target.text.toString())

        // set hidden value
        AppGlobal.instance.set_compo_wos_idx(_selected_wos_idx)
        AppGlobal.instance.set_compo_component_idx(_selected_component_idx)
        AppGlobal.instance.set_compo_size_idx(_selected_size_idx)

        if (_selected_wos_index > -1) {
            finish(true, 1, "ok", _list_for_wos[_selected_wos_index])
        } else {
            finish()
        }
    }

    private fun fetchWosAll() {
        val uri = "/wos.php"
        var params = listOf("code" to "wos")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var list = result.getJSONArray("item")
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "wosno" to item.getString("wosno"),
                        "styleno" to item.getString("styleno"),
                        "model" to item.getString("model"),
                        "size" to item.getString("size"),
                        "target" to item.getString("target")
                    )
                    _list_for_wos.add(map)
                }
                _list_for_wos_adapter?.notifyDataSetChanged()
                filterWosData()
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchWosData() {
        val uri = "/wos.php"
        var params = listOf("code" to "wos_list")

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "wosno" to item.getString("wosno"),
                        "styleno" to item.getString("styleno"),
                        "model" to item.getString("model")
                    )
                    lists.add(map)
                    arr.add(item.getString("wosno") + " - " + item.getString("model"))
                }
                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_wos_idx = lists[c]["idx"] ?: ""
                        tv_compo_wos.text = lists[c]["wosno"] ?: ""
                        tv_compo_model.text = lists[c]["model"] ?: ""
                        tv_compo_style.text = lists[c]["styleno"] ?: ""
                        tv_compo_component.text = ""
                        tv_compo_size.text = ""
                        tv_compo_target.text = ""
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchComponentData() {
        if (tv_compo_wos.text.toString() == "") {
            Toast.makeText(this, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            return
        }
        val uri = "/wos.php"
        var params = listOf(
            "code" to "wos_comp",
            "wosno" to tv_compo_wos.text.toString())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "c_code" to item.getString("c_code"),
                        "c_name" to item.getString("c_name")
                    )
                    lists.add(map)
                    arr.add(item.getString("c_name"))
                }
                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_component_idx = lists[c]["idx"] ?: ""
                        _selected_component_code = lists[c]["c_code"] ?: ""
                        tv_compo_component.text = lists[c]["c_name"] ?: ""
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchSizeData() {
        if (tv_compo_wos.text.toString() == "") {
            Toast.makeText(this, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            return
        }
        val uri = "/wos.php"
        var params = listOf(
            "code" to "wos_size",
            "wosno" to tv_compo_wos.text.toString())

        request(this, uri, false, params, { result ->
            var code = result.getString("code")
            var msg = result.getString("msg")
            if (code == "00") {
                var arr: ArrayList<String> = arrayListOf<String>()
                var list = result.getJSONArray("item")
                var lists : ArrayList<HashMap<String, String>> = arrayListOf()
                for (i in 0..(list.length() - 1)) {
                    val item = list.getJSONObject(i)
                    var map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "s_name" to item.getString("s_name"),
                        "s_target" to item.getString("s_target")
                    )
                    lists.add(map)
                    arr.add(item.getString("s_name"))
                }
                val intent = Intent(this, PopupSelectList::class.java)
                intent.putStringArrayListExtra("list", arr)
                startActivity(intent, { r, c, m, d ->
                    if (r) {
                        _selected_size_idx = lists[c]["idx"] ?: ""
                        tv_compo_size.text = lists[c]["s_name"] ?: ""
                        tv_compo_target.text = lists[c]["s_target"] ?: ""
                        filterWosData()
                    }
                })
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterWosData() {
        var wos = tv_compo_wos.text.toString()
        var size = tv_compo_size.text.toString()
        var target = tv_compo_target.text.toString()

        if (wos == "" || size == "" || target == "") return

        for (j in 0..(_list_for_wos.size-1)) {
            val item = _list_for_wos[j]
            val wos2 = item["wosno"] ?: ""
            val size2 = item["size"] ?: ""
            val target2 = item["target"] ?: ""
            if (wos == wos2 && size == size2 && target == target2) {
                _selected_wos_index = j
                _list_for_wos_adapter?.select(j)
                _list_for_wos_adapter?.notifyDataSetChanged()
                lv_wos_info.smoothScrollToPosition(j)
                break
            }
        }
    }

    private fun fetchLayerData() {
        var arr: ArrayList<String> = arrayListOf<String>()
        var lists : ArrayList<HashMap<String, String>> = arrayListOf()

        if (AppGlobal.instance.get_layer_pairs("1") != "") {
            arr.add("1 Layer - " + AppGlobal.instance.get_layer_pairs("1") + " pair")
            lists.add(hashMapOf("layer_no" to "1", "pair" to AppGlobal.instance.get_layer_pairs("1")))
        }
        if (AppGlobal.instance.get_layer_pairs("2") != "") {
            arr.add("2 Layer - " + AppGlobal.instance.get_layer_pairs("2") + " pair")
            lists.add(hashMapOf("layer_no" to "2", "pair" to AppGlobal.instance.get_layer_pairs("2")))
        }
        if (AppGlobal.instance.get_layer_pairs("4") != "") {
            arr.add("4 Layer - " + AppGlobal.instance.get_layer_pairs("4") + " pair")
            lists.add(hashMapOf("layer_no" to "4", "pair" to AppGlobal.instance.get_layer_pairs("4")))
        }
        if (AppGlobal.instance.get_layer_pairs("6") != "") {
            arr.add("6 Layer - " + AppGlobal.instance.get_layer_pairs("6") + " pair")
            lists.add(hashMapOf("layer_no" to "6", "pair" to AppGlobal.instance.get_layer_pairs("6")))
        }
        if (AppGlobal.instance.get_layer_pairs("8") != "") {
            arr.add("8 Layer - " + AppGlobal.instance.get_layer_pairs("8") + " pair")
            lists.add(hashMapOf("layer_no" to "8", "pair" to AppGlobal.instance.get_layer_pairs("8")))
        }
        if (AppGlobal.instance.get_layer_pairs("10") != "") {
            arr.add("10 Layer - " + AppGlobal.instance.get_layer_pairs("10") + " pair")
            lists.add(hashMapOf("layer_no" to "10", "pair" to AppGlobal.instance.get_layer_pairs("10")))
        }

        val intent = Intent(this, PopupSelectList::class.java)
        intent.putStringArrayListExtra("list", arr)
        startActivity(intent, { r, c, m, d ->
            if (r) {
                tv_compo_layer.text = lists[c]["layer_no"] ?: ""
                _selected_layer_no = lists[c]["layer_no"] ?: ""
                _selected_pair_info = lists[c]["pair"] ?: ""
            }
        })
    }

    private class ListWosAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
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

            vh.tv_item_wosno.text = _list[position]["wosno"]
            vh.tv_item_model.text = _list[position]["model"]
            vh.tv_item_size.text = _list[position]["size"]
            vh.tv_item_target.text = _list[position]["target"]
            vh.tv_item_actual.text = "0"
            vh.tv_item_balance.text = _list[position]["target"]

            if (_selected_index==position) {
                vh.tv_item_wosno.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_model.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_size.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_target.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_actual.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
                vh.tv_item_balance.setTextColor(ContextCompat.getColor(_context, R.color.list_item_filtering_text_color))
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