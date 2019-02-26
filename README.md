# MinaClientSample
Mina作为客户端连接任意tcp目标，实现消息的同步发送和接收，具有连接共享和保活机制。  
  
使用方法:  
        String tcpServer = "127.0.0.1:4002"; // 目标tcp端口  
        String message = "11 22 33"; // 16进制命令字符串 11 22 33  
        int timeout = 10000; // 接收信息超时时间  
        String msg = MinaFactory.send(tcpServer, message, timeout);  
        logger.info("Mina发送后接收到的信息:{}", msg);  
        
使用步骤:  
1.开启tcp server  
![图1](https://github.com/kickTec/MinaClientSample/blob/master/readmePicture/1.%E5%BC%80%E5%90%AFtcp%E7%AB%AF%E5%8F%A3.png)
