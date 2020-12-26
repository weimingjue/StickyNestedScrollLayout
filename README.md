# 主要解决AppBarLayout的置顶效果存在的以下问题

1.头部超过一屏回拉会出现明显卡顿

2.惯性滑动无法用手指停下

3.悬浮下必须有1屏的大小
### 还有其他置顶控件的问题：
1.滑动类似RecyclerView并不会悬浮

2.悬浮控件ui变化会导致错乱（如悬浮是可横滑的控件）

## 使用方式：
基于NestedScrollView所以只允许有一个child：

然后加上android:tag="sticky"即可实现悬浮（目前只支持直接子类，在三级、四级child使用无效）
```
    <com.wang.sticky.StickyNestedScrollLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">
            ...
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:tag="sticky"//目前只支持直接子类
                android:text="悬浮"
                android:textSize="30sp" />
            ...
        </LinearLayout>
    </com.wang.sticky.StickyNestedScrollLayout>
```
子类如果需要撑满StickyNestedScrollLayout只需要加上android:tag="match"即可
```
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rv_main"
                android:layout_width="match_parent"
                android:layout_height="match_parent"//这里的match无效
                android:tag="match"//高度match，任意child均可
                app:layoutManager="androidx.recyclerview.widget.StaggeredGridLayoutManager"
                app:spanCount="2" />
```
如果tag和其他第三方冲突则可以使用代码设置：
```
StickyNestedScrollLayout.setChildTag(tv,"sticky");
```
也可以设置悬浮监听，获得悬浮距离：
```
StickyNestedScrollLayout.setOnStickyScrollChangedListener();
StickyNestedScrollLayout.getStickyTop();
```

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
`implementation（或api） 'com.github.weimingjue:sticky:0.9.0'`

## 说明
如果没有tag="sticky"则它就是一个可嵌套滑动的view

支持androidX所有view及第三方的View嵌套，不支持ListView

无需混淆配置