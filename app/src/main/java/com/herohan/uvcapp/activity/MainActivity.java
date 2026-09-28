package com.uvccamera.demo;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack;
import com.jiangdg.ausbc.camera.CameraUVC;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.widget.CameraView;

public class MainActivity extends AppCompatActivity {
    private CameraUVC mCameraController;
    private CameraView mCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 保持屏幕常亮，和系统相机一致
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        
        mCameraView = findViewById(R.id.camera_view);
        // 开启沉浸式全屏
        setImmersiveFullScreen();
        // 初始化UVC摄像头
        initUVCCamera();
    }

    /**
     * 粘性沉浸式全屏，和系统相机体验一致：滑动边缘临时呼出系统栏，3秒后自动隐藏
     */
    private void setImmersiveFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void initUVCCamera() {
        mCameraController = CameraUVC.getInstance();
        // 设置预览默认参数：优先1080P，YUYV格式兼容性最好避免黑屏
        mCameraController.setCameraRequest(new CameraRequest.Builder()
                .setPreviewWidth(1920)
                .setPreviewHeight(1080)
                .setFormat(CameraRequest.FORMAT_YUYV)
                .setPreviewRotation(0) // 预览方向，可根据需求调整90/180/270
                .isMirror(false) // 是否镜像，前置摄像头可设为true
                .create());
        
        // 设备连接回调
        mCameraController.setConnectCallBack(new IDeviceConnectCallBack() {
            @Override
            public void onAttachDev() {
                // USB摄像头插入，自动请求权限
                mCameraController.requestPermission(0);
            }

            @Override
            public void onConnectDev() {
                // 摄像头连接成功，开始全屏预览（自动缩放铺满屏幕不拉伸）
                mCameraController.startPreview(mCameraView, true); // 第二个参数true=铺满裁剪，false=留白
            }

            @Override
            public void onDisConnectDev() {
                // 摄像头拔出，停止预览
                mCameraController.stopPreview();
            }

            @Override
            public void onDetachDev() {
                mCameraController.stopPreview();
            }
        });
    }

    // 生命周期绑定，避免内存泄漏和崩溃
    @Override
    protected void onStart() {
        super.onStart();
        mCameraController.registerUsbReceiver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersiveFullScreen(); // 返回页面时重新隐藏系统栏
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraController.unRegisterUsbReceiver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraController.releaseCamera();
        mCameraController.destroy();
    }

    // 窗口焦点变化时自动恢复全屏
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setImmersiveFullScreen();
        }
    }
}
