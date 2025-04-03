package panda;

import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Server {
    protected int leaderPort;
    protected final int gatewayPort = 8080;
    protected final int MAX_CONNECTIONS = 300;
    protected CopyOnWriteArrayList<Integer> serverPorts;

    abstract void initialize();
}
