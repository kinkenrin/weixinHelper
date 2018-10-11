package com.github.king.weixinhelper;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

/**
 * @author Created by jinxl on 2018/9/12.
 */
public class SimCardUtil {

    public static SimCardInfo getSimCardInfoBySlot(Context context, int slotID) {
        boolean isSIM1Ready = getSIMStateBySlot(context, "getSimState", slotID);
//        boolean isSIM2Ready = getSIMStateBySlot(context, "getSimState", 1);
        if (!isSIM1Ready) {
            return null;
        }
        context = context.getApplicationContext();
        String sim2_ICCID = getDeviceIdBySlot(context, "getSimSerialNumber", slotID);
        String sim2_MSISDN = getDeviceIdBySlot(context, "getLine1Number", slotID);
        String sim2_IMEI = getDeviceIdBySlot(context, "getDeviceId", slotID);
        return new SimCardInfo(sim2_ICCID, sim2_MSISDN, sim2_IMEI);
    }

    public static class SimCardInfo {
        String iccid;
        String imei;
        String phoneNum;

        public SimCardInfo(String iccid, String phoneNum, String imei) {
            this.iccid = iccid;
            this.phoneNum = phoneNum;
            this.imei = imei;
        }

        public String getIccid() {
            return iccid;
        }

        public void setIccid(String iccid) {
            this.iccid = iccid;
        }

        public String getPhoneNum() {
            return phoneNum;
        }

        public void setPhoneNum(String phoneNum) {
            this.phoneNum = phoneNum;
        }

    }

    private static boolean getSIMStateBySlot(Context context, String predictedMethodName, int slotID) {
        boolean isReady = false;
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimState = telephonyClass.getMethod(predictedMethodName, parameter);
            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimState.invoke(telephony, obParameter);

            if (ob_phone != null) {
                int simState = Integer.parseInt(ob_phone.toString());
                String sim2_STATE = simState(simState);
                if ((simState != TelephonyManager.SIM_STATE_ABSENT) && (simState != TelephonyManager.SIM_STATE_UNKNOWN)) {
                    isReady = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isReady;
    }

    private static String simState(int simState) {
        switch (simState) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "ABSENT";
            case 2:
                return "REQUIRED";
            case 3:
                return "PUK_REQUIRED";
            case 4:
                return "NETWORK_LOCKED";
            case 5:
                return "READY";
            case 6:
                return "NOT_READY";
            case 7:
                return "PERM_DISABLED";
            case 8:
                return "CARD_IO_ERROR";
        }
        return "??? " + simState;
    }

    private static String getDeviceIdBySlot(Context context, String predictedMethodName, int slotID) {

        String imsi = null;
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);

            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimID.invoke(telephony, obParameter);

            if (ob_phone != null) {
                imsi = ob_phone.toString();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imsi;
    }

}
