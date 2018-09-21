/**
 ===============================================================================================
 * Project Name:
 * Class Description: ExIDCardReco
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
package com.exidcard;

import android.graphics.Bitmap;
import android.util.Log;


public final class ExIDCardReco {
	private static final String tag = "ExIDCardDecoder";
	
	public static final int mMaxStreamBuf = 1024;
	
	private int typeId = 1; // 身份证正反面，1为正面
	// NDK STUFF
	static {
		System.loadLibrary("exidcard");
	}
	
	//Results
	public byte []bResultBuf;
	public int    nResultLen;
	public boolean ok;
	public ExIDCardResult cardcode;
	
	public ExIDCardReco(){
		bResultBuf = new byte[mMaxStreamBuf];
		nResultLen = 0;
		ok = false;
		cardcode = new ExIDCardResult(typeId);
	}
	
	/*** @return raw text encoded by the decoder */
	public String getText() {
		//String text = "EXIDCardAPP by www.exocr.com: \n";
		//String text = "EXIDCardAPP\n";
		String text = "";
		text += cardcode.getText();
		return text;
	}
	
	//decode result stream
	public int DecodeResult(byte []bResultBuf, int nResultLen) {
		int i, len;	
		
		if(nResultLen < 1) return 0;
		
		i = 0;
		cardcode.type = 0;
		cardcode.type = bResultBuf[i++];
		while(i < nResultLen){
			len = cardcode.decode(bResultBuf, nResultLen, i);
			i += len;
		}
		
		if (cardcode.type == 1 && (cardcode.cardnum == null || cardcode.name == null || cardcode.nation == null || cardcode.sex == null || cardcode.address == null) ||
			cardcode.type == 2 && (cardcode.office == null || cardcode.validdate == null ) ||
			cardcode.type == 0 ){
			ok = false;
		}else{
			if (cardcode.type == 1 && (cardcode.cardnum.length() != 18 || cardcode.name.length() < 2 || cardcode.address.length() < 10)) {
				ok = false;
			} else {
				ok = true;
			}
		}
		
		return 1;
	}
	//
	public int PrintResult(){
		if (nResultLen > 0){
			int i;
			int []vals = new int[nResultLen];
			for (i = 0; i < nResultLen; ++i){
				vals[i] = (int)bResultBuf[i];
			}
			return 1;
		}
		return 0;
	}
	
	//natives/////////////////////////////////////////////////////
	public static native int nativeInit(byte []dbpath);
	public static native int nativeDone();
	public static native int nativeRecoRawdat(byte []imgdata, int width, int height, int pitch, int imgfmt, byte []bresult, int maxsize);
	public static native int nativeRecoBitmap(Bitmap bitmap, byte[]bresult, int maxsize);
}