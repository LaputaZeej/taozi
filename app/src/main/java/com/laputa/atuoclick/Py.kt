package com.laputa.atuoclick

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.laputa.atuoclick.util.ld
import com.laputa.atuoclick.util.li
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import java.lang.IllegalStateException
import kotlin.math.abs

class Py(private val lifecycleOwner: LifecycleOwner) {

    private val mContext: Context = lifecycleOwner.tryContext().applicationContext
    private val mWindowManager: WindowManager =
        mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val coroutineScope =
        CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineName("py"))
    private var mView: View? = null

    init {
        AutoAccessibilityService.stateChannel.receiveAsFlow().onEach {
            try {
                refreshState(it)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }.launchIn(coroutineScope)

        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {

                    Lifecycle.Event.ON_CREATE -> {
                        if (!Settings.canDrawOverlays(mContext)) {
                            alertExplainBeforeRequestOverlayPermission(true)
                        } else {
                            onStateChanged(true)
                        }
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        hide()
                        onDragDoubleClickListener=null
                        coroutineScope.cancel()
                    }
                    else -> {
                    }
                }
            }
        })
    }

    private fun refreshState(status: AutoAccessibilityService.Status = AutoAccessibilityService.Status()) {
        mView?.run {
            val pause = status.pause
            findViewById<ImageView>(R.id.iv_logo).setBackgroundColor(if (pause) Color.RED else Color.TRANSPARENT)
        }
    }

