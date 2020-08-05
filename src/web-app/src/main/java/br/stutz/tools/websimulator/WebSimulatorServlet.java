package br.stutz.tools.websimulator;

import com.rabbitmq.client.*;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Servlet implementation class WebSimulatorServlet
 */
public class WebSimulatorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Random random = new Random();
    private DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    public WebSimulatorServlet() {
        httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long now = System.currentTimeMillis();

        String timeStr = request.getParameter("time");
        String mbytesStr = request.getParameter("mbytes");
        String msgbytesStr = request.getParameter("msgbytes");
        String msgCountStr = request.getParameter("msgcount");
        String countStr = request.getParameter("count");
        String randomStr = request.getParameter("random");
        String cacheTTLStr = request.getParameter("cacheTTL");
        String logStr = request.getParameter("log");
        String httpStatusStr = request.getParameter("http-status");

        int httpStatus = (httpStatusStr != null ? Integer.parseInt(httpStatusStr) : 200);
        int maxMbytes = (mbytesStr != null ? Integer.parseInt(mbytesStr) : 5000);
        int msgbytes = (msgbytesStr != null ? Integer.parseInt(msgbytesStr) : 2000);
        int msgcount = (msgCountStr != null ? Integer.parseInt(msgCountStr) : 100);
        long count = (countStr != null ? Long.parseLong(countStr) : 0L);
        long maxTime = (timeStr != null ? Long.parseLong(timeStr) : 0L);
        int cacheTTL = (cacheTTLStr != null ? Integer.parseInt(cacheTTLStr) : 0);
        boolean isRandom = "true".equals(randomStr);
        boolean isLog = "true".equals(logStr);

        long time = maxTime;
        int mbytes = maxMbytes;

        if (isRandom) {
            time = 1 + (long) (random.nextDouble() * (double) maxTime);
            mbytes = 1 + (int) (random.nextDouble() * (double) maxMbytes);
        }

        int nbytes = mbytes * 1000000;

        if (request.getRequestURI().endsWith("/cpu")) {

            if ((time == 0 && count == 0) || (time != 0 && count != 0)) {
                finishTest(request, response, (System.currentTimeMillis() - now), 400, "error", 0,
                        "Use GET '/cpu' with either 'time' or 'count' parameter. 'time' for consuming 100% cpu for that time, 'count' for counting from 0 up to cout. Ex.: http://localhost:8080/web-stress-simulator/cpu?count=10000 - will count from 0 to 10000", isLog);

            } else if (time > 0) {
                double value = 9.9;
                while (System.currentTimeMillis() <= (now + time)) {
                    value = value / 1.0000001;
                    value = value * 1.00000015;
                    if (value > Double.MAX_VALUE / 2) {
                        value = 1.0;
                    }
                }
                finishTest(request, response, (System.currentTimeMillis() - now), httpStatus, "success", cacheTTL, "value=" + value, isLog);

            } else if (count > 0) {
                //avoid jvm optimizations because of empty bodies
                double value = 9.9;
                for (long i = 0; i < count; i++) {
                    value = value / 1.0000001;
                    value = value * 1.00000015;
                    if (value > Double.MAX_VALUE / 2) {
                        value = 1.0;
                    }
                }
                finishTest(request, response, (System.currentTimeMillis() - now), httpStatus, "success", cacheTTL, "value=" + value, isLog);
            }

        } else if (request.getRequestURI().endsWith("/mem")) {
            byte[] b = new byte[nbytes];
            random.nextBytes(b);
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            finishTest(request, response, (System.currentTimeMillis() - now), httpStatus, "success", cacheTTL, "mem=" + b.length / 1000000 + "MB", isLog);

        } else if (request.getRequestURI().endsWith("/delay")) {
            if (time == 0) {
                throw new RuntimeException();
            }
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            finishTest(request, response, (System.currentTimeMillis() - now), httpStatus, "success", cacheTTL, null, isLog);

        } else if (request.getRequestURI().endsWith("/write")) {
            String fileName = "/tmp/" + System.currentTimeMillis() + ".tmp";
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(fileName, "rw");
                for (int i = 0; i < nbytes; i++) {
                    raf.write((char) (32 + (i % 94)));
                }
                if (isLog) {
                    System.out.println("create:" + fileName);
                }
            } catch (IOException e) {
                throw e;
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (Exception e2) {
                        System.out.println(e2.getMessage());
                    }
                }
            }

            finishTest(request, response, (System.currentTimeMillis() - now), httpStatus, "success", cacheTTL, null, isLog);
        } else if (request.getRequestURI().endsWith("/mq")) {
            try {
                putMessage(msgcount, msgbytes);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }

            finishTest(request, response, (System.currentTimeMillis() - now), httpStatus, "success", cacheTTL, null, isLog);
        } else if (request.getRequestURI().endsWith("/output")) {
            // generate random content to the outputstream
            long timeElapsed = System.currentTimeMillis() - now;
            response.setHeader("X-WebSimulator-TimeElapsedMillis", timeElapsed + "");
            response.setHeader("X-WebSimulator-Timestamp", System.currentTimeMillis() + "");
            response.setHeader("X-WebSimulator-Result", "");
            response.setHeader("X-WebSimulator-Info", "");
            response.setHeader("X-WebSimulator-SessionId", request.getRequestedSessionId());
            response.setHeader("X-WebSimulator-UserPrincipal", (request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "null"));
            response.setHeader("X-WebSimulator-RequestUrl", request.getRequestURL().toString());
            response.setHeader("X-WebSimulator-LocalAddr", request.getLocalAddr() + ":" + request.getLocalPort());
            response.setHeader("X-WebSimulator-RemoteClient", request.getRemoteAddr() + ":" + request.getRemotePort());
            response.setHeader("X-WebSimulator-ContentLength", nbytes + "");
            response.setContentType("text/plain");
            response.setContentLength(nbytes);
            response.setStatus(httpStatus);

            if (isLog) {
                String body = "{\n   \"requestUrl\":\"" + request.getRequestURL() + "\",\n   \"localAddr\":\"" + request.getLocalAddr() + ":" + request.getLocalPort() + "\",\n   \"result\":\"success\",\n   \"timeElapsedMillis\":\"" + timeElapsed + "\",\n   \"remoteClient\":\"" + request.getRemoteHost() + ":" + request.getRemotePort() + "\"\n   \"info\":\"\"\n   \"timestamp\":\"" + System.currentTimeMillis() + "\"\n   \"website\":\"\"\n}";
                System.out.println(body);
            }

            double timeBetweenBytes = 0;
            if (time > 0) {
                timeBetweenBytes = 1 / ((double) nbytes / (double) time);
            }

            ServletOutputStream responseOS = response.getOutputStream();
            for (int i = 0; i < nbytes; i++) {
                responseOS.print((char) (32 + (i % 94)));
                if (timeBetweenBytes > 0) {
                    //when using a controlled throughput, flush on each iteration
                    response.flushBuffer();
                    try {
                        Thread.sleep((long) timeBetweenBytes);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        } else {
            finishTest(request, response, (System.currentTimeMillis() - now), 400, "error", 0,
                    "Use GET '/cpu', '/mem', '/delay', '/output', '/write', '/mq' or POST '/input' with parameters 'time' [time in milliseconds], 'mbytes' [number of mbytes], 'msgbytes' [message size(byte)], 'msgcount' [message count], 'cacheSeconds' [cache TTL in seconds], 'log' [true to sysout] and/or 'random' [true or false for randomizing time and mbytes]. Ex.: http://localhost:8080/web-simulator/mem?mbytes=1&time=1000 - will allocate 1M and delay the request for 1s", isLog);
        }

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("POST Request URI: " + request.getRequestURI());
        long now = System.currentTimeMillis();

        String nbytesStr = request.getParameter("nbytes");
        int nbytes = -1;
        if (nbytesStr != null) {
            nbytes = Integer.parseInt(nbytesStr);
        }

        if (request.getRequestURI().endsWith("/input")) {
            if (true)
                throw new RuntimeException("not implemented yet");
            ServletInputStream sis = request.getInputStream();
            // TODO drain entire input stream and output size
            int inputLength = 0;

            // check for stream length
            if (nbytes != -1 && inputLength != nbytes) {
                response.setHeader("X-WebSimulator-Result", "failure");
                response.setHeader("X-WebSimulator-TimeElapsed", (System.currentTimeMillis() - now) + "");
                response.setStatus(400);
            } else {
                response.setHeader("X-WebSimulator-Result", "not-verified");
                response.setHeader("X-WebSimulator-TimeElapsed", (System.currentTimeMillis() - now) + "");
                response.setStatus(200);
            }
        }
    }

    private void finishTest(HttpServletRequest request, HttpServletResponse response, long timeElapsed, int statusCode, String result, int cacheTTL, String info, boolean isLog) {
        try {
            setCacheTTL(response, cacheTTL);
            response.setHeader("X-WebSimulator-TimeElapsedMillis", timeElapsed + "");
            response.setHeader("X-WebSimulator-Timestamp", System.currentTimeMillis() + "");
            response.setHeader("X-WebSimulator-Result", result);
            response.setHeader("X-WebSimulator-SessionId", request.getRequestedSessionId());
            response.setHeader("X-WebSimulator-UserPrincipal", (request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "null"));
            response.setHeader("X-WebSimulator-Info", (info != null ? info : ""));
            response.setHeader("X-WebSimulator-EnvInfo", (System.getenv("info") != null ? System.getenv("info") : ""));
            response.setHeader("X-WebSimulator-RequestUrl", request.getRequestURL().toString() + ("".equals(request.getQueryString()) ? "" : "?" + request.getQueryString()));
            response.setHeader("X-WebSimulator-LocalAddr", request.getLocalAddr() + ":" + request.getLocalPort());
            response.setHeader("X-WebSimulator-RemoteClient", request.getRemoteAddr() + ":" + request.getRemotePort());
            response.setContentType("application/json");
            response.setStatus(statusCode);
            ServletOutputStream responseOS = response.getOutputStream();
            String body = "{\n   \"requestUrl\":\"" + request.getRequestURL() + ("".equals(request.getQueryString()) ? "" : "?" + request.getQueryString()) + "\",\n   \"localAddr\":\"" + request.getLocalAddr() + ":" + request.getLocalPort() + "\",\n   \"result\":\"" + result + "\",\n   \"timeElapsedMillis\":\"" + timeElapsed + "\",\n   \"remoteClient\":\"" + request.getRemoteHost() + ":" + request.getRemotePort() + "\",\n   \"info\":\"" + (info != null ? info : "") + "\",\n   \"timestamp\":\"" + System.currentTimeMillis() + "\",\n   \"statusCode\":\"" + statusCode + "\"\n}";
            responseOS.print(body);
            if (isLog) {
                System.out.println(body);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCacheTTL(HttpServletResponse response, int cacheTTL) {
        Calendar cal = new GregorianCalendar();
        cal.roll(Calendar.SECOND, cacheTTL);

        if (cacheTTL == 0) {
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Expires", httpDateFormat.format(cal.getTime()));
        } else {
            response.setHeader("Cache-Control", "public, max-age=" + cacheTTL + ", must-revalidate");
            response.setHeader("Expires", httpDateFormat.format(cal.getTime()));
        }
    }

    /**
     * MQ操作
     *
     * @throws IOException
     * @throws TimeoutException
     */
    public void putMessage(int msgCount, int msgBytes) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        String userName = System.getProperty("mq.user_name");
        String password = System.getProperty("mq.pwd");
        String virtualHost = System.getProperty("mq.vhost");
        String hostName = System.getProperty("mq.host");
        String portNumber = System.getProperty("mq.port");
        String queueName = System.getProperty("mq.queue");
        String exchangeName = System.getProperty("mq.exchange");
        String routingKey = System.getProperty("mq.routing_key");

        userName = userName != null ? userName : "daoda";
        password = password != null ? password : "daoda";
        virtualHost = virtualHost != null ? virtualHost : "/";
        hostName = hostName != null ? hostName : "192.168.0.170";
        portNumber = portNumber != null ? portNumber : "5672";
        queueName = queueName != null ? queueName : "demo";
        exchangeName = exchangeName != null ? exchangeName : "daoda";
        routingKey = routingKey != null ? routingKey : queueName;

        factory.setUsername(userName);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setHost(hostName);
        factory.setPort(Integer.parseInt(portNumber));

        Connection conn = factory.newConnection();

        Channel channel = conn.createChannel();

        channel.exchangeDeclare(exchangeName, "direct", true);
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, exchangeName, routingKey);

        // put msg
        for (int i = 0; i < msgCount; i++) {
            byte[] msg = new byte[msgBytes];
            random.nextBytes(msg);
            channel.basicPublish(exchangeName, routingKey,
                    new AMQP.BasicProperties.Builder()
                            .expiration("60000")
                            .build(),
                    msg);
        }

        //get msg
        boolean autoAck = false;
        for (int i = 0; i < msgCount / 2; i++) {
            GetResponse response = channel.basicGet(queueName, autoAck);
            if (response == null) {
                // No message retrieved.
            } else {
//                AMQP.BasicProperties props = response.getProps();
                byte[] body = response.getBody();
                long deliveryTag = response.getEnvelope().getDeliveryTag();

                String fileName = "/tmp/" + deliveryTag + ".msg";
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(fileName, "rw");
                    raf.write(body);
                } finally {
                    if (raf != null) {
                        raf.close();
                    }
                }

                channel.basicAck(deliveryTag, false); // acknowledge receipt of the message
            }
        }

        channel.close();
        conn.close();
    }
}
