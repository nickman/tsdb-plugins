import net.opentsdb.catalog.*;
import net.opentsdb.meta.*;
//import net.opentsdb.meta.api.*;
import com.stumbleupon.async.*
import org.slf4j.*;
import ch.qos.logback.classic.*;

logger = LoggerFactory.getLogger(SQLCatalogMetricsMetaAPIImpl.class).setLevel(Level.valueOf("DEBUG"));




q = new net.opentsdb.meta.api.QueryContext().setPageSize(1);

renderTags = { tags ->
    b = new StringBuilder();
    k = true;
    tags.each() { uid ->
        if(k) {
            b.append("${uid.getName()}=");
        } else {
            b.append("${uid.getName()},");
        }
        k = !k;
    }
    if(!tags.isEmpty()) {
        b.deleteCharAt(b.length()-1); 
    }
    return b.toString();
}
cbrow = 1;
pageSize = 2;
eor = false;
cb = [

    call : { result -> 
        def last = null;
        if(result.size() < pageSize) {
            eor = true;
        }
        result.each() {
            cbrow++;
            if(it instanceof UIDMeta) {
                println "$cbrow: ${it.getName()}"; 
            } else if(it instanceof TSMeta) {
                def tagStr = renderTags(it.getTags());
                println "$cbrow: ${it.getMetric().getName()}:$tagStr"; 
            } else if(it instanceof Annotation) {
            
            }
            last = it;            
        }
        if(last!=null) {
            //println "\tLAST --->  ${last.dump()}";
            if(last instanceof UIDMeta) {
                q.setNextIndex(last.getUID());        
            } else if(last instanceof TSMeta) {
                q.setNextIndex(last.getTSUID());        
            } else if(last instanceof Annotation) {
            
            }
            
        } else {
            q.setNextIndex(null);
        }
     }
]
callback  = cb as Callback;

metrics = pluginContext.namedResources.get("SQLCatalogMetricsMetaAPIImpl");
tsdb = metrics.tsdb;




long start = System.currentTimeMillis();
cbrow = 0;
batches = 0;
eor = false;
tsuids = [];
for(;;){ // infinite for
    //metrics.tsMetasNoOverflow(q.setPageSize(300),'sys.cpu', ['dc' : 'dc3|dc4', 'host' : 'Web*1', 'type' : 'combined' ]);
    //a = metrics.getTSMetas(q.setPageSize(300),'sys.cpu', ['dc' : 'dc3|dc4', 'host' : 'Web*1', 'type' : 'combined' ]).addCallback(callback).join(5000);  //, "host" 
    //a = metrics.getTSMetas(q.setPageSize(2),true, 'sys.cpu', ['host' : 'PP-WK-NWHI-01', 'type' : '*']).addCallback(callback).join(5000);  //, "host" 
    //println a;
    //metrics.getMetricNames(q.setPageSize(2), ['host' : '*', 'type' : 'combined']).addCallback(callback).join(5000);  //, "host" 
    //metrics.getMetricNames(q.setPageSize(2), "host", "type", "cpu").addCallback(callback).join(5000);  //, "host" 
    //metrics.getMetricNamesFor(q.setPageSize(2)).addCallback(callback).join(5000);  //, "host" 
    //metrics.getTagKeys(q.setPageSize(100), "sys.cpu", 'dc', 'host', 'cpu').addCallback(callback).join(5000);  //, "host" 
    
    // getTagValues(final QueryContext queryOptions, final String metricName, final Map<String, String> tags, final String tagKey)
    
    //metrics.evaluate(q.setPageSize(100), "sys.cpu:dc=dc1,host=WebServer1|WebServer5,type=combined,cpu=0|1").addCallback(callback).join(5000);
    //metrics.evaluate(q.setPageSize(100), "sys*:dc=dc1,host=WebServer1|WebServer5").addCallback(callback).join(5000);
    
    //metrics.getTagValues(q.setPageSize(40), "sys.cpu", ['dc': 'dc1', 'host' : 'WebServer*', 'cpu' : '*'], 'type').addCallback(callback).join(5000);  //, "host" 
    batches++;
    //metrics.getTagValues(q.setPageSize(pageSize), "sys.cpu", 'type').addCallback(callback).join(5000);  //, "host" 
    if( q.isExhausted() || q.getNextIndex()==null){ //condition to break, oppossite to while 
        println "=============\nRows: $cbrow, Batches: $batches";
        break
    }
}
cbrow = 0;
long elapsed = System.currentTimeMillis()-start;
println "Elapsed: $elapsed ms.";

println "========================================"


return null;