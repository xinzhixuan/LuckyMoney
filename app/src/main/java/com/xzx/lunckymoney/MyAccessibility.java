package com.xzx.lunckymoney;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

/**
 * Created by xinzhixuan on 2016/12/29.
 * 辅助功能微信抢红包
 */
public class MyAccessibility extends AccessibilityService {

    private static final String TAG = MyAccessibility.class.getName();
    private LuckyMoneyOrder order = LuckyMoneyOrder.INIT;

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
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //得到键盘锁管理器对象
        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        //得到键盘锁管理器对象
        mkeyguardLock = keyguardManager.newKeyguardLock("unLock");
        heartbeat();
    }

    private void heartbeat() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, "===heartbeat=");
                //告诉服务器我活着
            }
        };
        new Timer().schedule(task, 1000, 4000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "==destory");
        super.onDestroy();
        startService(new Intent(this, MyAccessibility.class));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "====" + event.getEventType());
        int eventType = event.getEventType();
        switch (eventType) {
            case TYPE_WINDOW_STATE_CHANGED:
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

    private void handleViewScrolled(AccessibilityEvent event) {

    }

    /**
     * 找红包
     */
    @SuppressLint("NewApi")
    private void findMoney() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        while (nodeInfo == null) {
            nodeInfo = getRootInActiveWindow();
        }
        List<AccessibilityNodeInfo> weixinMoney = nodeInfo.findAccessibilityNodeInfosByText("领取红包");//""
        if (!weixinMoney.isEmpty()) {
            AccessibilityNodeInfo accessibilityNodeInfo = weixinMoney.get(weixinMoney.size() - 1);
            //找到红包，进入下一步
            order = LuckyMoneyOrder.CLICK_MONEY;
            Log.i(TAG, "==============找到红包=================");
            if (accessibilityNodeInfo.getParent() != null) {
                click(accessibilityNodeInfo.getParent());
            }
        } else {
            //没有找到红包
            order = LuckyMoneyOrder.FINISHED;
            Log.i(TAG, "==============没有找到红包，发的假消息=================");
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            if (needSleep) {
                releaseWakelock();
            }
        }
    }

    private void handleWindowChangeEvent(AccessibilityEvent event) {
        if (order == LuckyMoneyOrder.RECEIVED_MONEY) {
            //找红包
            Log.i(TAG, "==============找红包==============");
            findMoney();
        } else if (order == LuckyMoneyOrder.CLICK_MONEY) {
            Log.i(TAG, "==============有红包==============");
            take(event);
        } else if (order == LuckyMoneyOrder.CLICK_KAI_BTN_FINSHED) {
            order = LuckyMoneyOrder.MONEY_DESC;
        } else if (order == LuckyMoneyOrder.MONEY_DESC) {
            Log.i(TAG, "==============返回到微信主界面==============");
            order = LuckyMoneyOrder.FINISHED;
            //返回到微信界面
            backToWeiXin();
        }
    }

    /**
     * 领红包
     *
     * @param event
     */
    @SuppressLint("NewApi")
    private void take(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        while (nodeInfo == null) {
            nodeInfo = getRootInActiveWindow();
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("发了一个红包");
        if (!list.isEmpty()) {
            //说明当前页面是点“开”的页面
            int childCount = nodeInfo.getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    if ("android.widget.Button".equals(nodeInfo.getChild(i).getClassName().toString())) {
                        click(nodeInfo.getChild(i));
                    }
                }
                order = LuckyMoneyOrder.CLICK_KAI_BTN_FINSHED;;
                return;
            }
        }
        //处理没有抢到红包的场景
        list = nodeInfo.findAccessibilityNodeInfosByText("手慢了");
        if (!list.isEmpty()) {
            //手慢了，没有抢到，退出
            order = LuckyMoneyOrder.MONEY_DESC;
            return;
        }
        //别人发了一个假红包，被引进入到了上个红包的红包领情查看详情页面，直接重新返回到微信页面
        list = nodeInfo.findAccessibilityNodeInfosByText("红包详情");
        if (!list.isEmpty()) {
            Log.i(TAG, "============红包详情================");
            //处理别人发了一个假红包｛微信红包四个字｝
            order = LuckyMoneyOrder.MONEY_DESC;
            return;
        }
    }

    @SuppressLint("NewApi")
    private void backToWeiXin() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        try {
            //抢完等500毫秒退出聊天界面
            Thread.sleep(800);
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

    private void handleNotificationEvent(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        Log.i(TAG, "======texts=======" + texts);
        boolean contains = texts.toString().contains("微信红包");
        if (contains) {
            //1.点亮屏幕
            Log.i(TAG, "screeOn=" + String.valueOf(powerManager.isScreenOn()));
            if (!powerManager.isScreenOn()) {
                mWakelock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                        | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "target"); // this target for tell OS which app
                //点亮屏幕
                mWakelock.acquire();
                needSleep = true;
            }
            //2.解锁
            if (keyguardManager.inKeyguardRestrictedInputMode()) {
                mkeyguardLock.disableKeyguard();
                Log.i(TAG, "解锁");
            }
            Notification notification = (Notification) event.getParcelableData();
            try {
                order = LuckyMoneyOrder.RECEIVED_MONEY;
                notification.contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("NewApi")
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
        //重新启动
        startService(new Intent(this, MyAccessibility.class));
    }
}
