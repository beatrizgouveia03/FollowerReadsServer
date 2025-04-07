package panda;

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
    private BooksDatabase booksDB;
    private AtomicInteger leaderPort;
    private CopyOnWriteArrayList<Integer> followerPorts;
    private String serverType;

    public ProcessPayload(String serverType, Socket socket, AtomicInteger leaderPort, CopyOnWriteArrayList<Integer> followerPorts, BooksDatabase booksDB) {
        this.socket = socket;
        this.booksDB = booksDB;
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
                    } else if(serverType.equals("FOLLOWER") && type.equals("LEADER")){                        
                        leaderPort.set(Integer.parseInt(st.nextToken()));
                    } else {
                        response = "ERROR;Invalid server type";
                        break;
                    }
                    
                    response = "INIT_OK;";
                    break;
                case "ADD_BOOK":
                    if (serverType.equals("FOLLOWER")) {
                        response = "ERROR;Only leader can add books";
                        break;
                    }
                    String book = st.nextToken();
                    booksDB.addBook(book);
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
                case "UPDATE BOOK":
                    if (serverType.equals("FOLLOWER")) {
                        response = "ERROR;Only leader can update books";
                        break;
                    }
                    String oldBook = st.nextToken();
                    String newBook = st.nextToken();
                    booksDB.updateBook(oldBook, newBook);
                    response = "UPDATE_OK;";
                    broadcastToFollowers("UPDATE_BOOK;" + oldBook + ";" + newBook);
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

				
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(response);
            out.close();
            in.close();
            socket.close();
				
			} catch (IOException e) {
				e.printStackTrace();
		}
    }

    public void broadcastToFollowers(String message) {
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
