import net.opentsdb.catalog.*;
import net.opentsdb.meta.*;
import net.opentsdb.core.*;
//import net.opentsdb.meta.api.*;
import com.stumbleupon.async.*
import org.slf4j.*;
import ch.qos.logback.classic.*;
import java.util.concurrent.*;

logger = LoggerFactory.getLogger(SQLCatalogMetricsMetaAPIImpl.class).setLevel(Level.valueOf("DEBUG"));




q = new net.opentsdb.meta.api.QueryContext().setPageSize(100);


metrics = pluginContext.namedResources.get("SQLCatalogMetricsMetaAPIImpl");
tsdb = metrics.tsdb;

renderDataPoints = { dps ->
    def b = new StringBuilder("${dps.metricName()}:${dps.getTags()}");
    b.append("\n\tSize:${dps.size()}");
    b.append("\n\tAggr Size:${dps.aggregatedSize()}");
    b.append("\n\tAggr Tags:${dps.getAggregatedTags()}");
    b.append("\n\tMetric Name:${dps.metricName()}");
    b.append("\n\tTSUIDs:${dps.getTSUIDs()}");
    return b.toString();
}


long start = System.currentTimeMillis();
tsuids = new ArrayList<String>();
startTime = start - (1000*30);
tsmetas = metrics.evaluate(q.setPageSize(100), "sys.cpu:dc=dc1,host=tpmint,type=*,cpu=0|1").join(5000);

tsmetas.each() {
    tsuids.add(it.getTSUID());
}
println tsuids;
query = tsdb.newQuery();
query.setTimeSeries(tsuids, Aggregators.AVG, false);
query.downsample(100000, Aggregators.AVG);
//query.setStartTime(TimeUnit.SECONDS.convert(startTime, TimeUnit.MILLISECONDS));
query.setStartTime(startTime);
dps = query.run();
println " =========== Data Points =========== ";
dps.each() { dp ->
    println renderDataPoints(dp);
    println " =========== ";
    dp.each() { sv ->
        while(sv.hasNext()) {
            p = sv.next();
            //println "\tDP: ${p.toDouble()}";
        }
    }
}


long elapsed = System.currentTimeMillis()-start;
println "Elapsed: $elapsed ms.";

println "========================================"


return null;