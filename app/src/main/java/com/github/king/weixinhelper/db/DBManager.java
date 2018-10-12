package com.github.king.weixinhelper.db;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.github.king.weixinhelper.SPUtils;
import com.github.king.weixinhelper.WeixinUtils;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private String mMainPath;
    private String mVoicePath;
    private String mImgPath;
    private String mVideoPath;
    private String mEmojiPath;

    private DBManager(Context context) {
        mContext = context;
        mDbFile = WeixinUtils.getInstance().getWxDbDataFilePath(mContext).getAbsolutePath();
        mMainPath = WeixinUtils.getInstance().getWxMainPath();
        mVoicePath = mMainPath + "/voice2/";
        mImgPath = mMainPath + "/image2/";
        mVideoPath = mMainPath + "/video/";
        mEmojiPath = mMainPath + "/emoji/";
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
            c1 = db.rawQuery("select *, message.type as mType, rcontact.type as rType from message left join rcontact on message.talker=rcontact.username where talker='jinxianl_2005' order by msgId", null);
            while (c1.moveToNext()) {
                int isSend = c1.getInt(c1.getColumnIndex("isSend"));
                String status = c1.getString(c1.getColumnIndex("status"));
                String talker = c1.getString(c1.getColumnIndex("talker"));
                String content = c1.getString(c1.getColumnIndex("content"));
                String msgSvrId = c1.getString(c1.getColumnIndex("msgSvrId"));
                String msgId = c1.getString(c1.getColumnIndex("msgId"));
                String imgPath = c1.getString(c1.getColumnIndex("imgPath"));
                String alias = c1.getString(c1.getColumnIndex("alias"));
                String nickname = c1.getString(c1.getColumnIndex("nickname"));
                int type = c1.getInt(c1.getColumnIndex("mType"));
                if (type == 47) {
                    String calculateEmojiUrl = calculateEmojiUrl2(content, db);
                    if (calculateEmojiUrl != null) {
                        imgPath = calculateEmojiUrl;
                    }
                } else if (type == 34) {
                    //语音
                    imgPath = calculateVoiceFilePath(imgPath);
                } else if (type == 3) {
                    //图片
                    imgPath = calculateImgFilePath(imgPath);
                } else if (type == 43) {
                    //小视频
                    //预览图片
                    String previewFilePath = calculateVideoPreviewFilePath(imgPath);
                    Log.i("message", "previewFilePath:" + previewFilePath + "   exists:" + new File(previewFilePath).exists());
                    //视频地址
                    imgPath = calculateVideoFilePath(imgPath);
                }
                Log.i("message", "type:" + type + "  msgSvrId:" + msgSvrId + " isSend:" + isSend + "   status:" + status + "   talker:" + talker + "   content:" + content
                        + "  exists:" + (imgPath != null && new File(imgPath).exists())
                        + "  imgPath:" + imgPath
                        + "  nickname:" + nickname
                        + "  alias:" + alias);
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

    public String searchEmoji(String md5, SQLiteDatabase db) {
        Cursor c1 = null;
        String imgUrl = null;
        try {
//            SQLiteDatabase db = getSQLiteDatabase();
            c1 = db.rawQuery("select EmojiInfo.md5 as eMd5,EmojiInfoDesc.md5 as eMd5_2, cdnurl,desc,EmojiInfo.groupId as rGroupId from EmojiInfo left join EmojiInfoDesc on EmojiInfoDesc.md5=EmojiInfo.md5 where EmojiInfo.md5 = ?", new String[]{md5});
            while (c1.moveToNext()) {
                String md5Str = c1.getString(c1.getColumnIndex("eMd5"));
                String cdnurl = c1.getString(c1.getColumnIndex("cdnUrl"));
                String desc = c1.getString(c1.getColumnIndex("desc"));
                String groupId = c1.getString(c1.getColumnIndex("rGroupId"));
                /*Log.i("searchEmoji", "md5Str:" + md5Str
                        + "  name:" + name
                        + "  cdnurl:" + cdnurl
                        + "  content:" + content
                );*/
                if (!TextUtils.isEmpty(cdnurl)) {
                    imgUrl = cdnurl;
                } else {
                    imgUrl = mEmojiPath + groupId + "/" + md5Str + "_cover";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c1 != null) {
                c1.close();
            }
            //暂时不关交给调用方关闭
//            closeDB();
        }
        return imgUrl;
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

    private String calculateVideoFilePath(String imgPath) {
        if (!TextUtils.isEmpty(imgPath)) {
            return mVideoPath + imgPath + ".mp4";
        }
        return imgPath;
    }

    private String calculateVideoPreviewFilePath(String imgPath) {
        if (!TextUtils.isEmpty(imgPath)) {
            return mVideoPath + imgPath + ".jpg";
        }
        return imgPath;
    }

    private String calculateEmojiUrl(String content) {
        String tmpstr = null;
        if (!TextUtils.isEmpty(content)) {
            InputStream in = null;
            try {
                String[] split = content.split(":");
                String substring = split[4];
                in = new ByteArrayInputStream(substring.getBytes());
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
                            if ("emoji".equals(name)) {
                                tmpstr = parser.getAttributeValue(null, "cdnurl");
                                tmpstr = tmpstr.replace("*#*", "s:");
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            break;
                    }
                    eventType = parser.next();
                }
            } catch (Exception e) {

            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return tmpstr;
    }

    private String calculateEmojiUrl2(String content, SQLiteDatabase db) {
        if (!TextUtils.isEmpty(content)) {
            String[] split = content.split(":");
            String substring = split[3];
            return searchEmoji(substring, db);
        }
        return null;
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
