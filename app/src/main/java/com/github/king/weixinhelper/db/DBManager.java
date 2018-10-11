package com.github.king.weixinhelper.db;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.github.king.weixinhelper.SPUtils;
import com.github.king.weixinhelper.WeixinUtils;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Created by jinxl on 2018/9/13.
 */
public class DBManager {
    private static final String TAG = "DBManager";
    private static DBManager mInstance = null;
    private AtomicInteger mCount = new AtomicInteger();

    private SQLiteDatabase mDatabase;
    private Context mContext;
    private SQLiteDatabaseHook mHook;
    private String mDbFile;
    private String mVoicePath;
    private String mImgPath;

    private DBManager(Context context) {
        mContext = context;
        mDbFile = WeixinUtils.getInstance().getWxDbDataFilePath(mContext).getAbsolutePath();
        String wxDbMainDirName = WeixinUtils.getInstance().getWxDbMainDirName();
        mVoicePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tencent/MicroMsg/" + wxDbMainDirName + "/voice2/";
        mImgPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tencent/MicroMsg/" + wxDbMainDirName + "/image2/";
        SQLiteDatabase.loadLibs(context);
        mHook = new SQLiteDatabaseHook() {
            public void preKey(SQLiteDatabase database) {
            }

            public void postKey(SQLiteDatabase database) {
                database.rawExecSQL("PRAGMA cipher_migrate;"); //兼容2.0的数据库
            }
        };
    }

