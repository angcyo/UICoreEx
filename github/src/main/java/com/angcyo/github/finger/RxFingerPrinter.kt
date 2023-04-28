package com.angcyo.github.finger

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.angcyo.github.finger.lifecycle.LifecycleListener
import com.angcyo.github.finger.lifecycle.SupportFingerPrinterManagerFragment
import com.angcyo.library.L
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.subjects.PublishSubject

/**
 * Created by Administrator on 2016/12/31.
 *
 *
 * 2022-1-15 指纹识别
 * 1.2.1
 * https://github.com/Zweihui/RxFingerPrinter
 */
class RxFingerPrinter(private val fragmentActivity: FragmentActivity) : LifecycleListener {

    companion object {
        const val TAG = "RxFingerPrinter"
    }

    var publishSubject: PublishSubject<IdentificationInfo>? = null

    var supportFingerPrinterManagerFragment: SupportFingerPrinterManagerFragment?

    @SuppressLint("NewApi")
    var mCancellationSignal: CancellationSignal? = null

    @SuppressLint("NewApi")
    var authenticationCallback: FingerprintManager.AuthenticationCallback? = null
    private var manager: FingerprintManager? = null
    private var mKeyManager: KeyguardManager? = null
    private var mLogging = false
    private var mSelfCompleted = false
    private var mDisposables: CompositeDisposable? = null

    init {
        supportFingerPrinterManagerFragment = getRxPermissionsFragment(fragmentActivity)
    }

    /**[androidx.fragment.app.FragmentManager.executePendingTransactions]*/
    private fun getRxPermissionsFragment(activity: FragmentActivity): SupportFingerPrinterManagerFragment? {
        var fragment = findRxPermissionsFragment(activity)
        val isNewInstance = fragment == null
        if (isNewInstance) {
            fragment = SupportFingerPrinterManagerFragment()
            val fragmentManager = activity.supportFragmentManager
            fragmentManager
                .beginTransaction()
                .add(fragment, TAG)
                .commitAllowingStateLoss()
            fragmentManager.executePendingTransactions()
            fragment.fragmentLifecycle.addListener(this)
        }
        return fragment
    }

    private fun findRxPermissionsFragment(activity: Activity): SupportFingerPrinterManagerFragment? {
        return activity.fragmentManager.findFragmentByTag(TAG) as? SupportFingerPrinterManagerFragment
    }

    fun begin(): RxFingerPrinter {
        dispose()
        publishSubject = PublishSubject.create()
        return this
    }

    fun subscribe(observer: DisposableObserver<IdentificationInfo>?) {
        if (observer == null) {
            throw RuntimeException("Observer can not be null!")
        }
        publishSubject?.subscribe(observer)
        addDispose(observer)
        if (Build.VERSION.SDK_INT < 23) {
            publishSubject?.onNext(IdentificationInfo(CodeException.SYSTEM_API_ERROR))
        } else {
            initManager()
            if (confirmFinger()) {
                startListening(null)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun startListening(cryptoObject: FingerprintManager.CryptoObject?) {
        if (ActivityCompat.checkSelfPermission(
                fragmentActivity,
                Manifest.permission.USE_FINGERPRINT
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            publishSubject?.onNext(IdentificationInfo(CodeException.PERMISSION_DENIED_ERROE))
        }
        mCancellationSignal = CancellationSignal()
        if (manager != null && authenticationCallback != null) {
            mSelfCompleted = false
            manager!!.authenticate(
                cryptoObject,
                mCancellationSignal,
                0,
                authenticationCallback!!,
                null
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun initManager() {
        manager = fragmentActivity.getSystemService(FingerprintManager::class.java)
        mKeyManager = fragmentActivity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        authenticationCallback = object : FingerprintManager.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                //多次指纹密码验证错误后，进入此方法；并且，不能短时间内调用指纹验证
                if (mCancellationSignal != null) {
                    publishSubject?.onNext(IdentificationInfo(CodeException.FINGERPRINTERS_FAILED_ERROR))
                    mCancellationSignal!!.cancel()
                    mSelfCompleted = true
                }
            }

            override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                L.i("helpCode:$helpCode, helpString:$helpString")
            }

            override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
                publishSubject?.onNext(IdentificationInfo(true))
                mSelfCompleted = true
            }

            override fun onAuthenticationFailed() {
                publishSubject?.onNext(IdentificationInfo(CodeException.FINGERPRINTERS_RECOGNIZE_FAILED))
            }
        }
    }

    @SuppressLint("NewApi")
    @TargetApi(23)
    fun confirmFinger(): Boolean {
        var isDeviceSupport = true

        //android studio 上，没有这个会报错
        if (ActivityCompat.checkSelfPermission(
                fragmentActivity,
                Manifest.permission.USE_FINGERPRINT
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            publishSubject?.onNext(IdentificationInfo(CodeException.PERMISSION_DENIED_ERROE))
            isDeviceSupport = false
        }
        //判断硬件是否支持指纹识别
        if (!manager!!.isHardwareDetected) {
            publishSubject?.onNext(IdentificationInfo(CodeException.HARDWARE_MISSIING_ERROR))
            isDeviceSupport = false
        }
        //判断 是否开启锁屏密码
        if (!mKeyManager!!.isKeyguardSecure) {
            publishSubject?.onNext(IdentificationInfo(CodeException.KEYGUARDSECURE_MISSIING_ERROR))
            isDeviceSupport = false
        }
        //判断是否有指纹录入
        if (!manager!!.hasEnrolledFingerprints()) {
            publishSubject?.onNext(IdentificationInfo(CodeException.NO_FINGERPRINTERS_ENROOLED_ERROR))
            isDeviceSupport = false
        }
        return isDeviceSupport
    }

    fun addDispose(disposable: Disposable?) {
        if (mDisposables == null || mDisposables?.isDisposed == true) {
            mDisposables = CompositeDisposable()
        }
        mDisposables?.add(disposable!!)
    }

    fun dispose() {
        if (mDisposables != null && mDisposables?.isDisposed == false) {
            mDisposables?.dispose()
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    fun stopListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal!!.cancel()
            mCancellationSignal = null
        }
    }

    override fun onStart() {
        log("LifeCycle--------onStart")
    }

    override fun onStop() {
        log("LifeCycle--------onStop")
    }

    override fun onResume() {
        if (!mSelfCompleted) {
            startListening(null)
        }
        log("LifeCycle--------onResume")
    }

    override fun onPause() {
        stopListening()
        log("LifeCycle--------onPause")
    }

    override fun onDestroy() {
        dispose()
        log("LifeCycle--------onDestroy")
    }

    fun setLogging(logging: Boolean) {
        mLogging = logging
    }

    fun log(message: String?) {
        if (mLogging) {
            Log.d(TAG, message!!)
        }
    }
}