//    private val callRequestPermissionActivityResultLauncher =
//        lifecycleOwner.tryContext().registerForActivityResult(ActivityResultContracts.RequestPermission()) {
//            updateResult(it)
//        }

    private val requestOverlayActivityResultLauncher = lifecycleOwner.tryContext().run {
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            coroutineScope.launch {
                delay(1000) // ?????????????????????
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this@run)) {
                        onStateChanged(false)
                    } else {
                        onStateChanged(true)
                    }
                } else {
                    onStateChanged(true)
                }
            }
        }
    }

    private fun alertExplainBeforeRequestOverlayPermission(show: Boolean) {
        if (show) {
            explainRequestOverlayPermission()
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        requestOverlayActivityResultLauncher.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${mContext.packageName}")
            )
        )
    }

    private fun explainRequestOverlayPermission() {
        val activity = lifecycleOwner.tryContext()
        AlertDialog.Builder(activity).setTitle("?????????????????????")
            .setMessage("Walle?????????????????????????????????????????????????????????????????????").setPositiveButton("??????") { _, _ ->
                requestOverlayPermission()
            }.setNegativeButton("??????") { _, _ -> onStateChanged(false) }
            .create().show()

    }

    private fun onStateChanged(b: Boolean) {
        if (b) {
            show()
        }
    }

    private fun LifecycleOwner.tryContext(): FragmentActivity {
        return when (this) {
            is FragmentActivity -> {
                this
            }
            is Fragment -> {
                requireActivity()
            }
            else -> throw IllegalStateException("?????????LifecycleOwner ???$this")
        }
    }


    fun show() {
        mView = LayoutInflater.from(mContext).inflate(R.layout.view_py, null, false)
            ?.also { view ->
                initView(view)
                //initTouchEvent(view)
                mWindowManager.addView(
                    view,
                    WindowManager.LayoutParams().apply {
                        width = WindowManager.LayoutParams.WRAP_CONTENT
                        height = WindowManager.LayoutParams.WRAP_CONTENT
                        x = 0
                        y = 0
                        format = PixelFormat.RGBA_8888
                        gravity = Gravity.LEFT or Gravity.TOP
                        flags =
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        windowAnimations = 0
                    }.apply {
                        // ???Android 8.0????????????????????????????????????TYPE_PHONE?????????????????????????????????????????????????????????????????????
                        // ???Android 8.0????????????API?????????????????????????????????SYSTEM_ALERT_WINDOW???????????????????????????????????????????????????????????????????????????????????????????????????
                        // - TYPE_PHONE
                        // - TYPE_PRIORITY_PHONE
                        // - TYPE_SYSTEM_ALERT
                        // - TYPE_SYSTEM_OVERLAY
                        // - TYPE_SYSTEM_ERROR
                        // ???????????????????????????????????????????????????????????????????????????????????????TYPE_APPLICATION_OVERLAY????????????
                        flags = FLAG_NOT_FOCUSABLE
                        type =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            else WindowManager.LayoutParams.TYPE_PHONE
                    }
                )
            }
    }

    fun hide() {
        mView?.run {
            if (this.isAttachedToWindow) {
                mWindowManager.removeViewImmediate(this)
            }
        }
    }

    private fun initView(view: View) {
        view.setOnClickListener {
            li("click")
            channel.sendBlocking(Unit)
        }
        refreshState()
    }

    private var mDrag = false
    private var downX = 0f
    private var downY = 0f
    private val mDragViewLocation = IntArray(2)
    private val mDragViewRect = Rect()
    private val mViewRect = Rect()
    private var lastClickTime: Long = 0
    private var onDragDoubleClickListener: ((View) -> Unit)? = {
        mContext.jumpActivity<MainActivity>()
    }

    // ??????
    private var mSpread: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    private fun initTouchEvent(view: View) {
        val v = view.findViewById<View>(R.id.iv_logo)
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    ld("down (x,y) = ($downX,$downY)")
                    val positionInView = v.containsPoint(event.rawX, event.rawY)
                    if (positionInView) {
                        if (!mSpread) {//???????????????
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime < 500) {
                                onDragDoubleClickListener?.invoke(view)
                                return@setOnTouchListener true
                            } else {
                                lastClickTime = now
                                view.performClick()
                                //return@setOnTouchListener true
                            }
                        }
                        mDrag = true
                        v.setBackgroundColor(Color.WHITE)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mDrag) {
                        val scaledTouchSlop = ViewConfiguration.get(mContext).scaledTouchSlop
                        val moveX = event.rawX
                        val moveY = event.rawY
                        val offsetX = moveX - downX
                        val offsetY = moveY - downY
                        if (!(abs(offsetX) < scaledTouchSlop && abs(offsetY) < scaledTouchSlop)) {
                            moveBy(offsetX.toInt(), offsetY.toInt())
                            downX = moveX
                            downY = moveY
                            return@setOnTouchListener true
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    ld("up")
                    if (mDrag) {
                        mDrag = false
                        val offsetX = event.rawX - downX
                        val offsetY = event.rawY - downY
                        val scaledTouchSlop = ViewConfiguration.get(mContext).scaledTouchSlop
                        // ????????????
                        val newLayoutParam = view.layoutParams.run {
                            this as WindowManager.LayoutParams
                        }.also {
                            val lastX = it.x + offsetX
                            val lastY = it.y + offsetY
                            // ?????????????????????View????????????????????????
                            /*val viewWidth =if (mSpread){
                                mViewRect.right - mViewRect.left
                            }else{
                                mDragViewRect.right-mViewRect.left
                            }*/

                            val viewWidth = view.measuredWidth
                            val finalX: Float = when {
                                lastX + viewWidth / 2 < mContext.screenWidth / 2 -> {
                                    0f
                                }
                                else -> {
                                    mContext.screenWidth - viewWidth.toFloat()
                                }

                            }
                            val viewHeight = view.measuredHeight
                            val topLimit = mContext.screenHeight / 10
                            val bottomLimit = mContext.screenHeight * 9 / 10
                            li(
                                "lastY = $lastY bottomLimit=$bottomLimit topLimit=$topLimit"
                            )
                            val finalY: Float = when {

                                lastY < topLimit -> {
                                    topLimit.toFloat()
                                }
                                lastY >= bottomLimit.toFloat() - viewHeight -> {
                                    bottomLimit.toFloat() - viewHeight
                                }
                                else -> {
                                    lastY
                                }
                            }
                            it.x = finalX.toInt()
                            it.y = finalY.toInt()
                        }
                        v.setBackgroundColor(Color.GREEN)
                        mWindowManager.updateViewLayout(view, newLayoutParam)
                        return@setOnTouchListener true
                    }
                }
            }

            false
        }
    }

    private fun View.containsPoint(x: Float, y: Float): Boolean {
        this.getLocationOnScreen(mDragViewLocation)//???????????????
        this.getLocalVisibleRect(mDragViewRect)//?????????????????????
        return x > mDragViewLocation[0] && x < mDragViewLocation[0] + mDragViewRect.right - mDragViewRect.left && y > mDragViewLocation[1] && y < mDragViewLocation[1] + mDragViewRect.bottom - mDragViewRect.top
    }


    private fun moveBy(x: Int, y: Int) {
        mView?.run {
            if (isAttachedToWindow) {
                val newLayoutParam = this.layoutParams.run {
                    this as WindowManager.LayoutParams
                }.also { param ->
                    param.x += x
                    param.y += y
                }
                mWindowManager.updateViewLayout(this, newLayoutParam)
            }
        }


    }

    companion object {
        val channel =
            kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)

        private const val DEFAULT_REQUEST = 0xFC
    }
}