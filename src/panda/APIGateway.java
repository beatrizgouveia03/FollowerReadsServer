package panda;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class APIGateway extends Server{
    private HashMap<Integer, String> followerRegions;

    public APIGateway(){
        System.out.println("API Gateway Started");
        serverPorts = new CopyOnWriteArrayList<>();
        followerRegions = new java.util.HashMap<>();
        leaderPort = new AtomicInteger();

        try ( ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            ServerSocket server = new ServerSocket(gatewayPort, MAX_CONNECTIONS)){			
			while (true) {
				try {
					System.out.println("Waiting for client request " + server.getInetAddress());

					Socket remote = server.accept();

					System.out.println("Connection made");

					executor.execute(new HandleRequest(remote, leaderPort, serverPorts, followerRegions));
				} catch (IOException e) {
					System.err.println("Error handling client: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		catch (IOException ex) {
			System.err.println("Error handling client: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

    void addServer(int serverPort, String region){
        serverPorts.add(serverPort);
        followerRegions.put(serverPort, region);
        System.out.println("Server " + serverPort + " added in region " + region);
    }

    void removeServer(int serverPort){
        serverPorts.remove((Integer)serverPort);
    }

    public static void main(String[] args){
        APIGateway gateway = new APIGateway();
        HeartbeatMonitor monitor = new HeartbeatMonitor(gateway);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

    }
    
}
