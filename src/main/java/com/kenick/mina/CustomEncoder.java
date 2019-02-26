package com.kenick.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.textline.LineDelimiter;

import java.nio.charset.Charset;

public class CustomEncoder implements ProtocolEncoder {
	//设置字符编码
	Charset charset = Charset.forName("UTF-8");
	
	public void dispose(IoSession arg0){
	}

	/**
	 * 编码操作
	 */
	public void encode(IoSession session, Object message, ProtocolEncoderOutput output)
			throws Exception {
		//初始化buffer容量，设置缓冲区为100可扩容
		IoBuffer buffer = IoBuffer.allocate(100).setAutoExpand(true);
		//编码工作
		buffer.putString(message.toString(), charset.newEncoder());
		buffer.putString(LineDelimiter.DEFAULT.getValue(), charset.newEncoder());
		//为下一次读写做准备
		buffer.flip();
		//写出
		output.write(buffer);
	}
}