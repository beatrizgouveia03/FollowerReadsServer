package panda;

import java.io.BufferedReader;
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
			String request = in.readLine(); 
            String response = null;
			System.out.println("Operação recebida: "+request);
			
            StringTokenizer st = new StringTokenizer(request, ";");
            String OP = st.nextToken();

            if(OP.equals("INIT")){
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
                response = "INIT_OK;";
            } else if(OP.equals("ADD_BOOK")){
                response = forwardRequest(request, leaderPort.get());
            } else if(OP.equals("DELETE_BOOK")){                
                response = forwardRequest(request,  leaderPort.get());
            }  else if(OP.equals("UPDATE_BOOK")){                
                response = forwardRequest(request,  leaderPort.get());
            } else if(OP.equals("SEARCH_BOOK")){                 
                String region = st.nextToken(); 
                int port = locateClosestServer(region);           
                response = forwardRequest(request, port);
            } else {
                System.out.println("Operation Unknown");
            }
				
			try{
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(response);
                out.close();
            } catch (IOException e) {
                System.out.println("Error sending response");
                e.printStackTrace();
            }
            
            System.out.println("Response sent: " + response);
            in.close();
            socket.close();
				
			} catch (Exception e) {
				e.printStackTrace();
		}
    }

    int locateClosestServer(String region){
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

    void addServer(String type, String region, int port){
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

    String forwardRequest(String request, int port){
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
