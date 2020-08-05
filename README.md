# web-stress-simulator

[<img src="https://img.shields.io/docker/pulls/flaviostutz/web-stress-simulator"/>](https://hub.docker.com/r/flaviostutz/web-stress-simulator)
[<img src="https://img.shields.io/docker/automated/flaviostutz/web-stress-simulator"/>](https://hub.docker.com/r/flaviostutz/web-stress-simulator)

Application for simulating various conditions of an Web Infrastructure under stress (high load on cpus, high memory usage, high network throughput, high backend delays, unusual HTTP return codes). 

Usage:
* Run the container using "docker run -p 8080:8080 flaviostutz/web-stress-simulator" OR Install the WAR package on any Java Servlet Container
* Perform HTTP GET calls in the form "/web-stress-simulator-1.0.0/[operation]?[parameters]"
* Where:

[operation] may be:
* "cpu": use all available cpu resources for "time" milliseconds, so the Thread serving the request will cause a high load on the server (affects a single core per request). The server will be performing basic division and multiplication of double numbers in order to cause the high load.
* "mem": allocate "mbytes" on an internal array and wait for "time" milliseconds before releasing resources then return request info. A new array is allocated and filled with random values on each request.
* "delay": delay request for "time" milliseconds than return request info. This is based on Thread.sleep(time);
* "output": generates "mbytes" of text data as response. Mime type is "text/plain".

[parameters] may be:
* "time": time in milliseconds - minimum time of the request
* "mbytes": number of MB - memory allocation or output generation
* "random": true or false - evenly randomizes "time" and "mbytes" so that each request will have a different value whose max value are the parameters "time" and "mbytes"
* "log": outputs basic information to System.out on each request
* "cacheTTL": time in seconds - time in seconds for validity of the resource in client cache. This setups "Cache-Control" on HTTP response headers
* "http-status": http status code - http status code to be returned on the response for the request
* "msgbytes": message size(byte)
* "msgcount": message count

Examples:
* http://localhost:8080/web-stress-simulator-1.0.0/cpu?time=1000 - causes a request to last one second. During that time it will try to use 100% of a CPU core.
* http://localhost:8080/web-stress-simulator-1.0.0/mem?mbytes=10&time=5000 - causes a request to last five seconds. During that time 10MB of memory will be allocated
* http://localhost:8080/web-stress-simulator-1.0.0/delay?time=30000 - causes a request to last 30 seconds. During that time no CPU resources are spent, simulating slow or hung backend calls
* http://localhost:8080/web-stress-simulator-1.0.0/delay?time=3000&random=true - causes a request to last, randomically, from 0 to 3 seconds
* http://localhost:8080/web-stress-simulator-1.0.0/write?mbytes=3 - write 3MB tmp file in /tmp 
* http://localhost:8080/web-stress-simulator-1.0.0/output?mbytes=3 - causes 3MB of random text data to be returned from the server
* http://localhost:8080/web-stress-simulator-1.0.0/output?mbytes=1&time=10000 - the same as above, but now it will generate 1MB of data with a data rate of 100KB/s so that it will last 60 seconds to output the whole data. It's usefull to test network appliances under slow connections conditions
    * 这里`mbytes`转换为`KB`后必须小于`time`(毫秒)
* http://localhost:8080/web-stress-simulator-1.0.0/delay?time=3000&random=true&http-status=500 - causes a request with random duration (0-3s) to return a response indicating an internal error
* http://localhost:8080/web-stress-simulator-1.0.0/mq?msgbytes=1000&msgcount=10 - send 10 message, every message 1000 byte

Tips:
* Use JMeter in order to simulate various different workloads on your web infrastructure by varying the URL parameters as above
* Perform stress tests and measure the infrastructure reaction:
  * Create tests in JMeter that explores various web invocations situations (slow requests, big requests, stuck requests, fast and small requests, lots of users, high users ramp, lots of different URLs, attached files etc). Be sure to create assertions for each request in order to validate the server responses. This is important for you to know what is going right or wrong during stress.
  * Start JMeter tests. Perform some "monkey caos" (thanks, Netflix...) and verify assertions in your test in order to learn how your infrastructure is handling diversity.
    * Turn off a server node, Add a new node, slow down connection bandwidth, turn off server networking and restore again, fill up the entire disk of a node, kill available RAM, starve a node CPU
* Have fun creating CAOS!
