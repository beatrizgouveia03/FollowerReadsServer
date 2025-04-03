package panda;

import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class HandleRequest implements Runnable {
    private Socket socket;
    private int leaderPort;
    private CopyOnWriteArrayList<Integer> serverPorts;

    public HandleRequest(Socket socket, int leaderPort, CopyOnWriteArrayList<Integer> serverPorts){
        this.socket = socket;
        this.leaderPort = leaderPort;
        this.serverPorts = serverPorts;        
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }
    
}
