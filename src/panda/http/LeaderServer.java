package panda.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
            ServerSocket serverSocket = new ServerSocket(leaderPort.get(), MAX_CONNECTIONS)){            
            initialize("LEADER;CENTER;8081;");
            System.out.println("Leader server started on port " + leaderPort);
            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("Connection made");
                executor.execute(new ProcessPayload("LEADER", leaderPort.get(), socket, leaderPort, followerPorts, booksDB));
            }
        }   catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    void initialize(String initMessage) {
        super.initialize(initMessage);
    }

    public static void main(String[] args) {
        new LeaderServer();
    }
    
}
