TSDBHOST = "localhost";
TSDBPORT = 4242;
tsdbSocket = null;
buff = new StringBuilder();
random = new Random(System.currentTimeMillis());

nextInt = {
	return Math.abs(random.nextInt(90));
}

nextLong = {
	return Math.abs(random.nextInt());
}

stime = {
    return (long)System.currentTimeMillis()/1000;
}

flush = { 
    tsdbSocket << buff;    
    tsdbSocket.getOutputStream().flush();
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

cpuCounts = ["WebServer" : 1, "AppServer" : 3, "DBServer" : 7];

try {
    tsdbSocket = new Socket(TSDBHOST, TSDBPORT);
    println "Connected";
    for(d in 1..5) {
    	dc = "dc$d";   
    	for(h in 1..(d%2==0 ? 10 : 20)) {
    		["WebServer", "AppServer", "DBServer"].each() { hostType ->
    			host = "$hostType$h";
    			['root', 'var', 'tmp'].each() { vol ->
    				trace("sys.fs.free", nextInt(), ['dc':dc, 'host':host, 'vol': vol]);
    				trace("sys.fs.writes", nextLong(), ['dc':dc, 'host':host, 'vol': vol]);
    				trace("sys.fs.reads", nextLong(), ['dc':dc, 'host':host, 'vol': vol]);
    			}
    			if(host.equals("WebServer3")) {    			

    			} else if(hostType.equals("WebServer")) {
		    			['combined', 'idle', 'sys', 'wait'].each() { ctype ->    				
		    				for(c in 0..cpuCounts[hostType]) {
		    					trace("sys.cpu", nextInt(), ['dc':dc, 'host':host, 'cpu':c, 'type': ctype]);
		    				}
		    			}    			
    			} else {
		    			['combined', 'idle', 'irq', 'nice', 'softirq', 'stolen', 'sys', 'user', 'wait'].each() { ctype ->    				
		    				for(c in 0..cpuCounts[hostType]) {
		    					trace("sys.cpu", nextInt(), ['dc':dc, 'host':host, 'cpu':c, 'type': ctype]);
		    				}
		    			}    			
    			}
    		}
    	}
    	flush();
    	println "Completed $dc";


    }
    
    println "Done";
    Thread.currentThread().join(3000);
} finally {
	try { tsdbSocket.close(); } catch (e) {}
}
