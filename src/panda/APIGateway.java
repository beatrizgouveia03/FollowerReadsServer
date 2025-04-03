package panda;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import panda_v0.RequestHandler;

public class APIGateway extends Server{

    @Override
    void initialize() {
        return;
    }

    void addServer(int serverPort){
        serverPorts.add(serverPort);
    }

    void removeServer(int serverPort){
        serverPorts.remove((Integer)serverPort);
    }

    public APIGateway(){
        System.out.println("API Gateway Started");

        try ( ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            ServerSocket server = new ServerSocket(gatewayPort, MAX_CONNECTIONS)){			
			while (true) {
				try {
					System.out.println("Waiting for client request " + server.getInetAddress());

					Socket remote = server.accept();

					System.out.println("Connection made");

					executor.execute(new HandleRequest(remote, leaderPort, serverPorts));
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
        APIGateway gateway = new APIGateway();
        HeartbeatMonitor monitor = new HeartbeatMonitor(gateway);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

    }
    
}
