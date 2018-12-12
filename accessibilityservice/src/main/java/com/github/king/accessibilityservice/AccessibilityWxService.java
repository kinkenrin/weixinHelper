package com.github.king.accessibilityservice;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * @author Created by jinxl on 2018/11/6.
 */
public class AccessibilityWxService extends AccessibilityService {
    static final String TAG = "AccessibilityWxService";
    static final String TAG1 = "AccessibilityWxService11";
    Handler mHandler = new Handler(Looper.getMainLooper());

    //发送对象的名字
    private String mSenderName = "Alan";
    //发送的内容
    private String mSendMsgContent = "你好";

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction("sendMsg");

        registerReceiver(mReceiver, filter);
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("sendMsg".equals(intent.getAction())) {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                        if (nodeInfo != null) {
                            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText("微信");
                            AccessibilityNodeInfo nodeInfo1 = nodes.get(0);
                            if ("微信".equals(nodeInfo1.getText().toString())) {
                                nodeInfo1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        openGroup();
                                    }
                                }, 200);

                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        openProfile();
                                    }
                                }, 500);

                            }
                        }
                        Log.i(TAG, "11");
                    }
                }, 300);
            }
        }
    };

    private void openGroup() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText("通讯录");
            for (AccessibilityNodeInfo node : nodes) {
                if ("通讯录".equals(node.getText().toString())) {
                    node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }

    private AccessibilityNodeInfo findParentByClassName(String classname, AccessibilityNodeInfo node) {
        if (node == getRootInActiveWindow()) {
            return null;
        }
        AccessibilityNodeInfo parent = node.getParent();
        if (classname.equals(parent.getClassName().toString())) {
            return parent;
        } else {
            AccessibilityNodeInfo parentByClassName = findParentByClassName(classname, parent.getParent());
            if (parentByClassName != null) {
                return parentByClassName;
            }
        }
        return null;
    }

    private void openProfile() {
        AccessibilityNodeInfo node = getRootInActiveWindow();
        AccessibilityNodeInfo listview = null;
        List<AccessibilityNodeInfo> newFriends = node.findAccessibilityNodeInfosByText("新的朋友");
        if (newFriends != null && newFriends.size() > 0) {
            if ("android.widget.ListView".equals(newFriends.get(0).getParent().getParent().getParent().getParent().getClassName().toString())) {
                listview = newFriends.get(0).getParent().getParent().getParent().getParent();
            }
        }
//        List<AccessibilityNodeInfo> accessibilityNodeInfosByViewId = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/li");
//        AccessibilityNodeInfo listview = accessibilityNodeInfosByViewId.get(0);
        if (listview != null) {
            int childCount = listview.getChildCount();
            for (int i = 1; i < childCount; i++) {
                final AccessibilityNodeInfo child1 = listview.getChild(i);
                if (child1 != null && "android.widget.LinearLayout".equals(child1.getClassName().toString())) {
                    for (int j = 0; j < child1.getChildCount(); j++) {
                        final AccessibilityNodeInfo child = child1.getChild(j);
                        if ("android.view.View".equals(child.getClassName().toString())) {
                            if (mSenderName.equals(child.getContentDescription().toString())) {
                                //聊天对象
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        child1.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                                        openChatRoom(200);
                                    }
                                });
                                break;
                            }
                        }
                    }
                }

            }
        }
    }

    private void openChatRoom(int delayMillis) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                if (nodeInfo != null) {
                    List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText("发消息");
                    for (AccessibilityNodeInfo node : nodes) {
                        if ("发消息".equals(node.getText().toString())) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            sendMsg();
                        }
                    }
                }
            }
        }, delayMillis);

    }

    private void sendMsg() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final AccessibilityNodeInfo node = getRootInActiveWindow();
                final AccessibilityNodeInfo bottomLayout = node.getChild(0).getChild(0).getChild(1);
                AccessibilityNodeInfo editText = findNodeByClassName("android.widget.EditText", node.getChild(0).getChild(0));
//                List<AccessibilityNodeInfo> accessibilityNodeInfosByViewId = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/aie");
//                AccessibilityNodeInfo editText = accessibilityNodeInfosByViewId.get(0);
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, mSendMsgContent + System.currentTimeMillis());
                editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                Log.i("aaa", "");

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List<AccessibilityNodeInfo> nodes = bottomLayout.findAccessibilityNodeInfosByText("发送");
                        for (AccessibilityNodeInfo data : nodes) {
                            if ("发送".equals(data.getText().toString())) {
                                data.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                            }
                        }
                    }
                }, 200);
            }
        }, 400);
    }

    private AccessibilityNodeInfo findNodeByClassName(String className) {
        return findNodeByClassName(className, getRootInActiveWindow());
    }

    private AccessibilityNodeInfo findNodeByClassName(String className, AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            int childCount = nodeInfo.getChildCount();
            if (childCount == 0) {
                return null;
            } else {
                for (int i = 0; i < childCount; i++) {
                    AccessibilityNodeInfo child = nodeInfo.getChild(i);
                    if (!TextUtils.isEmpty(className) && className.equals(child.getClassName().toString())) {
                        return child;
                    }
                    AccessibilityNodeInfo nodeByClassName = findNodeByClassName(className, child);
                    if (nodeByClassName != null) {
                        return nodeByClassName;
                    }
                }
            }
        }
        return null;
    }

    public void recycle(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
            Log.i(TAG, "child widget----------------------------" + info.getClassName().toString());
            Log.i(TAG, "Text：" + info.getText());
            Log.i(TAG, "showDialog:" + info.canOpenPopup());
            Log.i(TAG, "windowId:" + info.getWindowId());
            Log.i(TAG, "desc:" + info.getContentDescription());
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recycle(info.getChild(i));
                }
            }
        }
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        // 此方法是在主线程中回调过来的，所以消息是阻塞执行的
        // 获取包名
        String pkgName = event.getPackageName().toString();
        Log.i(TAG, event.toString());
        int eventType = event.getEventType();

        final AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            /*mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText("通讯录");
                    nodes.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.i(TAG, event.toString());
                }
            }, 200);*/

        }


        /*getRootInActiveWindow()：获取当前整个活动窗口的根节点
        返回的是一个AccessibilityNodeInfo类，代表View的状态信息，
        提供了下述几个非常实用的方法：

        getParent：获取父节点。
        getChild：获取子节点。
        performAction：在节点上执行一个动作。
        findAccessibilityNodeInfosByText：通过字符串查找节点元素。
        findAccessibilityNodeInfosByViewId：通过视图id查找节点元素。
        */
        // AccessibilityOperator封装了辅助功能的界面查找与模拟点击事件等操作
//        AccessibilityOperator.getInstance().updateEvent(this, event);
//        AccessibilityLog.printLog("eventType: " + eventType + " pkgName: " + pkgName);
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                //通知栏发生改变时
                List<CharSequence> text = event.getText();
                
               /* getEventType()：事件类型

                getSource()：获取事件源对应的结点信息

                getClassName()：获取事件源对应类的类型，比如点击事件是有某个Button产生的，那么此时获取的就是Button的完整类名

                getText()：获取事件源的文本信息，比如事件是有TextView发出的,此时获取的就是TextView的text属性。如果该事件源是树结构，那么此时获取的是这个树上所有具有text属性的值的集合

                isEnabled()：事件源(对应的界面控件)是否处在可用状态

                getItemCount()：如果事件源是树结构，将返回该树根节点下子节点的数量*/
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                //当窗口的状态发生改变时
                break;
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
