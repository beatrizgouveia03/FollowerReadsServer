package panda;

public class HeartbeatMonitor implements Runnable{
    private APIGateway gateway;
    private final int HEARTBEAT_INTERVAL = 5000;

    public HeartbeatMonitor(APIGateway gateway){
        this.gateway = gateway;
    }

    @Override
    public void run() {
        while (true) { 
            try {
                Thread.sleep(HEARTBEAT_INTERVAL); 
                gateway.removeServer(1); 
            } catch (InterruptedException e) {
                System.out.println("Monitor interrompido!");
                break; 
            }
        }
    }
    
}
