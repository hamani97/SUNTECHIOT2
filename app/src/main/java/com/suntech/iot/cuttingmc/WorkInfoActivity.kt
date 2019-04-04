package com.suntech.iot.cuttingmc

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import com.suntech.iot.cuttingmc.base.BaseActivity
import kotlinx.android.synthetic.main.activity_work_info.*

class WorkInfoActivity : BaseActivity() {

    private var tab_pos : Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_info)
        initView()
    }

    private fun initView() {

        // Tab button click
        btn_work_info_server.setOnClickListener { tabChange(1) }
        btn_work_info_manual.setOnClickListener { tabChange(2) }

        btn_setting_cancel.setOnClickListener { finish() }
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