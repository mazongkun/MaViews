package com.mama.views.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import java.util.*

object SystemUtils {
    private const val TAG = "SystemUtils"

    /**
     * 获取当前手机系统语言。
     *
     * @return 返回当前系统语言。例如：当前设置的是“中文-中国”，则返回“zh-CN”
     */
    val systemLanguage: String
        get() = Locale.getDefault().language

    /**
     * 获取当前系统上的语言列表(Locale列表)
     *
     * @return  语言列表
     */
    val systemLanguageList: Array<Locale>
        get() = Locale.getAvailableLocales()

    /**
     * 获取当前手机系统版本号
     *
     * @return  系统版本号
     */
    val systemVersion: String
        get() = Build.VERSION.RELEASE

    /**
     * 获取手机型号
     *
     * @return  手机型号
     */
    val systemModel: String
        get() = Build.MODEL

    /**
     * 获取手机厂商
     *
     * @return  手机厂商
     */
    val deviceBrand: String
        get() = Build.BRAND

    /**
     * 获取手机IMEI(需要“android.permission.READ_PHONE_STATE”权限)
     *
     * @return  手机IMEI
     */
    @SuppressLint("MissingPermission")
    fun getIMEI(context: Context): String {
        val deviceId: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            Log.d(TAG, "getIMEI 0")
        } else {
            val mTelephony =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (mTelephony.deviceId != null) {
                deviceId = mTelephony.deviceId
                Log.d(TAG, "getIMEI 1")
            } else {
                deviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                Log.d(TAG, "getIMEI 2")
            }
        }
        return deviceId
    }

    fun showSystemParameter(context: Context) {
        val TAG = "系统参数："
        Log.e(TAG, "手机厂商：$deviceBrand")
        Log.e(TAG, "手机型号：$systemModel")
        Log.e(TAG, "手机当前系统语言：$systemLanguage")
        Log.e(TAG, "Android系统版本号：$systemVersion")
        Log.e(TAG, "手机IMEI：" + getIMEI(context))
    }
}