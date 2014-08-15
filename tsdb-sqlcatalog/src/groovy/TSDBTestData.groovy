TSDBHOST = "localhost";
TSDBPORT = 4242;
tsdbSocket = null;
buff = new StringBuilder();
random = new Random(System.currentTimeMillis());

nextInt = {
	return Math.abs(random.nextInt(90));
}

stime = {
    return (long)System.currentTimeMillis()/1000;
}

flush = { 
    tsdbSocket << buff;    
    buff.setLength(0);
}

pflush = { 
    print buff;
    buff.setLength(0);
}

trace = { metric, value, tags ->
    now = stime();
    buff.append("put $metric $now $value ");
    tags.each() { k, v ->
        buff.append(k).append("=").append(v).append(" ");
    }
    buff.append("\n");   
}

try {
    tsdbSocket = new Socket(TSDBHOST, TSDBPORT);
    println "Connected";
    for(d in 1..5) {
    	dc = "dc$d";
    	for(h in 1..35) {
    		["WebServer", "AppServer", "DBServer"].each() { hostType ->
    			host = "$hostType$h";
    			['combined', 'idle', 'irq', 'nice', 'softirq', 'stolen', 'sys', 'user', 'wait'].each() { ctype ->
    				for(c in 0..3) {
    					trace("sys.cpu", nextInt(), ['dc':dc, 'host':host, 'cpu':c, 'type': ctype]);
    				}
    			}
    		}
    	}

    }
    flush();
} finally {
	try { tsdbSocket.close(); } catch (e) {}
}
