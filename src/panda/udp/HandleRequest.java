package panda.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class HandleRequest implements Runnable {
    private String reply;
    private String request;
    private AtomicInteger leaderPort;
    private CopyOnWriteArrayList<Integer> serverPorts;
    private HashMap<Integer, String> followerRegions;

    public HandleRequest(String request, String reply, AtomicInteger leaderPort, CopyOnWriteArrayList<Integer> serverPorts, HashMap<Integer, String> followerRegions) {
        this.reply = reply;
        this.request = request;
        this.leaderPort = leaderPort;
        this.serverPorts = serverPorts;        
        this.followerRegions = followerRegions;
    }

    @Override
    public void run() {    
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
            reply = "INIT_OK;";
        } else if(OP.equals("ADD_BOOK")){
            reply = forwardRequest(request, leaderPort.get());
        } else if(OP.equals("DELETE_BOOK")){                
            reply = forwardRequest(request,  leaderPort.get());
        }  else if(OP.equals("UPDATE_BOOK")){                
            reply = forwardRequest(request,  leaderPort.get());
        } else if(OP.equals("SEARCH_BOOK")){                 
            String region = st.nextToken(); 
            int port = locateClosestServer(region);           
            reply = forwardRequest(request, port);
        } else {
            System.out.println("Operation Unknown");
        }
        System.out.println("Reply: " + reply);
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
            DatagramSocket forwardSocket = new DatagramSocket();
            byte[] requestData = request.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, InetAddress.getByName("localhost"), port);
            forwardSocket.send(requestPacket);
            System.out.println("Request sent: " + request);
            forwardSocket.close();

            DatagramSocket replySocket = new DatagramSocket(8080);
            DatagramPacket replyPacket = new DatagramPacket(new byte[1024], 1024);
            replySocket.receive(replyPacket);
            String reply = new String(replyPacket.getData(), 0, replyPacket.getLength());
            replySocket.close();

            System.out.println("Reply received: " + reply);
            return reply;
            } catch (IOException e) {
                System.out.println("Error forwarding request");
                e.printStackTrace();
                return null;
            }
    }
}
