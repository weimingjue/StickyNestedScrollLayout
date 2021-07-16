package com.wang.sticky

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * 悬浮view，基于NestedScrollView，所以只能有一个child（不使用悬浮可当做NestedScrollView的优化版）
 *
 *
 * 使用：
 * 设置tag：sticky即可悬浮
 *
 *
 * 附加：[.setOnStickyScrollChangedListener][.getStickyTop][.setChildTag]
 * tag：match撑满布局
 */
class StickyNestedScrollLayout @JvmOverloads
constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val scrollView: MyScrollView =
            MyScrollView(LinearLayout(context).apply { orientation = LinearLayout.VERTICAL })

    /**
     * 套一层，可以少很多逻辑
     */
    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (childCount > 0) {
            throw RuntimeException("只允许添加一个子类")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            scrollView.clipChildren = clipChildren
        }
        scrollView.addView(child, index, params)

        //添加sv
        super.addView(scrollView, -1, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        //添加ll
        super.addView(scrollView.llAdd, -1, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    /**
     * 只支持固定宽高
     */
    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        if (params.width == WRAP_CONTENT) {
            params.width = MATCH_PARENT
        }
        if (params.height == WRAP_CONTENT) {
            params.height = MATCH_PARENT
        }
        super.setLayoutParams(params)
    }

    /**
     * 高必须确定
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var hms = heightMeasureSpec
        if (MeasureSpec.getMode(hms) != MeasureSpec.EXACTLY) {
            hms = MeasureSpec.makeMeasureSpec(
                    min(resources.displayMetrics.heightPixels, MeasureSpec.getSize(hms)),
                    MeasureSpec.EXACTLY
            )
        }
        super.onMeasure(widthMeasureSpec, hms)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 私有类
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private class MyScrollView(val llAdd: LinearLayout) : NestedScrollView(llAdd.context) {
        /**
         * 请使用[getStickyViews]
         */
        @Suppress("PropertyName")
        var _stickyViews: ArrayList<StickyInfo>? = null
        private var isDown = false
        var listener: ((stickyPosition: Int, stickyTop: Int) -> Unit)? = null

        /**
         * 惯性滑动时手指按下则立即停止
         */
        override fun onNestedScroll(
                target: View,
                dxConsumed: Int,
                dyConsumed: Int,
                dxUnconsumed: Int,
                dyUnconsumed: Int,
                type: Int, consumed: IntArray
        ) {
            if (isDown && type != ViewCompat.TYPE_TOUCH) {
                return
            }
            super.onNestedScroll(
                    target,
                    dxConsumed,
                    dyConsumed,
                    dxUnconsumed,
                    dyUnconsumed,
                    type,
                    consumed
            )
        }

        /**
         * 修复滑动类似RecyclerView时不跟随的问题
         */
        override fun onNestedPreScroll(
                target: View,
                dx: Int,
                dy: Int,
                consumed: IntArray,
                type: Int
        ) {
            if (dy > 0) { //nsv往上滑不跟随，这里处理一下
                if (canScrollVertically(1)) {
                    val stickyTop = getStickyTop(getStickyViews().size - 1)
                    val dy2 = if (stickyTop > 0) min(dy, stickyTop) else dy
                    consumed[1] = consumed[1] + dy2
                    scrollBy(0, dy2)
                    return  //暂时先全部消耗掉（几像素）
                }
            }
            super.onNestedPreScroll(target, dx, dy, consumed, type)
        }

        /**
         * 如果子有match就设置为当前高
         */
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val height: Int = MeasureSpec.getSize(heightMeasureSpec)
            if (childCount > 0) {
                changeMatch(getChildAt(0), height)
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }

        private fun changeMatch(view: View, height: Int) {
            if (TAG_MATCH == view.tag || TAG_MATCH == view.getTag(R.id.tag_sticky)) {
                view.layoutParams?.height = height
            }
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    changeMatch(view.getChildAt(i), height)
                }
            }
        }

        /**
         * 当按下时停掉惯性滑动
         */
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDown = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDown = false
                }
            }
            return super.dispatchTouchEvent(ev)
        }

        fun getStickyViews(): ArrayList<StickyInfo> {
            if (_stickyViews != null) {
                return _stickyViews!!
            }
            _stickyViews = ArrayList(2)
            if (childCount > 0) {
                initStickyViews(getChildAt(0), 0)
            }
            return _stickyViews!!
        }

        /**
         * 遍历得到所有悬浮view
         */
        private fun initStickyViews(checkView: View, viewIndex: Int) {
            if (TAG_STICKY == checkView.tag || TAG_STICKY == checkView.getTag(R.id.tag_sticky)) {
                val info = StickyInfo(checkView, viewIndex)
                if (!getStickyViews().contains(info)) {
                    getStickyViews().add(info)
                }
                return
            }
            if (checkView is ViewGroup) {
                for (i in 0 until checkView.childCount) {
                    initStickyViews(checkView.getChildAt(i), i)
                }
            }
        }

        /**
         * 悬浮切换
         */
        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super.onScrollChanged(l, t, oldl, oldt)
            for ((i, info) in getStickyViews().withIndex()) {
                val top = getStickyTop(i)
                val index = llIndexOfSticky(info.stickyView)
                if (top > 0 && index >= 0) {//去掉悬浮
                    info.stickyParent.removeView(info.emptyView)
                    llAdd.removeView(info.stickyView)
                    info.stickyParams.height = info.stickyParamsHeight
                    info.stickyParent.addView(info.stickyView, info.stickyIndex, info.stickyParams)
                } else if (top <= 0 && index < 0) {//添加悬浮
                    info.stickyParent.removeView(info.stickyView)
                    llAdd.removeView(info.emptyView)
                    info.stickyParamsHeight = info.stickyParams.height
                    info.stickyParams.height = info.stickyView.height
                    info.stickyParent.addView(info.emptyView, info.stickyIndex, info.stickyParams)
                    val params: LinearLayout.LayoutParams = newLlParams(info.stickyParams)
                    params.leftMargin += getStickyParentLeftToThis(info.stickyParent)
                    llAdd.addView(info.stickyView, params)
                }
                listener?.let { it(i, top) }
            }
        }

        private fun llIndexOfSticky(sv: View): Int {
            for (i in 0 until llAdd.childCount) {
                if (llAdd.getChildAt(i) === sv) {
                    return i
                }
            }
            return -1
        }

        /**
         * 悬浮view的父类距自己左边
         */
        private fun getStickyParentLeftToThis(v: View?): Int {
            return if (v == null || v == this) 0 else v.left + getStickyParentLeftToThis(v.parent as View)
        }

        /**
         * 悬浮view距自己顶端
         */
        private fun getStickyTopToThis(v: View?): Int {
            return if (v == null || v == this) 0 else v.top + getStickyTopToThis(v.parent as View)
        }

        /**
         * @return 悬浮view距顶部距离
         */
        @IntRange(from = 0)
        fun getStickyTop(stickyIndex: Int): Int {
            if (stickyIndex < 0 || stickyIndex >= getStickyViews().size) {
                return 0
            }
            val info = getStickyViews()[stickyIndex]
            val v: View = info.stickyParent.getChildAt(info.stickyIndex)
            var top = getStickyTopToThis(v)
            if (v.layoutParams is MarginLayoutParams) {
                top -= (v.layoutParams as MarginLayoutParams).topMargin
            }
            top = top - scrollY + paddingTop - llAdd.height //去掉scroll和悬浮遮挡的
            if (llIndexOfSticky(info.stickyView) >= 0) { //去掉自己悬浮高
                val params: LinearLayout.LayoutParams =
                        info.stickyView.layoutParams as LinearLayout.LayoutParams
                top += info.stickyView.height + params.topMargin + params.bottomMargin
            }
            return max(top, 0)
        }

        /**
         * new 出来的，不是原对象
         */
        private fun newLlParams(lp: ViewGroup.LayoutParams): LinearLayout.LayoutParams {
            when (lp) {
                is LinearLayout.LayoutParams -> {
                    val params: LinearLayout.LayoutParams =
                            LinearLayout.LayoutParams(lp as MarginLayoutParams)
                    params.gravity = lp.gravity
                    return params
                }
                is LayoutParams -> {
                    val params: LinearLayout.LayoutParams =
                            LinearLayout.LayoutParams(lp as MarginLayoutParams)
                    params.gravity = lp.gravity
                    return params
                }
                is MarginLayoutParams -> {
                    return LinearLayout.LayoutParams(lp)
                }
                else -> return LinearLayout.LayoutParams(lp)
            }
        }

        /**
         * @param stickyIndex 可能会因parent addView变化导致不准，暂时先忽略这种情况
         */
        private class StickyInfo(val stickyView: View, val stickyIndex: Int) {
            val stickyParams: ViewGroup.LayoutParams = stickyView.layoutParams
            val stickyParent = stickyView.parent as ViewGroup
            var stickyParamsHeight = 0

            val emptyView = ViewProxy(stickyView)

            override fun equals(other: Any?) =
                    if (other is StickyInfo) stickyView === other.stickyView else super.equals(other)

            override fun hashCode(): Int {
                var result = stickyView.hashCode()
                result = 31 * result + stickyParams.hashCode()
                result = 31 * result + stickyParent.hashCode()
                result = 31 * result + stickyIndex
                result = 31 * result + stickyParamsHeight
                result = 31 * result + emptyView.hashCode()
                return result
            }
        }
    }

    /**
     * 代理view（目前只代理findViewById）
     */
    private class ViewProxy(private val targetView: View) : View(targetView.context) {

        init {
            id = targetView.id
        }

        /**
         * 此处重写findViewById来代理
         */
        protected fun <T : View?> findViewTraversal(@IdRes id: Int) = targetView.findViewById<T>(id)
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 公共方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取第Index悬浮view距顶部的距离（默认第一个）
     * @return 0为悬浮中
     */
    @JvmOverloads
    @IntRange(from = 0)
    fun getStickyTop(stickyIndex: Int = 0): Int {
        return scrollView.getStickyTop(stickyIndex)
    }

    /**
     * @param listener stickyPosition：第几个悬浮，单悬浮可忽略
     *                 stickyTop      悬浮view距顶部距离，从max-0
     */
    fun setOnStickyScrollChangedListener(listener: ((stickyPosition: Int, stickyTop: Int) -> Unit)?) {
        scrollView.listener = listener
    }

    /**
     * 获得悬浮时的父布局
     * 你可以设置background、padding等
     */
    fun getStickyParent() = scrollView.llAdd

    /**
     * 获得内部滑动的view，可设置Scroll相关操作
     */
    fun getScrollView(): NestedScrollView = scrollView

    /**
     * 备用方法：当tag被占用时
     *
     * @param child 直接或间接子类
     * @param tag   [TAG_STICKY]、[TAG_MATCH]
     */
    fun setChildTag(child: View, tag: String) {
        child.setTag(R.id.tag_sticky, tag)
        scrollView._stickyViews = null //重新获取
        requestLayout()
    }

    companion object {
        const val TAG_STICKY = "sticky"
        const val TAG_MATCH = "match"
        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}