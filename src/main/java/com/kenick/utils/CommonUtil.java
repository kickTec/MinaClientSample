package com.kenick.utils;

import java.util.ArrayList;
import java.util.List;

public abstract class CommonUtil {
    public static void main(String[] args) {
		String crc16Str = getModBusCRC("AABB06000000FF1101");
		System.out.println(crc16Str);
	}

    /**
     *  将十进制数字转换为16进制字符串，中间填充split分隔符
     * @param dec
     * @param split
     * @return
     */
	public static String dec2Hex(int dec,String split){
		String cmdTmp = "";
		String hexStr = Integer.toHexString(dec);
		if(hexStr.length()<4){
			int len = 4 - hexStr.length();
			for(int i=0;i<len;i++){
				cmdTmp += "0";
			}
			cmdTmp += hexStr;
		}
		return cmdTmp.substring(2) + split + cmdTmp.substring(0,2);
	}

    /**
     * 将16进制字符串转换为byte[]
     * @param hexStr 16进制字符串
     * @return 字节数组
     */
    public static byte[] hex2Bytes(String hexStr) {
		hexStr = hexStr.replaceAll("\\s",""); // 去掉空格
        if(hexStr == null || hexStr.trim().equals("")) {
            return new byte[0];
        }

        byte[] bytes = new byte[hexStr.length() / 2];
        for(int i = 0; i < hexStr.length() / 2; i++) {
            String subStr = hexStr.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }

        return bytes;
    }

    /**
	* 字节数组转换为16进制字符串
	* @param byteArray 返回byte数组
	* @return 16进制字符串
	*/
	public static String bytesToHexString(byte[] byteArray){
	   StringBuilder stringBuilder = new StringBuilder();
	   if (byteArray == null || byteArray.length <= 0) {   
	       return null;   
	   }
		for (byte aByteArray : byteArray) {
			int v = aByteArray & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
			stringBuilder.append(" ");
		}
	   return stringBuilder.toString();   
	}

	/**
	 * 对外提供的获取CRC16方法
	 * @param hexStr 16进制字符串
	 * @return 校验码
	 */
	public static String getModBusCRC(String hexStr) {
        byte[] bytes = hex2Bytes(hexStr);
        return getCRC3(bytes);
    }

