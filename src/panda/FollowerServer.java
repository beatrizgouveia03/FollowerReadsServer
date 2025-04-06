package panda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class FollowerServer extends Server{
    private BooksDatabase booksDB;

    public FollowerServer(String region, int port) {
        this.booksDB = new BooksDatabase();
        
        try(ServerSocket serverSocket = new ServerSocket(port)){
            initialize("INIT;FOLLOWER;"+region.toUpperCase()+";"+port+";");
            System.out.println("Follower server started in region " + region + " on port " + port);
            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("Connection made");
                processPayload(socket);
            }
        }   catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processPayload(Socket socket){
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String request = in.readLine();
            System.out.println("Operation received: " + request);

            StringTokenizer st = new StringTokenizer(request, ";");
            String OP = st.nextToken();
            String response = null;
            switch(OP){
                case "INIT":
                    String type = st.nextToken();
                    if (!type.equals("LEADER")) {
                        response = "ERROR;Invalid type";
                        break;
                    }
                    this.leaderPort = Integer.parseInt(st.nextToken());
                    response = "INIT_OK;";
                    break;
                case "HEARTBEART":
                    response = "HEARTBEAT_OK;";
                    break;
                case "SEARCH_BOOK":
                    int bookToSearch = Integer.parseInt(st.nextToken());
                    String result = booksDB.getBook(bookToSearch);
                    if (result != null) {
                        response = "SEARCH_OK;" + result;
                    } else {
                        response = "SEARCH_NOT_FOUND;";
                    }
                    break;
                default:
                    response = "ERROR;Invalid operation";
                    break;
            }

            out.println(response);
            out.flush();

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error processing payload: " + e.getMessage());
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
