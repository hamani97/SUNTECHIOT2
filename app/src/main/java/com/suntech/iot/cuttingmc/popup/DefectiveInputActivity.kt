package com.suntech.iot.cuttingmc.popup

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.suntech.iot.cuttingmc.R
import com.suntech.iot.cuttingmc.base.BaseActivity
import kotlinx.android.synthetic.main.activity_defective_input.*

class DefectiveInputActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defective_input)
        initView()
    }

    fun parentSpaceClick(view: View) {
        var view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun initView() {
        btn_confirm.setOnClickListener {
        }
        btn_cancel.setOnClickListener {
            finish(false, 1, "ok", null)
        }
    }
}
