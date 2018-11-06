/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.decoding;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.utils.AutoFocusUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.R;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.activity.CaptureActivity;
import com.google.zxing.camera.CameraManager;
import com.google.zxing.camera.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.Hashtable;
import java.util.Map;

/**
 * 只识别二维码，不用使用MultiFormatReader，直接用QRCodeReader，据说可以提高识别率
 *  GlobalHistogramBinarizer 对手机CPU和内存要求不高。但它选择了全部的黑点来计算，因此无法处理阴影和渐变这两种情况
 *  HybridBinarizer 专门以白色为背景的连续黑块二维码图像解析而设计，也更适合来解析更具有严重阴影和渐变的二维码图像。
 *  识别率 HybridBinarizer > GlobalHistogramBinarizer 执行率 GlobalHistogramBinarizer > HybridBinarizer
 */
final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final CaptureActivity activity;
  //private final MultiFormatReader multiFormatReader;

  private final QRCodeReader mQrCodeReader;
  private final Map<DecodeHintType, Object> mHints;

  DecodeHandler(CaptureActivity activity, Hashtable<DecodeHintType, Object> hints) {
   // multiFormatReader = new MultiFormatReader();
  //  multiFormatReader.setHints(hints);
    this.activity = activity;
    mQrCodeReader = new QRCodeReader();
    mHints = new Hashtable<>();
    mHints.put(DecodeHintType.CHARACTER_SET, "utf-8");
    mHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    mHints.put(DecodeHintType.POSSIBLE_FORMATS, BarcodeFormat.QR_CODE);
  }

  @Override
  public void handleMessage(Message message) {
    if (message.what == R.id.decode) {//Log.d(TAG, "Got decode message");
      decode((byte[]) message.obj, message.arg1, message.arg2);
    } else if (message.what == R.id.quit) {
      Looper.myLooper().quit();

    }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    long start = System.currentTimeMillis();
    Result rawResult = null;
    
    //modify here
    byte[] rotatedData = new byte[data.length];
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++)
            rotatedData[x * height + height - y - 1] = data[x + y * width];
    }
    int tmp = width; // Here we are swapping, that's the difference to #11
    width = height;
    height = tmp;
    
    PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(rotatedData, width, height);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    AutoFocusUtils.autoFocus(bitmap,mHints);
    try {
     // rawResult = multiFormatReader.decodeWithState(bitmap);
      rawResult = mQrCodeReader.decode(bitmap,mHints);
    } catch (ReaderException re) {
      // continue
    } finally {
      //multiFormatReader.reset();
      mQrCodeReader.reset();
    }

    if (rawResult != null) {
      long end = System.currentTimeMillis();
      Log.d(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());
      Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, rawResult);
      Bundle bundle = new Bundle();
      bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
      message.setData(bundle);
      //Log.d(TAG, "Sending decode succeeded message...");
      message.sendToTarget();
    } else {
      Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
      message.sendToTarget();
    }
  }

}
