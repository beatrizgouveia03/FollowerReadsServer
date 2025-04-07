package panda;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
            ServerSocket serverSocket = new ServerSocket(port, MAX_CONNECTIONS)){
            initialize("INIT;FOLLOWER;"+region.toUpperCase()+";"+port+";");
            System.out.println("Follower server started in region " + region + " on port " + port);
            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("Connection made");
                executor.execute(new ProcessPayload("FOLLOWER", socket, leaderPort, followerPorts, booksDB));
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
