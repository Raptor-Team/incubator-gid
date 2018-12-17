# 1.配置Sequence.xml

## default
	<default>
		<seq>aid</seq>
		<seq>bid</seq>
	</default>
默认配置：

increment：1 自增值

start：0 初始值

cache：1000 缓存大小

说明：默认配置的类型为Breadcrumb，seq中为序列名称，不可为空。


## breadcrumb
	<breadcrumb increment="-2" start="1000" cache="100">
		<seq>cid</seq>
		<seq>did</seq>
	</breadcrumb>
Breadcrumb类型提供自增值、初始值、缓存等属性的sequence，
其中increment、start、cache可省略。

## snowflake
	<snowflake>
		<seq>eid</seq>
		<seq>fid</seq>
	</snowflake>
snowflake类型提供64位二进制粗略序、全局唯一、自增长的sequence，
此类型不可有increment、start、cache等属性

## ticktock
	<ticktock>
		<seq>hid</seq>
		<seq>gid</seq>
	</ticktock>
ticktock类型提供19位十进制长度粗略有序、时间相关、全局唯一、自增长的sequence,此类型不可有increment、start、cache等属性。


# 2.使用
## 默认配置文件用法
	SequenceServer seqServer = new SequenceServer(clientAddr);
	seqServer.getSequence("aid").nextId();
clientAddr为当前业务应用地址，（127.0.0.1-80）

aid为配置文件中定义的序列名称

## 配置文件自定义名用法
	SequenceServer SequenceServer(filePath, clientAddr)
	seqServer.getSequence("bid").nextId();
filePath为配置文件自定义名

clientAddr为当前业务应用地址，（127.0.0.1-80）

bid为配置文件中定义的序列名称

## 动态添加序列用法
	SequenceServer seqServer = new SequenceServer(zkAddr, namespace, clientAddr);
	Sequence testSeq = SequenceFactory.builder()
				.name("testSeq")
				.type(SequenceType.BREADCRUMB)
				.cache(1).start(0).increment(1).build();
	seqServer.addSequence(testSeq);
	seqServer.getSequence("cid").nextId();
zkAddr为ZK服务端地址

namespace为当前业务域命名空间

clientAddr为当前业务应用地址，（127.0.0.1-80）