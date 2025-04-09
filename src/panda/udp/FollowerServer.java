package panda.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FollowerServer extends Server{
    private BooksDatabase booksDB;

    public FollowerServer(String region, int port) {
        this.booksDB = new BooksDatabase();
        this.leaderPort = new AtomicInteger();
        this.followerPorts = new CopyOnWriteArrayList<Integer>();
        
        try(ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            DatagramSocket serverSocket = new DatagramSocket(port)){
            initialize("INIT;FOLLOWER;"+region.toUpperCase()+";"+port+";");
            System.out.println("Follower server started in region " + region + " on port " + port);
            while(true){
                 byte[] receiveMsg = new byte[1024];
                DatagramPacket packet = new DatagramPacket(receiveMsg, receiveMsg.length);
                serverSocket.receive(packet);
                String requestData = new String(packet.getData(), 0, packet.getLength());
                String reply = null;
                System.out.println("Connection made");
                executor.execute(new ProcessPayload("FOLLOWER", leaderPort.get(), requestData, reply, leaderPort, followerPorts, booksDB));            
                System.out.println("Reply sent: " + reply);
                byte[] replyData = reply.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(replyData, replyData.length, packet.getAddress(), packet.getPort());
                serverSocket.send(sendPacket);
            }
        }   catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java FollowerServer <region> <port>");
            System.exit(1);
        }
        String region = args[0];
        int port = Integer.parseInt(args[1]);
        new FollowerServer(region, port);
    }
    
}
