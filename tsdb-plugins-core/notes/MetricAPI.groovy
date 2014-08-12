//println pluginContext.namedResources.keySet()
import net.opentsdb.catalog.*;
import net.opentsdb.meta.*;
import com.stumbleupon.async.*

// def readable = { it.put("12 34".reverse()); 5 } as Readable

q = new QueryOptions().setPageSize(1);

cb = [
    
    call : { result -> 
        def last = null;
        result.each() {
            if(it instanceof UIDMeta) {
                println "RESULT: ${it.getName()}"; 
            } else if(it instanceof TSMeta) {
                println "RESULT: ${it.getMetric().getName()}:${it.getTags()}"; 
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

metrics = pluginContext.getResource("SQLCatalogMetricsMetaAPIImpl", SQLCatalogMetricsMetaAPIImpl.class);

long start = System.currentTimeMillis();
for(;;){ // infinite for
    a = metrics.getTSMetas(q.setPageSize(100),true, 'sys.cpu', ['host' : 'tpmint']).join(5000);  //, "host" 
    println a;
    //metrics.getMetricNamesFor(q.setPageSize(2), ['host' : 'tpmint', 'type' : 'combined']).addCallback(callback).join(5000);  //, "host" 
    //metrics.getMetricNamesFor(q.setPageSize(2), "host", "type", "cpu").addCallback(callback).join(5000);  //, "host" 
    //metrics.getMetricNamesFor(q.setPageSize(2)).addCallback(callback).join(5000);  //, "host" 
    //metrics.getTagKeysFor(q.setPageSize(2), "sys.cpu").addCallback(callback).join(5000);  //, "host" 
    if( q.getNextIndex()==null ){ //condition to break, oppossite to while 
        println "   STOPPING. NEXT INDEX WAS NULL";
        break
    }
}
long elapsed = System.currentTimeMillis()-start;
println "Elapsed: $elapsed ms.";



return null;