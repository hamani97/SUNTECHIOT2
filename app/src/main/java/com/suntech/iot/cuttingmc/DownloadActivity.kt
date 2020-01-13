package com.suntech.iot.cuttingmc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.NetworkOnMainThreadException
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.suntech.iot.cuttingmc.base.BaseActivity
import com.suntech.iot.cuttingmc.common.AppGlobal
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class DownloadActivity : BaseActivity() {

    val temp_file = "Cutting_Debug.apk"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        AppGlobal.instance.setContext(this)

        if (Build.VERSION.SDK_INT >= 23) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }

//        download_WebLink()

        object : Thread() {
            override fun run() {
                download_WebLink()
            }
        }.start()
    }

    fun download_WebLink() {

        try {
            Log.e("DOWNLOAD", " Start")
            var url = URL("http://115.68.227.31" + "/apk/cutting" + "/" + "cutting-debug_1.2.7.apk")

            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            conn.setDoOutput(true)
            conn.connect()

            Log.e("DOWNLOAD", " Connect")

            val sd_card_root = Environment.getExternalStorageDirectory()
            val file = File(sd_card_root, temp_file)
            val file_output = FileOutputStream(file)

            Log.e("DOWNLOAD", " File output")

            val input_stream: InputStream = conn.getInputStream()
            val total_size = conn.getContentLength()
            var down_size = 0

            val buffer = ByteArray(1024)
            var bufferLength = 0

            while (input_stream.read(buffer).also({ bufferLength = it }) > 0) {
                file_output.write(buffer, 0, bufferLength)
                down_size += bufferLength
                //mProgressBar.setProgress(downloadedSize);
                Log.e("DOWNLOAD", "saving...")
            }
            file_output.close()

            Log.e("DOWNLOAD", "End")

            Log.e("DOWNLOAD", "InstallAPK Method Called")
            installAPK()

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            finish()
        } catch (e: NetworkOnMainThreadException) {
            e.printStackTrace()
            finish()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            finish()
        } catch (e: IOException) {
            e.printStackTrace()
            finish()
        }
    }

    fun installAPK() {

        Log.e("InstallApk", "Start")

//        val sd_card_root = Environment.getExternalStorageDirectory()
//        val file = File(sd_card_root, temp_file)

        val file = File("/sdcard/" + temp_file)

//        val apkUri: Uri = Uri.fromFile(file)

        val webLinkIntent = Intent(Intent.ACTION_VIEW)
        webLinkIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        webLinkIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        webLinkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")

        startActivity(webLinkIntent)
    }
}