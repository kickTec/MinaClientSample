package com.kenick.utils;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttUtil {
    private final static Logger logger = LoggerFactory.getLogger(MqttUtil.class);

    public static void sendMessage(String url,String mqttClientId,String topic,byte[] msgBytes,int qos,boolean retain){
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(10);
            connOpts.setKeepAliveInterval(20);
            MqttClient mqttClient = new MqttClient(url, mqttClientId, persistence);
            mqttClient.connect(connOpts);

            MqttMessage message = new MqttMessage(msgBytes);
            message.setQos(qos);
            message.setRetained(retain);
            mqttClient.publish(topic, message);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }
}
