package panda;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class HeartbeatMonitor implements Runnable{
    private APIGateway gateway;
    private final int HEARTBEAT_INTERVAL = 5000;

    public HeartbeatMonitor(APIGateway gateway){
        this.gateway = gateway;
    }

    @Override
    public void run() {
        while (true) { 
            CopyOnWriteArrayList<Integer> serverPorts = gateway.serverPorts;
            
            System.out.println("Checking servers...");

            for (int i = 0; i < serverPorts.size(); i++) {
                int serverPort = serverPorts.get(i);
                heartbeat(serverPort);
                System.out.println("Server " + serverPort + " is alive.");
            }

            try {
                Thread.sleep(HEARTBEAT_INTERVAL);
            } catch (InterruptedException e) {
                System.err.println("Error in heartbeat monitor: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void heartbeat(int port){
        try{
            Socket socket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("HEARTBEAT;");
            out.flush();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error broadcasting heartbeat on port " + port + ": " + e.getMessage());
            e.printStackTrace();
            gateway.removeServer(port);
            System.out.println("Server " + port + " is down. Removing from list.");
        }
    }    
}
