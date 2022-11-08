package com.example.network02;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;


/**
 * @program: network
 * @description: 一个简单的Web服务器
 * @author: zys
 * @create: 2022-10-23 18:51
 *
 * 压力测试 ab -c 1000 -n 100000 http://localhost:8081/web1.html
 *
 **/


public class SimpleHttpServer {

    /**
     * 处理HttpRequest的线程类
    * */
    static ThreadPool<HttpRequestHandler> threadPool = new DefaultThreadPool<HttpRequestHandler>(5);


    /**
     *  SimpleHttpServer的根路径
     * */
    static String basePath = "E:/IdeaProjects/network02/src/main/resources/static";

    /**
     *  服务端Socket
     * */
    static ServerSocket serverSocket;

    /**
     *  服务端监听端口
     * */
    static int port = 8081;


    public static void setPort(int port) {
        if (port > 0) {
            SimpleHttpServer.port = port;
        }
    }


    public static void setBasePath(String basePath) {
        if (basePath != null && new File(basePath).exists() && new File(basePath).isDirectory()) {
            SimpleHttpServer.basePath = basePath;
        }
    }

    // 启动SimpleHttpServer
    public static void start() throws Exception {
        serverSocket = new ServerSocket(port);
        Socket socket = null;
        while ((socket = serverSocket.accept()) != null) {
            // 接收一个客户端的Socket，生成一个HttpRequestHandler， 放入线程池中执行
            threadPool.execute(new HttpRequestHandler(socket));
        }
        serverSocket.close();
    }


    public static void main(String[] args) throws Exception {
        SimpleHttpServer.start();
    }



    static class HttpRequestHandler implements Runnable{

        private Socket socket;

        public HttpRequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            InputStream in = null;
            OutputStream out = null;
            InputStream bis = null;

            //接收客户端的请求
            try {
                in = socket.getInputStream();
                byte[] bytes = new byte[1024];
                //一般来说1024个字节大小存肯定够了，但如果需要接收一长串的话可以 s += new String(bytes,0,len)
                int len = in.read(bytes);
                String header = new String(bytes, 0, len);
                // 由相对路径计算出绝对路径
                String filePath = basePath + header.split(" ")[1];

                File file = new File(filePath);


                //根据请求内容向客户端返回相应的文件
                out = socket.getOutputStream();

                // 如果资源不存在，返回404 Not Found
                if ( !file.exists()) {
                    out.write("HTTP/1.1 404 Not Found".getBytes());
                    out.write("Server: Molly".getBytes());
                    out.write("".getBytes());
                } else {
                    bis = new FileInputStream(file);
                    out.write("HTTP/1.1 200 OK\r\n".getBytes());
                    out.write("Content-Type:text/html\r\n".getBytes());
                    out.write("\r\n".getBytes());
                    byte[] buffer=new byte[1024];
                    len=-1;
                    while ((len = bis.read(buffer)) != -1) {
                        out.write(buffer,0,len);
                    }

                }

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                close(socket,out,in,bis);
            }


//            String line = null;
//            BufferedReader br = null;
//            BufferedReader reader = null;
//            PrintWriter out = null;
//            InputStream in = null;
//
//            try{
//                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                String header = reader.readLine();
//                // 由相对路径计算出绝对路径
//                String filePath = basePath + header.split(" ")[1];
//                System.out.println("请求路径 ：=== " + filePath);
//                out = new PrintWriter(socket.getOutputStream());
//
//                // 如果资源不存在，返回404 Not Found
//                if (!new File(filePath).exists()) {
//                    System.out.println(" 文件不存在 ");
//                    out.println("HTTP/1.1 404 Not Found");
//                    out.println("Server: Molly");
//                    out.println("");
//                }
//                } // 如果读取的资源后缀是jpg或者ico，则读取资源并输出
//                else if (filePath.endsWith("png") || filePath.endsWith("ico")) {
//
//                    System.out.println( "png" );
//                    in = new FileInputStream(filePath);
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    int i = 0;
//                    while ((i = in.read()) != -1) {
//                        baos.write(i);
//                    }
//                    byte[] array = baos.toByteArray();
//                    out.println("HTTP/1.1 200 OK");
//                    out.println("Server: Molly");
//                    out.println("Content-Type: image/jpeg");
//                    out.println("Content-Length: " + array.length);
//                    out.println("");
//                    // 对字节数组Base64编码
//                     BASE64Encoder encoder = new BASE64Encoder();
//                     String encode = encoder.encode(array);
////                    socket.getOutputStream().write(array,0,array.length);
//
//                    out.write(encode);
//                } else {
//                    br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
//                    out.println("HTTP/1.1 200 OK");
//                    out.println("Server: Molly");
//                    out.println("Content-Type: text/html; charset=UTF-8");
//                    out.println("");
//                    while ((line = br.readLine()) != null) {
//                        System.out.println("line === " + line);
//                        out.println(line);
//                    }
//                }
//                out.flush();
//            } catch (Exception ex) {
//                out.println("HTTP/1.1 500");
//                out.println("");
//                out.flush();
//            } finally {
//                close(br, in, reader, out, socket);
//            }
//
//        }



        }
        // 关闭流或者socket
        private static void close(Closeable... closeables){
            if (closeables!=null) {
                for (Closeable closeable : closeables) {
                    try{
                        if (closeable==null){
                            continue;
                        }
                        closeable.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

}
