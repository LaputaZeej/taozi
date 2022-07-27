package com.laputa.atuoclick.util

import android.util.Log

private const val DEFAULT_TAG = "_py_"
private const val DEBUG = true

fun ld(msg: String) {
    if (!DEBUG) return
    Log.d(DEFAULT_TAG, msg)
}

fun li(msg: String) {
    if (!DEBUG) return
    Log.i(DEFAULT_TAG, msg)
}

fun le(msg: String) {
    if (!DEBUG) return
    Log.e(DEFAULT_TAG, msg)
}

fun li(tag: String, msg: String) {
    if (!DEBUG) return
    if (tag.isBlank()) li(msg)
    else Log.i(tag, msg)
}

fun le(tag: String, msg: String) {
    if (!DEBUG) return
    if (tag.isBlank()) le(msg)
    else Log.e(tag, msg)
}