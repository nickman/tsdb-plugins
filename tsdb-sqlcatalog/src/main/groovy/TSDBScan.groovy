import org.helios.tsdb.plugins.meta.MetaSynchronizer;
import org.hbase.async.*;
import java.nio.charset.*;
import org.helios.tsdb.plugins.util.SystemClock;
import net.opentsdb.uid.*;
import net.opentsdb.meta.*;
import net.opentsdb.core.*;
import net.opentsdb.uid.UniqueId.UniqueIdType;

ms = new MetaSynchronizer(tsdb);
//ms.process();
long max_id = MetaSynchronizer.getMaxMetricID(tsdb);
println max_id;
short metric_width = tsdb.metrics_width();
Scanner scanner = tsdb.getClient().newScanner(tsdb.dataTable());
byte[] start_row =  Arrays.copyOfRange(Bytes.fromLong(0L), 8 - metric_width, 8);
byte[] end_row =    Arrays.copyOfRange(Bytes.fromLong(max_id), 8 - metric_width, 8);
scanner.setStartKey(start_row);
scanner.setStopKey(end_row);
scanner.setFamily("t".getBytes(Charset.forName("ISO-8859-1")));
scanResult = scanner.nextRows().joinUninterruptibly();
println "Outser Size:${scanResult.size()}";
Set<String> seenUids = new HashSet<String>();
Set<String> seenTSUids = new HashSet<String>();
boolean tsIns = false;
scanResult.each() {
    //println "Inner:${it.size()}";
    byte[] tsuid = UniqueId.getTSUIDFromKey(it.get(0).key(), tsdb.metrics_width(), Const.TIMESTAMP_BYTES);    
    String tsuid_string = UniqueId.uidToString(tsuid);
    if(seenTSUids.add(tsuid_string)) {
        //println "TS UID:${tsuid_string}";
        byte[] metric_uid_bytes = Arrays.copyOfRange(tsuid, 0, TSDB.metrics_width());     
        String metric_uid = UniqueId.uidToString(metric_uid_bytes);
        //println "Metric UID:${metric_uid}";
        List<byte[]> tags = UniqueId.getTagPairsFromTSUID(tsuid_string, tsdb.metrics_width(), tsdb.tagk_width(),  tsdb.tagv_width()); 
        int idx = 0;
        tags.each() { tag ->
            //println "IDX:$idx";
            UniqueIdType type = (idx % 2 == 0) ? UniqueIdType.TAGK : UniqueIdType.TAGV;
            String uid = UniqueId.uidToString(tag);
            if(seenUids.add(uid)) {
                //println "UID:${uid}";            
                try {
                    uidMeta = UIDMeta.getUIDMeta(tsdb, type, tag).joinUninterruptibly();
                    //println "UIDMeta:$uid : $type : ${uidMeta.name} : ${uidMeta.custom}";
                    //tsdb.indexUIDMeta(uidMeta);
                } catch (e) {}
            }
            idx++;
        } 
        tsMeta = TSMeta.getTSMeta(tsdb, tsuid_string).joinUninterruptibly();
        //println tsMeta.metric;
        //tsdb.indexUIDMeta(tsMeta.metric);
        if(!tsIns) {
            
            println "TSMeta: ${tsMeta.tsuid}";
            println "Metric: (${tsMeta.metric}) --> ${tsMeta.metric.name}";
            tsMeta.tags.each() {
                println "Tag: (${it}) --> ${it.name}";
                //tsdb.indexUIDMeta(it);
            }
            
            tsdb.indexTSMeta(tsMeta);
            tsIns = true;
        }
        
    }
}
println"Done";


return null;

/*
update TSD_TAGK set description = 'The category of the value defined by the associated metric name' where name = 'kind'

*/