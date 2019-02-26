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
2.代码运行  
![图2](https://github.com/kickTec/MinaClientSample/blob/master/readmePicture/2.%E8%BF%90%E8%A1%8CMinaFactory%E4%B8%AD%E7%9A%84main%E6%96%B9%E6%B3%95.png)  
3.tcp server回应  
![图3](https://github.com/kickTec/MinaClientSample/blob/master/readmePicture/3.%E5%9B%9E%E5%BA%94556677%E5%93%8D%E5%BA%94.png)  
4.运行效果  
![图4](https://github.com/kickTec/MinaClientSample/blob/master/readmePicture/4.%E6%95%88%E6%9E%9C.png)  
