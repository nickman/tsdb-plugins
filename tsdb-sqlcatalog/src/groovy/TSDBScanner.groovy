import java.nio.charset.*;
import net.opentsdb.utils.*;
import net.opentsdb.meta.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import groovy.sql.*;

scanner = null;
Charset CHARSET = Charset.forName("ISO-8859-1");

groovySql = new Sql(pluginContext.getResource("CatalogDataSource", Object.class));
Set<String> tsuids = new HashSet<String>(10240);
groovySql.eachRow("SELECT TSUID FROM TSD_TSMETA") {
    tsuids.add(it.TSUID);
}
println "Cached ${tsuids.size()} TSUIDs";
try {
    scanner = tsdb.getClient().newScanner(tsdb.metaTable());
    scanner.setMaxNumRows(1024);
    scanner.setFamily("name".getBytes(CHARSET));
    AtomicInteger counter = new AtomicInteger(0);
    threadPool = Executors.newCachedThreadPool();
    int cnt = 0;
    start = System.currentTimeMillis();
    scanner.nextRows().join().each() {
        it.each() { kv ->
          if("ts_meta".equals(fromBytes(kv.qualifier()))) { 
              tsMeta = JSON.parseToObject(kv.value(), TSMeta.class);
              if(tsuids.add(tsMeta.getTSUID())) {
                  def xtsuid = tsMeta.getTSUID();
                  future = threadPool.submit({
                      run: {           
                          try {               
                              def xMeta = TSMeta.getTSMeta(tsdb, xtsuid).join(); 
                              tsdb.indexTSMeta(xMeta);
                              tsdb.indexUIDMeta(xMeta.getMetric());
                              xMeta.getTags().each() {
                                  tsdb.indexUIDMeta(it);
                              }
                          } finally {
                              counter.decrementAndGet();
                          }                                                       
                      }
                  } as Runnable);
                  counter.incrementAndGet();
              }
              cnt++;
          }
        }
    }
    
    while(counter.get() > 0) {
        println "Waiting for $counter Tasks...";
        Thread.sleep(300);
    }
    elapsed = System.currentTimeMillis() - start;
    println "Completed in $elapsed ms";
    println "Done. Rows: $cnt";
} finally {
    try { scanner.close(); println "Closed Scanner"; } catch (e) {}
}