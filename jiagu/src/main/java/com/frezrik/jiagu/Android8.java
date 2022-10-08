package com.frezrik.jiagu;

import android.util.Log;

import com.frezrik.jiagu.util.ZipUtil;

public class Android8 {
    private static final String TAG = "Android8";
    public static void attach(StubApp application) {
        // 从/data/app/xxx/base.apk获取dex
        byte[] dexArray = getDex(application);

        // 内存加载dex
        loadDex(application, dexArray);
    }

    private static byte[] getDex(StubApp application) {
        String sourceDir = application.getApplicationInfo().sourceDir;
        Log.w(TAG, "getDex: " + sourceDir);
        return ZipUtil.getDexData(sourceDir);
    }

    private static void loadDex(StubApp application, byte[] dexArray) {
        int dexlen = dexArray.length;
        int shell_len = byte2Int(dexArray, dexlen - 4);
        int decryptdex_len = dexlen - shell_len - 16 - 4;
        byte[] decryptdex = new byte[decryptdex_len];



    }


    private static int byte2Int(byte[] b, int index) {
        return ((b[index] & 0xff) << 24) + ((b[index + 1] & 0xff) << 16) + ((b[index + 2] & 0xff) << 8) +(b[index + 3] & 0xff);
    }
}
