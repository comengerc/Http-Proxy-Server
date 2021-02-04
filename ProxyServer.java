/*

OSMAN MANTICI

 */


import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class ProxyServer {
    static final String proxyIP = "127.0.0.1";
    static final int httpPort = 8080;
    static final int proxyPort = 8888; // defining proxy server port to be listened

    static final File path = new File("."); //when the code runs on cmd, this pathname must be ".."
    // because cmd takes .. as parent directory, but intelliJ takes . as parent directory
    static final  String successMessage = "HTTP/1.1 200 OK"; // defining HTTP success message
    static final String contentType = "text/html"; // defining content type since we just send HTML file it's text/html
    static final boolean log = true; // defining log flag in order to write log messages
    static File cache = new File(path + "\\cache.txt"); // defining cache file
    static List<String> cachedRequests = new ArrayList<String>(); // keeping cache in an ArrayList

    public static void main(String[] args) {

//        System.out.println("Enter the same HttpServer socket port for ProxyServer: ");
//        Scanner scanner = new Scanner(System.in); // taking  socket port number from user
//        int httpPort = scanner.nextInt();
//
//        scanner.close();

        try {

            System.out.println("Proxy Server started.\nProxy address: " + proxyIP + ":" + httpPort + " on proxy port: " + proxyPort);
            if(!cache.exists()){ // if cache.txt does not exist
                cache.createNewFile(); // create cache.txt
            }
            startProxyServer();

        } catch (Exception e) {
            System.err.println(e);
        }
    }


    public static void startProxyServer()
            throws IOException {

        // Create a ServerSocket to listen for connections with
        ServerSocket serverConnect = new ServerSocket(proxyPort);  // connecting server to entered port socket

        final byte[] byteRequest = new byte[1024];
        byte[] byteResponse = new byte[4096];

        while (true) {
            Socket client = null;
            Socket webServer = null;
            try {
                // Wait for a connection on the local port
                client = serverConnect.accept(); // incoming requests are accepted

                // get client streams
                final InputStream isFromClientToProxy = client.getInputStream(); // Input Stream From Client To Proxy
                final OutputStream osFromProxyToClient = client.getOutputStream(); // Output Stream From Proxy To Client


                try {
                    webServer = new Socket(proxyIP, httpPort); // defining socket from client to server
                } catch (IOException e) { // if http server could not be reachable or is not running
                    PrintWriter responseWriter = new PrintWriter(osFromProxyToClient);
                    String requestedFile = "Error404.html";

                    File file = new File(path, requestedFile);
                    int fileLength = (int) file.length();

                    // reading the content of the requested file
                    byte[] fileData = readFileData(file, fileLength);

                    // sending error message response to client
                    sendResponse(responseWriter, osFromProxyToClient, fileLength, contentType, fileData, successMessage);

                    if (log) { // printing out logs
                        System.out.println("File " + requestedFile + " of type " + contentType + " returned");
                    }
                    continue;
                }

                // Get webServer/HttpServer streams.
                final InputStream isFromServerToProxy = webServer.getInputStream(); // Input Stream From Server To Proxy
                final OutputStream osFormProxyToServer = webServer.getOutputStream(); // Output Stream From Proxy To Server

                // this thread reads the client's requests and pass them to the webServer
                Thread thread = new Thread() {
                    public void run() {
                        int bytesRead;
                        int is414result=0;
                        String requestedFile;
                        String methodName;
                        String line;
                        int size=-1;
                        try {
                            while ((bytesRead = isFromClientToProxy.read(byteRequest)) != -1) { // getting request from client
                                line = requestedLine(byteRequest); // getting request line
 //                               System.out.println(line);
//                                line = lineToServer(line);
                                methodName = getMethodName(line); // extracting method name info

                                if(methodName.equals("GET")){ // if method is GET
                                    is414result = is414(byteRequest); // is414() method called
                                    System.out.println(line);//kalakcak*********************************************
                                    line = lineToServer(line);
                                    size = getSize(line); // extracting requested file size info

                                }

                                if(is414result == 1){ // is414result is 1 when requested file size greater than 9999
                                    // Error message is request uri is too long
                                    PrintWriter responseWriter = new PrintWriter(osFromProxyToClient);
                                    requestedFile = "Error414.html";

                                    File file = new File(path, requestedFile);
                                    int fileLength = (int) file.length();

                                    // reading the content of the requested file
                                    byte[] fileData = readFileData(file, fileLength);

                                    // sending error message response to client
                                    sendResponse(responseWriter, osFromProxyToClient, fileLength, contentType, fileData, successMessage);

                                    if (log) { // printing out logs
                                        System.out.println("File " + requestedFile + " of type " + contentType + " returned");
                                    }

                                }else if(is414result == 2){ // is414result is 2 when request is cached
                                    PrintWriter responseWriter = new PrintWriter(osFromProxyToClient);

                                    requestedFile = requestedFileName(line); // extracting file name info
                                    requestedFile += ".html"; // adding file extension
                                    File file = new File(path, requestedFile);
                                    int fileLength = (int) file.length();

                                    // reading the content of the requested file
                                    byte[] fileData = readFileData(file, fileLength);

                                    if (log) { // printing out logs
                                        System.out.println("File " + file.getName() + " is returned from cache" );
                                    }

                                    // sending requested file data as response to client
                                    sendResponse(responseWriter, osFromProxyToClient, fileLength, contentType, fileData, successMessage);

                                    if (log) { // printing out logs
                                        System.out.println("File " + file.getName() + " of type " + contentType + " returned");
                                    }
                                }
                                else{ // if is414() is 0 means this is new request
                                    if(methodName.equals("GET")){
                                        System.out.println(size);
                                        if(size >= 100 && size <20000){ // if request meets requirements

                                            addToCache(line); // it will be added to cache
                                            if (log) { // printing out logs
                                                System.out.println(line + " is added to the cache.");
                                            }
                                        }
                                        if (log) { // printing out logs
                                            System.out.println("Request redirected to the HTTPServer. \nRequest: " + line + "\n");
                                        }
                                        osFormProxyToServer.write(requestSentToServer(byteRequest), 0, bytesRead); // passing request
                                        // from Client to HttpServer
                                        osFormProxyToServer.flush(); // flushing output stream
                                    }

                                }
                            }
                        } catch (IOException e) {
                        }

                        // if client closes the connection we close our connection too
                        try {
                            osFormProxyToServer.close();
                        } catch (IOException e) {
                        }
                    }
                };

                thread.start();

                // passing requested file(response) from HttpServer to client
                int bytesRead;
                try {
                    while ((bytesRead = isFromServerToProxy.read(byteResponse)) != -1) {
                        if (log) { // printing out logs
                            System.out.println("File returned");
                        }
                        osFromProxyToClient.write(byteResponse, 0, bytesRead);
                        osFromProxyToClient.flush();
                    }
                } catch (IOException e) {
                }

                // if HttpServer closes its connection we close our connection too
                osFromProxyToClient.close();
            } catch (IOException e) {
                System.err.println(e);
            } finally {
                try {// closing all opened sockets
                    if (webServer != null)
                        webServer.close();
                    if (client != null)
                        client.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static byte[] readFileData(File file, int fileLength) throws IOException {
        /*
        this method reads file byte-by-byte
         */
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    private void fileNotFound(PrintWriter responseWriter, OutputStream httpResponse, String requestedFile) throws IOException {

        File file = new File(path, "Error404.html");
        int fileLength = (int) file.length();

        // read error message content to return to client
        byte[] fileData = readFileData(file, fileLength);
        String message = "HTTP/1.1 404 File Not Found";

        // sending error message response to client
        sendResponse(responseWriter, httpResponse, fileLength, contentType, fileData, message);

        if (log) { // printing out logs
            System.out.println("File " + requestedFile + " not found");
        }
    }

    private static void sendResponse(PrintWriter responseWriter, OutputStream httpResponse, int length, String content,
                                     byte[] data, String message) throws IOException {
        //printing http headers
        responseWriter.println(message); // printing http message
        responseWriter.println("Server: Java HTTP Server" + "\nDate: " + new Date() + "\nContent-type: " + content +
                "\nContent-length: " + length); // printing server, date, content type and length information
        responseWriter.println(); // new line between headers and content
        responseWriter.flush(); // flush character output stream buffer

        httpResponse.write(data, 0, length); // writing data
    }

    public static boolean inCache(String request){
        /*
        this method returns a boolean value whether request cached or not
         */
        if(cachedRequests.contains(request)){
            return true;
        }else{
            return false;
        }
    }

    public static void addToCache(String request) throws IOException {
        /*
        this method writes new request to the cache.txt
        and adds it to the cachedRequests ArrayList, so request is cached here
         */
        cachedRequests.add(request);
        FileWriter fw = new FileWriter(cache, true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(request);
        bw.newLine();
        bw.close();
    }

    public static String requestedLine(byte [] byteRequest) {
        /*
        this method returns requested line
         */
        String stringContentOfByteRequest = new String(byteRequest);
        String requestLine = stringContentOfByteRequest.split("\\r?\\n")[0];
        return requestLine;
    }

    public static String getMethodName(String requestLine){
        /*
        this method extracts method name from requestLine
        and returns method name
         */
        StringTokenizer parse = new StringTokenizer(requestLine);
        String method = parse.nextToken().toUpperCase();
        return method;
    }

    public static String requestedFileName(String requestLine){
        /*
        this method extracts file name from requestLine
        and returns file name
         */
        StringTokenizer parse = new StringTokenizer(requestLine);
        String method = parse.nextToken().toUpperCase();
        String fileName = parse.nextToken();
        fileName = fileName.replace("/","");
        return fileName;
    }

    public static String lineToServer(String line){
        String[] requestline = line.split(" ");
        String[] lineToServer =requestline[1].split("/");
        requestline[1] = "/"+lineToServer[3];
        String newline="";
        for(String s: requestline){
            newline += " " + s;
        }
        newline = newline.substring(1);
        return newline;
    }

    public static int getSize(String line){
        /*
        this method extracts file size from request
        and returns file size
         */
        StringTokenizer parse = new StringTokenizer(line);
        String method = parse.nextToken().toUpperCase();
        String request = parse.nextToken();
        request = request.replace("/","");

        int size;
        try {
            size = Integer.parseInt(request); // checking size of the requested file whether it contains just number or not
        } catch (NumberFormatException e) {
            size = -1; // requested file is not just number, it may .../abc
        }
        return size;
    }

    public static int is414(byte[] byteRequest){
        /*
        this method takes byte version of request and
        if it returns 1 it means requested uri is too long
        if it returns 2 request already cached
        if it returns 0 request passed to HttpServer
         */
        String stringContentOfByteRequest = new String(byteRequest);
        String requestLine = stringContentOfByteRequest.split("\\r?\\n")[0];
        requestLine = lineToServer(requestLine);
        StringTokenizer parse = new StringTokenizer(requestLine);
        String method = parse.nextToken().toUpperCase();
        String request = parse.nextToken();
        request = request.replace("/","");
        int returnValue = 0;

        int size;
        try {
            size = Integer.parseInt(request); // checking size of the requested file whether it contains just number or not
        } catch (NumberFormatException e) {
            size = -1; // requested file is not just number, it may .../abc
        }

        if(method.equals("GET")){
            if(size>9999) {
                returnValue = 1; // if error 414
            }
            else if(inCache(requestLine)){
                returnValue =  2; // if request in cache
            }
        }else{
            returnValue = 0;
        }

        return returnValue;
    }

    public static byte[] requestSentToServer(byte [] byteRequest) {
        /*
        this method returns requested line
         */
        String stringContentOfByteRequest = new String(byteRequest);
        String requestLine = stringContentOfByteRequest.split("\\r?\\n")[0];
    //    System.out.println("method içi requestSentToServer: "  + requestLine);
        String requestLineResult = lineToServer(requestLine);
        stringContentOfByteRequest = stringContentOfByteRequest.replace(requestLine,requestLineResult);

        return stringContentOfByteRequest.getBytes(StandardCharsets.UTF_8);
    }

}
