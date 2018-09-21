/**
 ===============================================================================================
 * Project Name:
 * Class Description: DecodeThread
 * Created by timeyoyo 
 * Created at 2018/9/21 10:53
 * -----------------------------------------------------------------------------------------------
 * ★ Some Tips For You ★
 * 1.
 * 2.
 ===============================================================================================
 * HISTORY
 *
 * Tag                      Date       Author           Description
 * ======================== ========== ===============  ========================================
 * MK                       2018/9/21   timeyoyo         Create new file
 ===============================================================================================
 */
package com.exidcard.decoding;

import com.exidcard.CaptureActivity;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;


final class DecodeThread extends Thread {

  //public static final String BARCODE_BITMAP = "barcode_bitmap";

  private final CaptureActivity activity;
  private Handler handler;
  private final CountDownLatch handlerInitLatch;

  DecodeThread(CaptureActivity activity) {

    this.activity = activity;
    handlerInitLatch = new CountDownLatch(1);
  }

  Handler getHandler() {
    try {
      handlerInitLatch.await();
    } catch (InterruptedException ie) {
      // continue?
    }
    return handler;
  }

  @Override
  public void run() {
    Looper.prepare();
    handler = new DecodeHandler(activity);
    handlerInitLatch.countDown();
    Looper.loop();
  }

}
