package com.kenick.mina;

import com.kenick.utils.CommonUtil;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;

public class MinaFactory {
    private final static Logger logger = LoggerFactory.getLogger(MinaFactory.class);

    private static int minaIdleTime = 5; // 连接空闲时间 秒
    private static int connectTimeout = 3000; // 连接超时时间 毫秒

    // ip:port <--> ConnectFuture 所有连接
    private static Map<String, List<ConnectFuture>> connectFutureList = Collections.synchronizedMap(new HashMap<String, List<ConnectFuture>>());

    // ip:port <--> listId[] 已建立连接,无handle,listId为connectFutureList的list位置
    private static Map<String,List<Integer>> connectNodeList = Collections.synchronizedMap(new HashMap<String, List<Integer>>());

    // ip:port <--> listId[] 正在使用的连接列表,listId为connectFutureList的list位置
    private static Map<String, List<Integer>> useConnList = Collections.synchronizedMap(new HashMap<String, List<Integer>>());

    // ip:port:nodeType <--> listId 主动上报mina
    public static Map<String,Integer> uploadMinaListIds = Collections.synchronizedMap(new HashMap<String, Integer>());

    /**
     *  获取mina连接指定ip的ConnectFuture数量
     * @param ipPort ip:port
     * @return ConnectFuture数量
     */
    public static int getConnectorSize(String ipPort){
        List<ConnectFuture> ioConnectors = connectFutureList.get(ipPort);
        if(ioConnectors == null){
            return 0;
        }
        return ioConnectors.size();
    }

    /**
     *  获取连接到指定ip port的连接
     * @param ipPort ip:port
     * @return 连接数组 第一个是listId 第二个是ConnectFuture
     */
    public static synchronized ConnectFuture getConnect(String ipPort){
        logger.trace("开始获取{}连接!",ipPort);
        int listId = -1;
        ConnectFuture connectFuture = null;

        List<ConnectFuture> connectFutures = connectFutureList.get(ipPort);
        List<Integer> useList = useConnList.get(ipPort); // 已使用连接集合
        if(useList == null){
            useList = new ArrayList<>();
        }

        if(connectFutures != null && connectFutures.size() > 0){  // 已有对应ip port的连接
            logger.trace("已有{}的连接!",ipPort);

            // 获取既不在invalidConnList列表 也不在useConnList列表中的节点
            for(int i=0; i<connectFutures.size(); i++){
                if(!useList.contains(i)){
                    connectFuture = connectFutures.get(i);
                    if(connectFuture != null){
                        listId = i;
                        logger.trace("当前使用listId:{},本地连接:{}",listId,connectFuture.getSession().getLocalAddress());
                        break;
                    }
                }
            }

            // 未获取到可用连接 自动连接指定ip port
            if(listId == -1){
                logger.trace("{}未获取到可用连接,开始重新获取连接!",ipPort);
                listId = connect(ipPort);
                logger.trace("{}重新连接后的listId为{}!",ipPort,listId);
            }
        }else{ // 没有对应ip port的连接
            logger.trace("{}没有建立的连接，开始连接!",ipPort);
            listId = connect(ipPort);
            logger.trace("{}连接后的listId为{}!",ipPort,listId);
        }

        if( listId != -1){ // 获取到连接
            useList.add(listId);
            useConnList.put(ipPort, useList);
            connectFuture = connectFutureList.get(ipPort).get(listId);
        }
        return connectFuture;
    }

    /**
     * 释放占用，连接暂时还存在，等待超时自己删除
     * @param ipPort
     * @param listId
     */
    public static void releaseConn(String ipPort,Integer listId,String nodeType){
        // 连接不使用时，将activeNum减半
        List<ConnectFuture> connectFutures = connectFutureList.get(ipPort);
        ConnectFuture connectFuture = connectFutures.get(listId.intValue());
        IoHandler handler = connectFuture.getSession().getHandler();
        if(handler != null){
            ConnectHandler connectHandler = (ConnectHandler) handler;
            connectHandler.setActiveNum(10);
            connectHandler.setMinaMsgHandler(null);
        }

        // 清除普通缓存使用记录
        useConnList.get(ipPort).remove(listId);

        // 清空上报缓冲使用记录
        if(nodeType != null && !"".equals(nodeType)){
            uploadMinaListIds.remove(ipPort+":"+nodeType);
        }
    }

