package com.kenick.mina;

import com.kenick.utils.CommonUtil;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class ConnectHandler implements IoHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectHandler.class);

    private IoConnector ioConnector; // mina IoConnector
    private int activeNum = 20; // 默认保活次数
    private Integer listId; // connectFutureList所处位置
    public StringBuilder msg = new StringBuilder(); // 接收到的消息
    private MinaMsgHandler minaMsgHandler; // 自定义消息处理handle
    private long sendStartTime; // 指令发送开始时间
    private int timeout; // 指令接收等待超时时间
    private String uploadNodeType; // 上报节点类型
    private boolean uploading; // 上报开启标记

    @Override
    public void sessionCreated(IoSession ioSession)  {

    }

    @Override
    public void sessionOpened(IoSession ioSession)  {

    }

    @Override
    public void sessionClosed(IoSession ioSession) {
        SocketAddress remoteAddress = ioSession.getRemoteAddress();
        String remoteIpPort = remoteAddress.toString().substring(1);
        String localAddress = ioSession.getLocalAddress().toString();

        if(activeNum != 0){
            // 自身保活时间不为0 重新连接并添加连接对象
            if(ioConnector != null){
                logger.debug("{} {} socket ioSession 非正常关闭，开始重连!", remoteIpPort, localAddress);
                ConnectFuture connectFuture = ioConnector.connect();
                connectFuture.awaitUninterruptibly();
                if(connectFuture.isConnected()){
                    logger.debug("{} {} socket ioSession 重连成功!",remoteIpPort, localAddress);
                    this.listId = MinaFactory.add(remoteIpPort,connectFuture);
                    String uploadKey = remoteIpPort + ":" + this.uploadNodeType;
                    if(MinaFactory.uploadMinaListIds.containsKey(uploadKey)){
                        MinaFactory.uploadMinaListIds.remove(uploadKey);
                        MinaFactory.uploadMinaListIds.put(uploadKey,listId);
                    }
                }else{
                    logger.debug("{} {} socket ioSession 重连失败，开始关闭socket连接,不影响使用，下次若端口可用，将新建连接!",remoteIpPort, localAddress);
                    ioConnector.dispose();
                    MinaFactory.remove(remoteIpPort,this.listId,this.uploadNodeType);
                }
            }
        } else{
            // 自身保活时间为0
            ioConnector.dispose();
            MinaFactory.remove(remoteIpPort,this.listId,this.uploadNodeType);
            logger.debug("{} {} ioSession保活期超限正常关闭，socket连接已关闭,mina缓存已清空!", remoteIpPort, localAddress);
        }
    }

    @Override
    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus)  {
        String remoteAddress = ioSession.getRemoteAddress().toString();
        String localAddress = ioSession.getLocalAddress().toString();

        byte[] sendBytes = CommonUtil.hex2Bytes("5555020f");
        ioSession.write(IoBuffer.wrap(sendBytes));
        if(this.activeNum == 0){
            logger.info("{}{} activeNum:{},超过保活期限，关闭socket连接!", remoteAddress, localAddress, this.activeNum);
            ioSession.close(true);
        }else{
            this.activeNum--;
            logger.debug("{} {} socket连接空闲,activeNum--,activeNum:{}!", remoteAddress, localAddress, this.activeNum);
        }
    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) {
        logger.error(throwable.getMessage());
    }

    @Override
    public void messageReceived(IoSession ioSession, Object message){
        String remoteAddress = ioSession.getRemoteAddress().toString();
        String localAddress = ioSession.getLocalAddress().toString();

        // 保活使用
//        if(activeNum < 10){
//            activeNum = 10;
//            logger.debug("{} {} socket连接接收到消息,activeNum设置为10,activeNum:{}", remoteAddress, localAddress, activeNum);
//        }
        
        String hexString = CommonUtil.bytesToHexString((byte[])message);
        logger.info("{} {} 接收到的16进制字符串:{}", remoteAddress, localAddress, hexString);

        // 在超时时间段内的消息都保存下来,待后续使用
        if(timeout > 0 && sendStartTime>0 && System.currentTimeMillis() - sendStartTime <= timeout){
            msg.append(hexString);
        }

        // 自定义消息处理
        if(minaMsgHandler != null){
            minaMsgHandler.handle(hexString);
        }
    }

    @Override
    public void messageSent(IoSession ioSession, Object message) {
    }

    @Override
    public void inputClosed(IoSession ioSession) throws Exception{
        SocketAddress remoteAddress = ioSession.getRemoteAddress();
        logger.debug("{} {} input close!", remoteAddress.toString(), ioSession.getLocalAddress().toString());

        // 通过发送ff测试连接是否可用,若连接异常将触发session close事件
        byte[] sendBytes = CommonUtil.hex2Bytes("5555020f");
        ioSession.write(IoBuffer.wrap(sendBytes));
        Thread.sleep(3000);
    }
    
    public IoConnector getIoConnector() {
        return ioConnector;
    }

    public void setIoConnector(IoConnector ioConnector) {
        this.ioConnector = ioConnector;
    }

    public int getActiveNum() {
        return activeNum;
    }

    public void setActiveNum(int activeNum) {
        this.activeNum = activeNum;
    }

    public Integer getListId() {
        return listId;
    }

    public void setListId(Integer listId) {
        this.listId = listId;
    }

    public MinaMsgHandler getMinaMsgHandler() {
        return minaMsgHandler;
    }

    public void setMinaMsgHandler(MinaMsgHandler minaMsgHandler) {
        this.minaMsgHandler = minaMsgHandler;
    }

    public long getSendStartTime() {
        return sendStartTime;
    }

    public void setSendStartTime(long sendStartTime) {
        this.sendStartTime = sendStartTime;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getUploadNodeType() {
        return uploadNodeType;
    }

    public void setUploadNodeType(String uploadNodeType) {
        this.uploadNodeType = uploadNodeType;
    }

    public boolean isUploading() {
        return uploading;
    }

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }

    @Override
    public String toString() {
        return "ConnectHandler{" +
                "ioConnector=" + ioConnector +
                ", activeNum=" + activeNum +
                ", listId=" + listId +
                ", msg=" + msg +
                '}';
    }
}