	/**
	 * 分体式特高频读卡器返回信息crc校验
	 * @param buff 16进账字符串，不同字节之间用空格分隔
	 * @param len 待校验字节长度，基本上等于空格数+1
	 * @return 0:校验通过，非0校验不通过
	 */
	public static int checkCRC16(String buff,int len){
		int uiCrcValue = 0xFFFF;
		try {
			String[] hexArray = buff.split(" ");
			if(len==-1){
				len = hexArray.length;
			}
			for (int i = 0; i < len; i++) {
				uiCrcValue = uiCrcValue ^ Integer.parseInt(hexArray[i], 16);
				for (int j = 0; j < 8; j++) {
					if ((uiCrcValue & 0x0001) == 1) {
						uiCrcValue = (uiCrcValue >> 1) ^ 0x8408;
					} else {
						uiCrcValue = uiCrcValue >> 1;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return uiCrcValue;
	}

	/**
	 * 将接收到的16进制字符串转换为rfid集合
	 * @param hexString 16进制字符串
	 * @return rfid集合
	 */
	public static List<String> getRFID(String hexString){
		 //主动模式返回字符串 Len Adr reCmd Status Data[] CRC-16
		 List<String> rfidList = new ArrayList<>();
		 while(hexString.contains("ee 00")){
			 // 特定标签位置
			 int index = hexString.indexOf("ee 00");
			 try {	 
				 // 主动模式返回信息
				 int retLen = Integer.parseInt(hexString.substring(index-6, index-4),16); 
				 String singleRet = hexString.substring(index-6, index-6+(retLen+1)*3-1);
				 
				 if(checkCRC16(singleRet,retLen+1)==0){
					 // CRC检验通过，获取返回信息中rfid标签长度
					 String rfidLenStr = singleRet.substring(15*3,15*3+2);
					 int rfidLen = Integer.parseInt(rfidLenStr, 16);
					 String rfid = singleRet.substring(16*3, 16*3+rfidLen*3).replace(" ", "");
					 if(!rfidList.contains(rfid)){
						 rfidList.add(rfid);
					 }
					 
					 // 去掉已获取过rfid的字符串信息
					 hexString = hexString.substring(index+(retLen-1)*3-1);
				 }else{
					 // CRC校验未通过，跳过ee 00标记信息
					 hexString = hexString.substring(index+6);
				 }
			} catch (Exception e) {
				e.printStackTrace();
				// 发生异常，跳过该次标记
				hexString = hexString.substring(index+6);
			}
		 }
	 	 return rfidList;
	 }

	/**
	 * 查表法计算CRC16校验
	 *
	 * @param data 需要计算的字节数组
	 */
	public static String getCRC3(byte[] data) {
		byte[] crc16_h = {
				(byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40,
				(byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41,
				(byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41,
				(byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40,
				(byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41,
				(byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40,
				(byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40,
				(byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41,
				(byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41,
				(byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40,
				(byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40,
				(byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41,
				(byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40,
				(byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41,
				(byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41,
				(byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x01, (byte) 0xC0, (byte) 0x80, (byte) 0x41, (byte) 0x00, (byte) 0xC1, (byte) 0x81, (byte) 0x40
		};

		byte[] crc16_l = {
				(byte) 0x00, (byte) 0xC0, (byte) 0xC1, (byte) 0x01, (byte) 0xC3, (byte) 0x03, (byte) 0x02, (byte) 0xC2, (byte) 0xC6, (byte) 0x06, (byte) 0x07, (byte) 0xC7, (byte) 0x05, (byte) 0xC5, (byte) 0xC4, (byte) 0x04,
				(byte) 0xCC, (byte) 0x0C, (byte) 0x0D, (byte) 0xCD, (byte) 0x0F, (byte) 0xCF, (byte) 0xCE, (byte) 0x0E, (byte) 0x0A, (byte) 0xCA, (byte) 0xCB, (byte) 0x0B, (byte) 0xC9, (byte) 0x09, (byte) 0x08, (byte) 0xC8,
				(byte) 0xD8, (byte) 0x18, (byte) 0x19, (byte) 0xD9, (byte) 0x1B, (byte) 0xDB, (byte) 0xDA, (byte) 0x1A, (byte) 0x1E, (byte) 0xDE, (byte) 0xDF, (byte) 0x1F, (byte) 0xDD, (byte) 0x1D, (byte) 0x1C, (byte) 0xDC,
				(byte) 0x14, (byte) 0xD4, (byte) 0xD5, (byte) 0x15, (byte) 0xD7, (byte) 0x17, (byte) 0x16, (byte) 0xD6, (byte) 0xD2, (byte) 0x12, (byte) 0x13, (byte) 0xD3, (byte) 0x11, (byte) 0xD1, (byte) 0xD0, (byte) 0x10,
				(byte) 0xF0, (byte) 0x30, (byte) 0x31, (byte) 0xF1, (byte) 0x33, (byte) 0xF3, (byte) 0xF2, (byte) 0x32, (byte) 0x36, (byte) 0xF6, (byte) 0xF7, (byte) 0x37, (byte) 0xF5, (byte) 0x35, (byte) 0x34, (byte) 0xF4,
				(byte) 0x3C, (byte) 0xFC, (byte) 0xFD, (byte) 0x3D, (byte) 0xFF, (byte) 0x3F, (byte) 0x3E, (byte) 0xFE, (byte) 0xFA, (byte) 0x3A, (byte) 0x3B, (byte) 0xFB, (byte) 0x39, (byte) 0xF9, (byte) 0xF8, (byte) 0x38,
				(byte) 0x28, (byte) 0xE8, (byte) 0xE9, (byte) 0x29, (byte) 0xEB, (byte) 0x2B, (byte) 0x2A, (byte) 0xEA, (byte) 0xEE, (byte) 0x2E, (byte) 0x2F, (byte) 0xEF, (byte) 0x2D, (byte) 0xED, (byte) 0xEC, (byte) 0x2C,
				(byte) 0xE4, (byte) 0x24, (byte) 0x25, (byte) 0xE5, (byte) 0x27, (byte) 0xE7, (byte) 0xE6, (byte) 0x26, (byte) 0x22, (byte) 0xE2, (byte) 0xE3, (byte) 0x23, (byte) 0xE1, (byte) 0x21, (byte) 0x20, (byte) 0xE0,
				(byte) 0xA0, (byte) 0x60, (byte) 0x61, (byte) 0xA1, (byte) 0x63, (byte) 0xA3, (byte) 0xA2, (byte) 0x62, (byte) 0x66, (byte) 0xA6, (byte) 0xA7, (byte) 0x67, (byte) 0xA5, (byte) 0x65, (byte) 0x64, (byte) 0xA4,
				(byte) 0x6C, (byte) 0xAC, (byte) 0xAD, (byte) 0x6D, (byte) 0xAF, (byte) 0x6F, (byte) 0x6E, (byte) 0xAE, (byte) 0xAA, (byte) 0x6A, (byte) 0x6B, (byte) 0xAB, (byte) 0x69, (byte) 0xA9, (byte) 0xA8, (byte) 0x68,
				(byte) 0x78, (byte) 0xB8, (byte) 0xB9, (byte) 0x79, (byte) 0xBB, (byte) 0x7B, (byte) 0x7A, (byte) 0xBA, (byte) 0xBE, (byte) 0x7E, (byte) 0x7F, (byte) 0xBF, (byte) 0x7D, (byte) 0xBD, (byte) 0xBC, (byte) 0x7C,
				(byte) 0xB4, (byte) 0x74, (byte) 0x75, (byte) 0xB5, (byte) 0x77, (byte) 0xB7, (byte) 0xB6, (byte) 0x76, (byte) 0x72, (byte) 0xB2, (byte) 0xB3, (byte) 0x73, (byte) 0xB1, (byte) 0x71, (byte) 0x70, (byte) 0xB0,
				(byte) 0x50, (byte) 0x90, (byte) 0x91, (byte) 0x51, (byte) 0x93, (byte) 0x53, (byte) 0x52, (byte) 0x92, (byte) 0x96, (byte) 0x56, (byte) 0x57, (byte) 0x97, (byte) 0x55, (byte) 0x95, (byte) 0x94, (byte) 0x54,
				(byte) 0x9C, (byte) 0x5C, (byte) 0x5D, (byte) 0x9D, (byte) 0x5F, (byte) 0x9F, (byte) 0x9E, (byte) 0x5E, (byte) 0x5A, (byte) 0x9A, (byte) 0x9B, (byte) 0x5B, (byte) 0x99, (byte) 0x59, (byte) 0x58, (byte) 0x98,
				(byte) 0x88, (byte) 0x48, (byte) 0x49, (byte) 0x89, (byte) 0x4B, (byte) 0x8B, (byte) 0x8A, (byte) 0x4A, (byte) 0x4E, (byte) 0x8E, (byte) 0x8F, (byte) 0x4F, (byte) 0x8D, (byte) 0x4D, (byte) 0x4C, (byte) 0x8C,
				(byte) 0x44, (byte) 0x84, (byte) 0x85, (byte) 0x45, (byte) 0x87, (byte) 0x47, (byte) 0x46, (byte) 0x86, (byte) 0x82, (byte) 0x42, (byte) 0x43, (byte) 0x83, (byte) 0x41, (byte) 0x81, (byte) 0x80, (byte) 0x40
		};

		int crc = 0x0000ffff;
		int ucCRCHi = 0x00ff;
		int ucCRCLo = 0x00ff;
		int iIndex;
		for (int i = 0; i < data.length; ++i) {
			iIndex = (ucCRCLo ^ data[i]) & 0x00ff;
			ucCRCLo = ucCRCHi ^ crc16_h[iIndex];
			ucCRCHi = crc16_l[iIndex];
		}

		crc = ((ucCRCHi & 0x00ff) << 8) | (ucCRCLo & 0x00ff) & 0xffff;
		//高低位互换，输出符合相关工具对Modbus CRC16的运算
		crc = ( (crc & 0xFF00) >> 8) | ( (crc & 0x00FF ) << 8);
		return String.format("%04X", crc).toLowerCase();
	}
}