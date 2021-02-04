/*

OSMAN MANTICI

 */



import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

//  http://localhost:8080/500

public class HttpServer implements Runnable{

    static final File path = new File("."); //when the code runs on cmd, this pathname must be ".."
                                                    // because cmd takes .. as parent directory, but intelliJ takes . as parent directory
//    static final int portNumber = 8080;
    static final  String successMessage = "HTTP/1.1 200 OK"; // defining HTTP success message
    static final String contentType = "text/html"; // defining content type since we just send HTML file it's text/html
    static final boolean log = true; // defining log flag in order to write log messages


    private final Socket socket;

    public HttpServer(Socket s) {
        socket = s;
    } // constructor method of HttpServer class

    public static void main(String[] args) {

        System.out.println("Enter a HttpServer socket port: ");
        Scanner scanner = new Scanner(System.in); // taking  socket port number from user
        int portNumber = scanner.nextInt();

        scanner.close();
        try {
            ServerSocket serverConnect = new ServerSocket(portNumber); // connecting server to entered port socket
            System.out.println("Server started.\nListening for connections on port : " + portNumber + " ...\n");

            // listens to given port until user stops execution
            while (true) {
                HttpServer myHttpServer = new HttpServer(serverConnect.accept()); // incoming requests are accepted

                if (log) { // printing out openning moment of the connection
                    System.out.println("Connection opened at: " + new Date() );
                }

                // creating new thread
                Thread thread = new Thread(myHttpServer);
                thread.start();
            }

        } catch (IOException e) { // printing error message and time if connection error occurs
            System.err.println("Connection error at: " + new Date() + "\nError message is: " + e.getMessage());
        }
    }

