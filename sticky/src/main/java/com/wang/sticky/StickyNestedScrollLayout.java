package com.wang.sticky;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import java.util.ArrayList;

/**
 * 悬浮view，基于NestedScrollView，所以只能有一个child（不使用悬浮可当做NestedScrollView的优化版）
 * <p>
 * 使用：
 * 设置tag：sticky即可悬浮
 * <p>
 * 附加：{@link #setOnStickyScrollChangedListener}{@link #getStickyTop}{@link #setChildTag}
 * tag：match撑满布局
 */
public final class StickyNestedScrollLayout extends FrameLayout {
    public static final String TAG_STICKY = "sticky", TAG_MATCH = "match";
    private MyScrollView mScrollView;

    public StickyNestedScrollLayout(Context context) {
        this(context, null);
    }

    public StickyNestedScrollLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyNestedScrollLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public StickyNestedScrollLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * 套一层，可以少很多逻辑
     */
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new RuntimeException("只允许添加一个子类");
        }
        LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.VERTICAL);
        mScrollView = new MyScrollView(getContext(), ll);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mScrollView.setClipChildren(getClipChildren());
        }
        mScrollView.addView(child, index, params);
        super.addView(mScrollView, -1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        super.addView(ll, -1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /**
     * 只支持固定宽高
     */
    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if (params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        super.setLayoutParams(params);
    }

    /**
     * 高必须确定
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        if (mode != MeasureSpec.EXACTLY) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(getResources().getDisplayMetrics().heightPixels, size), MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 私有类
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final class MyScrollView extends NestedScrollView {

        /**
         * 请使用{@link #getStickyViews()}
         */
        @Nullable
        private ArrayList<StickyInfo> mStickyViews;
        private final LinearLayout mLlAdd;

        private boolean mIsDown = false;

        private OnStickyScrollChangedListener mListener;

        public MyScrollView(Context context, LinearLayout llAdd) {
            super(context);
            mLlAdd = llAdd;
        }

        /**
         * 惯性滑动时手指按下则立即停止
         */
        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
            if (mIsDown && type != ViewCompat.TYPE_TOUCH) {
                return;
            }
            super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
        }

        /**
         * 修复滑动类似RecyclerView时不跟随的问题
         */
        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
            if (dy > 0) {//nsv往上滑不跟随，这里处理一下
                if (canScrollVertically(1)) {
                    int stickyTop = getStickyTop(getStickyViews().size() - 1);
                    int dy2;
                    if (stickyTop > 0) {
                        dy2 = Math.min(dy, stickyTop);
                    } else {
                        dy2 = dy;
                    }
                    consumed[1] = consumed[1] + dy2;
                    scrollBy(0, dy2);
                    return;//暂时先全部消耗掉（几像素）
                }
            }
            super.onNestedPreScroll(target, dx, dy, consumed, type);
        }

        /**
         * 如果子有match就设置为当前高
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = MeasureSpec.getSize(heightMeasureSpec);
            if (getChildCount() > 0) {
                changeMatch(getChildAt(0), height);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        private void changeMatch(View view, int height) {
            if (TAG_MATCH.equals(view.getTag()) || TAG_MATCH.equals(view.getTag(R.id.tag_sticky))) {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                if (params != null) {
                    params.height = height;
                }
            }
            if (view instanceof ViewGroup) {
                ViewGroup vg0 = (ViewGroup) view;
                for (int i = 0; i < vg0.getChildCount(); i++) {
                    changeMatch(vg0.getChildAt(i), height);
                }
            }
        }

        /**
         * 当按下时停掉惯性滑动
         */
        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsDown = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIsDown = false;
                    break;
                default:
                    break;
            }
            return super.dispatchTouchEvent(ev);
        }

        @NonNull
        public ArrayList<StickyInfo> getStickyViews() {
            if (mStickyViews != null) {
                return mStickyViews;
            }
            mStickyViews = new ArrayList<>(2);

            if (getChildCount() > 0) {
                initStickyViews(getChildAt(0), 0);
            }
            return mStickyViews;
        }

        /**
         * 遍历得到所有悬浮view
         */
        private void initStickyViews(View checkView, int viewIndex) {
            if (TAG_STICKY.equals(checkView.getTag()) || TAG_STICKY.equals(checkView.getTag(R.id.tag_sticky))) {
                StickyInfo info = new StickyInfo(checkView, checkView.getLayoutParams(), (ViewGroup) checkView.getParent(), viewIndex);
                //noinspection ConstantConditions 这里不可能是null
                if (!mStickyViews.contains(info)) {
                    mStickyViews.add(info);
                }
                return;
            }
            if (checkView instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) checkView;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    initStickyViews(vg.getChildAt(i), i);
                }
            }
        }

        /**
         * 悬浮切换
         */
        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            super.onScrollChanged(l, t, oldl, oldt);
            for (int i = 0; i < getStickyViews().size(); i++) {
                StickyInfo info = getStickyViews().get(i);
                int top = getStickyTop(i);
                int index = llIndexOfSticky(info.mStickyView);
                if (top > 0) {
                    if (index >= 0) {
                        info.mStickyParent.removeView(info.mEmptyView);
                        mLlAdd.removeView(info.mStickyView);

                        info.mStickyParams.height = info.mStickyParamsHeight;
                        info.mStickyParent.addView(info.mStickyView, info.mStickyIndex, info.mStickyParams);
                    }
                } else {
                    if (index < 0) {
                        info.mStickyParent.removeView(info.mStickyView);
                        mLlAdd.removeView(info.mEmptyView);

                        info.mStickyParamsHeight = info.mStickyParams.height;
                        info.mStickyParams.height = info.mStickyView.getHeight();
                        info.mStickyParent.addView(info.mEmptyView, info.mStickyIndex, info.mStickyParams);

                        LinearLayout.LayoutParams params = newLlParams(info.mStickyParams);
                        params.leftMargin += getStickyParentLeftToThis(info.mStickyParent);
                        mLlAdd.addView(info.mStickyView, params);
                    }
                }

                if (mListener != null) {
                    mListener.onScroll(i, top);
                }
            }
        }

        private int llIndexOfSticky(View sv) {
            for (int i = 0; i < mLlAdd.getChildCount(); i++) {
                if (mLlAdd.getChildAt(i) == sv) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * 悬浮view的父类距自己左边
         */
        private int getStickyParentLeftToThis(View v) {
            if (v == null || v == this) {
                return 0;
            }
            return v.getLeft() + getStickyParentLeftToThis((View) v.getParent());
        }

        /**
         * 悬浮view距自己顶端
         */
        private int getStickyTopToThis(View v) {
            if (v == null || v == this) {
                return 0;
            }
            return v.getTop() + getStickyTopToThis((View) v.getParent());
        }

        /**
         * @return 悬浮view距顶部距离
         */
        @IntRange(from = 0)
        public int getStickyTop(int stickyIndex) {
            if (stickyIndex < 0 || stickyIndex >= getStickyViews().size()) {
                return 0;
            }
            StickyInfo info = getStickyViews().get(stickyIndex);
            View v = info.mStickyParent.getChildAt(info.mStickyIndex);
            int top = getStickyTopToThis(v);
            if (v.getLayoutParams() instanceof MarginLayoutParams) {
                top -= ((MarginLayoutParams) v.getLayoutParams()).topMargin;
            }
            top = top - getScrollY() + getPaddingTop() - mLlAdd.getHeight();//去掉scroll和悬浮遮挡的
            if (llIndexOfSticky(info.mStickyView) >= 0) {//去掉自己悬浮高
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) info.mStickyView.getLayoutParams();
                top += info.mStickyView.getHeight() + params.topMargin + params.bottomMargin;
            }
            return Math.max(top, 0);
        }

        public void setOnStickyScrollChangedListener(OnStickyScrollChangedListener listener) {
            mListener = listener;
        }

        /**
         * new 出来的，不是原对象
         */
        private LinearLayout.LayoutParams newLlParams(ViewGroup.LayoutParams lp) {
            if (lp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((MarginLayoutParams) lp);
                params.gravity = ((LinearLayout.LayoutParams) lp).gravity;
                return params;
            } else if (lp instanceof FrameLayout.LayoutParams) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((MarginLayoutParams) lp);
                params.gravity = ((FrameLayout.LayoutParams) lp).gravity;
                return params;
            } else if (lp instanceof MarginLayoutParams) {
                return new LinearLayout.LayoutParams((MarginLayoutParams) lp);
            }
            return new LinearLayout.LayoutParams(lp);
        }

        private class StickyInfo {
            @NonNull
            private View mStickyView;
            @NonNull
            private ViewGroup.LayoutParams mStickyParams;
            private int mStickyParamsHeight;
            @NonNull
            private ViewGroup mStickyParent;
            private int mStickyIndex;
            private final View mEmptyView = new View(getContext());

            StickyInfo(@NonNull View stickyView, @NonNull ViewGroup.LayoutParams stickyParams,
                       @NonNull ViewGroup stickyParent, int stickyIndex) {
                mStickyView = stickyView;
                mStickyParams = stickyParams;
                mStickyParent = stickyParent;
                mStickyIndex = stickyIndex;
            }

            @Override
            public boolean equals(@Nullable Object obj) {
                if (obj instanceof StickyInfo) {
                    return mStickyView == ((StickyInfo) obj).mStickyView;
                }
                return super.equals(obj);
            }
        }
    }

    public interface OnStickyScrollChangedListener {
        /**
         * @param stickyPosition 第几个悬浮，单悬浮可忽略
         * @param stickyTop      悬浮view距顶部距离，从max-0
         */
        void onScroll(int stickyPosition, int stickyTop);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 公共方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取第一个悬浮view距顶部的距离
     * 0为悬浮中
     */
    @IntRange(from = 0)
    public int getStickyTop() {
        return mScrollView.getStickyTop(0);
    }

    /**
     * 获取第Index悬浮view距顶部的距离
     * 0为悬浮中
     */
    @IntRange(from = 0)
    public int getStickyTop(int stickyIndex) {
        return mScrollView.getStickyTop(stickyIndex);
    }

    public void setOnStickyScrollChangedListener(OnStickyScrollChangedListener listener) {
        mScrollView.setOnStickyScrollChangedListener(listener);
    }

    /**
     * 获得悬浮时的父布局
     * 你可以设置background、padding等
     */
    public LinearLayout getStickyParent() {
        return mScrollView.mLlAdd;
    }

    /**
     * 获得内部滑动的view，可设置Scroll相关操作
     */
    public NestedScrollView getScrollView() {
        return mScrollView;
    }

    /**
     * 备用方法：当tag被占用时
     *
     * @param child 直接或间接子类
     * @param tag   {@link #TAG_STICKY}{@link #TAG_MATCH}
     */
    public void setChildTag(View child, String tag) {
        child.setTag(R.id.tag_sticky, tag);
        mScrollView.mStickyViews = null;//重新获取
        requestLayout();
    }
}