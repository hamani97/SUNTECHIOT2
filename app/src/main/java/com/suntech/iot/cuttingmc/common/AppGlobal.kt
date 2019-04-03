package com.suntech.iot.cuttingmc.common

import android.content.Context
import android.util.Log
import com.suntech.iot.cuttingmc.util.UtilLocalStorage
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

class AppGlobal private constructor() {

    private var _context : Context? = null
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

    fun set_mc_no1(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_no1", idx) }
    fun get_mc_no1() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_no1") }
    fun set_mc_model(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_mc_model", idx) }
    fun get_mc_model() : String { return UtilLocalStorage.getString(instance._context!!, "current_mc_model") }

    fun set_long_touch(state: Boolean) { UtilLocalStorage.setBoolean(instance._context!!, "current_long_touch", state) }
    fun get_long_touch() : Boolean { return UtilLocalStorage.getBoolean(instance._context!!, "current_long_touch") }

    fun set_server_ip(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_ip", idx) }
    fun get_server_ip() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_ip") }
    fun set_server_port(idx: String) { UtilLocalStorage.setString(instance._context!!, "current_server_port", idx) }
    fun get_server_port() : String { return UtilLocalStorage.getString(instance._context!!, "current_server_port") }

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


    // 작업자 정보 설정
    fun set_worker_no(name: String) { UtilLocalStorage.setString(instance._context!!, "current_worker_no", name) }
    fun get_worker_no() : String { return UtilLocalStorage.getString(instance._context!!, "current_worker_no") }
    fun set_worker_name(name: String) { UtilLocalStorage.setString(instance._context!!, "current_worker_name", name) }
    fun get_worker_name() : String { return UtilLocalStorage.getString(instance._context!!, "current_worker_name") }


    // Shift info
    fun get_current_shift_name() : String {
//        var item: JSONObject = get_current_shift_time() ?: return ""
//        return item["shift_name"].toString()
        return ""
    }
}