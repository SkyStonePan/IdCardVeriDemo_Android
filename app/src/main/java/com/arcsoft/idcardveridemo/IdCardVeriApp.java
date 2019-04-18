package com.arcsoft.idcardveridemo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Environment;

import com.blankj.utilcode.util.CrashUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.Utils;

import java.io.File;

public class IdCardVeriApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        initLog();
        initCrash();
    }

    public void initLog() {
        final LogUtils.Config config = LogUtils.getConfig()
                .setLogSwitch(BuildConfig.DEBUG)
                .setConsoleSwitch(BuildConfig.DEBUG)
                .setGlobalTag(null)
                .setLogHeadSwitch(true)
                .setLog2FileSwitch(false)
                .setDir("")
                .setFilePrefix("")
                .setBorderSwitch(true)
                .setSingleTagSwitch(true)
                .setConsoleFilter(LogUtils.V)
                .setFileFilter(LogUtils.V)
                .setStackOffset(0);
        LogUtils.d(config.toString());
    }

    @SuppressLint("MissingPermission")
    private void initCrash() {
        CrashUtils.init(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "IdCardVeriDemo" + File.separator + "crash",
                (crashInfo, e) -> LogUtils.e(crashInfo));
    }
}
