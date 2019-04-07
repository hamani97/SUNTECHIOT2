package com.suntech.iot.cuttingmc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.suntech.iot.cuttingmc.base.BaseFragment
import com.suntech.iot.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.layout_bottom_info.*

class HomeFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun initViews() {
        tv_app_version.text = "v " + activity.packageManager.getPackageInfo(activity.packageName, 0).versionName

        btn_count_view.setOnClickListener {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            } else {
                (activity as MainActivity).countViewType = 1
                (activity as MainActivity).changeFragment(1)
            }
        }
        btn_component_info.setOnClickListener {
            if (AppGlobal.instance.get_worker_no() == "" || AppGlobal.instance.get_worker_name() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_operator), Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(activity, ComponentInfoActivity::class.java)
                getBaseActivity().startActivity(intent, { r, c, m, d ->
                    if (r && d != null) {
                        (activity as MainActivity).countViewType = 2
                        (activity as MainActivity).changeFragment(1)
                    }
                })
            }
        }
        btn_work_info.setOnClickListener {
            if (AppGlobal.instance.get_factory() == "" || AppGlobal.instance.get_room() == "" || AppGlobal.instance.get_line() == "") {
                Toast.makeText(activity, getString(R.string.msg_no_setting), Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(activity, WorkInfoActivity::class.java))
            }
        }
        btn_setting_view.setOnClickListener { startActivity(Intent(activity, SettingActivity::class.java)) }
        updateView()
    }

    override fun onSelected() {
        updateView()
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    private fun updateView() {
        tv_factory.text = AppGlobal.instance.get_factory()
        tv_room.text = AppGlobal.instance.get_room()
        tv_line.text = AppGlobal.instance.get_line()
        tv_mc_no.text = AppGlobal.instance.get_mc_no1() //+ "-" + AppGlobal.instance.get_mc_no2()
        tv_mc_model.text = AppGlobal.instance.get_mc_model()
        tv_employee_no.text = AppGlobal.instance.get_worker_no()
        tv_employee_name.text = AppGlobal.instance.get_worker_name()
        tv_shift.text = AppGlobal.instance.get_current_shift_name()
    }
}