package panda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;

public class HandleRequest implements Runnable {
    private Socket socket;
    private int leaderPort;
    private CopyOnWriteArrayList<Integer> serverPorts;
    private HashMap<Integer, String> followerRegions;

    public HandleRequest(Socket socket, int leaderPort, CopyOnWriteArrayList<Integer> serverPorts, HashMap<Integer, String> followerRegions) {
        this.socket = socket;
        this.leaderPort = leaderPort;
        this.serverPorts = serverPorts;        
        this.followerRegions = followerRegions;
    }

    @Override
    public void run() {
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
                response = "INIT_OK;";
            } else if(OP.equals("ADD_BOOK")){
                response = forwardRequest(request, leaderPort);
            } else if(OP.equals("READ_BOOK")){                
                response = forwardRequest(request, serverPorts.get(0));
            } else {
                System.out.println("Operation Unknown");
            }
				
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(response);
            out.close();
            in.close();
            socket.close();
				
			} catch (IOException e) {
				e.printStackTrace();
		}
    }

    void addServer(String type, String region, int port){
        if(type.equals("FOLLOWER")){
            serverPorts.add(port);
            followerRegions.put(port, region);
            System.out.println("Follower server added in region " + region + " on port " + port);
        } else if(type.equals("LEADER")){
            leaderPort = port;
            System.out.println("Leader server added on port " + port);
        } else {
            System.out.println("Unknown server type");
        }
    }

    String forwardRequest(String request, int port){
        try {
            Socket forwardSocket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(forwardSocket.getOutputStream(), true);
            out.println(request);
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(forwardSocket.getInputStream()));
            String response = in.readLine();
            in.close();
            forwardSocket.close();
            return response;
            } catch (IOException e) {
                System.out.println("Error forwarding request");
                e.printStackTrace();
                return null;
            }
    }
}
