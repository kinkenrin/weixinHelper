package com.github.king.accessibilityservice;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

/**
 * @author Created by jinxl on 2018/11/6.
 */
public class OpenAccessibilitySettingHelper {

    /**
     * 判断AccessibilityService服务是否已经启动
     *
     * @param context
     * @return
     */
    public static boolean isStartAccessibilityService(Context context) {
        String serviceName = "com.github.king.weixinhelper/com.github.king.accessibilityservice.AccessibilityWxService";
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            List<AccessibilityServiceInfo> serviceInfos = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
            for (AccessibilityServiceInfo info : serviceInfos) {
                String id = info.getId();
                Log.i("aaa", "all -->" + id);
                if (id.contains(serviceName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void jumpToSettingPage(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivity(intent);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }
}
