package com.kenick.mina;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class CustomCodecFactory implements ProtocolCodecFactory{

	public ProtocolDecoder getDecoder(IoSession arg0){
		return new CustomDecoder();
	}

	public ProtocolEncoder getEncoder(IoSession arg0){
		return new CustomEncoder();
	}
}