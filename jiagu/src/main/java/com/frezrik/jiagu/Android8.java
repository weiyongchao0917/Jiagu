package com.frezrik.jiagu;

import android.util.ArrayMap;
import android.util.Log;

import com.frezrik.jiagu.util.EncryptUtils;
import com.frezrik.jiagu.util.Reflect;
import com.frezrik.jiagu.util.ZipUtil;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;

import dalvik.system.InMemoryDexClassLoader;

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
        int shell_len = byte2Int(dexArray, dexArray.length - 4);
        Log.w(TAG, "shell len: " + shell_len);


        try {
            //AES加密后数据长度会增加16字节填充数据
            byte[] decrypt = EncryptUtils.decrypt(Arrays.copyOfRange(dexArray, shell_len, shell_len + 512 + 16), 512 + 16);

            String applicationName = new String(Arrays.copyOfRange(decrypt, 1, 1 + decrypt[0]));
            Log.w(TAG, "application: " + applicationName);

            // 总长度 - 壳长度 - AES填充16位 - application len - application长度 - 壳len
            byte[] dexData = new byte[dexArray.length - shell_len - 1 - decrypt[0] - 4 - 16];
            System.arraycopy(decrypt, 1 + decrypt[0], dexData, 0, 512 - 1 - decrypt[0]);
            System.arraycopy(dexArray, shell_len + 512 + 16, dexData, 512 - 1 - decrypt[0], dexArray.length - shell_len - 512 - 16 - 4);

            int index = 0;
            while (index < dexData.length) {
                int dexLen = byte2Int(dexData, index);
                Log.w(TAG, "classes.dex:" + dexLen);

                byte[] classes = Arrays.copyOfRange(dexData, 4 + index, 4 + index + dexLen);
                if (index != 0) {
                    for (int i = 0; i < 112; i++) {
                        classes[i] ^= 0x66;
                    }
                }

                // 配置动态加载环境
                //反射获取主线程对象，并从中获取所有已加载的package信息，并中找到当前的LoadApk对象的弱引用

                //创建一个新的inMemoryDexClassLoader用于加载源Apk，
                //  父节点的inMemoryDexClassLoader使其遵循双亲委托模型

                //getClassLoader()等同于 (ClassLoader) RefInvoke.getFieldOjbect()
                //但是为了替换掉父节点我们需要通过反射来获取并修改其值
                Object currentActivityThread = currentActivityThread();
                ArrayMap mPackages = (ArrayMap) Reflect.getFieldValue(
                        "android.app.ActivityThread", currentActivityThread,
                        "mPackages");

                Log.e("filename","android.app.LoadedApk");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    InMemoryDexClassLoader inMemoryDexClassLoader = new InMemoryDexClassLoader(
                            ByteBuffer.wrap(classes),
                            application.getClassLoader());
                    Object pathListObj = Reflect.getFieldValue("dalvik.system.InMemoryDexClassLoader", inMemoryDexClassLoader, "pathList");
                    Object[] dexElement = (Object[]) Reflect.getFieldValue("dalvik.system.DexPathList", pathListObj, "dexElements");

                }

                index += 4 + dexLen;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static Object currentActivityThread() {
        try {
            Class<?> cls = Class.forName("android.app.ActivityThread");
            Method declaredMethod = cls.getDeclaredMethod("currentActivityThread",
                                                          new Class[0]);
            declaredMethod.setAccessible(true);
            return declaredMethod.invoke(null, new Object[0]);
        } catch (Exception e) {
        }
        return null;
    }

    private static int byte2Int(byte[] b, int index) {
        return ((b[index] & 0xff) << 24) + ((b[index + 1] & 0xff) << 16) + ((b[index + 2] & 0xff) << 8) +(b[index + 3] & 0xff);
    }
}
