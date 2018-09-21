/**
 ===============================================================================================
 * Project Name:
 * Class Description: CaptureActivity
 * Created by timeyoyo 
 * Created at 2018/9/21 10:53
 * -----------------------------------------------------------------------------------------------
 * ★ Some Tips For You ★
 * 1.
 * 2.
 ===============================================================================================
 * HISTORY
 *
 * Tag                      Date       Author           Description
 * ======================== ========== ===============  ========================================
 * MK                       2018/9/21   timeyoyo         Create new file
 ===============================================================================================
 */
package com.exidcard;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.exidcard.camera.CameraManager;
import com.exidcard.decoding.CaptureActivityHandler;
import com.exidcard.decoding.InactivityTimer;
import com.exidcard.mycard.R;
import com.exidcard.view.ViewfinderView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class CaptureActivity extends AppCompatActivity implements Callback {
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;
    private int time;
    private byte dbpath[];

    //save last time recognize result
    private ExIDCardResult cardlast = null;
    //current time recognition result
    private ExIDCardResult cardRest = null;

    private final ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
        }
    };
    //===========指定识别分类器模型库文件加载到SD卡中的保存位置及文件名==========================================
    //注意：受动态库限制，路径及文件名不可更改
    private final String RESOURCEFILEPATH = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/exidcard/";   //模型库路径


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 透明状态栏
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 透明导航栏
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        //CameraManager
        CameraManager.init(getApplication());
        hardwareSupportCheck();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // FLAG_TRANSLUCENT_NAVIGATION
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        setContentView(R.layout.activity_exid_card);

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        time = 0;
//		logo = BitmapFactory.decodeResource(this.getResources(), R.drawable.yidaoboshi96);
//		viewfinderView.setLogo(logo);

        //ExIDCardReco tmp = new ExIDCardReco();
        String path = this.getApplicationContext().getFilesDir().getAbsolutePath();
        //InitDict(RESOURCEFILEPATH);
        InitDict(path);
    }

    public boolean hardwareSupportCheck() {
        // Camera needs to open
        Camera c = null;
        // check Android 6 permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST", "Granted");
            //init(barcodeScannerView, getIntent(), null);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);//1 can be another integer
        }
        try {
            c = Camera.open();
        } catch (RuntimeException e) {
            throw new RuntimeException();
        }
        if (c == null) {
            return false;
        } else {
            c.release();
        }
        return true;
    }

    public boolean InitDict(String dictpath) {
        dbpath = new byte[256];
        //if the dict not exist, copy from the assets
        if (CheckExist(dictpath + "/zocr0.lib") == false) {
            File destDir = new File(dictpath);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            boolean a = copyFile("zocr0.lib", dictpath + "/zocr0.lib");
            if (a == false) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("EXTrans dict Copy ERROR!\n");
                builder.setMessage(dictpath + " can not be found!");
                builder.setCancelable(true);
                builder.create().show();
                return false;
            }
        }

        String filepath = dictpath;

        //string to byte
        for (int i = 0; i < filepath.length(); i++)
            dbpath[i] = (byte) filepath.charAt(i);
        dbpath[filepath.length()] = 0;

        int nres = ExIDCardReco.nativeInit(dbpath);

        if (nres < 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("EXTrans dict Init ERROR!\n");
            builder.setMessage(filepath + " can not be found!");
            builder.setCancelable(true);
            builder.create().show();
            return false;
        } else {
            //just test
            //ExTranslator.nativeExTran(imgdata, width, height, pixebyte, pitch, flft, ftop, frgt, fbtm, result, maxsize)
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        ExIDCardReco.nativeDone();
        super.onDestroy();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }

    //show the decode result
    public void handleDecode(ExIDCardReco result) {
        inactivityTimer.onActivity();
        if (null != result) {
            Log.e("fnst", "handleDecode-> result="
                    + result.getText().toString() + "  /  "
                    + result.cardcode.cardnum);
            Toast.makeText(this, result.getText().toString(), Toast.LENGTH_LONG).show();
        }

        playBeepSoundAndVibrate();

//        txtResult.setText(obj.getBarcodeFormat().toString() + ":"+ obj.getText());
//        txtResult.setText("decode txt:\n"+ obj.getText());

        //show the result
    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        Point res = CameraManager.get().getResolution();

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (x > res.x * 8 / 10 && y < res.y / 4) {
                return false;
            }

            handleDecode(null);
            cardlast = null;
            //点击重新聚焦
            handler.restartAutoFocus();
            return true;
        }
        return false;
    }

    public boolean copyFile(String from, String to) {
        // 例：from:890.salid;
        // to:/mnt/sdcard/to/890.salid
        boolean ret = false;
        try {
            int bytesum = 0;
            int byteread = 0;

            InputStream inStream = getResources().getAssets().open(from);// 将assets中的内容以流的形式展示出�?
            File file = new File(to);
            OutputStream fs = new FileOutputStream(file);// to为要写入sdcard中的文件名称
            byte[] buffer = new byte[1024];
            while ((byteread = inStream.read(buffer)) != -1) {
                bytesum += byteread;
                fs.write(buffer, 0, byteread);
            }
            inStream.close();
            fs.close();
            ret = true;

        } catch (Exception e) {
            ret = false;
        }
        return ret;
    }

    //check one file
    public boolean CheckExist(String filepath) {
        int i;
        File file = new File(filepath);
        if (file.exists()) {
            return true;
        }
        return false;
    }

    public void SetRecoResult(ExIDCardResult result) {
        int typeId = 1; // 设置身份证正面为1
        cardRest = result;
        if (typeId == cardRest.getType()) {
            /* 回传身份证号延时关闭页面 */
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    String cardnum = cardRest.getCardnum();
                    JSONObject jsonObj = new JSONObject();
                    try {
                        jsonObj.put("data", cardnum);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//					WL.getInstance().sendActionToJS("ocrCheck", jsonObj);
                    Log.e("cardlast.cardnum", "cardlast:" + cardnum);
//                    finish();
                }

            }, 1000);
        }
    }

    //check is equal()
    public boolean CheckIsEqual(ExIDCardResult cardcur) {
        if (cardlast == null) {
            cardlast = cardcur;
            return false;
        } else {
            if (cardlast.name.equals(cardcur.name) &&
                    cardlast.sex.equals(cardcur.sex) &&
                    cardlast.nation.equals(cardcur.nation) &&
                    cardlast.cardnum.equals(cardcur.cardnum) &&
                    cardlast.address.equals(cardcur.address)) {

                return true;
            } else {
                cardlast = cardcur;
                cardcur = null;
                return false;
            }
        }
    }

    public ShutterCallback getShutterCallback() {
        return shutterCallback;
    }

    //////////////////////////////////////////////////////////////////////////
    public void OnShotBtnClick(View view) {
        Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show();
        handleDecode(null);
        playBeepSoundAndVibrate();
        handler.takePicture();
//        CaptureActivity.this.finish();
    }

}