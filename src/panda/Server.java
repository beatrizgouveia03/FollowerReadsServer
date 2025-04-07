package panda;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;

public abstract class Server {
    protected AtomicInteger leaderPort;
    protected final int gatewayPort = 8080;
    protected final int MAX_CONNECTIONS = 300;
    protected CopyOnWriteArrayList<Integer> followerPorts;

    void initialize(String initMessage){
        String response = "";

        while(true){
            try{
                System.out.println("Trying to initiate server");
                Socket socket = new Socket("localhost", gatewayPort);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(initMessage);
                out.flush();
                response = in.readLine();    

                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
				e.printStackTrace();
            } finally{
                if(response.equals("INIT_OK;")){
                    System.out.println("Server initiated");
                    break;
                } else if (response.isEmpty()){
                    System.out.println("Server not initiated, retrying...");
                }

            }

            try { Thread.sleep(1000);}
            catch (InterruptedException e) { 
                e.printStackTrace();
            }
        }
    };
}
