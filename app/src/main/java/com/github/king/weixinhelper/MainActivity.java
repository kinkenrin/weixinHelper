package com.github.king.weixinhelper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.king.accessibilityservice.OpenAccessibilitySettingHelper;
import com.github.king.weixinhelper.db.DBManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Handler mHandler = new Handler();
    Button bt_getMsg;
    Button btn_openSetting;
    Button btn_sendMsg;
    TextView tv_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt_getMsg = findViewById(R.id.bt_getMsg);
        tv_content = findViewById(R.id.tv_content);
        btn_openSetting = findViewById(R.id.btn_openSetting);
        btn_sendMsg = findViewById(R.id.btn_sendMsg);
        btn_openSetting.setOnClickListener(this);
        btn_sendMsg.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean startAccessibilityService = OpenAccessibilitySettingHelper.isStartAccessibilityService(getApplicationContext());
        tv_content.setText("无障碍模式：" + startAccessibilityService);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_openSetting:
                OpenAccessibilitySettingHelper.jumpToSettingPage(MainActivity.this);

                break;
            case R.id.btn_sendMsg:
                Intent sendMsg = new Intent("sendMsg");
                sendBroadcast(sendMsg);
                break;
        }
    }
}
