package com.android.vibratetest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class VibrateTest extends Activity implements  View.OnClickListener{

    private static final String TAG = "VibrateTest";

    public static final int PERMISSIONS_REQUEST_VIBRATE = 0;

    private static final int MINUTE = 60 * 1000;

    private static final int UPDATE_TIME_DELAYED = 1000;
    private static final int MSG_START_TEST = 1;
    private static final int MSG_UPDATE_TIME = 2;

    public static final String KEY_LAST_TEST_TIME = "vibrate_last_test_time";
    public static final String KEY_START_TIME = "vibrate_start_time";
    public static final String KEY_END_TIME = "vibrate_end_time";

    private EditText mTestTimeEt;
    private TextView mCurrentTestTimeTv;
    private TextView mTestedTimeTv;
    private TextView mLastTestTimeTv;
    private TextView mLastRealTestTimeTv;
    private Button mStartTestBt;
    private TextView mLastTestResultTv;
    private SharedPreferences mSharedPreferences;
    private Vibrator mVibrator;
    private Resources mResources;

    private long mStartTime = -1;
    private int mTestTime =  -1;
    private boolean mIsStartTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vibrate_test);

        mIsStartTest = false;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mResources = getResources();

        mTestTimeEt = (EditText) findViewById(R.id.test_time);
        mCurrentTestTimeTv = (TextView) findViewById(R.id.current_test_time);
        mTestedTimeTv = (TextView) findViewById(R.id.tested_time);
        mLastTestTimeTv = (TextView) findViewById(R.id.last_test_time);
        mLastRealTestTimeTv = (TextView) findViewById(R.id.last_real_test_time);
        mStartTestBt = (Button) findViewById(R.id.start_test);
        mLastTestResultTv = (TextView) findViewById(R.id.last_test_result);
        mStartTestBt.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermission(this, Manifest.permission.VIBRATE)) {
                requestPermissions(new String[] {Manifest.permission.VIBRATE}, PERMISSIONS_REQUEST_VIBRATE);
            } else {
                updateViews();
            }
        } else {
            updateViews();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTest();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_test:
                if (mIsStartTest) {
                    stopTest();
                } else {
                    startTest();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult=>requestCode:" + requestCode);
        if (permissions == null || permissions.length == 0 ||
                grantResults == null || grantResults.length == 0) {
            Log.d(TAG, "onRequestPermissionsResult=>Permission or grant res null");
            return;
        }
        Log.d(TAG, "onRequestPermissionsResult=>permission length: " + permissions.length + " grant length: " + grantResults.length);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_VIBRATE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateViews();
                } else {
                    if (!shouldShowRequestPermissionRationale(this, permissions[0])) {
                        Toast.makeText(this, R.string.denied_required_permission, Toast.LENGTH_LONG);
                    }
                    finish();
                }
                break;
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasPermission(Context ctx, String permission) {
        boolean granted = (ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
        Log.d(TAG, "hasPermission=>granted: " + granted + " permission: " + permission);
        return granted;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void requestPermission(Activity ctx, String[] permissions, int requestCode){
        Log.d(TAG, "requestPermission=>requestCode: " + requestCode + " permissions: " + Arrays.toString(permissions));
        ctx.requestPermissions(permissions, requestCode);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean shouldShowRequestPermissionRationale(Activity ctx, String permission){
        return ctx.shouldShowRequestPermissionRationale(permission);
    }

    private void updateViews() {
        int lastTestTime = mSharedPreferences.getInt(KEY_LAST_TEST_TIME, -1);
        long startTime = mSharedPreferences.getLong(KEY_START_TIME, -1);
        long endTime = mSharedPreferences.getLong(KEY_END_TIME, -1);
        if (mTestTime > 0) {
            mTestTimeEt.setText(mTestTime + "");
        } else {
            mTestTimeEt.setText("");
        }
        if (mIsStartTest) {
            mCurrentTestTimeTv.setText(getTestTimeString(mTestTime * MINUTE));
            mTestedTimeTv.setText(getTestTimeString(System.currentTimeMillis() - mStartTime));
        } else {
            mCurrentTestTimeTv.setText("");
            mTestedTimeTv.setText("");
        }
        if (lastTestTime > 0) {
            mLastTestTimeTv.setText(getTestTimeString(lastTestTime * MINUTE));
        } else {
            mLastTestTimeTv.setText("");
        }
        if (endTime - startTime > 0) {
            mLastRealTestTimeTv.setText(getTestTimeString(endTime - startTime));
        } else {
            mLastRealTestTimeTv.setText("");
        }
        if (startTime == -1 || endTime == -1) {
            mLastTestResultTv.setText("");
        } else {
            if ((endTime - startTime) / MINUTE >= lastTestTime) {
                mLastTestResultTv.setTextColor(0xff00ff00);
                mLastTestResultTv.setText(R.string.test_success);
            } else {
                mLastTestResultTv.setTextColor(0xffff0000);
                mLastTestResultTv.setText(R.string.test_fail);
            }
        }
    }

    private void startTest() {
        String testTimeStr = mTestTimeEt.getText().toString();
        try {
            mTestTime = Integer.parseInt(testTimeStr);
        } catch (Exception e) {
            mTestTime = -1;
            mTestTimeEt.setText("");
        }
        if (mTestTime > 0 && mTestTime < 30000) {
            mTestTimeEt.setEnabled(false);
            mIsStartTest = true;
            mStartTestBt.setText(getString(R.string.vibrate_cancel_test));
            mStartTime = System.currentTimeMillis();
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_TIME_DELAYED);
            vibrate();
        } else {
            Toast.makeText(this, R.string.test_time_limit_tip, Toast.LENGTH_SHORT).show();
        }
        updateViews();
    }

    private void stopTest() {
        if (mIsStartTest) {
            mIsStartTest = false;
            mHandler.removeMessages(MSG_UPDATE_TIME);
            mTestTimeEt.setEnabled(true);
            mStartTestBt.setText(R.string.start_test);
            cancelVibrate();
            mSharedPreferences.edit().putInt(KEY_LAST_TEST_TIME, mTestTime).commit();
            mSharedPreferences.edit().putLong(KEY_START_TIME, mStartTime).commit();
            mSharedPreferences.edit().putLong(KEY_END_TIME, System.currentTimeMillis()).commit();
            mTestTime = -1;
        }
        updateViews();
    }

    public void vibrate() {
        int[] vibrateTime = mResources.getIntArray(R.array.vibrate_time);
        long[] times = new long[vibrateTime.length];
        for (int i = 0; i < vibrateTime.length; i++) {
            times[i] = vibrateTime[i];
        }
        mVibrator.vibrate(times, mResources.getInteger(R.integer.vibrate_repeat_times));
    }

    public void cancelVibrate() {
        if (mVibrator.hasVibrator()) {
            mVibrator.cancel();
        }
    }

    private String getTestTimeString(long millisecond) {
        StringBuilder timeStr = new StringBuilder();
        long SECOND = 1000;
        long MINUTE = 60 * SECOND;
        long HOUR = MINUTE * 60;
        long DAY = HOUR * 24;
        if (millisecond <= 0) {
            return "";
        }
        long day = millisecond / DAY;
        long hour = (millisecond %  DAY) / HOUR;
        long minute = (millisecond % HOUR) / MINUTE;
        long second = millisecond % MINUTE / SECOND;
        if (day > 0) {
            timeStr.append(getString(R.string.day_string, day));
        }
        if (hour > 0 || day > 0) {
            if (day > 0) {
                timeStr.append(" ");
            }
            timeStr.append(getString(R.string.hour_string, hour));
        }
        if (minute > 0 || day > 0 || hour > 0) {
            if (day > 0 || hour > 0) {
                timeStr.append(" ");
            }
            timeStr.append(getString(R.string.minute_string, minute));
        }
        if (second > 0 || day > 0 || hour > 0 || minute > 0) {
            if (day > 0 || hour > 0 || minute > 0) {
                timeStr.append(" ");
            }
            timeStr.append(getString(R.string.second_string, second));
        }
        return timeStr.toString();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage=>what: " + msg.what + " start: " + mIsStartTest);
            switch (msg.what) {
                case MSG_UPDATE_TIME:
                    if (mIsStartTest) {
                        long testedTime = System.currentTimeMillis() - mStartTime;
                        mTestedTimeTv.setText(getTestTimeString(testedTime));
                        if ((testedTime - mTestTime * MINUTE) >= 0) {
                            stopTest();
                        } else {
                            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_TIME_DELAYED);
                        }
                    }
                    break;
            }
        }
    };
}
