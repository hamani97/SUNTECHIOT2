package com.suntech.iot.cuttingmc

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.base.BaseFragment
import com.suntech.iot.cuttingmc.common.AppGlobal
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_side_menu.*

class MainActivity : BaseActivity() {

    var countViewType = 1       // Count view 화면값 1=Total count, 2=Component count

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppGlobal.instance.setContext(this)

        // button click event
        if (AppGlobal.instance.get_long_touch()) {
            btn_home.setOnLongClickListener { changeFragment(0); true }
//            btn_push_to_app.setOnLongClickListener { startActivity(Intent(this, PushActivity::class.java));true }
//            btn_actual_count_edit.setOnLongClickListener { startActivity(Intent(this, ActualCountEditActivity::class.java)); true }
        } else {
            btn_home.setOnClickListener { changeFragment(0) }
//            btn_push_to_app.setOnClickListener { startActivity(Intent(this, PushActivity::class.java)) }
//            btn_actual_count_edit.setOnClickListener { startActivity(Intent(this, ActualCountEditActivity::class.java)) }
        }

        // fragment & swipe
        val adapter = TabAdapter(supportFragmentManager)
        adapter.addFragment(HomeFragment(), "")
        adapter.addFragment(CountViewFragment(), "")
        vp_fragments.adapter = adapter
        adapter.notifyDataSetChanged()
        vp_fragments.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(state: Int) {
                (adapter.getItem(state) as BaseFragment).onSelected()
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(position: Int) {}
        })
    }

    public override fun onResume() {
        super.onResume()
    }

    private fun updateView() {

    }

    fun changeFragment(pos:Int) {
        vp_fragments.setCurrentItem(pos, true)
    }


    private class TabAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        private val mFragments = ArrayList<Fragment>()
        private val mFragmentTitles = ArrayList<String>()

        override fun getCount(): Int { return mFragments.size }
        fun addFragment(fragment: Fragment, title: String) {
            mFragments.add(fragment)
            mFragmentTitles.add(title)
        }
        override fun getItem(position: Int): Fragment {
            return mFragments.get(position)
        }
        override fun getItemPosition(`object`: Any?): Int {
            return PagerAdapter.POSITION_NONE
        }
        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitles.get(position)
        }
    }
}