package panda.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessPayload implements Runnable{
    private Socket socket;
    private int localPort;
    private BooksDatabase booksDB;
    private AtomicInteger leaderPort;
    private CopyOnWriteArrayList<Integer> followerPorts;
    private String serverType;

    public ProcessPayload(String serverType, int localPort, Socket socket, AtomicInteger leaderPort, CopyOnWriteArrayList<Integer> followerPorts, BooksDatabase booksDB) {
        this.socket = socket;
        this.booksDB = booksDB;
        this.localPort = localPort;
        this.leaderPort = leaderPort;
        this.serverType = serverType;
        this.followerPorts = followerPorts;        
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String request = in.readLine(); 
            String response = null;
			System.out.println("Operação recebida: "+request);
			
           
            StringTokenizer st = new StringTokenizer(request, ";");
            String OP = st.nextToken();

            switch(OP){
                case "HEARTBEAT":
                    response = "HEARTBEAT_OK;";
                    break;
                case "INIT":
                    String type = st.nextToken();

                    if (serverType.equals("LEADER") && type.equals("FOLLOWER")) {                        
                        int port = Integer.parseInt(st.nextToken());
                        followerPorts.add(port);
                        sendAliveMessage(port);
                    } else if(serverType.equals("FOLLOWER") && type.equals("LEADER")){  
                        int port = Integer.parseInt(st.nextToken());                      
                        leaderPort.set(port);                        
                        sendAliveMessage(port);                        
                    } else {
                        response = "ERROR;Invalid server type";
                        break;
                    }          
                    
                    response = "INIT_OK;";
                    break;
                case "ALIVE":
                    String type2 = st.nextToken();
                    if (serverType.equals("LEADER") && type2.equals("FOLLOWER")) {                        
                        int port = Integer.parseInt(st.nextToken());
                        followerPorts.add(port);                        
                    } else if(serverType.equals("FOLLOWER") && type2.equals("LEADER")){  
                        int port = Integer.parseInt(st.nextToken());                      
                        leaderPort.set(port);                                               
                    } else {
                        response = "ERROR;Invalid server type";
                        break;
                    } 

                    response = "ALIVE_OK;";
                    break;                    
                case "ADD_BOOK":
                    if (serverType.equals("FOLLOWER")) {
                        response = "ERROR;Only leader can add books";
                        break;
                    }
                    String book = st.nextToken();
                    booksDB.addNewBook(book);
                    response = "ADD_OK;";
                    broadcastToFollowers("ADD_BOOK;" + book);
                    break;
                case "DELETE_BOOK":
                    if (serverType.equals("FOLLOWER")) {
                        response = "ERROR;Only leader can delete books";
                        break;
                    }
                    String bookToDelete = st.nextToken();
                    booksDB.deleteBook(bookToDelete);
                    response = "DELETE_OK;";
                    broadcastToFollowers("DELETE_BOOK;" + bookToDelete);
                    break;
                case "ADD_COPY":
                    if (serverType.equals("FOLLOWER")) {
                        response = "ERROR;Only leader can update books";
                        break;
                    }
                    String book2 = st.nextToken();
                    booksDB.addNewCopy(book2);
                    response = "ADD_COPY_OK;";
                    broadcastToFollowers("ADD_COPY;" + book2);
                    break;
                case "SEARCH_BOOK":  
                    @SuppressWarnings("unused") String region = st.nextToken();              
                    String bookToSearch = st.nextToken();
                    int result = booksDB.getBookCopies(bookToSearch);
                    if (result != -1) {
                        response = "SEARCH_OK;" + result;
                    } else {
                        response = "SEARCH_NOT_FOUND;";
                    }
                    break;
                default:
                    response = "ERROR;Invalid operation";
                    break;
            }

				
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(response);
            out.close();
            in.close();
            socket.close();
				
			} catch (IOException e) {
				e.printStackTrace();
		}
    }

    private void sendAliveMessage(int port) {
        try {
            Socket socket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("ALIVE;"+serverType+";"+localPort);
            out.flush();
            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Error sending alive message to follower on port " + port + ": " + e.getMessage());
        }
    }

    private void broadcastToFollowers(String message) {
        for (int port : followerPorts) {
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
    
}
