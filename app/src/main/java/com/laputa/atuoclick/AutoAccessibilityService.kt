package com.laputa.atuoclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.laputa.atuoclick.util.ld
import com.laputa.atuoclick.util.le
import com.laputa.atuoclick.util.li
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class AutoAccessibilityService : AccessibilityService() {
    private val coroutineScope =
        CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineName("auto"))
    private var mAutoJob: Job? = null
    private var mIdleJob: Job? = null
    private val mStart: AtomicBoolean = AtomicBoolean(false)
    private val mPause: AtomicBoolean = AtomicBoolean(false)
    private val mIdle: AtomicBoolean = AtomicBoolean(false)
    private var mCount: Long = 0
    private var mTotal: Long = 0L
    private var mStartTime = 0L

    class Status(
        val start: Boolean = false,
        val pause: Boolean = false,
        val idle: Boolean = false
    )

    init {
        Py.channel.receiveAsFlow()
            .onEach {
                try {
                    if (!mStart.get()) return@onEach
                    if (!mPause.get()) {
                        pause(true)
                        toast("【暂停】")
                    } else {
                        pause(false)
                        toast("【继续】")
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }.launchIn(coroutineScope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        // check
        ld("[AutoAccessibilityService]::onAccessibilityEvent event:$event")
        if (event.packageName != PKG) {

        } else {
            start()
            checkIdle(event.text.toList(), event.className)
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {

                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {

                }
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {

                }
                else -> {

                }
            }
        }
    }

    private fun checkIdle(
        tx: List<CharSequence>,
        clz: CharSequence?
    ) {
        if (mIdle.get()) return
        fun List<CharSequence>.has(text: String): Boolean {
            if (isEmpty()) return false
            le("has $this")
            this.forEach {
                if (it.toString().contains(text))
                    return true
            }
            return false
        }
        if (tx.has(TOAST_ERROR) && clz == CLZ) {
            le("[AutoAccessibilityService]::onAccessibilityEvent 休息一会儿!total=$mTotal tx=$tx ,clz=$clz")
            mIdleJob?.cancel()
            mIdleJob = null
            mIdle.set(true)
            mIdleJob = coroutineScope.launch {
                le("[AutoAccessibilityService]::onAccessibilityEvent 开始休息 ...")
                delay(INTERVAL_IDLE)
                le("[AutoAccessibilityService]::onAccessibilityEvent 休息好了")
                mIdle.set(false)
            }.also {
                it.invokeOnCompletion {
                    le("[AutoAccessibilityService] mIdleJob::invokeOnCompletion ")
                }
            }
        }
    }

    private fun pause(enable: Boolean) {
        mPause.set(enable)
        stateChannel.sendBlocking(Status(start = mStart.get(), pause = enable, idle = mIdle.get()))
    }

    private fun start() {
        if (mStart.get()) return
        mStart.set(true)
        pause(false)
        mIdle.set(false)
        if (mStartTime == 0L) {
            mStartTime = System.currentTimeMillis()
        }
        mCount = 0
        mTotal = 0

        li("=================================")
        li("[AutoAccessibilityService]::start")
        mAutoJob?.cancel()
        mAutoJob = null
        mAutoJob = coroutineScope.launch {
            li("[AutoAccessibilityService]5秒后开启")
            delay(INTERVAL_WAIT)
            li("[AutoAccessibilityService]开启")
            while (isActive) {
                if (!mStart.get()) {
                    break
                }
                if (mPause.get()) {
                    le("mPause.....")
                    delay(INTERVAL_WAIT)
                    continue
                }
                if (mIdle.get()) {
                    le("mIdle.....")
                    delay(INTERVAL_WAIT * 2)
                    continue
                }
                delay(randomInterval())
                val point = randomPoint()
                doClick(point.first, point.second)
                mCount++
                mTotal++
                if (mCount >= COUNT_MAX) {
                    val time = System.currentTimeMillis() - mStartTime
                    li("[AutoAccessibilityService]超过最大值${mCount}/${mTotal}，用时${time / 1000}秒。等待5秒继续")
                    onClickTotalChanged()
                    delay(INTERVAL_WAIT)
                    mCount = 0
                }
            }
        }
    }

    private fun randomOffset(): Int {
        val offset = Random.nextInt(OFFSET_MAX)
        val unit = if (Random.nextBoolean()) {
            1
        } else -1
        return offset * unit
    }

    private fun randomInterval(): Long {
        return INTERVAL_BASE + Random.nextInt(INTERVAL_OFFSET)
    }

    private fun randomPoint(): Pair<Float, Float> {
        val widthPixels = resources.displayMetrics.widthPixels
        val heightPixels = resources.displayMetrics.heightPixels
        val x = widthPixels / 3f + randomOffset()
        val y = heightPixels / 3f + randomOffset()
        return x to y
    }

    private fun stop() {
        li("[AutoAccessibilityService]::stop")
        mStart.set(false)
        mAutoJob?.cancel()
        mAutoJob = null
        mTotal = 0
        mCount = 0
        mStartTime = 0L
        mIdleJob?.cancel()
        mIdleJob = null
        mIdle.set(false)
        pause(false)
        toast("结束了")
    }

    override fun onInterrupt() {
        le("[AutoAccessibilityService]::onInterrupt")
        stop()
        onClickTotalChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        le("[AutoAccessibilityService]::onDestroy")
        coroutineScope.cancel()
    }

    private fun onClickTotalChanged() {
        if (mTotal > 0) {
            toast("此次你一共点击了${mTotal}次")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        li("[AutoAccessibilityService]::onServiceConnected")
        toast("\uD83C\uDF51🍑🍑准备完毕🍑🍑\uD83C\uDF51")
    }

    companion object {
        private const val PKG = "com.ss.android.ugc.aweme"
        private const val CLZ = "android.app.Dialog"
        private const val OFFSET_MAX = 100
        private const val INTERVAL_OFFSET = 100
        private const val INTERVAL_BASE = 250L
        private const val COUNT_MAX = 100L
        private const val INTERVAL_WAIT = 5000L
        private const val TOAST_ERROR = "手速太快了"
        private const val INTERVAL_IDLE = 3 * 60 * 1000L

        val stateChannel = Channel<Status>()
    }
}

fun AccessibilityService.doClick(x: Float, y: Float) {
    val tag = "doClick"
    ld(">>> $tag : ($x,$y)")
    val path = Path().also {
        it.moveTo(x, y)
        it.lineTo(x, y)
    }
    val gesture = GestureDescription.Builder().addStroke(
        GestureDescription.StrokeDescription(path, 0, 1)
    ).build()
    this.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
        override fun onCancelled(gestureDescription: GestureDescription?) {
            super.onCancelled(gestureDescription)
            ld(">>> $tag onCancelled")
        }

        override fun onCompleted(gestureDescription: GestureDescription?) {
            super.onCompleted(gestureDescription)
            ld(">>> $tag onCompleted")
        }
    }, null)
}