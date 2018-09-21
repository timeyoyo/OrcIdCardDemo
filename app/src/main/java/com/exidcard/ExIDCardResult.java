/**
 ===============================================================================================
 * Project Name:
 * Class Description: ExIDCardResult
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

import java.io.UnsupportedEncodingException;


public final class ExIDCardResult {
	
	String imgtype;
	//recognition data
	int type; 		  // 1:正面；2:反面
	String cardnum;   // 身份证号码
	String name;	  // 姓名
	String sex;		  // 性别
	String address;   // 地址
	String nation;
	String office;    // 签发机关
	String validdate; // 有效限期
	int nColorType;   //1 color, 0 gray
	
	
	public ExIDCardResult(int type) {
		//type = 0;
		this.type = type;
		imgtype = "Preview";
	}
	
	public String getCardnum() {
		return cardnum;
	}

	public void setCardnum(String cardnum) {
		this.cardnum = cardnum;
	}

	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	/** decode from stream
	 *  return the len of decoded data int the buf */
	public int decode(byte []pwBuf, int tLen, int index){
		byte code;
		int i = 0;
		int j = index;
		String content = null;
		
		code = pwBuf[j++];
		while(j < tLen){
			i++;
			j++;
			if (pwBuf[j] == 0x20) break;
		}
				
		try {
			content = new String(pwBuf, index+1, i, "GBK");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		if (code == 0x21){
			cardnum = content;
		}else if (code == 0x22){
			name = content;
		}else if (code == 0x23){
			sex = content;
		}else if (code == 0x24){
			nation = content;
		}else if (code == 0x25){
			address = content;
		}else if (code == 0x26){
			office = content;
		}else if (code == 0x27){
			validdate = content;
		}
		
		return i+1+1;
	}
	
	public void SetViewType(String viewtype)
	{
		this.imgtype = viewtype;
	}
	
	public void SetColorType(int aColorType)
	{
		nColorType = aColorType;
	}

	/** @return raw text to show */
	public String getText() {
		String text = "VeiwType = " + imgtype;
		if(nColorType == 1){
			text += "  类型:  彩色";
		}else{
			text += "  类型:  扫描";
		}
		if(type == 1){
			text += "\nname:" + name;
			text += "\nnumber:" + cardnum;
			text += "\nsex:" + sex;
			text += "\nnation:" + nation;
			text += "\naddress:" + address;
			
		}else if (type == 2){
			text += "\noffice:" + office;
			text += "\nValDate:" + validdate;
		}
		return text;
	}
}
