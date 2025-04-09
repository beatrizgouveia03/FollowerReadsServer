package panda.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class APIGateway extends Server{
    private HashMap<Integer, String> followerRegions;

    public APIGateway(){
        System.out.println("API Gateway Started");
        followerPorts = new CopyOnWriteArrayList<>();
        followerRegions = new java.util.HashMap<>();
        leaderPort = new AtomicInteger();

        HeartbeatMonitor monitor = new HeartbeatMonitor(leaderPort, followerPorts);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

        try ( ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            DatagramSocket server = new DatagramSocket(gatewayPort)){			
			while (true) {
				try {
					byte[] receiveMsg = new byte[1024];
                	DatagramPacket packet = new DatagramPacket(receiveMsg, receiveMsg.length);
                	server.receive(packet);
                	String requestData = new String(packet.getData(), 0, packet.getLength());
               		String reply = null;
					System.out.println("Connection made");

					executor.execute(new HandleRequest(requestData, reply, leaderPort, followerPorts, followerRegions));

					System.out.println("Reply sent: " + reply);
                	byte[] replyData = reply.getBytes();
                	DatagramPacket sendPacket = new DatagramPacket(replyData, replyData.length, packet.getAddress(), packet.getPort());
                	server.send(sendPacket);

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
