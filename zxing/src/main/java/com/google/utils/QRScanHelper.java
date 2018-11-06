package com.google.utils;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.google.QRCodeFragment;
import com.google.zxing.activity.CaptureActivity;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * author: Goach.zhong
 * Date: 2018/8/8 09:10.
 * Des: 打开二维码扫描
 */
public class QRScanHelper {

    private static final String TAG = QRScanHelper.class.getSimpleName();
    private Lazy<QRCodeFragment> mQRCodeFragment;

    public QRScanHelper(@NonNull final FragmentActivity activity) {
        mQRCodeFragment = getLazySingleton(activity.getSupportFragmentManager());
    }

    public QRScanHelper(@NonNull final Fragment fragment) {
        mQRCodeFragment = getLazySingleton(fragment.getChildFragmentManager());
    }

    @NonNull
    private Lazy<QRCodeFragment> getLazySingleton(@NonNull final FragmentManager fragmentManager) {

        return new Lazy<QRCodeFragment>() {

            private QRCodeFragment qrCodeFragment;

            @Override
            public synchronized QRCodeFragment get() {
                if (qrCodeFragment == null) {
                    qrCodeFragment = getQRCodeFragment(fragmentManager);
                }
                return qrCodeFragment;
            }

        };
    }

    private QRCodeFragment getQRCodeFragment(@NonNull final FragmentManager fragmentManager) {
        QRCodeFragment qrCodeFragment = findQRCodeFragment(fragmentManager);
        boolean isNewInstance = qrCodeFragment == null;
        if (isNewInstance) {
            qrCodeFragment = new QRCodeFragment();
            fragmentManager
                    .beginTransaction()
                    .add(qrCodeFragment, TAG)
                    .commitNow();
        }
        return qrCodeFragment;
    }

    private QRCodeFragment findQRCodeFragment(@NonNull final FragmentManager fragmentManager) {
        return (QRCodeFragment) fragmentManager.findFragmentByTag(TAG);
    }

    public Observable<String> scanQRCode(String className){
        try {
            QRCodeFragment qrCodeFragment = mQRCodeFragment.get();
            Intent mSourceIntent = new Intent(qrCodeFragment.getContext(), CaptureActivity.class);
            qrCodeFragment.startActivityForResult(mSourceIntent,
                    qrCodeFragment.getQRRequestCode());
            qrCodeFragment.setCurrentClassName(className);
        } catch (Exception ignore) {
        }
        return requestImplementation(className);
    }

    private Observable<String> requestImplementation(String className) {
        PublishSubject<String> subject = mQRCodeFragment.get().getSubjectByClass(className);
        // Create a new subject if not exists
        if (subject == null) {
            subject = PublishSubject.create();
            mQRCodeFragment.get().setSubjectForPermission(className, subject);
        }
        return subject;
    }
    @FunctionalInterface
    public interface Lazy<V> {
        V get();
    }

}
