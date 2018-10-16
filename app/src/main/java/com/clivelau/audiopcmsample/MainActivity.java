package com.clivelau.audiopcmsample;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    public static final String TAG = "AudioPCMSample";

    private static final int REQUEST_CODE_OPEN_BLUETOOTH = 0;
    private static final int REQUEST_CODE_PERMISSION = 1;

    //开始录音
    private Button mStartAudio;
    //结束录音
    private Button mStopAudio;
    //播放音频
    private Button mPlayAudio;
    //切换输出
    private Button mSwitchAudio;

    //音频输入源组
    private RadioGroup mSourceGroup;

    //音頻输出源组
    private RadioGroup mSinkGroup;

    //音频输入设备
    private TextView mSourceTxt;

    //音频输出设备
    private TextView mSinkTxt;

    //Log输出
    private TextView mLog;
    private ScrollView mScrollView;

    //是否在录制
    private boolean isRecording = false;

    //BT SCO功能
    private boolean mInBtScoMode = false;

    //pcm文件
    private File mPcmFile = null;

    private AudioManager mAudioManager = null;

    private AudioTrack mAudioTrack = null;

    private boolean mIsSwitchAudio = false;
    private int mOffsetInData = 0;

    //音频输入源
    private int mAudioSource = MediaRecorder.AudioSource.DEFAULT;
    //音频输出源
    private int mAudioSink = AudioManager.STREAM_VOICE_CALL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        checkPermissions();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (null == mAudioManager)
            printLog("初始化失败");
        else
            printLog("初始化成功");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //初始化View
    private void initView() {
        //开始录音
        mStartAudio = (Button) findViewById(R.id.idStartAudio);
        mStartAudio.setOnClickListener(this);
        //结束录音
        mStopAudio = (Button) findViewById(R.id.idStopAudio);
        mStopAudio.setOnClickListener(this);
        //播放音频
        mPlayAudio = (Button) findViewById(R.id.idPlayAudio);
        mPlayAudio.setOnClickListener(this);
        //切换输出
        mSwitchAudio = (Button) findViewById(R.id.idSwitchAudio);
        mSwitchAudio.setOnClickListener(this);
        //音频输入源
        mSourceGroup = (RadioGroup) findViewById(R.id.idSourceGroup);
        mSourceGroup.setOnCheckedChangeListener(this);
        //音频输出源
        mSinkGroup = (RadioGroup) findViewById(R.id.idSinkGroup);
        mSinkGroup.setOnCheckedChangeListener(this);
        //音频输入/输出设备
        mSourceTxt = (TextView) findViewById(R.id.idSourceTxt);
        mSinkTxt = (TextView) findViewById(R.id.idSinkTxt);
        //Log输出
        mScrollView = (ScrollView) findViewById(R.id.idScrollView);
        mLog = (TextView) findViewById(R.id.idLog);

        ButtonEnabled(true, false, false, false);
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        switch (checkedId) {
            case R.id.idSourceDefault:
                printLog("Source Default");
                mAudioSource = MediaRecorder.AudioSource.DEFAULT;
                break;

            case R.id.idSourceMic:
                printLog("Source Mic");
                mAudioSource = MediaRecorder.AudioSource.MIC;
                break;

            case R.id.idSourceVoiceUplink:
                printLog("Source VoiceUplink");
                mAudioSource = MediaRecorder.AudioSource.VOICE_UPLINK;
                break;

            case R.id.idSourceVoiceDownlink:
                printLog("Source VoiceDownlink");
                mAudioSource = MediaRecorder.AudioSource.VOICE_DOWNLINK;
                break;

            case R.id.idSourceVoiceCall:
                printLog("Source VoiceCall");
                mAudioSource = MediaRecorder.AudioSource.VOICE_CALL;
                break;

            case R.id.idSourceCamcorder:
                printLog("Source Camcorder");
                mAudioSource = MediaRecorder.AudioSource.CAMCORDER;
                break;

            case R.id.idSourceVoiceRecognition:
                printLog("Source VoiceRecognition");
                mAudioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
                break;

            case R.id.idSourceVoiceCommunication:
                printLog("Source VoiceCommunication");
                mAudioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
                break;

            case R.id.idSourceRemoteSubmix:
                printLog("Source RemoteSubmix");
                mAudioSource = MediaRecorder.AudioSource.REMOTE_SUBMIX;
                break;

            case R.id.idSourceUnprocessed:
                printLog("Source Unprocessed");
                mAudioSource = MediaRecorder.AudioSource.UNPROCESSED;
                break;

            case R.id.idSinkVoiceCall:
                printLog("Sink VoiceCall");
                mAudioSink = AudioManager.STREAM_VOICE_CALL;
                break;

            case R.id.idSinkSystem:
                printLog("Sink System");
                mAudioSink = AudioManager.STREAM_SYSTEM;
                break;

            case R.id.idSinkMusic:
                printLog("Sink Music");
                mAudioSink = AudioManager.STREAM_SYSTEM;
                break;

            case R.id.idSinkAlarm:
                printLog("Sink Alarm");
                mAudioSink = AudioManager.STREAM_ALARM;
                break;

            case R.id.idSinkNotification:
                printLog("Sink Notification");
                mAudioSink = AudioManager.STREAM_NOTIFICATION;
                break;

            case R.id.idSinkDtmf:
                printLog("Sink DTMF");
                mAudioSink = AudioManager.STREAM_DTMF;
                break;

            case R.id.idSinkAccessibility:
                printLog("Sink Accessibility");
                mAudioSink = AudioManager.STREAM_ACCESSIBILITY;
                break;

            default:
                printLog("Invalid checkedid:" + checkedId);
                break;
        }
    }

    //点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.idStartAudio:
                printLog("开始录音");
                ButtonEnabled(false, true, false, false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        onRecord();
                    }
                }).start();
                break;
            case R.id.idStopAudio:
                printLog("停止录音");
                ButtonEnabled(true, false, true, false);
                setRecording(false);
                break;
            case R.id.idPlayAudio:
                printLog("播放录音");
                ButtonEnabled(true, false, true, true);
                setOffsetInData(0);
                Log.i(TAG, "onClick: play, this.mAudioSink:" + this.mAudioSink);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        onPlay();
                    }
                }).start();
                break;
            case R.id.idSwitchAudio:
                printLog("切换输出");
                ButtonEnabled(true, false, true, true);
