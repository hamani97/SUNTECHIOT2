package com.suntech.iot.cuttingmc.popup

import android.os.Bundle
import android.widget.Toast
import com.suntech.iot.cuttingmc.R
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import com.suntech.iot.cuttingmc.db.DBHelperForComponent
import kotlinx.android.synthetic.main.activity_actual_count_edit_input.*

class ActualCountEditInputActivity : BaseActivity() {

    private var _origin_actual = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actual_count_edit_input)
        initView()
    }

    private fun initView() {
        val work_idx = intent.getStringExtra("work_idx")
        val actual = intent.getStringExtra("actual")

        _origin_actual = actual.toInt()

        var db = DBHelperForComponent(this)
        val row = db.get(work_idx)

        if (row == null) {
            Toast.makeText(this, getString(R.string.msg_has_not_server_info), Toast.LENGTH_SHORT).show()
            finish()
        }

        tv_work_idx.setText(row!!["wosno"].toString())
        tv_work_model.setText(row!!["model"].toString())
        tv_work_size.setText(row!!["size"].toString())
        tv_work_actual.setText(row!!["actual"].toString())
        et_defective_qty.setText(actual)

        btn_actual_count_edit_plus.setOnClickListener {
            var value = et_defective_qty.text.toString().toInt()
            value++
            et_defective_qty.setText(value.toString())
        }
        btn_actual_count_edit_minus.setOnClickListener {
            var value = et_defective_qty.text.toString().toInt()
            if (value > 0) {
                value--
                et_defective_qty.setText(value.toString())
            }
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

        var db = DBHelperForComponent(this)
        val row = db.get(work_idx)
        if (row == null) {
            Toast.makeText(this, getString(R.string.msg_data_not_found), Toast.LENGTH_SHORT).show()
            return
        } else {
            val actual = count.toInt()
            db.updateWorkActual(work_idx, actual)

            // 토탈 카운트도 재계산
            val total_actual = AppGlobal.instance.get_current_shift_actual_cnt()
            val new_actual = total_actual + actual - _origin_actual
            AppGlobal.instance.set_current_shift_actual_cnt(new_actual)

            finish(true, 0, "ok", null)
        }
//        val total_count = row!!["actual"].toString().toInt() + count.toInt()
//        val seq = row!!["seq"].toString().toInt()

//        val uri = "/senddata1.php"
//        var params = listOf("mac_addr" to AppGlobal.instance.getMACAddress(),
//            "didx" to AppGlobal.instance.get_design_info_idx(),
//            "count" to count,
//            "total_count" to total_count,
//            "factory_parent_idx" to AppGlobal.instance.get_factory_idx(),
//            "factory_idx" to AppGlobal.instance.get_room_idx(),
//            "line_idx" to AppGlobal.instance.get_line_idx(),
//            "shift_idx" to  AppGlobal.instance.get_current_shift_idx(),
//            "seq" to seq)
//
//        request(this, uri, true,true, params, { result ->
//
//            var code = result.getString("code")
//            var msg = result.getString("msg")
//            if(code == "00"){
//
//                var db = SimpleDatabaseHelper(this)
//                db.updateWorkActual(work_idx, total_count)
//
//                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//                finish(true, 0, "ok", null)
//            }else{
//                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
//            }
//        })
    }
}