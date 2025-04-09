package panda.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;


public class HeartbeatMonitor implements Runnable{
    private AtomicInteger leaderPort;
    private CopyOnWriteArrayList<Integer> serverPorts;

    private final int HEARTBEAT_INTERVAL = 5000;

    public HeartbeatMonitor(AtomicInteger leaderPort, CopyOnWriteArrayList<Integer> serverPorts) {
        this.leaderPort = leaderPort; 
        this.serverPorts = serverPorts;}

    @Override
    public void run() {
        while (true) {             
            System.out.println("Heartbeat...");

            if(leaderPort.get() == 0){
                System.out.println("Leader port is not set. Skipping heartbeat for leader.");
            } else {
                heartbeat(leaderPort.get());
                System.out.println("Leader server " + leaderPort.get() + " is alive.");
            }
            
            if(serverPorts.isEmpty()){
                System.out.println("No servers to monitor.");
            } else {
                for (int serverPort : serverPorts) {
                    heartbeat(serverPort);
                    System.out.println("Server " + serverPort + " is alive.");
                }
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
        String response = "";
        try{
            Socket socket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("HEARTBEAT;");
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            response = in.readLine();
            if (response == null || response.isEmpty()) {
                System.out.println("No response from server " + port + ". It may be down.");
            } else {
                System.out.println("Received heartbeat response from server " + port + ": " + response);
            }

            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error broadcasting heartbeat on port " + port + ": " + e.getMessage());
            e.printStackTrace();
            if (port == leaderPort.get()){
                System.out.println("Leader server " + port + " is down. Setting leader port to 0.");
                leaderPort.set(0);
            } else {
                serverPorts.remove(Integer.valueOf(port));
                System.out.println("Server " + port + " is down. Removing from list.");
            }
        } finally{
            if (response.isEmpty()){
                System.out.println("Server not responding, retrying...");
            }

        }
    }    
}
