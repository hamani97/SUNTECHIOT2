package com.suntech.iot.cuttingmc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suntech.iot.cuttingmc.base.BaseFragment

class CountViewFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_count_view, container, false)
    }
}