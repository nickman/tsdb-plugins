import org.elasticsearch.common.transport.*;
import org.elasticsearch.client.transport.*;
import org.elasticsearch.common.settings.ImmutableSettings;
//import org.elasticsearch.action.admin.cluster.ping.single.SinglePingRequest;
import org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
import static org.elasticsearch.node.NodeBuilder.*;
import org.elasticsearch.indices.*;
import org.elasticsearch.action.admin.indices.create.*;
import org.elasticsearch.action.admin.indices.exists.indices.*;
import org.elasticsearch.action.admin.indices.delete.*;
// on startup



jsonDir = new File("C:\\hprojects\\tsdb-plugins\\tsdb-elastic\\src\\main\\resources\\scripts");

settings = ImmutableSettings.settingsBuilder()
        .put("client.transport.sniff", false)
        //.put("client.transport.ping_timeout", 1)
        //.put("transport.tcp.connect_timeout", 10)
        //.put("connect_timeout", 10)
        .build();

println InetSocketTransportAddress.class.getProtectionDomain().getCodeSource().getLocation();
        
indexStatus = { indexAdmin, indexes->    
    return indexAdmin.status(new IndicesStatusRequest(indexes)).actionGet();
}

indexExists = { indexAdmin, index ->
    return indexAdmin.exists(new IndicesExistsRequest(index)).actionGet().isExists();
}

deleteIndex = { indexAdmin, index ->
    return indexAdmin.delete(new DeleteIndexRequest(index)).actionGet();
}


createIndex = { indexAdmin, index ->
    indexAdmin.create(new CreateIndexRequest(index)).actionGet();
}
        
client = null;
try {
    
        /*
        client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("10.12.114.37", 9300));
        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300))
        .addTransportAddress(new InetSocketTransportAddress("127.0.0.1", 9300))
        .addTransportAddress(new InetSocketTransportAddress("PP-WK-NWHI-01", 9300))        
        .addTransportAddress(new InetSocketTransportAddress("FOO", 9300))        
        .addTransportAddress(new InetSocketTransportAddress("10.12.114.37", 9300))
        .addTransportAddress(new InetSocketTransportAddress("FOO", 9300));
        */

    client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
//    Node node = nodeBuilder().node();
//    client = node.client();

    admin = client.admin();
    clusterAdmin = admin.cluster();
    indexAdmin = admin.indices();
        
    print "Connected. Connected Servers:";
    client.transportAddresses().each() {
        //spr = new SinglePingRequest();
        //clusterAdmin.ping(spr).actionGet();
        println "\n\t${it.toString()}";        
    }
    println "Connected Nodes:";
    client.connectedNodes().each() {
        println "\t${it.getId()}@${it.getAddress()}";
        println "\tAttributes:";
        it.attributes().each() { k,v ->
            println "\t\t$k  :  $v}";
        }
    }
    INDEX = "opentsdb";
    
    if(indexExists(indexAdmin, INDEX)) {
        println "Index Exists. Deleting....";
        deleteIndex(indexAdmin, INDEX);
        println "Index Deleted";
    }
    
    if(indexExists(indexAdmin, INDEX)) {
        println "Index Exists";
        println indexStatus(indexAdmin, INDEX).dump();
    } else {
        println "No Index... creating....";
        createIndex(indexAdmin, INDEX);
        println "Index Created";
        println indexStatus(indexAdmin, INDEX).dump();
    }
    
} finally {
    if(client!=null) try { client.close(); println "Client Closed"; } catch (e) {}
}

return null;


/*
MAPPING EXISTS: curl -i -X HEAD http://localhost:9200/opentsdb/tsmeta/
ADD MAPPING:   curl -X PUT -d @scripts/tsmeta_map.json http://localhost:9200/opentsdb/tsmeta/_mapping
ADD INDEX:    http://localhost:9200/opentsdb/
INDEX EXISTS: curl -X HEAD  http://localhost:9200/opentsdb/


        if (settings.getAsBoolean("netty.epollBugWorkaround", false)) {
            System.setProperty("org.jboss.netty.epollBugWorkaround", "true");
        }

        this.workerCount = componentSettings.getAsInt("worker_count", EsExecutors.boundedNumberOfProcessors(settings) * 2);
        this.bossCount = componentSettings.getAsInt("boss_count", 1);
        this.blockingServer = settings.getAsBoolean("transport.tcp.blocking_server", settings.getAsBoolean(TCP_BLOCKING_SERVER, settings.getAsBoolean(TCP_BLOCKING, false)));
        this.blockingClient = settings.getAsBoolean("transport.tcp.blocking_client", settings.getAsBoolean(TCP_BLOCKING_CLIENT, settings.getAsBoolean(TCP_BLOCKING, false)));
        this.port = componentSettings.get("port", settings.get("transport.tcp.port", "9300-9400"));
        this.bindHost = componentSettings.get("bind_host", settings.get("transport.bind_host", settings.get("transport.host")));
        this.publishHost = componentSettings.get("publish_host", settings.get("transport.publish_host", settings.get("transport.host")));
        this.compress = settings.getAsBoolean("transport.tcp.compress", false);
        this.connectTimeout = componentSettings.getAsTime("connect_timeout", settings.getAsTime("transport.tcp.connect_timeout", settings.getAsTime(TCP_CONNECT_TIMEOUT, TCP_DEFAULT_CONNECT_TIMEOUT)));
        this.tcpNoDelay = componentSettings.getAsBoolean("tcp_no_delay", settings.getAsBoolean(TCP_NO_DELAY, true));
        this.tcpKeepAlive = componentSettings.getAsBoolean("tcp_keep_alive", settings.getAsBoolean(TCP_KEEP_ALIVE, true));
        this.reuseAddress = componentSettings.getAsBoolean("reuse_address", settings.getAsBoolean(TCP_REUSE_ADDRESS, NetworkUtils.defaultReuseAddress()));
        this.tcpSendBufferSize = componentSettings.getAsBytesSize("tcp_send_buffer_size", settings.getAsBytesSize(TCP_SEND_BUFFER_SIZE, TCP_DEFAULT_SEND_BUFFER_SIZE));
        this.tcpReceiveBufferSize = componentSettings.getAsBytesSize("tcp_receive_buffer_size", settings.getAsBytesSize(TCP_RECEIVE_BUFFER_SIZE, TCP_DEFAULT_RECEIVE_BUFFER_SIZE));
        this.connectionsPerNodeLow = componentSettings.getAsInt("connections_per_node.low", settings.getAsInt("transport.connections_per_node.low", 2));
        this.connectionsPerNodeMed = componentSettings.getAsInt("connections_per_node.med", settings.getAsInt("transport.connections_per_node.med", 6));
        this.connectionsPerNodeHigh = componentSettings.getAsInt("connections_per_node.high", settings.getAsInt("transport.connections_per_node.high", 1));
        this.connectionsPerNodePing = componentSettings.getAsInt("connections_per_node.ping", settings.getAsInt("transport.connections_per_node.ping", 1));

        this.maxCumulationBufferCapacity = componentSettings.getAsBytesSize("max_cumulation_buffer_capacity", null);
        this.maxCompositeBufferComponents = componentSettings.getAsInt("max_composite_buffer_components", -1);



*/