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
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_LONG_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

/**
 * Created by Administrator on 2016/12/29.
 *
 */
public class MyAccessibility extends AccessibilityService {

    private static final String TAG = MyAccessibility.class.getName();
    private static final String IS_TAKE = "is_take";
    private SharedPreferences sharedPreferences;


    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("weixin_accessibility", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(IS_TAKE, true).commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
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
        Log.i(TAG, event.toString());
        Log.i(TAG, event.getRecordCount() + "");
        Log.i(TAG, event.getBeforeText() + "");
        Log.i(TAG, event.getMovementGranularity() + "");
        Log.i(TAG, event.getAddedCount() + "");
    }

    /**
     * 找红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean findMoney() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        List<AccessibilityNodeInfo> weixinMoney = nodeInfo.findAccessibilityNodeInfosByText("领取红包");//""
        if (weixinMoney.size() > 0) {
            AccessibilityNodeInfo accessibilityNodeInfo = weixinMoney.get(weixinMoney.size() - 1);
            click(accessibilityNodeInfo.getParent());
            return true;
        }
        return false;
    }

    private void handleWindowChangeEvent(AccessibilityEvent event) {
        boolean take = sharedPreferences.getBoolean(IS_TAKE, false);
        if (take) {
            return;
        }
        boolean result = findMoney();
        if (result) {
            //找到红包了，所以返回，因为模拟点击触发，进入到另一个界面
            return;
        }
        take(event);
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
        Log.i(TAG, "给你发了一个红包size()=" + list.size());
        if (list.size() > 0) {
            //说明当前页面是点“开”的页面
            int childCount = nodeInfo.getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    //每一个都点一下
                    click(nodeInfo.getChild(i));
                }
                sharedPreferences.edit().putBoolean(IS_TAKE, true).commit();
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                try {
                    //抢完等500hao毫秒退出聊天界面
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            }
        }
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
            Notification notification = (Notification) event.getParcelableData();
            sharedPreferences.edit().putBoolean(IS_TAKE, false).commit();
            try {
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

    /**
     * 判断是否黑屏
     * @param context
     * @return
     */
    public final static boolean isScreenLocked(Context context) {
        android.app.KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(context.KEYGUARD_SERVICE);
        return !mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    @Override
    public void onInterrupt() {

    }
}
