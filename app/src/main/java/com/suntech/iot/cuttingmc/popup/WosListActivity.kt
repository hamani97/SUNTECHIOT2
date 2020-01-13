package com.suntech.iot.cuttingmc.popup

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.suntech.iot.cuttingmc.R
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.activity_wos_list.*

class WosListActivity : BaseActivity() {

    private var _list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()
    private var _filtered_list: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wos_list)
        initView()
        fetchData()
    }

    fun parentSpaceClick(view: View) {
        var view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initView() {
        tv_title.text = "SELECT " + AppGlobal.instance.get_wos_name()

        _list_adapter = ListAdapter(this, _filtered_list)
        lv_products.adapter = _list_adapter

        lv_products.setOnItemClickListener { adapterView, view, i, l ->
            finish(true, i, "ok", _filtered_list[i])
        }
        et_search.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s != "") {
                    filterData()
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
//        btn_confirm.setOnClickListener {
//            finish(false, -1, "ok", null)
//        }
        btn_close.setOnClickListener {
            finish(false, -1, "ok", null)
        }
    }

    private fun filterData() {
        _filtered_list.removeAll(_filtered_list)
        _list_adapter?.select(-1)

        val filter_text = et_search.text.toString()

        for (i in 0..(_list.size-1)) {

            val item = _list[i]
            val wosno = item["wosno"] ?: ""

            val a = wosno.toUpperCase().contains(filter_text.toUpperCase())
            if (filter_text=="" || a) {
                _filtered_list.add(item)
            }
        }

        _list_adapter?.notifyDataSetChanged()
    }

    private fun fetchData() {

        val uri = "/wos.php"
        var params = listOf("code" to "wos_list")

        request(this, uri, false, params, { result ->

            var code = result.getString("code")
            if (code == "00") {

                var list = result.getJSONArray("item")

                for (i in 0..(list.length() - 1)) {

                    val item = list.getJSONObject(i)

                    val map = hashMapOf(
                        "idx" to item.getString("idx"),
                        "wosno" to item.getString("wosno"),
                        "styleno" to item.getString("styleno"),
                        "model" to item.getString("model"),
                        "planday" to item.getString("planday")
                    )
                    _list.add(map)
                }
                filterData()

            } else {
                Toast.makeText(this, result.getString("msg"), Toast.LENGTH_SHORT).show()
            }
        })
    }

    class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

        private var _list: ArrayList<HashMap<String, String>>
        private val _inflator: LayoutInflater
        private var _context : Context? = null
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
                view = this._inflator.inflate(R.layout.list_item_wos, parent, false)
                vh = ViewHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as ViewHolder
            }

            vh.tv_item_date.text = _list[position]["planday"]
            vh.tv_item_wosno.text = _list[position]["wosno"]
            vh.tv_item_model.text = _list[position]["model"]

            return view
        }

        private class ViewHolder(row: View?) {
            val tv_item_date: TextView
            val tv_item_wosno: TextView
            val tv_item_model: TextView

            init {
                this.tv_item_date = row?.findViewById<TextView>(R.id.tv_item_date) as TextView
                this.tv_item_wosno = row?.findViewById<TextView>(R.id.tv_item_wosno) as TextView
                this.tv_item_model = row?.findViewById<TextView>(R.id.tv_item_model) as TextView
            }
        }
    }
}
