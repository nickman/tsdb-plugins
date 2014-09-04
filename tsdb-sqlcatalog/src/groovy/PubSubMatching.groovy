import javax.management.*;
import java.util.regex.*;


r = {p ->
    if(p.indexOf("|") != -1) p = "[${p.replace('|', ',|')},]";
    return p.replace("*", ".*?");
}

actuals = [];
new File("/tmp/matches.txt").eachLine() {
    actuals.add(it.replace("\"", ""));
}

println "Loaded ${actuals.size()} Samples";

patr = 'sys*:dc=dc*,host=WebServer1|WebServer5';
on = new ObjectName(patr);
b = new StringBuilder(r(on.getDomain())).append("\\:.*?");
on.getKeyPropertyList().each() { k, v ->
    b.append(r(k)).append("=").append(r(v)).append(",");
}
b.deleteCharAt(b.length()-1);
b.append(".*?");


p = Pattern.compile(b.toString());
println p;
int matches = 0;
//actuals.clear();
actuals.each() {
    if(p.matcher(it).matches()) {
        matches++;
        println it;
    }
}

println "Matches: $matches";