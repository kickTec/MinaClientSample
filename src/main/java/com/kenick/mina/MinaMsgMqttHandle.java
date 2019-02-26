package com.kenick.mina;

import com.alibaba.fastjson.JSON;
import com.kenick.pojo.HandSensor;
import com.kenick.utils.MqttUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class MinaMsgMqttHandle implements MinaMsgHandler{
    private final static Logger logger = LoggerFactory.getLogger(MinaMsgMqttHandle.class);
    private String ipPort;
    private String unit; // 上,下，左，右,无，未知
    private String valueType; // 数值类型 手势值

    @Override
    public void handle(String msg) {
        try {
            // 解析手势上报数据 010300a6020003ffff 01 03 00 a6 02 00 03 ff ff
            msg = msg.replace(" ","").toLowerCase();
            logger.info("上报接收数据:{}", msg);
            String receiveNodeId = msg.substring(0,2).replace(" ","").toLowerCase();

            if(!"ff".equals(receiveNodeId)){
                String receiveNodeType = msg.substring(4,8).replace(" ","").toLowerCase();
                String dataLen = msg.substring(8,10);
                String legalData  = msg.substring(10, 10+Integer.parseInt(dataLen)*2);
                String handMean = "未知";
                if(legalData.length()>=4){
                    int handData = Integer.parseInt(legalData.substring(2,4),16);
                    switch (handData){
                        case 0:
                            handMean = "无";
                            break;
                        case 1:
                            handMean = "左";
                            break;
                        case 2:
                            handMean = "右";
                            break;
                        case 3:
                            handMean = "上";
                            break;
                        case 4:
                            handMean = "下";
                            break;
                    }
                }

                HandSensor handSensor = new HandSensor();
                handSensor.setArea(this.ipPort);
                handSensor.setNodeId(receiveNodeId);
                handSensor.setNodeType(receiveNodeType);
                handSensor.setUnit(this.unit);
                handSensor.setValueType(this.valueType);
                handSensor.setValue(handMean);
                String handMsg = JSON.toJSONString(handSensor);
                logger.info("上报mqtt发送数据:{}", handMsg);
                MqttUtil.sendMessage("tcp://127.0.0.1:3005","test1","fro/k12/home",handMsg.getBytes(StandardCharsets.UTF_8),2,false);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public String getIpPort() {
        return ipPort;
    }

    public void setIpPort(String ipPort) {
        this.ipPort = ipPort;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    @Override
    public String toString() {
        return "MinaMsgMqttHandle{" +
                "ipPort='" + ipPort + '\'' +
                ", unit='" + unit + '\'' +
                ", valueType='" + valueType + '\'' +
                '}';
    }
}