package com.google;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.zxing.activity.CaptureActivity;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.subjects.PublishSubject;
/**
 * author: Goach.zhong
 * Date: 2018/11/5 14:14.
 * Des: 统一处理QRCode扫码后结果,在QRCodeHelper里面使用
 */
public class QRCodeFragment extends Fragment {
    private final String TAG = QRCodeFragment.class.getSimpleName();
    private static final int REQUEST_QRCODE = 0x01;
    private Map<String, PublishSubject<String>> mSubjects = new HashMap<>();
    private String mCurrentClassName;

    public QRCodeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }


    public PublishSubject<String> getSubjectByClass(@NonNull String className) {
        return mSubjects.get(className);
    }

    public boolean containsByClassName(@NonNull String className) {
        return mSubjects.containsKey(className);
    }
    public void setCurrentClassName(String className){
        mCurrentClassName = className;
    }
    public int getQRRequestCode(){
        return REQUEST_QRCODE;
    }
    public void setSubjectForPermission(@NonNull String className, @NonNull PublishSubject<String> subject) {
        mSubjects.put(className, subject);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_QRCODE && data != null) {
            Bundle bundle = data.getExtras();
            if(bundle==null){
                return;
            }
            String scanResult = bundle.getString(CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN);
            PublishSubject<String> subject = mSubjects.get(mCurrentClassName);
            if (subject == null) {
                // No subject found
                Log.e(TAG, "没有当前className对应的PublishSubject");
                return;
            }
            mSubjects.remove(mCurrentClassName);
            subject.onNext(scanResult);
            subject.onComplete();
        }
    }
}
