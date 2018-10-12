package com.github.king.weixinhelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.github.king.weixinhelper.db.DBManager;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import org.xmlpull.v1.XmlPullParser;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Created by jinxl on 2018/10/11.
 */
public class WeixinUtils {
    private static final boolean isDebug = true;
    private static final String TAG = "WeixinUtils";

    public static final String WX_ROOT_PATH = "/data/data/com.tencent.mm/";
    private static final String WX_DB_DIR_PATH = WX_ROOT_PATH + "MicroMsg";
    private List<File> mWxDbPathList = new ArrayList<>();
    private static final String WX_DB_FILE_NAME = "EnMicroMsg.db";
    private static final String WX_SP_UIN_PATH = WX_ROOT_PATH + "shared_prefs/auth_info_key_prefs.xml";

    //    private String mCurrApkPath = "/data/data/" + MyApplication.getContextObject().getPackageName() + "/";
    private static final String COPY_WX_DATA_DB = "wx_data.db";
    public static final String SP_KEY_CURRENT_DB_PWD = "sp_key_current_db_pwd";

    private static WeixinUtils instance;

    public static WeixinUtils getInstance() {
        if (instance == null) {
            synchronized (WeixinUtils.class) {
                if (instance == null) {
                    instance = new WeixinUtils();
                }
            }
        }
        return instance;
    }

    private WeixinUtils() {
        securityCheck();
    }

    public void securityCheck() {
        if (upgradeRootPermission(WX_ROOT_PATH)) {
            upgradeRootPermission(WX_DB_DIR_PATH);
//            upgradeRootPermission(WX_DB_FILE_NAME);
            upgradeRootPermission(WX_SP_UIN_PATH);
        }
    }

    /**
     * 初始化微信db
     * 1、将db文件拷贝到私有目录
     * 2、计算出db密码
     *
     * @param context
     * @return 成功执行以上两项操作后 会返回正确的密码
     * 反之则返回Null
     */
    public String ininWxDb(Context context) {
        context = context.getApplicationContext();
        List<String> weixinDbPwd = WeixinUtils.getInstance().getWeixinDbPwd(context);
        for (int i = 0; i < weixinDbPwd.size(); i++) {
            boolean pwdSuccess = DBManager.getInstance(context).checkPwdSuccess(weixinDbPwd.get(i));
            if (pwdSuccess) {
                SPUtils.getInstance(context).put(WeixinUtils.SP_KEY_CURRENT_DB_PWD, weixinDbPwd.get(i));
                return weixinDbPwd.get(i);
            } else {
                SPUtils.getInstance(context).remove(WeixinUtils.SP_KEY_CURRENT_DB_PWD);
            }
        }
        return null;
    }

