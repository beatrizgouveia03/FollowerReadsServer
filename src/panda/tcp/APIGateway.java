package panda.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class APIGateway extends Server{
    private ConcurrentHashMap<Integer, String> followerRegions;

    public APIGateway(){
        System.out.println("API Gateway Started");
        followerPorts = new CopyOnWriteArrayList<>();
        followerRegions = new ConcurrentHashMap<>();
        leaderPort = new AtomicInteger();

        HeartbeatMonitor monitor = new HeartbeatMonitor(leaderPort, followerPorts, followerRegions);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

        try ( ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            ServerSocket server = new ServerSocket(gatewayPort, MAX_CONNECTIONS)){			
			while (true) {
				try {
					System.out.println("Waiting for client request " + server.getInetAddress());

					Socket remote = server.accept();

					remote.setSoTimeout(5000);

					System.out.println("Connection made");

					executor.execute(new HandleRequest(remote, leaderPort, followerPorts, followerRegions));
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

    public static void main(String[] args){
        new APIGateway();
    }
    
}
