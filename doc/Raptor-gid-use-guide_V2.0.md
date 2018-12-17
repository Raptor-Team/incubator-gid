# 全局序列使用说明

## 一、ZK节点创建

    [namespace]
        |
        |----sequences
        |
        |----workers=[dataCenterId]
                |
                |----[ip]-[port]=[workerId]
           
###节点解释

[namespace] 为App名，如CRM、PPM等，需在使用API时指定；

sequences 为序列目录，自动添加；

workers 为APP进程目录，[dataCenterId]为目录值，需手工指定，0~9按需分配；

[ip]-[port] 为APP进程即主机物理ip地址+进程端口，需手工添加，[workerId]为节点值，需手工指定，0~999 按需分配，例：节点192.168.1.23-80 值0；

###图示
构造全局：
![zk结构示例](http://git.oschina.net/uploads/images/2017/0103/113654_7c21b283_1025511.png)

DataCenterId添加：
![dataCenterId](http://git.oschina.net/uploads/images/2017/0103/114259_7ed20f1a_1025511.png)

WorkerId添加：
![workerId](http://git.oschina.net/uploads/images/2017/0103/114326_b5517717_1025511.png)

## 二、工程中使用
### POM配置
    <dependency>
        <groupId>studio.raptor</groupId>
        <artifactId>raptor-gid</artifactId>
        <version>4.0.0-SNAPSHOT</version>
    </dependency>
注：需将包添加至Maven私服

### sequence.xml配置
    <?xml version="1.0" encoding="UTF-8"?>
    <config
        xmlns="http://gid.raptor.studio/sequence"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
        xsi:schemaLocation="http://gid.raptor.studio/sequence">
        
        <sequences>
            <!-- 
                default config
                type：breadcrumb 
                incr：1
                start：0
                cache: 1000
            -->
            <default>
                <name>aid</name>
                <name>bid</name>
            </default>
            
            <!-- breadcrumb类型提供可控步长、初始值、排序方式等属性的sequence -->
            <breadcrumb incr="1" start="0" cache="100">
                <name>cid</name>
                <name>did</name>
            </breadcrumb>
        </sequences>
    </config>
注：若使用步长为一、初始值为0、缓存为1000的序列可用default类型，若要自定义则使用breadcrumb类型；

### JAVA代码
    import studio.raptor.gid.SequenceServer;
    
    public class SequenceMain {
    
        private static final String NAMESPACE   = "test";
    
        private static final String ZK_CONN_STR = "192.168.199.24:2181";
    
        private static String       ip          = "192.168.199.113";
    
        private static int          port        = 0;
    
        public static void main(String[] args) throws Exception {
            SequenceServer server = new SequenceServer(ZK_CONN_STR, NAMESPACE, ip, port);
            server.startup();
    
            server.get("aid").nextId();
        }
    }
注：此为示例代码，具体按实际使用配置NAMESPACE、ZK_CONN_STR、ip、port