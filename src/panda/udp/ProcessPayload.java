package panda.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessPayload implements Runnable{
    private String reply;
    private String request;
    private int localPort;
    private BooksDatabase booksDB;
    private AtomicInteger leaderPort;
    private CopyOnWriteArrayList<Integer> followerPorts;
    private String serverType;

    public ProcessPayload(String serverType, int localPort, String request, String reply, AtomicInteger leaderPort, CopyOnWriteArrayList<Integer> followerPorts, BooksDatabase booksDB) {
        this.reply = reply;
        this.booksDB = booksDB;
        this.request = request;
        this.localPort = localPort;
        this.leaderPort = leaderPort;
        this.serverType = serverType;
        this.followerPorts = followerPorts;        
    }

    @Override
    public void run() {
        System.out.println("Operação recebida: "+request);
		       
        StringTokenizer st = new StringTokenizer(request, ";");
        String OP = st.nextToken();
        switch(OP){
            case "HEARTBEAT":
                reply = "HEARTBEAT_OK;";
                break;
            case "INIT":
                String type = st.nextToken();
                if (serverType.equals("LEADER") && type.equals("FOLLOWER")){                        
                    int port = Integer.parseInt(st.nextToken());
                    followerPorts.add(port);
                    sendAliveMessage(port);
                } else if(serverType.equals("FOLLOWER") && type.equals("LEADER")){  
                    int port = Integer.parseInt(st.nextToken());                      
                    leaderPort.set(port);                        
                    sendAliveMessage(port);                        
                } else {
                    reply = "ERROR;Invalid server type";
                    break;
                }          
                
                reply = "INIT_OK;";
                break;
            case "ALIVE":
                String type2 = st.nextToken();
                if (serverType.equals("LEADER") && type2.equals("FOLLOWER")){                        
                    int port = Integer.parseInt(st.nextToken());
                    followerPorts.add(port);                        
                } else if(serverType.equals("FOLLOWER") && type2.equals("LEADER")){  
                    int port = Integer.parseInt(st.nextToken());                      
                    leaderPort.set(port);                                               
                } else {
                    reply = "ERROR;Invalid server type";
                    break;
                } 
                reply = "ALIVE_OK;";
                break;                    
            case "ADD_BOOK":
                if (serverType.equals("FOLLOWER")) {
                    reply = "ERROR;Only leader can add books";
                    break;
                }
                String book = st.nextToken();
                booksDB.addBook(book);
                reply = "ADD_OK;";
                broadcastToFollowers("ADD_BOOK;" + book);
                break;
            case "DELETE_BOOK":
                if (serverType.equals("FOLLOWER")) {
                    reply = "ERROR;Only leader can delete books";
                    break;
                }
                String bookToDelete = st.nextToken();
                booksDB.deleteBook(bookToDelete);
                reply = "DELETE_OK;";
                broadcastToFollowers("DELETE_BOOK;" + bookToDelete);
                break;
            case "UPDATE BOOK":
                if (serverType.equals("FOLLOWER")) {
                    reply = "ERROR;Only leader can update books";
                    break;
                }
                String oldBook = st.nextToken();
                String newBook = st.nextToken();
                booksDB.updateBook(oldBook, newBook);
                reply = "UPDATE_OK;";
                broadcastToFollowers("UPDATE_BOOK;" + oldBook + ";" + newBook);
                break;
            case "SEARCH_BOOK":  
                @SuppressWarnings("unused") String region = st.nextToken();              
                int bookToSearch = Integer.parseInt(st.nextToken());
                String result = booksDB.getBook(bookToSearch);
                if (result != null) {
                    reply = "SEARCH_OK;" + result;
                } else {
                    reply = "SEARCH_NOT_FOUND;";
                }
                break;
            default:
                reply = "ERROR;Invalid operation";
                break;
        }
        
        System.out.println("Reply: " + reply);
    }

    private void sendAliveMessage(int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            String alive = "ALIVE;"+serverType+";"+localPort;
            byte[] aliveMsg = alive.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(aliveMsg, aliveMsg.length, InetAddress.getByName("localhost"), port);
            socket.send(sendPacket);
            socket.close();
        } catch (IOException e) {
            System.err.println("Error sending alive message to follower on port " + port + ": " + e.getMessage());
        }
    }

    private void broadcastToFollowers(String message) {
        for (int port : followerPorts) {
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] messageBytes = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, InetAddress.getByName("localhost"), port);
                socket.send(sendPacket);                
                socket.close();
            } catch (IOException e) {
                System.err.println("Error broadcasting to follower on port " + port + ": " + e.getMessage());
            }
        }
    }
    
}
