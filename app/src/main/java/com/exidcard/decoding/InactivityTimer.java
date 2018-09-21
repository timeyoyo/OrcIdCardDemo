/**
 ===============================================================================================
 * Project Name:
 * Class Description: InactivityTimer
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import android.app.Activity;

/**
 * Finishes an activity after a period of inactivity.
 */
public final class InactivityTimer {

  private static final int INACTIVITY_DELAY_SECONDS = 5 * 60;

  private final ScheduledExecutorService inactivityTimer =
      Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
  private final Activity activity;
  private ScheduledFuture<?> inactivityFuture = null;

  public InactivityTimer(Activity activity) {
    this.activity = activity;
    onActivity();
  }
  

  public void onActivity() {
    cancel();
    inactivityFuture = inactivityTimer.schedule(new FinishListener(activity),
                                                INACTIVITY_DELAY_SECONDS,
                                                TimeUnit.SECONDS);
  }

  private void cancel() {
    if (inactivityFuture != null) {
      inactivityFuture.cancel(true);
      inactivityFuture = null;
    }
  }

  public void shutdown() {
    cancel();
    inactivityTimer.shutdown();
  }

  private static final class DaemonThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);
      return thread;
    }
  }

}
