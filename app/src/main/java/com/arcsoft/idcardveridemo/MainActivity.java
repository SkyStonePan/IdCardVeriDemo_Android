package com.arcsoft.idcardveridemo;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.idcardveri.CompareResult;
import com.arcsoft.idcardveri.DetectFaceResult;
import com.arcsoft.idcardveri.IdCardVerifyError;
import com.arcsoft.idcardveri.IdCardVerifyListener;
import com.arcsoft.idcardveri.IdCardVerifyManager;
import com.arcsoft.idcardveridemo.camera.CameraHelper;
import com.arcsoft.idcardveridemo.camera.CameraListener;
import com.arcsoft.idcardveridemo.draw.DrawHelper;
import com.arcsoft.idcardveridemo.draw.DrawInfo;
import com.arcsoft.idcardveridemo.draw.FaceRectView;
import com.arcsoft.util.ImageUtils;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ViewTreeObserver.OnGlobalLayoutListener {

    private static final String TAG = "IdCardVeriDemo";

    private static final int READ_DELAY = 5000;
    private static final int SOUND_DELAY = 500;
    private static final int RESTART_DELAY = 3000;
    /**
     * 比对阈值，推荐0.82，可根据实际需求修改
     */
    private static final double THRESHOLD = 0.82d;

    private LinearLayout llCompare;
    private ImageView ivCompareResult;
    private ImageView ivTip;
    private ImageView ivIdCard;
    private TextView tvCompareTip;
    private TextureView textureView;
    private FaceRectView faceRectView;

    /**
     * 预览数据是否特征提取完成
     */
    private boolean isCurrentReady = false;
    /**
     * 身份证数据是否特征提取完成
     */
    private boolean isIdCardReady = false;
    private static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    /**
     * 模拟身份证姓名，可根据实际名称修改
     */
    private static final String SAMPLE_NAME = "张三";
    /**
     * 模拟身份证图像数据路径,可根据实际路径修改
     */
    private static final String SAMPLE_FACE = ROOT_PATH + File.separator + "sample.jpg";

    /**
     * 读卡线程
     */
    private ReadThread readThread;
    /**
     * 是否进行读卡
     */
    private boolean isRead = true;
    /**
     * 身份证姓名
     */
    private String name;
    /**
     * 身份证图像数据
     */
    private Bitmap bmp;

    /**
     * 音频播放器
     */
    private MediaPlayer mediaPlayer;
    /**
     * 相机工具类
     */
    private CameraHelper cameraHelper;
    /**
     * 画框工具类
     */
    private DrawHelper drawHelper;
    /**
     * 预览窗口大小
     */
    private Camera.Size previewSize;
    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private Runnable readRunnable = () -> {
        isRead = true;
        isIdCardReady = false;
        runOnUiThread(this::initCompareLayout);
    };
    private Handler readHandler = new Handler();

    private Runnable restartRunnable = () -> runOnUiThread(this::initCompareLayout);
    private Handler restartHandler = new Handler();

    private Runnable soundRunnable = () -> runOnUiThread(() -> playSound(R.raw.look_screen));
    private Handler soundHandler = new Handler();

    private IdCardVerifyListener idCardVerifyListener = new IdCardVerifyListener() {
        @Override
        public void onPreviewResult(DetectFaceResult detectFaceResult, byte[] bytes, int i, int i1) {
            runOnUiThread(() -> {
                //预览人脸特征提取成功
                if (detectFaceResult.getErrCode() == IdCardVerifyError.OK) {
                    isCurrentReady = true;
                    compare();
                }
            });
        }

        @Override
        public void onIdCardResult(DetectFaceResult detectFaceResult, byte[] bytes, int i, int i1) {
            LogUtils.dTag(TAG, "onIdCardResult: " + detectFaceResult.getErrCode());
            runOnUiThread(() -> {
                //身份证人脸特征提取成功
                if (detectFaceResult.getErrCode() == IdCardVerifyError.OK) {
                    isIdCardReady = true;
                    restartHandler.removeCallbacks(restartRunnable);
                    readHandler.postDelayed(readRunnable, READ_DELAY);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.PNG, 80, baos);
                    byte[] bmpBytes = baos.toByteArray();
                    Glide.with(MainActivity.this).load(bmpBytes).into(ivIdCard);
                    compare();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mediaPlayer = MediaPlayer.create(this, R.raw.look_screen);
        initIdReader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(cameraHelper != null) {
            cameraHelper.start();
        }

    }

    @Override
    protected void onPause() {
        if(cameraHelper != null) {
            cameraHelper.stop();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unInitEngine();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        unInitReader();
        unInitCamera();
        super.onDestroy();
    }

    private void initView() {
        ivTip = findViewById(R.id.iv_tip);
        ivIdCard = findViewById(R.id.iv_idcard);
        llCompare = findViewById(R.id.ll_compare);
        ivCompareResult = findViewById(R.id.iv_compare_result);
        tvCompareTip = findViewById(R.id.tv_compare_tip);
        textureView = findViewById(R.id.texture_view_preview);
        faceRectView = findViewById(R.id.face_rect_view);
        textureView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * 引擎初始化
     */
    private void initEngine() {
        int result = IdCardVerifyManager.getInstance().init(this, idCardVerifyListener);
        LogUtils.dTag(TAG, "initResult: " + result);
        if (result == IdCardVerifyError.MERR_ASF_NOT_ACTIVATED) {
            Executors.newSingleThreadExecutor().execute(() -> {
                int activeResult = IdCardVerifyManager.getInstance().active(
                        MainActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                runOnUiThread(() -> {
                    LogUtils.dTag(TAG, "activeResult: " + activeResult);
                    if (activeResult == IdCardVerifyError.OK) {
                        int initResult = IdCardVerifyManager.getInstance().init(
                                MainActivity.this, idCardVerifyListener);
                        LogUtils.dTag(TAG, "initResult: " + initResult);
                        if(initResult != IdCardVerifyError.OK) {
                            toast(getString(R.string.tip_init_fail, initResult));
                        }
                    } else {
                        toast(getString(R.string.tip_active_fail, activeResult));
                    }
                });
            });
        } else if(result != IdCardVerifyError.OK) {
            toast(getString(R.string.tip_init_fail, result));
        }
    }

    /**
     * 销毁引擎
     */
    private void unInitEngine() {
        IdCardVerifyManager.getInstance().unInit();
    }

    /**
     * 相机初始化
     */
    private void initCamera() {
        int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        //相机是否镜像
        boolean isMirror = false;
        //画框是否竖直镜像
        boolean verticalMirror = false;
        //画框是否水平镜像
        boolean horizontalMirror = false;

        CameraListener cameraListener = new CameraListener() {

            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                LogUtils.dTag(TAG, "onCameraOpened: " + cameraId + "  " + displayOrientation + " " + isMirror);
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height,
                        textureView.getWidth(), textureView.getHeight(), displayOrientation,
                        cameraId, isMirror, horizontalMirror, verticalMirror);
            }


            @Override
            public void onPreview(byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                if (nv21 == null) {
                    return;
                }
                //预览数据传入
                DetectFaceResult result = IdCardVerifyManager.getInstance().onPreviewData(nv21,
                        previewSize.width, previewSize.height, true);
                Rect rect = result.getFaceRect();

                if (faceRectView != null && drawHelper != null && rect != null) {
                    //生成实时人脸框
                    drawHelper.draw(faceRectView, new DrawInfo(drawHelper.adjustRect(rect), "", Color.YELLOW));
                }
            }

            @Override
            public void onCameraClosed() {
            }

            @Override
            public void onCameraError(Exception e) {
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {

            }

            @Override
            public void onTakePicture(int width, int height, CameraHelper.TakePictureType takePictureType, int displayOrientation, byte[] data) {

            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(textureView.getMeasuredWidth(), textureView.getMeasuredHeight()))
                .rotation(ActivityUtils.getTopActivity().getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(cameraId)
                .isMirror(isMirror)
                .previewOn(textureView)
                .cameraListener(cameraListener)
//                .specificPreviewSize(new Point(1280, 720))
                .build();
        //相机工具类初始化
        cameraHelper.init();
        cameraHelper.start();
    }

    /**
     * 销毁相机
     */
    private void unInitCamera() {
        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }
    }

    /**
     * 初始化读卡器并启动读卡线程
     */
    private void initIdReader() {
        int initResult = -1;
        // TODO: 初始化读卡器，初始化成功后启动读卡线程，默认initResult为0代表初始化成功
        if(initResult == 0) {
            readThread = new ReadThread();
            readThread.start();
        }
    }

    /**
     * 关闭读卡器及停止读卡线程
     */
    private void unInitReader() {
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
        // TODO: 关闭读卡器
    }

    private void playSound(int soundRes) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer = MediaPlayer.create(this, soundRes);
        mediaPlayer.start();
    }

    private int maxRetryTime = 2;
    private int tryTime = 0;

    /**
     * 比对接口，身份证数据与预览数据都提取人脸特征成功后可进行人证特征比对
     */
    private void compare() {
        if (!isIdCardReady) {
            return;
        }
        if (!isCurrentReady) {
            ivTip.setBackgroundResource(R.mipmap.look_camera);
            ivTip.setVisibility(View.VISIBLE);
            llCompare.setVisibility(View.GONE);
            soundHandler.postDelayed(soundRunnable, SOUND_DELAY);
            return;
        }
        soundHandler.removeCallbacks(soundRunnable);
        ivTip.setBackgroundResource(R.mipmap.comparing);
        ivTip.setVisibility(View.VISIBLE);
        llCompare.setVisibility(View.GONE);
        //人证特征比对接口
        CompareResult compareResult = IdCardVerifyManager.getInstance().compareFeature(THRESHOLD);
        LogUtils.dTag(TAG, "compareResult: result " + compareResult.getResult() + ", isSuccess "
                + compareResult.isSuccess() + ", errCode " + compareResult.getErrCode());
        if (compareResult.isSuccess()) {
            playSound(R.raw.compare_success);
            ivCompareResult.setBackgroundResource(R.mipmap.compare_success);
            tvCompareTip.setText(name);
        } else {
            playSound(R.raw.compare_fail);
            ivCompareResult.setBackgroundResource(R.mipmap.compare_fail);
            tvCompareTip.setText(R.string.tip_retry);
        }
        ivTip.setVisibility(View.GONE);
        llCompare.setVisibility(View.VISIBLE);
        isIdCardReady = false;
        isCurrentReady = false;
        isRead = true;
        readHandler.removeCallbacks(readRunnable);
        restartHandler.postDelayed(restartRunnable, RESTART_DELAY);
        //失败重试
//        if(!compareResult.isSuccess() && tryTime < maxRetryTime) {
//            tryTime++;
//            inputIdCard();
//        } else {
//            tryTime = 0;
//        }
    }

    private void initCompareLayout() {
        ivTip.setBackgroundResource(R.mipmap.put_idcard);
        ivTip.setVisibility(View.VISIBLE);
        llCompare.setVisibility(View.GONE);
    }

    @Override
    public void onGlobalLayout() {
        textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        PermissionUtils.permission(NEEDED_PERMISSIONS)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(List<String> permissionsGranted) {
                        //初始化引擎
                        initEngine();
                        //初始化相机
                        initCamera();
                    }

                    @Override
                    public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                        toast(R.string.tip_permission_deny);
                        AppUtils.exitApp();
                    }
                })
                .request();
    }

    /**
     * 模拟身份证输入
     * @param view
     */
    public void onClickIdCard(View view) {
        //模拟身份证姓名，可修改
        name = SAMPLE_NAME;
        FileInputStream fis;
        //身份证图像数据
        bmp = null;
        try {
            //模拟身份证图像数据来源，可修改
            fis = new FileInputStream(SAMPLE_FACE);
            bmp = BitmapFactory.decodeStream(fis);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        inputIdCard();
    }

    private void inputIdCard() {
        if (bmp == null) {
            return;
        }
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        //图像裁剪
        boolean needAdjust = false;
        while (width % 4 != 0) {
            width--;
            needAdjust = true;
        }
        if (height % 2 != 0) {
            height--;
            needAdjust = true;
        }
        if (needAdjust) {
            bmp = ImageUtils.imageCrop(bmp, new Rect(0, 0, width, height));
        }
        //转换为NV21数据格式
        byte[] nv21Data = ImageUtils.getNV21(width, height, bmp);
        //身份证图像数据输入
        DetectFaceResult result = IdCardVerifyManager.getInstance().inputIdCardData(
                nv21Data, width, height);
        LogUtils.dTag(TAG, "inputIdCardData result: " + result.getErrCode());
    }

    /**
     * 读卡线程
     */
    public class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (true) {
                if (isRead) {
                    int res = -1;
                    // TODO: 读卡器认证、读卡及身份证照片解码，默认res为0代表读卡成功
                    if(res != 0) {
                        continue;
                    }
                    isRead = false;
                    // TODO:  身份证姓名及图像数据拷贝（name、bmp）
                    //主线程执行
                    runOnUiThread(MainActivity.this::inputIdCard);
                }
            }
        }
    }

    public void toast(String str) {
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
    }

    public void toast(int res) {
        Toast.makeText(MainActivity.this, res, Toast.LENGTH_LONG).show();
    }


}
