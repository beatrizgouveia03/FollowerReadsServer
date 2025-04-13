package panda.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class HandleRequest implements Runnable {
    private Socket socket;
    private AtomicInteger leaderPort;
    private CopyOnWriteArrayList<Integer> serverPorts;
    private HashMap<Integer, String> followerRegions;

    public HandleRequest(Socket socket, AtomicInteger leaderPort, CopyOnWriteArrayList<Integer> serverPorts, HashMap<Integer, String> followerRegions) {
        this.socket = socket;
        this.leaderPort = leaderPort;
        this.serverPorts = serverPorts;        
        this.followerRegions = followerRegions;
    }

    @Override
    public void run() {
        System.out.println("Handling request from " + socket.getInetAddress());
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String headerLine = in.readLine(); 
            String response = null;
			
            StringTokenizer st = new StringTokenizer(headerLine);
            String httpMethod = st.nextToken();
            String inputLine;
            StringBuilder requestBuilder = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                requestBuilder.append(inputLine).append("\n");
            }

            switch (httpMethod) {
                case "INIT":
                    String type = st.nextToken();
                    String region = st.nextToken();
                    int port = Integer.parseInt(st.nextToken());
                    addServer(type, region, port);

                    if(type.equals("LEADER") && serverPorts.size() > 0){
                        for(int server : serverPorts){
                            forwardRequest("INIT;LEADER;"+port, server);
                        }
                    } else if(type.equals("FOLLOWER") && leaderPort.get() != 0){
                        forwardRequest("INIT;FOLLOWER;"+port, leaderPort.get());
                    }
                    sendResponse(200, "Server added successfully");
                    in.close();
                    socket.close();
                    return;
                case "DELETE":
                    response = forwardRequest(requestBuilder.toString(), leaderPort.get());
                    break;
                case "POST":
                    response = forwardRequest(requestBuilder.toString(), leaderPort.get());
                    break;
                case "PUT":
                    response = forwardRequest(requestBuilder.toString(), leaderPort.get());
                    break;
                case "GET":
                    String regionToGet = st.nextToken();
                    int closestServerPort = locateClosestServer(regionToGet);
                    if(closestServerPort == -1){
                        sendResponse(500, "No server available");
                        in.close();
                        socket.close();
                        return;
                    }
                    response = forwardRequest(requestBuilder.toString(), closestServerPort);
                    break;            
                default:
                    System.out.println("Operation Unknown");
                    sendResponse(405, "Method Not Allowed");
                    in.close();
                    socket.close();
                    return;
            }
				
			try{
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(response);
                out.close();
            } catch (IOException e) {
                System.out.println("Error sending response");
                e.printStackTrace();
            }
            
            in.close();
            socket.close();
				
			} catch (Exception e) {
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

    private int locateClosestServer(String region){
        if(serverPorts.size() == 0) {
            if(leaderPort.get() == 0){
                return -1;
            }

            return leaderPort.get();
        }

        for(int port : serverPorts){
            if(followerRegions.get(port).equals(region)){
                return port;
            } 
        }

        return serverPorts.get(0);
    }

    private void addServer(String type, String region, int port){
        if(type.equals("FOLLOWER")){
            serverPorts.add(port);
            followerRegions.put(port, region);
            System.out.println("Follower server added in region " + region + " on port " + port);
        } else if(type.equals("LEADER")){
            leaderPort.set(port);
            System.out.println("Leader server added on port " + leaderPort);
        } else {
            System.out.println("Unknown server type");
        }
    }

    private String forwardRequest(String request, int port){
        try {
            System.out.println("Forwarding request to port " + port);
            Socket forwardSocket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(forwardSocket.getOutputStream(), true);
            out.println(request);
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(forwardSocket.getInputStream()));
            String response = in.readLine();
            in.close();
            out.close();
            forwardSocket.close();
            System.out.println("Response received: " + response);
            return response;
            } catch (IOException e) {
                System.out.println("Error forwarding request");
                e.printStackTrace();
                return null;
            }
    }
}
