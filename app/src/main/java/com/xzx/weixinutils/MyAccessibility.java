package com.xzx.weixinutils;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_LONG_CLICKED;
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
    private PrintStream printStream;
    @Override
    public void onCreate() {
        super.onCreate();
        File directory = Environment.getExternalStorageDirectory();
        File file = new File(directory + "/weixin_accessibility_log.txt");
        try {
            printStream = new PrintStream(new FileOutputStream(file, true));
            printStream.println("====================log start=====================");
            printStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        sharedPreferences = getSharedPreferences("weixin_accessibility", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(IS_TAKE, true).commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        printStream.close();
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
                handleWindowChangeEvent();
                printStream.println("window state change");
                break;
            case TYPE_WINDOW_CONTENT_CHANGED:
                handleWindowChangeEvent();
                printStream.println("window content change");
                break;
            case TYPE_NOTIFICATION_STATE_CHANGED:
                handleNotificationEvent(event);
                break;
//            case TYPE_VIEW_SCROLLED:
//                printStream.println("view scrolled");
//                handleViewScrolled(event);
//                break;

        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleViewScrolled(AccessibilityEvent event) {
        findMoney();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        List<AccessibilityNodeInfo> nodeInfoList = nodeInfo.findAccessibilityNodeInfosByText("已存入零钱，可直接转账");
        if (nodeInfoList.size() > 0) {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            return;
        }
        printStream.println("=======================================");
        take();
    }

    /**
     * 找红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean findMoney() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        List<AccessibilityNodeInfo> weixinMoney = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a50");//"微信红包"
        printStream.println("微信红包size=" + weixinMoney.size());
        if (weixinMoney.size() > 0) {
            AccessibilityNodeInfo accessibilityNodeInfo = weixinMoney.get(weixinMoney.size() - 1);
            printStream.println("微信红包" + accessibilityNodeInfo.getText());
            click(accessibilityNodeInfo.getParent());
            return true;
        }
        return false;
    }

    private void handleWindowChangeEvent() {
//        AccessibilityNodeInfo nodeInfo = event.getSource();
        boolean take = sharedPreferences.getBoolean(IS_TAKE, false);
        printStream.println("take=" + take);
        if (take) {
            return;
        }
        boolean result = findMoney();
        if (result) {
            //找到红包了，所以返回，因为模拟点击触发，进入到另一个界面
            return;
        }
        take();

//        printNodeInfo(nodeInfo);


    }

    /**
     * 领红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void take() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        //这里不太清除点击那个view会拆开红包，所以两个都点一下
        List<AccessibilityNodeInfo> nodeInfos = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bdh");
        printStream.println("nodeinfos.size()=" + nodeInfos.size());
        if (nodeInfos.size() > 0) {
            click(nodeInfos.get(0));
            sharedPreferences.edit().putBoolean(IS_TAKE, true).commit();
        }

        List<AccessibilityNodeInfo> nodeInfos2 = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bdl");
        printStream.println("nodeinfos2.size()=" + nodeInfos2.size());
        if (nodeInfos2.size() > 0) {
            click(nodeInfos2.get(0));
            sharedPreferences.edit().putBoolean(IS_TAKE, true).commit();
//            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }
    }

    private void click(AccessibilityNodeInfo nodeInfo) {
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        nodeInfo.recycle();
    }

    private void handleNotificationEvent(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        Log.i(TAG, "======texts=======" + texts);
        printStream.println(texts);
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
            CharSequence text = nodeInfo.getText();
            printStream.println("text:" + text);
            String name = nodeInfo.getViewIdResourceName();
            printStream.println("name:" + name);
        }
    }
    @Override
    public void onInterrupt() {

    }
}
