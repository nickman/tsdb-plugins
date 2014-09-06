import org.helios.tsdb.plugins.remoting.subpub.TSMetaPatternSelector;

selector = new TSMetaPatternSelector("sys*:dc=dc*,host=WebServer1|WebServer5,*");

actuals = [];
int cnt = 0;
new File("/tmp/matches.txt").eachLine() {
    actuals.add(it.replace("\"", ""));
    cnt++;
}

println "Loaded ${actuals.size()} Samples";

println "==========================================";
int matches = 0;
int sb = 0;
long totalTime = 0;
//actuals.clear();
println "Warmup";
for(i in 0..100) {
    actuals.each() { selector.matches(it); }
}
println "Warmup Complete";
actuals.each() {
    if(it.contains("WebServer1,") || it.contains("WebServer5,")) { 
        sb++;
    }
    long start = System.nanoTime();
    if(selector.matches(it)) {
        matches++;
    }
    totalTime += System.nanoTime() - start;
}

println "Matches: $matches";
println "Should have matched: $sb";
println "Avg: ${totalTime/cnt} ns.";