    @Override
    public void run() {//running thread

        BufferedReader httpRequest = null;
        PrintWriter responseWriter = null;
        BufferedOutputStream httpResponse = null;
        String requestedFile = null;

        int size;
        String body;

        try {
            // reading data come from client
            httpRequest = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // character output stream to client for messages, status, file length and date informations
            responseWriter = new PrintWriter(socket.getOutputStream());
            // binary output stream to client to keep requested data
            httpResponse = new BufferedOutputStream(socket.getOutputStream());

            String input = httpRequest.readLine(); // reading first line of the request which is request text
            StringTokenizer parse = new StringTokenizer(input); // tokenizing request
            String method = parse.nextToken().toUpperCase(); // getting the HTTP method of the client
            requestedFile = parse.nextToken(); // getting requested file name
            requestedFile = requestedFile.replace("/",""); // extracting slash(/) from requested file name

            if(!requestedFile.contains("/favicon.ico")){ // if requested file /favicon.ico
                                                        // skip that request
                try {
                    size = Integer.parseInt(requestedFile); // checking size of the requested file whether it contains just number or not
                } catch (NumberFormatException e) {
                    size = -1; // requested file is not just number, it may .../abc
                }

                if (!method.equals("GET")) { // if method is not GET
                    if (log) { // printing out not implemented method error info
                        System.out.println("501 Not Implemented : " + method + " method.");
                    }

                    File file = new File(path, "Error501.html");
                    int fileLength = (int) file.length();
                    //read error message content to return to client
                    byte[] fileData = readFileData(file, fileLength);

                    // sending error message response to client
                    sendResponse(responseWriter, httpResponse, fileLength, contentType, fileData, successMessage);

                    if (log) { // printing out logs
                        System.out.println("File " + requestedFile + " of type " + contentType + " returned");
                    }
                } else { // if method is GET
                    if (requestedFile.endsWith("/") || size == -1) { // if requested file is unsupported format, error 400 will be sent
                        requestedFile = "Error400.html";

                        File file = new File(path, requestedFile);
                        int fileLength = (int) file.length();

                        // read error message content to return to client
                        byte[] fileData = readFileData(file, fileLength);

                        // sending error message response to client
                        sendResponse(responseWriter, httpResponse, fileLength, contentType, fileData, successMessage);

                        if (log) { // printing out logs
                            System.out.println("File " + requestedFile + " of type " + contentType + " returned");
                        }
                    }
                    else if (size<100){ // if the size of the requested file is less than 100, error message LessThan100 will be sent
                        requestedFile = "LessThan100.html";
                        File file = new File(path, requestedFile);
                        int fileLength = (int) file.length();

                        // read error message content to return to client
                        byte[] fileData = readFileData(file, fileLength);

                        // sending error message response to client
                        sendResponse(responseWriter, httpResponse, fileLength, contentType, fileData, successMessage);

                        if (log) { // printing out logs
                            System.out.println("File " + requestedFile + " of type " + contentType + " returned");
                        }
                    }
                    else if(size>20000){ // if the size of the requested file is greater than 20000, error message GreaterThan20000 will be sent
                        requestedFile = "GreaterThan20000.html";
                        File file = new File(path, requestedFile);
                        int fileLength = (int) file.length();

                        // read error message content to return to client
                        byte[] fileData = readFileData(file, fileLength);

                        // sending error message response to client
                        sendResponse(responseWriter, httpResponse, fileLength, contentType, fileData, successMessage);

                        if (log) { // printing out logs
                            System.out.println("File " + requestedFile + " of type " + contentType + " returned");
                        }
                    }else{ // if the size of the requested file is appropriate
                        File newFile = new File(path,requestedFile+ ".html"); // naming requested file with html extension
                        try {
                            if(newFile.createNewFile()){ // creating requested file
                                System.out.println("File created: " + newFile.getName()); // printing out its name
                            } else { // printing out log, if file already exist
                                System.out.println("File already exists.");
                            }
                        } catch (IOException e) {
                            System.out.println("An error occurred.");
                            e.printStackTrace();
                        }

                        String line;
                        StringBuilder contentBuilder = new StringBuilder();
                        String htmlString;
                        int numOfAs = size - requestedFile.length() - 74; // calculating number of character 'a'

                        // opening template.html file and creating BufferedReader
                        BufferedReader templateReader = new BufferedReader(new FileReader(path + "\\Template.html"));

                        while ((line = templateReader.readLine()) != null) {  // reading Template.html file line-by-line
                            contentBuilder.append(line);  // appending each line to the StringBuilder
                        }
                        templateReader.close(); // closing BufferedReader

                        /* Template.html (74bytes long)
                        <HTML>
                        <HEAD>
                            <TITLE>I am -size- bytes long</TITLE>
                        </HEAD>
                        <BODY>-content-</BODY>
                        </HTML>
                         */

                        htmlString = contentBuilder.toString(); // keeping content of the Template.html file in the htmlString variable
                        htmlString = htmlString.replace("-size-", String.valueOf(size)); // updating title of the Template.html file

                        contentBuilder.setLength(0); // flushing contentBuilder
                        contentBuilder.append("a".repeat(Math.max(0, numOfAs))); // appending character 'a' in order to provide requested file size

                        body = contentBuilder.toString(); // converting content to string type and keeping it in the body variable

                        htmlString = htmlString.replace("-content-", body); // updating body of the Template.html file
                        BufferedWriter writeHtml = new BufferedWriter(new FileWriter(newFile)); // creating BufferedWriter
                            // in order to write updated Template.html file into the requested file (e.g. 500.html)
                        writeHtml.write(htmlString); // writing  updated Template.html into the requested file
                        writeHtml.close(); // closing bufferedwriter

                        int fileLength = (int) newFile.length(); // getting length of the requested file

                        // reading the content of the requested file
                        byte[] fileData = readFileData(newFile, fileLength);

                        // sending error message response to client
                        sendResponse(responseWriter, httpResponse, fileLength, contentType, fileData, successMessage);

                        if (log) { // printing out logs
                            System.out.println("File " + newFile + " of type " + contentType + " returned");
                        }
                    }

                }

            }

        } catch (FileNotFoundException fnfe) {
            try {
                fileNotFound(responseWriter, httpResponse, requestedFile);
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
            }

        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } finally {
            try { // closing all opened writers & readers etc.
                assert httpRequest != null;
                httpRequest.close();
                assert responseWriter != null;
                responseWriter.close();
                httpResponse.close();
                socket.close(); // we close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }

            if (log) {
                System.out.println("Connection closed.\n");
            }
        }

    }


    private byte[] readFileData(File file, int fileLength) throws IOException {
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
        /*
        this method
         */
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

    private void sendResponse(PrintWriter responseWriter, OutputStream httpResponse, int length, String content,
                              byte[] data, String message) throws IOException {
        //printing http headers
        responseWriter.println(message); // printing http message
        responseWriter.println("Server: Java HTTP Server" + "\nDate: " + new Date() + "\nContent-type: " + content +
                "\nContent-length: " + length); // printing server, date, content type and length information
        responseWriter.println(); // new line between headers and content
        responseWriter.flush(); // flush character output stream buffer

        httpResponse.write(data, 0, length); // writing data
        httpResponse.flush(); // flushing output stream
    }

}
