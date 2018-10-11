package com.github.king.weixinhelper;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.king.weixinhelper.db.DBManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    Handler mHandler = new Handler();
    Button bt_getMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt_getMsg = findViewById(R.id.bt_getMsg);
    }

    public void initDbPwd(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bt_getMsg.setEnabled(false);
                    }
                });
                WeixinUtils.getInstance().ininWxDb(getApplicationContext());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "初始化完成", Toast.LENGTH_SHORT).show();
                        bt_getMsg.setEnabled(true);
                    }
                });

            }
        }).start();
    }

    public void clickGetMsg(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DBManager.getInstance(getApplicationContext()).getMsg();

            }
        }).start();

    }

}