    @SuppressLint("HardwareIds")
    public String getDeviceIdBySlot(Context context, int slotId) {
        if (context == null) {
            return null;
        }
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return null;
        }
        //864682034287227
        //todo check permission
        String deviceId = null;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            deviceId = telephonyManager.getImei(slotId);
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            deviceId = telephonyManager.getDeviceId(slotId);
        } else {
            if (slotId == 0) {
                deviceId = telephonyManager.getDeviceId();
            } else {
                SimCardUtil.SimCardInfo simCardInf = SimCardUtil.getSimCardInfoBySlot(context, slotId);
                if (simCardInf != null) {
                    deviceId = simCardInf.iccid;
                }
            }
        }
        if (deviceId != null) {
            return deviceId.trim();
        }
        return null;
    }

    /**
     * 获取微信db密码
     *
     * @param context
     * @return
     */
    public List<String> getWeixinDbPwd(Context context) {
        String wxDbMainDirName = getWxDbMainDirName();
        List<String> pwds = new ArrayList<>();
        if (!TextUtils.isEmpty(wxDbMainDirName)) {
            String copyFilePath = getCurrApkPath(context) + COPY_WX_DATA_DB;
            //将微信数据库拷贝出来，因为直接连接微信的db，会导致微信崩溃
            File file = new File(getWxDbPath(wxDbMainDirName));
            if (file.exists()) {
                copyFile(file.getAbsolutePath(), copyFilePath);
                for (int i = 0; i < 2; i++) {
                    String deviceId = getDeviceIdBySlot(context.getApplicationContext(), i);
                    if (i > 0 && TextUtils.isEmpty(deviceId)) {
                        continue;
                    }
                    String dbPwd = calculateDbPassword(deviceId, getCurrWxUin());
                    pwds.add(dbPwd);
                }
            }

        }
        return pwds;
    }

    /**
     * 获取拷贝后的数据库路径
     *
     * @param context
     * @return
     */
    public File getWxDbDataFilePath(Context context) {
        return new File(getCurrApkPath(context) + COPY_WX_DATA_DB);
    }

    /**
     * 获取微信db的路径
     *
     * @param wxDbMainDirName
     * @return
     */
    public String getWxDbPath(String wxDbMainDirName) {
        return WX_DB_DIR_PATH + "/" + wxDbMainDirName + "/" + WX_DB_FILE_NAME;
    }

    /**
     * 根据uin获取 数据库主目录
     *
     * @return
     */
    public String getWxDbMainDirName() {
        String wxUin = getCurrWxUin();
        if (TextUtils.isEmpty(wxUin)) {
            return "";
        }
        return md5(("mm" + wxUin).getBytes());
    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public void copyFile(String oldPath, String newPath) {
        try {
            int byteRead = 0;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteRead = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();

        }
    }


    /**
     * 获取微信的uid
     * 微信的uid存储在SharedPreferences里面
     * 存储位置\data\data\com.tencent.mm\shared_prefs\auth_info_key_prefs.xml
     */
    public String getCurrWxUin() {
        String wxUin = null;
        File file = new File(WX_SP_UIN_PATH);
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, "utf-8");
            //得到事件类型
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        /**
                         * 通过getName判断读到哪个标签, 然后通过nextText获取文本节点值，
                         * 或者通过getAttributeValue(i)获取属性节点值
                         */
                        String name = parser.getName();
                        if ("int".equals(name)) {
                            String attr1 = parser.getAttributeValue(null, "name");
                            if ("_auth_uin".equals(attr1)) {
                                wxUin = parser.getAttributeValue(null, "value");
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (isDebug) {
                Log.i(TAG, "获取微信uid失败，请检查auth_info_key_prefs文件权限");
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wxUin;
    }

    /**
     * 计算数据库密码
     *
     * @param imei
     * @param uin
     * @return
     */
    private String calculateDbPassword(String imei, String uin) {
        if (TextUtils.isEmpty(imei) || TextUtils.isEmpty(uin)) {
            if (isDebug) {
                Log.i(TAG, "初始化数据库密码失败：imei或uid为空");
            }
            return "";
        }
        String md5 = md5((imei + uin).getBytes());
        return md5.substring(0, 7);
    }

    public static String md5(byte[] bArr) {
        int i = 0;
        char[] cArr = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(bArr);
            byte[] digest = instance.digest();
            int length = digest.length;
            char[] cArr2 = new char[(length * 2)];
            int i2 = 0;
            while (i < length) {
                byte b = digest[i];
                int i3 = i2 + 1;
                cArr2[i2] = cArr[(b >>> 4) & 15];
                i2 = i3 + 1;
                cArr2[i3] = cArr[b & 15];
                i++;
            }
            String str = new String(cArr2);
            return str;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 应用程序运行命令获取 Root权限，设备必须已破解(获得ROOT权限)
     *
     * @return 应用程序是/否获取Root权限
     */
    public boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd = "chmod 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }

    /**
     * 获取当前微信账号的主目录
     *
     * @param context
     * @return
     */
    public String getCurrApkPath(Context context) {
        return "/data/data/" + context.getPackageName() + "/";
    }


}
