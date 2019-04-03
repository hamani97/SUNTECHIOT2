package com.suntech.iot.cuttingmc.common

import android.content.Context
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import com.suntech.iot.cuttingmc.util.OEEUtil
import com.suntech.iot.cuttingmc.util.UtilLocalStorage
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

class AppGlobal private constructor() {

    private var _context : Context? = null
    var deviceToken : String = ""           // 디바이스 정보
    var _server_state : Boolean = false

    private object Holder { val INSTANCE = AppGlobal() }

    companion object {
        val instance: AppGlobal by lazy { Holder.INSTANCE }
    }
    fun setContext(ctx : Context) { _context = ctx }

    // Default Setting
    fun set_factory_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_factory_idx", idx) }
    fun get_factory_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_factory_idx") }
    fun set_factory(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_factory", idx) }
    fun get_factory() : String { return UtilLocalStorage.getString(instance._context!!, "current_factory") }

    fun set_room_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_room_idx", idx) }
    fun get_room_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_room_idx") }
    fun set_room(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_room", idx) }
    fun get_room() : String { return UtilLocalStorage.getString(instance._context!!, "current_room") }

    fun set_line_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_line_idx", idx) }
    fun get_line_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_line_idx") }
    fun set_line(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_line", idx) }
    fun get_line() : String { return UtilLocalStorage.getString(instance._context!!, "current_line") }

    fun set_mc_no_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_no_idx", idx) }
    fun get_mc_no_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_no_idx") }
    fun set_mc_no1(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_no1", idx) }
    fun get_mc_no1() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_no1") }
    fun set_mc_serial(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_serial", idx) }
    fun get_mc_serial() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_serial") }

    fun set_mc_model_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_model_idx", idx) }
    fun get_mc_model_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_model_idx") }
    fun set_mc_model(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_model", idx) }
    fun get_mc_model() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_model") }

    fun set_long_touch(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "current_long_touch", state) }
    fun get_long_touch() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "current_long_touch") }

    fun set_server_ip(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_ip", idx) }
    fun get_server_ip() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_ip") }
    fun set_server_port(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_port", idx) }
    fun get_server_port() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_port") }


    // Component 필터 세팅값
    fun set_compo_wos_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_wos_idx", idx) }
    fun get_compo_wos_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_wos_idx") }
    fun set_compo_wos(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_wos", value) }
    fun get_compo_wos() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_wos") }

    fun set_compo_model(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_model", value) }
    fun get_compo_model() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_model") }
    fun set_compo_style(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_style", value) }
    fun get_compo_style() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_style") }

    fun set_compo_component_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_component_idx", idx) }
    fun get_compo_component_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_component_idx") }
    fun set_compo_component(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_component", value) }
    fun get_compo_component() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_component") }
    //
    fun set_compo_size_idx(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_size_idx", idx) }
    fun get_compo_size_idx() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_size_idx") }
    fun set_compo_size(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_size", value) }
    fun get_compo_size() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_size") }
    fun set_compo_target(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_target", value) }
    fun get_compo_target() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_target") }

    fun set_compo_layer(value: String) { UtilLocalStorage.setString(instance._context!!, "current_compo_layer", value) }
    fun get_compo_layer() : String { return UtilLocalStorage.getString(instance._context!!, "current_compo_layer") }


    // 작업자 정보 설정
    fun set_worker_no(name: String) { UtilLocalStorage.setString(instance._context!!, "current_worker_no", name) }
    fun get_worker_no() : String { return UtilLocalStorage.getString(instance._context!!, "current_worker_no") }
    fun set_worker_name(name: String) { UtilLocalStorage.setString(instance._context!!, "current_worker_name", name) }
    fun get_worker_name() : String { return UtilLocalStorage.getString(instance._context!!, "current_worker_name") }


    // Layer정보 = pair 수
    fun set_layer_pairs(layer_no: String, pair: String) { UtilLocalStorage.setString(instance._context!!, "current_layer_" + layer_no, pair) }
    fun get_layer_pairs(layer_no: String) : String { return UtilLocalStorage.getString(instance._context!!, "current_layer_" + layer_no) }

    // server, manual 방식
    fun set_target_type(value: Int) { UtilLocalStorage.setInt(instance._context!!, "current_target_type", value) }
    fun get_target_type() : Int { return UtilLocalStorage.getInt(instance._context!!, "current_target_type") }

    fun set_target_manual_shift(shift_no: String, value: String) { UtilLocalStorage.setString(instance._context!!, "current_target_shift_" + shift_no, value) }
    fun get_target_manual_shift(shift_no: String) : String { return UtilLocalStorage.getString(instance._context!!, "current_target_shift_" + shift_no) }

    // Shift info
    fun get_current_shift_idx() : String {
        var item: JSONObject = get_current_shift_time() ?: return ""
        return item["shift_idx"].toString()
    }
    fun get_current_shift_name() : String {
//        var item: JSONObject = get_current_shift_time() ?: return ""
//        return item["shift_name"].toString()
        return ""
    }
    fun get_current_shift_time_idx() : Int {
        val list = get_current_work_time()
        if (list.length() == 0 ) return -1
        val now = DateTime()
        var current_shift_idx = -1

        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)
            var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

            if (now.millis <= shift_etime.millis) {
                current_shift_idx = i
                break
            }
        }
        return current_shift_idx
    }
    fun get_current_shift_time() : JSONObject? {
        val list = get_current_work_time()
        if (list.length() == 0 ) return null
        val idx = get_current_shift_time_idx()
        if (idx < 0) return null
        return list.getJSONObject(idx)
    }

    fun set_today_work_time(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "current_work_time", data) }
    fun get_today_work_time() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "current_work_time") }
    fun set_prev_work_time(data: JSONArray) { UtilLocalStorage.setJSONArray(instance._context!!, "current_prev_work_time", data) }
    fun get_prev_work_time() : JSONArray { return UtilLocalStorage.getJSONArray(instance._context!!, "current_prev_work_time") }

    // 어제시간과 오늘시간 중에 지나지 않은 날짜를 선택해서 반환
    fun get_current_work_time() : JSONArray {
        val today = get_today_work_time()
        val yesterday = get_prev_work_time()
        val now = DateTime()
        if (yesterday.length()>0) {
            val item = yesterday.getJSONObject(yesterday.length()-1)
            var shift_etime = OEEUtil.parseDateTime(item["work_etime"].toString())

            if (shift_etime.millis > now.millis) {
                return yesterday
            }
        }
        return today
    }

    fun get_mac_address(): String? {
        var mac = getMACAddress()
        if (mac == "") mac = "NO_MAC_ADDRESS"
        return mac
    }
    fun getMACAddress(): String {
        val interfaceName = "wlan0"
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.getName().equals(interfaceName)) continue

                val mac = intf.getHardwareAddress() ?: return ""
                val buf = StringBuilder()
                for (idx in mac.indices)
                    buf.append(String.format("%02X:", mac[idx]))
                if (buf.length > 0) buf.deleteCharAt(buf.length - 1)
                return buf.toString()
            }
        } catch (ex: Exception) {
            Log.e("Error", ex.toString())
        }
        return ""
    }

    fun get_local_ip(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val interf = en.nextElement()
                val ips = interf.inetAddresses
                while (ips.hasMoreElements()) {
                    val inetAddress = ips.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress.toString()
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e("Error", ex.toString())
        }
        return ""
    }

    // Network & Wifi check
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }
    fun isWifiConnected(context: Context): Boolean {
        if (isNetworkAvailable(context)) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }
        return false
    }
    fun isEthernetConnected(context: Context): Boolean {
        if (isNetworkAvailable(context)) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo.type == ConnectivityManager.TYPE_ETHERNET
        }
        return false
    }
    fun getWiFiSSID(context: Context): String {
        if (isWifiConnected(context)) {
            val manager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = manager.connectionInfo
            return wifiInfo.ssid
        }
        else if (isEthernetConnected(context)) {
            return "Ethernet"
        }
        return "unknown or no connected"
    }

    fun playSound(context: Context) {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isOnline(context: Context) : Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }
}