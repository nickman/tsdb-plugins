import javax.management.*;
import java.util.regex.*;


r = {p ->
    if(p.indexOf("|") != -1) p = "[${p.replace('|', '|')}]";
    return p.replace("*", ".*?");
}

rank = {p ->
    int wc = 0;
    if(p.indexOf("|")!=-1) wc++;
    if(p.indexOf("*")!=-1) wc++;    
    if(wc==2) wc = 3;
    return wc;
}

m = {p, map ->
    int rank = rank(p);
    def pattern = r(p);
    map.put(p, rank);
}

test = {fqn, matchers, dbg ->
    
    def objName = new ObjectName(fqn);
    if(!applyPattern(matchers, objName.getDomain(), dbg)) return false;
    boolean failed = false;
    objName.getKeyPropertyList().each() { k, v ->
        if(!failed) {
            failed = applyPattern(matchers, "$k=$v", dbg);
        }
    }
    if(dbg) println "test($fqn): $failed";
    return !failed;
}

applyPattern = {matchers, pattern, dbg ->
    boolean match = false;
    matchers.each() {
        if(!match) {
            match = it.matcher(pattern).matches()
             if(dbg) println "\t--->($pattern) ($it) : ${match}";
        }
    }
    if(dbg) println "applyPattern($pattern) : $match";
    return match;
}

actuals = [];
int cnt = 0;
new File("/tmp/matches.txt").eachLine() {
    if(cnt < 1000) {
        actuals.add(it.replace("\"", ""));
        //println "SAMPLE [$it]";
    }
    cnt++;
}

println "Loaded ${actuals.size()} Samples";

patr = 'sys*:dc=dc*,host=WebServer1|WebServer5';
//patr = 'sys*:dc=dc*,host=App*|WebServer5';
on = new ObjectName(patr);
patternMatchers = [:];
m(r(on.getDomain()), patternMatchers);

on.getKeyPropertyList().each() { k, v ->
    
    m("${r(k)}=${r(v)}", patternMatchers);
}
patterns = new LinkedHashSet();
for(i in 0..3) {
    patternMatchers.each() { k, v ->
        if(v==i) {
            patterns.add(Pattern.compile(k));
            println "Added pattern $k";
        }
    }
}

println "==========================================";
int matches = 0;
int sb = 0;
//actuals.clear();
actuals.each() {
    boolean dbg = false;
    if(it.contains("WebServer1") || it.contains("WebServer5")) { 
        println it;
        sb++;
        dbg = true;
    }
    if(test(it, patterns, dbg)) {
        matches++;
        if(matches%100==0) println it;
    }
}

println "Matches: $matches";
println "Should have matched: $sb";