    public static DBManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (DBManager.class) {
                if (mInstance == null) {
                    mInstance = new DBManager(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private synchronized SQLiteDatabase getSQLiteDatabase(String dbPassword, File dbFile) {
        if (mCount.incrementAndGet() == 1) {
            this.mDatabase = SQLiteDatabase.openOrCreateDatabase(dbFile, dbPassword, null, mHook);
        }

        return this.mDatabase;
    }

    private synchronized void closeDB() {
        if (mCount.decrementAndGet() == 0 && mDatabase != null && mDatabase.isOpen()) {
            this.mDatabase.close();
        }
    }

    private synchronized SQLiteDatabase getSQLiteDatabase() {
        String dbPassword = SPUtils.getInstance(mContext).getString(WeixinUtils.SP_KEY_CURRENT_DB_PWD);
        if (TextUtils.isEmpty(dbPassword)) {
            Log.e(TAG, "请先进行初始化操作");
            return null;
        }
        if (mCount.incrementAndGet() == 1) {
            this.mDatabase = SQLiteDatabase.openOrCreateDatabase(mDbFile, dbPassword, null, mHook);
        }

        return this.mDatabase;
    }

    public boolean checkPwdSuccess(String dbPassword) {
        boolean isSuccess = false;
        try {
            getSQLiteDatabase(dbPassword, WeixinUtils.getInstance().getWxDbDataFilePath(mContext));
            isSuccess = true;
        } catch (Exception e) {
            Log.i(TAG, "读取数据库信息失败");
        } finally {
            closeDB();
        }
        return isSuccess;
    }


    public void getMsg() {
        Cursor c1 = null;
        try {
            SQLiteDatabase db = getSQLiteDatabase();
            c1 = db.rawQuery("select * from message order by msgId", null);
            while (c1.moveToNext()) {
                String isSend = c1.getString(c1.getColumnIndex("isSend"));
                String status = c1.getString(c1.getColumnIndex("status"));
                String talker = c1.getString(c1.getColumnIndex("talker"));
                String content = c1.getString(c1.getColumnIndex("content"));
                String msgSvrId = c1.getString(c1.getColumnIndex("msgSvrId"));
                String msgId = c1.getString(c1.getColumnIndex("msgId"));
                String imgPath = c1.getString(c1.getColumnIndex("imgPath"));
                int type = c1.getInt(c1.getColumnIndex("type"));
                if (type == 34) {
                    //语音
                    imgPath = calculateVoiceFilePath(imgPath);
                } else if (type == 3) {
                    //图片
                    imgPath = calculateImgFilePath(imgPath);
                }
                Log.i("message", "msgId:" + msgId + "  msgSvrId:" + msgSvrId + " isSend:" + isSend + "   status:" + status + "   talker:" + talker + "   content:" + content
                        + "  imgPath:" + imgPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c1 != null) {
                c1.close();
            }
            closeDB();
        }
    }

    private String calculateVoiceFilePath(String imgPath) {
        if (!TextUtils.isEmpty(imgPath)) {
            String tmpstr = WeixinUtils.md5(imgPath.getBytes());
            if (tmpstr != null) {
                String p1 = tmpstr.substring(0, 2);
                String p2 = tmpstr.substring(2, 4);
                return mVoicePath + p1 + "/" + p2 + "/msg_" + imgPath + ".amr";
            }
        }
        return imgPath;
    }

    private String calculateImgFilePath(String imgPath) {
        if (!TextUtils.isEmpty(imgPath)) {
            String tmpstr = imgPath.replace("THUMBNAIL_DIRPATH://th_", "");
            String tmpstr2 = imgPath.replace("THUMBNAIL_DIRPATH://", "");
            String p1 = tmpstr.substring(0, 2);
            String p2 = tmpstr.substring(2, 4);
            return mImgPath + p1 + "/" + p2 + "/" + tmpstr2;
        }
        return imgPath;
    }

    /**
     * 获取所有未上传的数据
     *
     * @param isSobot true 获取所有未传给sobot的数据
     *                false 获取所有未传给客户的数据
     * @return
     */
    /*
    public List<CallLogDao> getUnUploadDatas(boolean isSobot) {
        List<CallLogDao> data = new ArrayList<>();
        String uploadType = isSobot ? "isUploadSt" : "isUploadOther";
        String sql = "select * from " + Sql.Tables.CALL_LOGS + " where " + uploadType + " = ?";
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getSQLiteDatabase();
            cursor = db.rawQuery(sql, new String[]{"0"});
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex("id"));
                String companyId = cursor.getString(cursor.getColumnIndex("companyId"));
                String serviceId = cursor.getString(cursor.getColumnIndex("serviceId"));
                String localNumber = cursor.getString(cursor.getColumnIndex("localNumber"));
                String toNumber = cursor.getString(cursor.getColumnIndex("toNumber"));
                String name = cursor.getString(cursor.getColumnIndex("name"));
                long callDate = cursor.getLong(cursor.getColumnIndex("callDate"));
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                int duration = cursor.getInt(cursor.getColumnIndex("duration"));
                long timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
                String recordFile = cursor.getString(cursor.getColumnIndex("recordFile"));
//                boolean isUploadSt = 1 == cursor.getInt(cursor.getColumnIndex("isUploadSt"));
//                boolean isUploadOther = 1 == cursor.getInt(cursor.getColumnIndex("isUploadOther"));
                data.add(new CallLogDao(id, companyId, serviceId, localNumber, toNumber, name, callDate, type, duration, recordFile));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            closeDB();
        }

        return data;
    }

    *//**
     * 完成上传后 更新标识
     *
     * @param id      记录的主键
     * @param isSobot 是否是上传到sobot  或者是客户
     *//*
    public void finishUploadData(String id, boolean isSobot) {
        try {
            SQLiteDatabase db = getSQLiteDatabase();
            ContentValues values = new ContentValues();
            values.put(isSobot ? "isUploadSt" : "isUploadOther", true);
            db.update(Sql.Tables.CALL_LOGS, values, "id=?", new String[]{id});

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            closeDB();
        }

    }*/

    //todo 保存 compnyid  serviceid  localnum
    /*public void saveUserInfo(CallLogDao data) {
        try {
            SQLiteDatabase db = getSQLiteDatabase();
            ContentValues values = new ContentValues();

            values.put("id", UUID.randomUUID().toString());
            values.put("companyId", data.getCompanyId());
            values.put("serviceId", data.getServiceId());
            values.put("localNumber", data.getLocalNumber());
            values.put("toNumber", data.getToNumber());
            values.put("name", data.getName());
            values.put("callDate", data.getCallDate());
            values.put("type", data.getType());
            values.put("duration", data.getDuration());

            values.put("timestamp", System.currentTimeMillis());
            values.put("recordFile", data.getRecordFile());
            values.put("isUploadSt", data.isUploadSt());
            values.put("isUploadOther", data.isUploadOther());
            db.insert(Sql.Tables.CALL_LOGS, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDB();
        }
    } */
/*
    public List<String> getDatas(long var1) {
        List<String> data = new ArrayList<>();
        SQLiteDatabase db = getSQLiteDatabase();
        String sql = "select * from " + Sql.Tables.BOOK + " where " + "MsgID" + " = ?";
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(sql, new String[]{"420"});
            while (cursor.moveToNext()) {
                String string = cursor.getString(0);
                data.add(string);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            closeDB();
        }

        return data;
    }
*/
}
