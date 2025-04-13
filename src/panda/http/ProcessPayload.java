package panda.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessPayload implements Runnable{
    private Socket socket;
    private int localPort;
    private BooksDatabase booksDB;
    private AtomicInteger leaderPort;
    private CopyOnWriteArrayList<Integer> followerPorts;
    private String serverType;

    public ProcessPayload(String serverType, int localPort, Socket socket, AtomicInteger leaderPort, CopyOnWriteArrayList<Integer> followerPorts, BooksDatabase booksDB) {
        this.socket = socket;
        this.booksDB = booksDB;
        this.localPort = localPort;
        this.leaderPort = leaderPort;
        this.serverType = serverType;
        this.followerPorts = followerPorts;        
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String headerLine = in.readLine(); 
            String request = null;
           
            StringTokenizer st = new StringTokenizer(headerLine);
            String httpMethod = st.nextToken();
            in.readLine();

            switch(httpMethod){
                case "HEARTBEAT":
                    sendResponse(200, "Heartbeat received");                   
                    break;
                case "INIT":                
                    String body = in.readLine();
                    request = body;
                    st = new StringTokenizer(request, ";");
                    String type = st.nextToken();

                    if (serverType.equals("LEADER") && type.equals("FOLLOWER")) {                        
                        int port = Integer.parseInt(st.nextToken());
                        followerPorts.add(port);
                        sendAliveMessage(port);
                    } else if(serverType.equals("FOLLOWER") && type.equals("LEADER")){  
                        int port = Integer.parseInt(st.nextToken());                      
                        leaderPort.set(port);                        
                        sendAliveMessage(port);                        
                    } else {
                        sendResponse(500, "Invalid server type");
                        break;
                    }          
                    
                    sendResponse(200, "Server added successfully");
                    break;
                case "ALIVE":
                    String body2 = in.readLine();
                    request = body2;
                    st = new StringTokenizer(request, ";");
                    String type2 = st.nextToken();
                    if (serverType.equals("LEADER") && type2.equals("FOLLOWER")) {                        
                        int port = Integer.parseInt(st.nextToken());
                        followerPorts.add(port);                        
                    } else if(serverType.equals("FOLLOWER") && type2.equals("LEADER")){  
                        int port = Integer.parseInt(st.nextToken());                      
                        leaderPort.set(port);                                               
                    } else {
                        sendResponse(500, "Invalid server type");
                        break;
                    } 

                    sendResponse(200, "Server added successfully");
                    break;                    
                case "DELETE":
                    if (serverType.equals("FOLLOWER")) {
                        sendResponse(450, "ERROR;Only leader can delete books");
                        break;
                    }
                    sendResponse(200, "Book deleted");
                    broadcastToFollowers(request);
                    break;
                case "POST":
                    if (serverType.equals("FOLLOWER")) {
                        sendResponse(450, "ERROR;Only leader can add books");
                        break;
                    }
                    sendResponse(200, "Book added");
                    broadcastToFollowers(request);
                    break;
                case "PUT":
                    if (serverType.equals("FOLLOWER")) {
                        sendResponse(450, "ERROR;Only leader can update books");
                        break;
                    }
                    sendResponse(200, "Book updated");
                    broadcastToFollowers(request);
                    break;
                case "GET":  
                    @SuppressWarnings("unused") String region = st.nextToken();              
                    String bookToSearch = st.nextToken();
                    int result = booksDB.getBookCopies(bookToSearch);
                    if (result != -1) {
                        sendResponse(200, "Book found: " + bookToSearch + " with " + result + " copies");
                    } else {
                        sendResponse(200, "Book not found: " + bookToSearch);
                    }
                    break;
                default:
                    sendResponse(405, "Method Not Allowed");
                    break;
            }

            in.close();
            socket.close();
				
			} catch (IOException e) {
				e.printStackTrace();
		}
    }

    private void sendResponse(int statusCode, String message) {
        String statusLine;
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            if(statusCode == 405){
                statusLine = "HTTP/1.0 405 Method Not Allowed\r\n";
                out.writeBytes(statusLine);
                out.writeBytes("\r\n");
            } else if(statusCode == 200){
                statusLine = "HTTP/1.0 200 OK\r\n";
                out.writeBytes(statusLine);
                out.writeBytes("Content-Type: text/plain\r\n");
                out.writeBytes("Content-Length: " + message.length() + "\r\n");
                out.writeBytes("\r\n");
                out.writeBytes(message);
            } else {
                statusLine = "HTTP/1.0 500 Internal Server Error\r\n";
                out.writeBytes(statusLine);
                out.writeBytes("\r\n");
            }

            out.close();
        } catch (IOException e) {
            System.out.println("Error sending error response");
            e.printStackTrace();
        }
    }

    private void sendAliveMessage(int port) {
        try ( Socket socket = new Socket("localhost", port);
        OutputStream out = socket.getOutputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ){           
            String request = serverType+";"+localPort;
            out.write("ALIVE /default\r\n".getBytes());
            out.write("User-Agent: Mozilla/5.0\r\n".getBytes());
            out.write("Content-Type: text/plain\r\n".getBytes());
            out.write(("Content-Length: " + request.length() + "\r\n").getBytes());
            out.write("\r\n".getBytes());
            out.write(request.getBytes());
            out.flush();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error sending alive message to follower on port " + port + ": " + e.getMessage());
        }
    }

    private void broadcastToFollowers(String message) {
        for (int port : followerPorts) {
            try {
                Socket socket = new Socket("localhost", port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(message);
                out.flush();
                out.close();
                socket.close();
            } catch (IOException e) {
                System.err.println("Error broadcasting to follower on port " + port + ": " + e.getMessage());
            }
        }
    }
    
}