//                if (AudioTrack.PLAYSTATE_PLAYING == mAudioTrack.getPlayState()) {
//                    mAudioTrack.pause();
//                }
                setSwitchAudio(true);
                Log.i(TAG, "onClick: mOffsetInData:" + getOffsetInData());
                Log.i(TAG, "onClick: this.mOffsetInData:" + this.mOffsetInData);
                Log.i(TAG, "onClick: mAudioTrack:" + getAudioTrack());
                Log.i(TAG, "onClick: this.mAudioTrack:" + this.mAudioTrack);
                if (AudioTrack.PLAYSTATE_PLAYING == mAudioTrack.getPlayState()) {
                    Log.i(TAG, "onClick: AudioTrack pause");
                    mAudioTrack.pause();
//                    mAudioTrack.stop();
                }

                Log.i(TAG, "onClick: play, this.mAudioSink:" + this.mAudioSink);

//                mAudioTrack.play();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        onPlay();
                    }
                }).start();

                break;
        }
    }

    //打印log
    private void printLog(final String resultString) {
        mLog.post(new Runnable() {
            @Override
            public void run() {
                mLog.append(resultString + "\n");
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private synchronized boolean getRecording() {
        return this.isRecording;
    }

    private synchronized void setRecording(boolean enable) {
        this.isRecording = enable;
    }

    private synchronized boolean getSwitchAudio() {
        return this.mIsSwitchAudio;
    }

    private synchronized void setSwitchAudio(boolean enable) {
        this.mIsSwitchAudio = enable;
    }

    private synchronized int getOffsetInData() {
        return this.mOffsetInData;
    }

    private synchronized void setOffsetInData(int val) {
        this.mOffsetInData = val;
    }

    private synchronized AudioTrack getAudioTrack() {
        return this.mAudioTrack;
    }

    private synchronized void setAudioTrack(AudioTrack track) {
        this.mAudioTrack = track;
    }

    //获取/失去焦点
    private void ButtonEnabled(boolean start, boolean stop, boolean play, boolean switcher) {
        mStartAudio.setEnabled(start);
        mStopAudio.setEnabled(stop);
        mPlayAudio.setEnabled(play);
        mSwitchAudio.setEnabled(switcher);
    }

    //切换BT SCO模式
    private void onSwitchBtSco(boolean enable) {
        int retries = 100;

        if (null == mAudioManager) {
            Log.e(TAG, "onSwitchBtSco: 无效的AudioManager");
            return;
        }

        if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
            Log.e(TAG, "onSwitchBtSco: 此平台不支持BT SCO调用");
            return;
        }

        // TODO:增加判断蓝牙是否已连接设备

        if (enable) {
            if (mAudioManager.isBluetoothScoOn()) {
                printLog("默认已打开BT SCO");
                return;
            }
            
            mAudioManager.startBluetoothSco();
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while ((!mAudioManager.isBluetoothScoOn()) && (--retries > 0));

            printLog("成功打开BT SCO, retries:" + retries);
        } else {
            if (!mAudioManager.isBluetoothScoOn()) {
                printLog("默认已关闭BT SCO");
                return;
            }

            mAudioManager.stopBluetoothSco();
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (mAudioManager.isBluetoothScoOn() && (--retries > 0));

            printLog("成功关闭BT SCO, retries:" + retries);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.idBtSco:
                printLog("InBtScoMode:" + mInBtScoMode + "-->" + !mInBtScoMode);
                onSwitchBtSco(!mInBtScoMode);
                mInBtScoMode = !mInBtScoMode;
                break;
            default:
                printLog("无效OptionsItemSelected");
                break;
        }
        return true;
    }

    //开始录音
    public void onRecord() {
        Log.i(TAG,"开始录音");
        //16K采集率
        int frequency = 16000;
        //格式
        int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        //16Bit
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

        //生成PCM文件
        mPcmFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/reverseme.pcm");
        Log.i(TAG,"生成文件:" + mPcmFile.getPath());
        //如果存在，就先删除再创建
        if (mPcmFile.exists())
            mPcmFile.delete();
        Log.i(TAG,"删除文件");
        try {
            mPcmFile.createNewFile();
            Log.i(TAG,"创建文件");
        } catch (IOException e) {
            Log.i(TAG,"未能创建");
            throw new IllegalStateException("未能创建" + mPcmFile.toString());
        }

        try {
            //输出流
            OutputStream os = new FileOutputStream(mPcmFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(
                    mAudioSource,
                    frequency,
                    channelConfiguration,
                    audioEncoding,
                    bufferSize);
            short[] buffer = new short[bufferSize];
            audioRecord.startRecording();
            Log.i(TAG, "开始录音");
            setRecording(true);
            while (getRecording()) {
                int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                for (int i = 0; i < bufferReadResult; i++) {
                    dos.writeShort(buffer[i]);
                }
            }
            audioRecord.stop();
            dos.close();
        } catch (Throwable t) {
            Log.e(TAG, "录音失败");
        }
    }

    //播放文件
    public void onPlay() {
        Log.i(TAG,"播放音频");

        if ((null == mPcmFile) || !mPcmFile.exists()) {
            Log.e(TAG, "无效PCM File");
            return;
        }
        Log.i(TAG, "onPlay: getOffsetInData:" + getOffsetInData());
        //读取文件
        int musicLength = (int) (mPcmFile.length() / 2);
        Log.e(TAG, "musicLength:" + musicLength);
        short[] music = new short[musicLength];
        try {
            InputStream is = new FileInputStream(mPcmFile);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readShort();
                i++;
            }
            dis.close();

            int audioBufSize = AudioTrack.getMinBufferSize(8000,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack audioTrack = new AudioTrack(
                    mAudioSink,
                    8000,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    audioBufSize,
                    AudioTrack.MODE_STREAM);
            setAudioTrack(audioTrack);
            audioTrack.play();
//            audioTrack.write(music, 0, musicLength);
            Log.i(TAG, "onPlay: getOffsetInData:" + getOffsetInData());
            for (i = getOffsetInData(); !getSwitchAudio() && (i < musicLength); i+=10) {
                audioTrack.write(music, i, 10);
                setOffsetInData(i);
            }
            Log.e(TAG, "Ending");
            setSwitchAudio(false);
            audioTrack.stop();
            audioTrack.release();
        } catch (Throwable t) {
            Log.e(TAG, "播放失败");
        }
    }

    //检查Bluetooth是否已开启
    private boolean checkBluetoothIsOpen() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter.isEnabled();
    }

    //检查权限
    private void checkPermissions() {
        onPermissionGranted(Manifest.permission.BLUETOOTH);

        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.BLUETOOTH:
            case Manifest.permission.BLUETOOTH_ADMIN:
                if (!checkBluetoothIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.notifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_BLUETOOTH);
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                } else {
                    Toast.makeText(this, getString(R.string.done_open_blue), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_BLUETOOTH) {
            onPermissionGranted(Manifest.permission.BLUETOOTH);
        }
    }
}
