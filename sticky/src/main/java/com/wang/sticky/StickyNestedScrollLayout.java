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
        FrameLayout fl = new FrameLayout(getContext());
        mScrollView = new MyScrollView(getContext(), fl);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mScrollView.setClipChildren(getClipChildren());
        }
        mScrollView.addView(child, index, params);
        super.addView(mScrollView, -1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        super.addView(fl, -1, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        private View mStickyView;
        private boolean mNoneSticky = false;//没有sticky tag，将不会再遍历直接返回null
        private ViewGroup.LayoutParams mStickyParams;
        private int mStickyParamsHeight;
        private ViewGroup mStickyParent;
        private int mStickyIndex;

        private View mEmptyView = new View(getContext());
        private final FrameLayout mFlAdd;

        private boolean mIsDown = false;

        private OnStickyScrollChangedListener mListener;

        public MyScrollView(Context context, FrameLayout flAdd) {
            super(context);
            mFlAdd = flAdd;
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
                    int stickyTop = getStickyTop();
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

        /**
         * 获得悬浮view
         */
        @Nullable
        private View getStickyView() {
            if (mNoneSticky) {
                return null;
            }

            if (getChildCount() > 0) {
                View stickyView = getStickyView(getChildAt(0), 0);
                if (stickyView == null) {
                    mNoneSticky = true;
                }
                return stickyView;
            }
            return null;
        }

        private View getStickyView(View checkView, int viewIndex) {
            if (mStickyView != null) {
                return mStickyView;
            }
            if (TAG_STICKY.equals(checkView.getTag()) || TAG_STICKY.equals(checkView.getTag(R.id.tag_sticky))) {
                mStickyView = checkView;
                mStickyParams = mStickyView.getLayoutParams();
                mStickyParent = (ViewGroup) mStickyView.getParent();
                mStickyIndex = viewIndex;
                return mStickyView;
            }
            if (checkView instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) checkView;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View sv = getStickyView(vg.getChildAt(i), i);
                    if (sv != null) {
                        return sv;
                    }
                }
            }
            return null;
        }

        /**
         * 悬浮切换
         */
        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            super.onScrollChanged(l, t, oldl, oldt);
            if (getStickyView() != null) {
                int top = getStickyTop();
                if (top > 0) {
                    if (mFlAdd.getChildCount() > 0 && mFlAdd.getChildAt(0) == getStickyView()) {
                        mFlAdd.removeAllViews();

                        mStickyParent.removeView(mEmptyView);
                        mStickyParams.height = mStickyParamsHeight;
                        mStickyParent.addView(getStickyView(), mStickyIndex, mStickyParams);
                    }
                } else {
                    if (mFlAdd.getChildCount() == 0) {
                        mStickyParent.removeView(getStickyView());
                        mStickyParamsHeight = mStickyParams.height;
                        mStickyParams.height = getStickyView().getHeight();
                        mStickyParent.addView(mEmptyView, mStickyIndex, mStickyParams);

                        mFlAdd.setPadding(getStickyParentLeftToThis(mStickyParent), 0, 0, 0);
                        mFlAdd.addView(getStickyView(), generateNewLayoutParams(mStickyParams));
                    }
                }

                if (mListener != null) {
                    mListener.onScroll(top);
                }
            }
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
        public int getStickyTop() {
            if (getStickyView() == null) {
                return 0;
            }
            View v = mStickyParent.getChildAt(mStickyIndex);
            int top = getStickyTopToThis(v);
            if (v.getLayoutParams() instanceof MarginLayoutParams) {
                top -= ((MarginLayoutParams) v.getLayoutParams()).topMargin;
            }
            return Math.max(top - getScrollY() + getPaddingTop(), 0);
        }

        public void setOnStickyScrollChangedListener(OnStickyScrollChangedListener listener) {
            mListener = listener;
        }

        /**
         * new 出来的，不是原对象
         */
        private LayoutParams generateNewLayoutParams(ViewGroup.LayoutParams lp) {
            if (lp instanceof LayoutParams) {
                LayoutParams params = new LayoutParams((MarginLayoutParams) lp);
                params.gravity = ((LayoutParams) lp).gravity;
                return params;
            } else if (lp instanceof LinearLayout.LayoutParams) {
                LayoutParams params = new LayoutParams((MarginLayoutParams) lp);
                params.gravity = ((LinearLayout.LayoutParams) lp).gravity;
                return params;
            } else if (lp instanceof MarginLayoutParams) {
                return new LayoutParams((MarginLayoutParams) lp);
            }
            return new LayoutParams(lp);
        }
    }

    public interface OnStickyScrollChangedListener {
        /**
         * @param stickyTop 悬浮view距顶部距离，从max-0
         */
        void onScroll(int stickyTop);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 公共方法
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 获取悬浮view距顶部距离
     * 0为悬浮中
     */
    @IntRange(from = 0)
    public int getStickyTop() {
        return mScrollView.getStickyTop();
    }

    public void setOnStickyScrollChangedListener(OnStickyScrollChangedListener listener) {
        mScrollView.setOnStickyScrollChangedListener(listener);
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
        mScrollView.mNoneSticky = false;
        requestLayout();
    }
}