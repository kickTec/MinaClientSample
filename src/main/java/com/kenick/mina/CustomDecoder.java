package com.kenick.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import java.nio.charset.Charset;

public class CustomDecoder implements ProtocolDecoder {
	Charset charset = Charset.forName("UTF-8");
	IoBuffer buffer = IoBuffer.allocate(100).setAutoExpand(true);

	/**
	 * 解码操作
	 */
	public void decode(IoSession session, IoBuffer bufIn, ProtocolDecoderOutput output) {
		while(bufIn.hasRemaining()){
			byte b = bufIn.get();
			buffer.put(b);
		}
		buffer.flip();
		byte[] bytes = new byte[buffer.limit()];
		buffer.get(bytes);
		output.write(bytes);
	}

	public void dispose(IoSession arg0){

	}

	public void finishDecode(IoSession arg0, ProtocolDecoderOutput arg1){

	}
}