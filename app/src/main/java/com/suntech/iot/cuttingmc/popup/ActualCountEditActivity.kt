package com.suntech.iot.cuttingmc.popup

import android.content.Intent
import android.os.Bundle
import com.suntech.iot.cuttingmc.R
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.DBHelperForComponent
import kotlinx.android.synthetic.main.activity_actual_count_edit.*
import kotlinx.android.synthetic.main.list_item_product_total.*

class ActualCountEditActivity : BaseActivity() {

    private var list_adapter: ProductListActivity.ListAdapter? = null
    private var _list: ArrayList<HashMap<String, String>> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actual_count_edit)
        initView()
        updateView()
    }

    public override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    private fun initView() {
        btn_confirm.setOnClickListener {
            finish(true, 1, "ok", null)
        }
        lv_products.setOnItemClickListener { adapterView, view, i, l ->
            val work_idx = _list[i]["work_idx"]
            val actual = _list[i]["actual"]

            val intent = Intent(this, ActualCountEditInputActivity::class.java)
            intent.putExtra("work_idx", work_idx)
            intent.putExtra("actual", actual)
            startActivity(intent, { r, c, m, d ->
                if (r) {
                    updateView()
                }
            })
        }
    }

    private fun updateView() {

        tv_item_row0.text = "TOTAL"
        tv_item_row1.text = ""
        tv_item_row2.text = ""

        var db = DBHelperForComponent(this)
        _list = db.gets() ?: _list

        list_adapter = ProductListActivity.ListAdapter(this, _list)
        lv_products.adapter = list_adapter
        var total_target = 0
        var total_actual = 0
        var total_balance = 0

        val def_wosno = AppGlobal.instance.get_compo_wos().trim()
        val def_size = AppGlobal.instance.get_compo_size().trim()

        // 현재 선택된 제품을 찾는다.
        for (i in 0..(_list.size - 1)) {
            val item = _list[i]
            val wosno = item["wosno"] ?: "0"
            val size = item["size"] ?: "0"

            if (wosno == def_wosno && size == def_size) {
                val target = item["target"]?.toInt() ?: 0
                val actual = item["actual"]?.toInt() ?: 0
                val balance = target - actual

                total_target += target
                total_actual += actual
                total_balance += balance

                item.put("target", target.toString())
                item.put("actual", actual.toString())
                item.put("balance", balance.toString())

                break
            }
        }

        for (i in 0..(_list.size - 1)) {
            val item = _list[i]
            val wosno = item["wosno"] ?: "0"
            val size = item["size"] ?: "0"

            if (wosno != def_wosno || size != def_size) {
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
        }

        tv_item_row3.text = total_target.toString()
        tv_item_row4.text = total_actual.toString()
        tv_item_row5.text = total_balance.toString()
    }
}
