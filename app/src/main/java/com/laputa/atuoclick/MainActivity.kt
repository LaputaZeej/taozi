package com.laputa.atuoclick

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.laputa.atuoclick.util.li
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var mJob: Job? = null
    private lateinit var py: Py
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        applyClick(R.id.tv_info) {
            this.jumpAccessibilitySetting()
        }

        applyClick(R.id.action_01) {
            toast("action_01")
            mJob?.cancel()
            mJob = null
            mJob = lifecycleScope.launch {
                li("start")
                delay(10 * 1000L)
                li("end")
            }
        }

        applyClick(R.id.action_02) {
            toast("action_02")
            py.show()
        }

        applyClick(R.id.action_03) {
            toast("action_03")
            py.hide()
        }
        py = Py(this)
        // checkOverlay()
    }

//    private fun checkOverlay() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                alertExplainBeforeRequestOverlayPermission(true)
//            } else {
//                updateResult(true)
//            }
//        } else {
//            updateResult(true)
//        }
//    }
//
//    private fun alertExplainBeforeRequestOverlayPermission(show: Boolean) {
//        if (show) {
//            explainRequestOverlayPermission()
//        } else {
//            requestOverlayPermission(DEFAULT_REQUEST)
//        }
//    }
//
//    private fun explainRequestOverlayPermission() {
//        AlertDialog.Builder(this).setTitle("申请悬浮框权限")
//            .setMessage("Walle需要获得你的授权才能使用，请进入设置页面授权。").setPositiveButton("进入") { _, _ ->
//                requestOverlayPermission(DEFAULT_REQUEST)
//            }.setNegativeButton("取消") { _, _ -> updateResult(false) }
//            .create().show()
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when (requestCode) {
//            DEFAULT_REQUEST -> {
//                lifecycleScope.launch {
//                    delay(1000) // 不加延时会失败
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                        if (!Settings.canDrawOverlays(this@MainActivity)) {
//                            updateResult(false)
//                        } else {
//                            updateResult(true)
//                        }
//                    } else {
//                        updateResult(true)
//                    }
//                }
//            }
//            else -> {
//            }
//        }
//    }
//
//    private fun updateResult(b: Boolean) {
//
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        py.hide()
//
//    }
//
//    private fun requestOverlayPermission(requestCode: Int) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            startActivityForResult(
//                Intent(
//                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                    Uri.parse("package:${packageName}")
//                ), requestCode
//            )
//        }
//    }

    companion object {
        private const val DEFAULT_REQUEST = 0xFC
    }
}