# 主要解决AppBarLayout的置顶效果存在的以下问题

1.头部超过一屏回拉会出现明显卡顿

2.惯性滑动无法用手指停下

3.悬浮下必须有1屏的大小
### 还有其他置顶控件的问题：
1.滑动类似RecyclerView并不会悬浮

2.悬浮的view重绘会导致ui错乱（如悬浮view是可横滑的控件）

## 使用方式：
基于NestedScrollView所以只允许有一个child：

然后加上android:tag="sticky"即可实现悬浮（任意child均可，支持多个tag悬浮）
```
    <com.wang.sticky.StickyNestedScrollLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <LinearLayout//只允许有一个child
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">
            ...

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end"
                android:tag="sticky"
                android:text="悬浮1"
                android:textSize="30sp" />
            ...
            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center_horizontal"
                    android:tag="sticky"
                    android:text="悬浮2"
                    android:textSize="30sp" />
            </FrameLayout>
            ...
        </LinearLayout>
    </com.wang.sticky.StickyNestedScrollLayout>
```
子级如果需要撑满StickyNestedScrollLayout只需要加上android:tag="match"即可
```
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rv_main"
                android:layout_width="match_parent"
                android:layout_height="match_parent"//这里的match无效
                android:tag="match"//高度match
                app:layoutManager="androidx.recyclerview.widget.StaggeredGridLayoutManager"
                app:spanCount="2" />
```
如果tag和其他第三方冲突则可以使用代码设置：
```
StickyNestedScrollLayout.setChildTag(tv,"sticky");
```
也可以设置悬浮监听、获得悬浮距离、修改悬浮背景、设置scroll监听等NestedScrollView的操作：
```
StickyNestedScrollLayout.setOnStickyScrollChangedListener();
StickyNestedScrollLayout.getStickyTop();
StickyNestedScrollLayout.getStickyParent();
StickyNestedScrollLayout.getScrollView();
```

### 相关问题：
首先说一下悬浮原理：当滑到顶端时，将要悬浮的view remove掉然后添加到顶部

背景色：请自己再设置一下或StickyNestedScrollLayout.getStickyParent()

左右空白：如想忽略左右margin，可以自行套一层FrameLayout

## 导入方式
你的build.gradle要有jitpack.io，大致如下：
```
allprojects {
    repositories {
        maven { url 'http://maven.aliyun.com/nexus/content/groups/public/' }
        maven { url 'https://jitpack.io' }
        google()
        jcenter()
    }
}
```
然后：
`implementation（或api） 'com.github.weimingjue:sticky:0.9.8'`

## 说明
如果没有tag="sticky"则它就是一个可嵌套滑动的view

支持androidX所有view及第三方的View嵌套，不支持ListView

无需混淆配置