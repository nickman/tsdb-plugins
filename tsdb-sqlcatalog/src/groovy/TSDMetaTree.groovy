import net.opentsdb.catalog.*;
import net.opentsdb.meta.*;
//import net.opentsdb.meta.api.*;
import com.stumbleupon.async.*
import org.slf4j.*;
import ch.qos.logback.classic.*;
import net.opentsdb.catalog.SQLCatalogMetricsMetaAPIImpl.*;

Class.forName("org.json.JSONObject");

logger = LoggerFactory.getLogger(SQLCatalogMetricsMetaAPIImpl.class).setLevel(Level.valueOf("DEBUG"));

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


q = new net.opentsdb.meta.api.QueryContext().setPageSize(50);

metrics = pluginContext.namedResources.get("SQLCatalogMetricsMetaAPIImpl");
tsdb = metrics.tsdb;




long start = System.currentTimeMillis();
cbrow = 0;
batches = 0;
eor = false;
tsuids = [];

tsmetas = metrics.evaluate(q.setPageSize(100), "sys.cpu:dc=dc1,host=WebServer1|WebServer5,type=combined,cpu=0|1").join(5000);
tsmetas.each() {
    println renderTags(it.getTags());
}
tree = SQLCatalogMetricsMetaAPIImpl.TSMetaTree.build("root", tsmetas);


String json =  net.opentsdb.utils.JSON.serializeToString(tree);
obj = net.opentsdb.utils.JSON.getMapper().readTree(json);
println new org.json.JSONObject(json).toString(2);

long elapsed = System.currentTimeMillis()-start;
println "Elapsed: $elapsed ms.";

println "========================================"


return null;