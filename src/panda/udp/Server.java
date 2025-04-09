package panda.udp;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
                DatagramSocket socket = new DatagramSocket();
                byte[] message = initMessage.getBytes();
                DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName("localhost"), gatewayPort);
                socket.send(packet);
                socket.close();

                DatagramSocket receiveSocket = new DatagramSocket(gatewayPort);
                byte[] receiveMsg = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveMsg, receiveMsg.length);
                receiveSocket.receive(receivePacket);
                response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                receiveSocket.close();
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
