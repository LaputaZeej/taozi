package com.laputa.atuoclick

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Context.jumpAccessibilitySetting() {
    try {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

fun Activity.applyClick(id: Int, onClick: (View) -> Unit) {
    findViewById<View>(id)?.setOnClickListener {
        onClick(it)
    }
}

fun Context.toast(msg:String){
    if (msg.isBlank())return
    Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
}

val Context.screenWidth: Int
    get() = resources.displayMetrics.widthPixels

val Context.screenHeight: Int
    get() = resources.displayMetrics.heightPixels

/**
 * 状态栏高度
 */
val Context.statusBarHeight: Int
    get() = resources.getDimensionPixelSize(
        resources.getIdentifier(
            "status_bar_height",
            "dimen",
            "android"
        )
    )

typealias IntentInit = (Intent) -> Unit

inline fun <reified T : Activity> Context.jumpActivity(noinline block: IntentInit? = null) =
    when (this) {
        is Activity -> {
            startActivity(Intent(this, T::class.java).apply {
                block?.invoke(this)
            })
        }
        is Fragment -> {
            requireActivity().startActivity(Intent(this, T::class.java).apply {
                block?.invoke(this)
            })
        }
        is Application -> {
            getPackageManager().getLaunchIntentForPackage(packageName)?.let {
                startActivity(it.apply {
                    block?.invoke(this)
                })
            }
        }
        else -> {
            throw IllegalStateException("错误的context")
        }
    }