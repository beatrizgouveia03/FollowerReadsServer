package panda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

public class LeaderServer extends Server{
    private BooksDatabase booksDB;
    private Vector<Integer> followersPorts;

    public LeaderServer() {
        booksDB = new BooksDatabase();
        leaderPort = 8081;
        initialize("INIT;LEADER;8081;");

        try(ServerSocket serverSocket = new ServerSocket(leaderPort)){            
            System.out.println("Leader server started on port " + leaderPort);
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

    @SuppressWarnings("unused")
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
                case "HEARTBEART":
                    response = "HEARTBEAT_OK;";
                    break;
                case "INIT":
                    String type = st.nextToken();
                    if (!type.equals("FOLLOWER")) {
                        response = "ERROR;Invalid type";
                        break;
                    }
                    String region = st.nextToken();
                    int port = Integer.parseInt(st.nextToken());
                    addFollower(port);
                    response = "INIT_OK;";
                    break;
                case "ADD_BOOK":
                    String book = st.nextToken();
                    booksDB.addBook(book);
                    response = "ADD_OK;";
                    broadcastToFollowers("ADD_BOOK;" + book);
                    break;
                case "DELETE_BOOK":
                    String bookToDelete = st.nextToken();
                    booksDB.deleteBook(bookToDelete);
                    response = "DELETE_OK;";
                    broadcastToFollowers("DELETE_BOOK;" + bookToDelete);
                    break;
                case "UPDATE BOOK":
                    String oldBook = st.nextToken();
                    String newBook = st.nextToken();
                    booksDB.updateBook(oldBook, newBook);
                    response = "UPDATE_OK;";
                    broadcastToFollowers("UPDATE_BOOK;" + oldBook + ";" + newBook);
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

    public void broadcastToFollowers(String message) {
        for (int port : followersPorts) {
            try {
                Socket socket = new Socket("localhost", port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(message);
                out.flush();
                out.close();
                socket.close();
            } catch (IOException e) {
                System.err.println("Error broadcasting to follower on port " + port + ": " + e.getMessage());
            }
        }
    }

    public void addFollower(int port) {
        followersPorts.add(port);
        System.out.println("Follower added on port " + port);
    }

    @Override
    void initialize(String initMessage) {
        super.initialize(initMessage);
        System.out.println("Leader server started");
        followersPorts = new Vector<Integer>();
    }

    public static void main(String[] args) {
        new LeaderServer();
    }
    
}
