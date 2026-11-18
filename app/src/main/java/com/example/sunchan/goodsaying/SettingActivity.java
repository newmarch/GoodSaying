package com.example.sunchan.goodsaying;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

public class SettingActivity extends AppCompatActivity {
    private static final String TAG = "MYD - SettingActivity";

    private Switch mSwitch;
    private Button mTimeButton;
    private TextView mTextView;
    private Toolbar mToolBar;

    private int iCount;
    private int iHour;
    private int iMin;

    private String mEnabledColor = "#4169E1";
    private String mDisabledColor = "#A8A8A8";

    private DBHandler db;


    private ArrayList<AlarmItem> arItem = new ArrayList<AlarmItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DBHandler(this);
        setContentView(com.example.sunchan.goodsaying.R.layout.alarm_layout);
        ScreenInsets.apply(this);

        setupViews();




        arItem = db.getAllAlarmItem();
        iCount = arItem.size();

        /*
        Log.d(TAG, "SIZE : " + String.valueOf(iCount));
        for (int i = 0; i < arItem.size(); i++) {
            Log.d(TAG, "flag : " + arItem.get(i).getFlag());
            Log.d(TAG, "hour : " + arItem.get(i).getHour());
            Log.d(TAG, "min : " + arItem.get(i).getMin());
        }
        */

        if (iCount == 0) {
            mSwitch.setChecked(false);

            //초기 시간 설정
            final Calendar c = Calendar.getInstance();
            iHour = c.get(Calendar.HOUR_OF_DAY);
            iMin = c.get(Calendar.MINUTE);

            mTimeButton.setEnabled(false);
            mTimeButton.setTextColor(Color.parseColor(mDisabledColor));
            mTextView.setText(com.example.sunchan.goodsaying.R.string.set_alarm_msg);
        } else if (arItem.get(0).getFlag() == 0) {
            mSwitch.setChecked(false);
            // 이전 설정 시간으로 초기화
            iHour = arItem.get(0).getHour();
            iMin = arItem.get(0).getMin();

            mTimeButton.setEnabled(false);
            mTimeButton.setTextColor(Color.parseColor(mDisabledColor));
            mTextView.setText(com.example.sunchan.goodsaying.R.string.set_alarm_off_msg);
        } else if (arItem.get(0).getFlag() == 1) {
            mSwitch.setChecked(true);

            iHour = arItem.get(0).getHour();
            iMin = arItem.get(0).getMin();

            String time = String.format("%02d", iHour) + ":" +
                    String.format("%02d", iMin) + " " +
                    getString(com.example.sunchan.goodsaying.R.string.set_alarm_done_msg);
            mTextView.setText(time);
        } else {
            mSwitch.setChecked(false);

            //초기 시간 설정
            final Calendar c = Calendar.getInstance();
            iHour = c.get(Calendar.HOUR_OF_DAY);
            iMin = c.get(Calendar.MINUTE);

            mTimeButton.setEnabled(false);
            mTimeButton.setTextColor(Color.parseColor(mDisabledColor));
            mTextView.setText(com.example.sunchan.goodsaying.R.string.set_alarm_msg);
        }

        //set the switch to ON


        //attach a listener to check for changes in state
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //Toast.makeText(SettingActivity.this, "Switch is currently ON", Toast.LENGTH_SHORT).show();
                    mTimeButton.setEnabled(true);
                    mTimeButton.setTextColor(Color.parseColor(mEnabledColor));
                    mTextView.setText(com.example.sunchan.goodsaying.R.string.set_alarm_msg);
                } else {
                    //Toast.makeText(SettingActivity.this, "Switch is currently OFF", Toast.LENGTH_SHORT).show();
                    mTimeButton.setEnabled(false);
                    mTimeButton.setTextColor(Color.parseColor(mDisabledColor));
                    cancelAlarm();
                    mTextView.setText(com.example.sunchan.goodsaying.R.string.set_alarm_off_msg);

                    Toast.makeText(SettingActivity.this, com.example.sunchan.goodsaying.R.string.set_alarm_off_msg, Toast.LENGTH_SHORT).show();
                }

            }
        });

        //check the current state before we display the screen
        /*
        if (mSwitch.isChecked()) {
            //switchStatus.setText("Switch is currently ON");
        } else {
            //switchStatus.setText("Switch is currently OFF");
        }
        */
    }


    private void setupViews() {
        mSwitch = (Switch) findViewById(com.example.sunchan.goodsaying.R.id.alarm_set);
        mTimeButton = (Button) findViewById(com.example.sunchan.goodsaying.R.id.btn_time);
        mTextView = (TextView) findViewById(com.example.sunchan.goodsaying.R.id.text_view);

        mToolBar = (Toolbar) findViewById(com.example.sunchan.goodsaying.R.id.sub_toolbar);
        setSupportActionBar(mToolBar);
        //mToolBar.setNavigationIcon(R.drawable.ic_chevron_left_black_24dp);
        /* It doesn't work
        mToolBar.setTitle(R.string.set_alarm_title);
        */
        getSupportActionBar().setTitle(com.example.sunchan.goodsaying.R.string.set_alarm_title);   /* It works */
    }


    public void onButtonClick(View view) {
        // 버튼 ID를 가져온다.
        switch (view.getId()) {
            case com.example.sunchan.goodsaying.R.id.btn_time:                // 시간 설정 버튼
                /*
                final Calendar c = Calendar.getInstance();
                iHour = c.get(Calendar.HOUR_OF_DAY);
                iMin = c.get(Calendar.MINUTE);
                */

                Dialog dlgTime = new TimePickerDialog(this, myTimeSetListener, iHour,
                        iMin, false);
                dlgTime.show();

                break;

        }
    }


    private TimePickerDialog.OnTimeSetListener myTimeSetListener
            = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            iHour = hourOfDay;
            iMin = minute;

            setAlarm();
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            }

            String time = String.format("%02d", iHour) + ":" +
                          String.format("%02d", iMin) + " " +
                          getString(com.example.sunchan.goodsaying.R.string.set_alarm_done_msg);

            mTextView.setText(time);

            Toast.makeText(SettingActivity.this, time, Toast.LENGTH_SHORT).show();

        }
    };


    public void setAlarm() {
        db.saveAlarm(iHour, iMin);
        AlarmReceive.schedule(this, iHour, iMin);
    }

    private void cancelAlarm() {
        db.deleteAlarmItem();
        AlarmReceive.cancel(this);
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}
