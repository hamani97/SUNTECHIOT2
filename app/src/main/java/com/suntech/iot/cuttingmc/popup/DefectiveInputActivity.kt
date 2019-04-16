package com.suntech.iot.cuttingmc.popup

import android.os.Bundle
import com.suntech.iot.cuttingmc.R
import com.suntech.iot.cuttingmc.base.BaseActivity
import kotlinx.android.synthetic.main.activity_defective_input.*

class DefectiveInputActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defective_input)
        initView()
    }

    private fun initView() {
        btn_confirm.setOnClickListener {
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }
}
