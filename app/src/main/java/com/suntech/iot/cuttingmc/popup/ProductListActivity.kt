package com.suntech.iot.cuttingmc.popup

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.suntech.iot.cuttingmc.R
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.SimpleDatabaseHelper
import kotlinx.android.synthetic.main.activity_product_list.*
import kotlinx.android.synthetic.main.list_item_product_total.*

class ProductListActivity : BaseActivity() {

    private var list_adapter: ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)
        initView()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initView() {
        tv_item_row0.text = "TOTAL"
        tv_item_row1.text = ""
        tv_item_row2.text = ""

        var db = SimpleDatabaseHelper(this)
        _list = db.gets() ?: _list

        list_adapter = ListAdapter(this, _list)
        lv_products.adapter = list_adapter

        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }

        var total_target = 0
        var total_actual = 0
        var total_balance = 0

        for (i in 0..(_list.size - 1)) {

            val item = _list[i]

            val target = item["target"]?.toInt() ?: 0
            val actual = item["actual"]?.toInt() ?: 0
            val balance = target - actual

            total_target += target
            total_actual += actual
            total_balance += balance

            item.put("target", target.toString())
            item.put("actual", actual.toString())
            item.put("balance", balance.toString())
        }

        tv_item_row3.text = total_target.toString()
        tv_item_row4.text = total_actual.toString()
        tv_item_row5.text = total_balance.toString()
    }

    class ListAdapter(context: Context, list: ArrayList<HashMap<String, String>>) : BaseAdapter() {

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
                view = this._inflator.inflate(R.layout.list_item_product, parent, false)
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
            vh.tv_item_actual.text = _list[position]["actual"]
            vh.tv_item_balance.text = _list[position]["balance"]

            if(_list[position]["work_idx"].toString() == AppGlobal.instance.get_work_idx()) {
                vh.tv_item_wosno.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_model.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_size.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_target.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_actual.setTextColor(Color.parseColor("#ff0000"))
                vh.tv_item_balance.setTextColor(Color.parseColor("#ff0000"))
            } else {
                vh.tv_item_wosno.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_model.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_size.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_target.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_actual.setTextColor(Color.parseColor("#000000"))
                vh.tv_item_balance.setTextColor(Color.parseColor("#000000"))
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
