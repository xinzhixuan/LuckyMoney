package com.xzx.lunckymoney;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;

import java.util.List;

import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_LONG_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

/**
 * Created by Administrator on 2016/12/29.
 *
 */
public class MyAccessibility extends AccessibilityService {

    private static final String TAG = MyAccessibility.class.getName();
    private boolean isTake = true;
    private boolean hasLuckyMoney = false;


    //锁屏、唤醒相关
    private KeyguardManager keyguardManager;
    private KeyguardManager.KeyguardLock mkeyguardLock;
    private PowerManager powerManager;
    private PowerManager.WakeLock mWakelock;
    private boolean needSleep = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        //获取电源管理器对象
        powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        //得到键盘锁管理器对象
        keyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        //得到键盘锁管理器对象
        mkeyguardLock = keyguardManager.newKeyguardLock("unLock");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isTake = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "====" + event.getEventType());
        int eventType = event.getEventType();
        switch (eventType) {
            case TYPE_VIEW_CLICKED:
                break;
            case TYPE_VIEW_LONG_CLICKED:
                break;
            case TYPE_WINDOW_STATE_CHANGED:
                handleWindowChangeEvent(event);
                break;
            case TYPE_WINDOW_CONTENT_CHANGED:
                handleWindowChangeEvent(event);
                break;
            case TYPE_NOTIFICATION_STATE_CHANGED:
                handleNotificationEvent(event);
                break;
            case TYPE_VIEW_SCROLLED:
                handleViewScrolled(event);
            break;


        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleViewScrolled(AccessibilityEvent event) {

    }

    /**
     * 找红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean findMoney() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        while (nodeInfo == null) {
            nodeInfo = getRootInActiveWindow();
        }
        List<AccessibilityNodeInfo> weixinMoney = nodeInfo.findAccessibilityNodeInfosByText("领取红包");//""
        if (weixinMoney.size() > 0) {
            AccessibilityNodeInfo accessibilityNodeInfo = weixinMoney.get(weixinMoney.size() - 1);
            click(accessibilityNodeInfo.getParent());
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleWindowChangeEvent(AccessibilityEvent event) {
        //如果领了，直接没必要在页面查找红包
        if (isTake) {
            return;
        }
        if (findMoney()) {
            Log.i(TAG, "classnme" + event.getClassName());
            //找到红包了，所以返回，因为模拟点击后自动触发window_changed事件
            hasLuckyMoney = true;
            return;
        } else {
            //返回到微信界面
            if (!hasLuckyMoney) {
                Log.i(TAG, "==============假红包==============");
                isTake = true;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                if (needSleep) {

                    releaseWakelock();
                }
            }
        }
        if (hasLuckyMoney) {
            Log.i(TAG, "==============有红包==============");
            take(event);
        }
    }

    /**
     * 领红包
     * @param event
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void take(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        while (nodeInfo == null) {
            nodeInfo = getRootInActiveWindow();
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("发了一个红包");
        if (list.size() > 0) {
            //说明当前页面是点“开”的页面
            int childCount = nodeInfo.getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    //每一个都点一下
                    click(nodeInfo.getChild(i));
                }
                isTake = true;
                //返回到微信界面
                backToWeiXin();
            }
        }
        //处理没有抢到红包的场景
        list = nodeInfo.findAccessibilityNodeInfosByText("手慢了");
        if (list.size() > 0) {
            //手慢了，没有抢到，退出
            isTake = true;
            backToWeiXin();
        }
        //别人发了一个假红包，被引进入到了上个红包的红包领情查看详情页面，直接重新返回到微信页面
        list = nodeInfo.findAccessibilityNodeInfosByText("红包详情");
        if (list.size() > 0) {
            //处理别人发了一个假红包｛微信红包四个字｝
            isTake = true;
            backToWeiXin();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void backToWeiXin() {
        hasLuckyMoney = false;
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        try {
            //抢完等500hao毫秒退出聊天界面
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        if (needSleep) {
            releaseWakelock();
        }
    }


    /**
     * 释放亮屏操作
     */
    private void releaseWakelock() {
        if (mWakelock != null) {
            mWakelock.release();
        }
        needSleep = false;
    }

    private void click(AccessibilityNodeInfo nodeInfo) {
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        nodeInfo.recycle();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleNotificationEvent(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        Log.i(TAG, "======texts=======" + texts);
        boolean contains = texts.toString().contains("微信红包");
        if (contains) {
            //1.点亮屏幕
            Log.i(TAG, "screeOn=" + String.valueOf(powerManager.isScreenOn()));
            if (!powerManager.isScreenOn()) {
                mWakelock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.SCREEN_BRIGHT_WAKE_LOCK,"target"); // this target for tell OS which app
                //点亮屏幕
                mWakelock.acquire();
                needSleep = true;
            }
            //2.解锁
            if(keyguardManager.inKeyguardRestrictedInputMode()) {
                mkeyguardLock.disableKeyguard();
                Log.i(TAG, "解锁");
            }
            Notification notification = (Notification) event.getParcelableData();
            try {
                isTake = false;
                notification.contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void printNodeInfo(AccessibilityNodeInfo nodeInfo) {

        int childCount = nodeInfo.getChildCount();
        if (childCount > 0) {
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = nodeInfo.getChild(i);
                printNodeInfo(child);
            }
        } else {
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            CharSequence text = nodeInfo.getText();
            Log.i(TAG, "======text=======" + text);
            String name = nodeInfo.getViewIdResourceName();
            Log.i(TAG, "======id=======" + name);
        }
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "发生yi8chang");
    }
}