    /**
     * 增加连接到指定socket的mina connectFuture
     * @param ipPort ip:port
     */
    public static Integer connect(String ipPort){
        int listId = -1;
        try {
            // mina配置
            String[] ipPortArray = ipPort.split(":");
            IoConnector ioConnector = new NioSocketConnector();
            ioConnector.setConnectTimeoutMillis(MinaFactory.connectTimeout);
            ioConnector.getSessionConfig().setReadBufferSize(10240);
            ioConnector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, MinaFactory.minaIdleTime);
            ioConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new CustomCodecFactory()));
            ConnectHandler connectHandler = new ConnectHandler();
            ioConnector.setHandler(connectHandler);

            // mina连接
            ioConnector.setDefaultRemoteAddress(new InetSocketAddress(ipPortArray[0], Integer.parseInt(ipPortArray[1])));
            connectHandler.setIoConnector(ioConnector); // 用于断线重连
            ConnectFuture connectFuture = ioConnector.connect();
            connectFuture.awaitUninterruptibly();

            // mina加入集合
            if(connectFuture.isConnected()){
                Thread.sleep(50); // 不能去掉，否则后续获取到的mina session无法发送数据
                listId = add(ipPort, connectFuture);
                connectHandler.setListId(listId);
            }else{
                connectHandler = null;
                ioConnector.dispose();
                ioConnector = null;
                logger.debug("{} socket连接失败!",ipPort);
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        return listId;
    }

    /**
     *  向公共连接集合中添加连接
     * @param ipPort ip:port
     * @param connectFuture 连接对象
     * @return 连接对象在connectFutureList的位置
     */
    public static Integer add(String ipPort,ConnectFuture connectFuture){
        List<ConnectFuture> connectFutures = connectFutureList.get(ipPort);
        int listId = -1;
        if (connectFutures == null){
            // ip port对应connectFuture集合未初始化
            connectFutures = new ArrayList<>();
            connectFutures.add(connectFuture);
            listId = 0;
            connectFutureList.put(ipPort, connectFutures);
        } else {
            // ip port对应connectFuture集合已初始化
            // 集合中有null的连接，进行替换
            for(int i=0; i<connectFutures.size(); i++){
                if(connectFutures.get(i) == null){
                    connectFutures.remove(i);
                    connectFutures.add(i, connectFuture);
                    listId = i;
                    break;
                }
            }
            // 集合中不存在null,添加到集合的尾部
            if(listId == -1){
                connectFutures.add(connectFuture);
                listId = connectFutures.size()-1;
            }
        }

        // 连接集合
        List<Integer> connNodes = connectNodeList.get(ipPort);
        if (connNodes == null){
            connNodes = new ArrayList<>();
        }
        if(!connNodes.contains(listId)){
            connNodes.add(listId);
        }

        connectFutureList.put(ipPort, connectFutures);
        connectNodeList.put(ipPort, connNodes);
        return listId;
    }

    /**
     *  删除ip port 对应连接信息
     * @param ipPort ip:port
     * @param listId 连接节点在connectFutureList的位置
     * @param uploadNodeType 上报节点类型
     */
    public static void remove(String ipPort,Integer listId,String uploadNodeType){
        List<ConnectFuture> connectFutures = connectFutureList.get(ipPort);
        if(connectFutures == null){
            // 不存在ip port对应的连接集合，直接清空所有记录
            connectNodeList.remove(ipPort);
            useConnList.remove(ipPort);

            // 清空包含ip port的所有上报listId
            Set<String> uploadKeys = uploadMinaListIds.keySet();
            for(String key:uploadKeys){
                if(key.contains(ipPort)){
                    uploadMinaListIds.remove(key);
                }
            }
            return;
        }

        // 存在ip port对应的连接集合
        // 存在上报节点类型
        if(uploadNodeType != null && !"".equals(uploadNodeType)){
            uploadMinaListIds.remove(ipPort+":" + uploadNodeType);
        }

        // 清除当前连接
        ConnectFuture listIdConn = connectFutures.remove(listId.intValue());
        listIdConn.cancel();
        listIdConn = null;

        if(connectFutures.size() < 1){ // 当前ip port连接集合中无对象，清空记录
            connectFutureList.remove(ipPort);
            useConnList.remove(ipPort);
            connectNodeList.remove(ipPort);
        }else{ // 当前ip port连接集合中还有其它连接
            // 将connectFutureList中对应位置对象替换为null，不能直接移除，否则会导致顺序乱掉
            connectFutures.add(listId,null);

            // 移除使用节点
            List<Integer> useList = useConnList.get(ipPort);
            if(useList != null && useList.size() > 0){
                useList.remove(listId);
            }

            // 移除已建立连接列表
            List<Integer> connList = MinaFactory.connectNodeList.get(ipPort);
            if(connList != null && connList.size() > 0){
                connList.remove(listId);
            }
        }
    }

    /**
     * 向指定connectFuture发送消息
     * @param connectFuture 建立好的socket连接
     * @param commandHex 待发现消息
     * @param  timeout 接收等待时间
     * @return 从socket接收到的返回信息
     */
    private static String _sendCommand(ConnectFuture connectFuture,String commandHex,int timeout){
        String receiveMsg = "";
        try {
            if(connectFuture.isConnected()){
                IoSession session = connectFuture.getSession();

                // 清空接收消息
                IoHandler handler = session.getHandler();
                ConnectHandler connectHandler = (ConnectHandler) handler;
                connectHandler.msg.delete(0, connectHandler.msg.length());

                // 发送消息
                connectHandler.setSendStartTime(System.currentTimeMillis());
                connectHandler.setTimeout(timeout); // 接收指令超时

                session.resumeRead();
                session.resumeWrite();

                if(!"".equals(commandHex)){
                    byte[] sendBytes = CommonUtil.hex2Bytes(commandHex);
                    session.write(IoBuffer.wrap(sendBytes));
                    logger.info("发送指令:"+commandHex);
                }

                // 每隔10毫秒查看下消息
                int interval = 10;
                for(int i=0; i<timeout/interval; i++){
                    if(connectHandler.msg.length() > 0){
                        receiveMsg = connectHandler.msg.toString();
                        break;
                    }
                    Thread.sleep(interval);
                }
                logger.info("最终返回的接收信息:" + receiveMsg);
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        return receiveMsg;
    }

    /**
     *  连接指定ip port socket 发送指令，并获取返回消息
     * @param ipPort ip:port
     * @param hexStr 16进制指令字符串
     * @param timeout 接收消息等待时间 毫秒
     * @return 接收到的socket返回信息
     */
    public static String send(String ipPort,String hexStr,int timeout){
        String receiveMsg = "";
        Integer listId = null;
        try {
            // 获取mina连接对象
            ConnectFuture connector = MinaFactory.getConnect(ipPort);

            if(connector != null && connector.isConnected()){
                IoSession ioSession = connector.getSession();

                // 获取mina listId 用于后续释放连接
                ConnectHandler handler = (ConnectHandler) ioSession.getHandler();
                listId = handler.getListId();

                if(ioSession.isClosing()){
                    ioSession.close(true);
                    MinaFactory.remove(ipPort,listId,null);
                }else{
                    // 发送16进制指令，接收timeout毫秒内收到的信息
                    receiveMsg = MinaFactory._sendCommand(connector, hexStr, timeout);
                    // 释放mina连接，在保活期内可继续使用
                    logger.trace("开始释放连接{},listId:{}",ipPort,listId);
                    MinaFactory.releaseConn(ipPort, listId,null);
                }
            }
        } catch (Exception e) {
            if(listId != null){
                MinaFactory.releaseConn(ipPort, listId,null);
            }
            logger.error(e.getMessage());
        }
        return receiveMsg;
    }

    /**
     *  发送设置主动上报指令
     * @param ipPort ip:port
     * @param sendHexStr 待发送的16进制指令字符串
     * @param timeout 接收消息等待时间 毫秒
     * @return 接收到的socket返回信息
     */
    public static String sendUpload(String ipPort,String nodeId,String nodeType,String operateParam,String sendHexStr,int timeout){
        String receiveMsg = "";
        Integer listId = null;
        ConnectFuture connector;
        try {
            // 从上报mina缓存中获取listId
            String uploadKey = ipPort + ":" + nodeType;
            Integer uploadId = uploadMinaListIds.get(uploadKey);
            if(uploadId == null){
                // 未获取到缓存中的上报mina，使用普通获取方式
                connector = MinaFactory.getConnect(ipPort);
            } else {
                List<ConnectFuture> ipPortCons = connectFutureList.get(ipPort);
                connector = ipPortCons.get(uploadId);
            }

            if(connector != null && connector.isConnected()){
                // 获取mina listId 用于后续释放连接
                ConnectHandler handler = (ConnectHandler)connector.getSession().getHandler();
                listId = handler.getListId();

                // 发送16进制指令，接收指定时间内收到的信息
                receiveMsg = MinaFactory._sendCommand(connector, sendHexStr, timeout);

                // 上报处理
                if(receiveMsg.replace(" ","").contains(sendHexStr)){
                    // 上报指令执行成功
                    handler.setUploadNodeType(nodeType);

                    if(listId != null){
                        uploadMinaListIds.put(uploadKey,listId);
                    }

                    // 关闭手势上报成功
                    if("ff 05".equals(nodeType) && "00 01 02 00 00".equals(operateParam)){
                        logger.info("{} {} 关闭手势上报成功,系统将自动释放占用的连接!",ipPort,nodeType);
                        handler.setUploading(false);
                        MinaFactory.releaseConn(ipPort,listId,nodeType);
                    }

                    // 打开手势上报成功
                    if("ff 05".equals(nodeType) && "00 01 02 00 01".equals(operateParam)){
                        logger.info("{} {} 打开手势上报成功,系统将保持连接!",ipPort,nodeType);
                        handler.setUploading(true);
                        handler.setActiveNum(100);

                        // 手势消息处理器
                        MinaMsgMqttHandle minaMsgMqttHandle = new MinaMsgMqttHandle();
                        minaMsgMqttHandle.setIpPort(ipPort);
                        minaMsgMqttHandle.setUnit("上,下，左，右,无，未知");
                        minaMsgMqttHandle.setValueType("手势值");
                        handler.setMinaMsgHandler(minaMsgMqttHandle);
                    }
                }else{
                    // 上报指令执行失败
                    if(handler.isUploading()){ // 正在上报
                        // 关闭手势上报
                        if("ff 05".equals(nodeType) && "00 01 02 00 00".equals(operateParam)){
                            logger.info("{} {} 关闭手势上报指令执行失败,当前连接正在上报中，暂不释放当前连接!",ipPort,nodeType);
                        }
                    }else{ // 未开始上报,释放当前连接
                        logger.info("{} {} 上报指令执行失败,当前连接未在进行上报,释放当前连接!",ipPort,nodeType);
                        MinaFactory.releaseConn(ipPort,listId,nodeType);
                    }
                }
            }else{
                logger.error("{} 连接异常!",connector);
            }
        } catch (Exception e) {
            if(listId != null){
                MinaFactory.releaseConn(ipPort, listId, nodeType);
            }
            logger.error(e.getMessage());
        }
        return receiveMsg;
    }

    public static void main(String[] args) {
        String tcpServer = "127.0.0.1:4002"; // 目标tcp端口
        String message = "11 22 33"; // 16进制命令字符串 11 22 33
        int timeout = 10000; // 接收信息超时时间
        String msg = MinaFactory.send(tcpServer, message, timeout);
        logger.info("Mina发送后接收到的信息:{}", msg);
    }
}