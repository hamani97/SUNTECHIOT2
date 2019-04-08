package com.suntech.iot.cuttingmc.popup

import android.os.Bundle
import android.widget.Toast
import com.suntech.iot.cuttingmc.R
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.SimpleDatabaseHelper
import kotlinx.android.synthetic.main.activity_actual_count_edit_input.*

class ActualCountEditInputActivity : BaseActivity() {

    private var _origin_actual = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actual_count_edit_input)
        initView()
    }

    private fun initView() {
        val design_idx = intent.getStringExtra("design_idx")
        val work_idx = intent.getStringExtra("work_idx")
        val actual = intent.getStringExtra("actual")

        _origin_actual = actual.toInt()

        tv_design_idx.text = "IDX " + design_idx
        et_defective_qty.setText("0")

        ib_list_item_1.isSelected = true

        ll_list_item_1.setOnClickListener {
            ib_list_item_1.isSelected = true
            ib_list_item_2.isSelected = false
            ib_list_item_3.isSelected = false
            ib_list_item_4.isSelected = false
        }
        ll_list_item_2.setOnClickListener {
            ib_list_item_1.isSelected = false
            ib_list_item_2.isSelected = true
            ib_list_item_3.isSelected = false
            ib_list_item_4.isSelected = false
        }
        ll_list_item_3.setOnClickListener {
            ib_list_item_1.isSelected = false
            ib_list_item_2.isSelected = false
            ib_list_item_3.isSelected = true
            ib_list_item_4.isSelected = false
        }
        ll_list_item_4.setOnClickListener {
            ib_list_item_1.isSelected = false
            ib_list_item_2.isSelected = false
            ib_list_item_3.isSelected = false
            ib_list_item_4.isSelected = true
        }

        btn_actual_count_edit_plus.setOnClickListener {
            var value = et_defective_qty.text.toString().toInt()
            value++
            et_defective_qty.setText(value.toString())
        }
        btn_actual_count_edit_minus.setOnClickListener {
            var value = et_defective_qty.text.toString().toInt()
            value--
            et_defective_qty.setText(value.toString())
        }

        btn_confirm.setOnClickListener {
            val value = et_defective_qty.text.toString()

            sendCountData(value, work_idx)
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }

    private fun sendCountData(count:String, work_idx:String) {

        if (AppGlobal.instance.get_server_ip()=="") {
            Toast.makeText(this, getString(R.string.msg_has_not_server_info), Toast.LENGTH_SHORT).show()
            return
        }

        var db = SimpleDatabaseHelper(this)
        val row = db.get(work_idx)
        val total_count = row!!["actual"].toString().toInt() + count.toInt()
        val seq = row!!["seq"].toString().toInt()

        val uri = "/senddata1.php"
        var params = listOf("mac_addr" to AppGlobal.instance.getMACAddress(),
            "didx" to AppGlobal.instance.get_design_info_idx(),
            "count" to count,
            "total_count" to total_count,
            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
            "factory_idx" to AppGlobal.instance.get_room_idx(),
            "line_idx" to AppGlobal.instance.get_line_idx(),
            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
            "seq" to seq)

        request(this, uri, true,true, params, { result ->

            var code = result.getString("code")
            var msg = result.getString("msg")
            if(code == "00"){

                var db = SimpleDatabaseHelper(this)
                db.updateWorkActual(work_idx, total_count)

                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish(true, 0, "ok", null)
            }else{
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }
}
