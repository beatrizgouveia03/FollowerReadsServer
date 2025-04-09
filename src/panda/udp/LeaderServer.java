package panda.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderServer extends Server{
    private BooksDatabase booksDB;

    public LeaderServer() {
        booksDB = new BooksDatabase();
        followerPorts = new CopyOnWriteArrayList<Integer>();
        leaderPort = new AtomicInteger();

        leaderPort.set(8081);
        
        try(ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            DatagramSocket serverSocket = new DatagramSocket(leaderPort.get())){        
            initialize("INIT;LEADER;CENTER;8081;");
            System.out.println("Leader server started on port " + leaderPort);
            while(true){
                byte[] receiveMsg = new byte[1024];
                DatagramPacket packet = new DatagramPacket(receiveMsg, receiveMsg.length);
                serverSocket.receive(packet);
                String requestData = new String(packet.getData(), 0, packet.getLength());
                String reply = null;
                System.out.println("Connection made");
                executor.execute(new ProcessPayload("LEADER", leaderPort.get(), requestData, reply, leaderPort, followerPorts, booksDB));            
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
        new LeaderServer();
    }
    
}
