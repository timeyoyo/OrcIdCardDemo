/**
 ===============================================================================================
 * Project Name:
 * Class Description: DecodeHandler
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

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.exidcard.CaptureActivity;
import com.exidcard.ExIDCardReco;
import com.exidcard.mycard.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

final class DecodeHandler extends Handler {

private static final String TAG = DecodeHandler.class.getSimpleName();
private final CaptureActivity activity;
private int gcount;

  DecodeHandler(CaptureActivity activity) {
    this.activity = activity;
    gcount = 0;
  }
  
  
  @Override
  public void handleMessage(Message message) {
    switch (message.what) {
      case R.id.decode:
        //Log.d(TAG, "Got decode message");
        decode((byte[]) message.obj, message.arg1, message.arg2);
        break;
      case R.id.quit:
        Looper.myLooper().quit();
        break;
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
		// arg
		int ret = 0;
		ExIDCardReco excard = new ExIDCardReco();		
		//savetofile(data, width, height);
		
		////////////////////////////////////////////////////////////////////////////////////////////////
		ret = ExIDCardReco.nativeRecoRawdat(data, width, height, width, 1, excard.bResultBuf, excard.bResultBuf.length);
		excard.cardcode.SetColorType(CardColorJudge(data, width, height));
		
		if (ret > 0) {
			long end = System.currentTimeMillis();
			Log.d(TAG, "Found text (" + (end - start) + " ms):\n");

			excard.nResultLen = ret;
			excard.cardcode.SetViewType("Preview");
			excard.cardcode.SetColorType(CardColorJudge(data, width, height));
			excard.DecodeResult(excard.bResultBuf, excard.nResultLen);
			//if we have the text to show
			//if ( excard.ok && activity.CheckIsEqual(excard.cardcode) ) {
			if ( excard.ok ) {
				activity.SetRecoResult(excard.cardcode);
				Message message = Message.obtain(activity.getHandler(),	R.id.decode_succeeded, excard);
				message.sendToTarget();
				return;
			}
		}
		
		// retry to focus to the text
		Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
		message.sendToTarget();
	}
	
	private void savetofile(byte[] data, int width, int height)
	{
		gcount++;
		String tofile = "/mnt/sdcard/test_"+gcount+".raw";
		String ssize = "size=width="+width+"height="+height;
		byte bsize[] = new byte[ssize.length()];
		
		for (int i = 0; i < ssize.length(); ++i){
			bsize[i] = (byte)ssize.charAt(i);
		}
		
		try {
		File file = new File(tofile);
		OutputStream fs = new FileOutputStream(file);// to为要写入sdcard中的文件名称
		fs.write(data, 0, width*height);
		fs.write(bsize);
		fs.close();
		} catch (Exception e) {
			return;
		}
	}
	
	private int CardColorJudge(byte []data, int width, int height)
	{
		int offset = width*height;
		int i;
		int iTht = 144;
		int iCnt = 255;
		int nNum = 0;
		int iSize = width*height/2;
		
		for(i = 0; i < iSize; ++i ){
			int val = data[i+offset]&0xFF;
			if( val > iTht){ ++nNum; }
		}
		
		if(nNum > iCnt) return 1;
		else return 0;
	}
	// save to jpeg
	private void savetoJPEG(byte[] data, int width, int height) {
		int w, h;
		gcount++;
		SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String date = sDateFormat.format(new java.util.Date());
		
		String tofile = Environment.getExternalStorageDirectory()+File.separator+Environment.DIRECTORY_DCIM+File.separator+date+"_"+gcount+".jpg";
		//String tofile = Environment.()+File.separator+Environment.DIRECTORY_DCIM+File.separator+date+"_"+gcount+".jpg";
		//String tofile = "/sdcard/DCIM/"+"NV21_"+ date+"_"+gcount+".jpg";

		int imageFormat = ImageFormat.NV21;
		Rect frame = new Rect(0, 0, width-1, height-1);
		if (imageFormat == ImageFormat.NV21) {
			YuvImage img = new YuvImage(data, ImageFormat.NV21, width, height, null);
			OutputStream outStream = null;
			File file = new File(tofile);
			try {
				outStream = new FileOutputStream(file);
				img.compressToJpeg(frame, 100, outStream);
				outStream.flush();
				outStream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
