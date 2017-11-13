import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable{

    private String neighbor;
    private Integer port;
    private Manager manager;
    private String neighborId;
    private Map<String, String> neighborRoutes;
    private boolean flag = false;

    public Client(String neighbor, Integer port, Manager manager){
        this.neighbor = neighbor;
        this.port = port;
        this.manager = manager;
        neighborId = "--------";
    }

    public void setFlag(boolean value){flag = value;}

    @Override
    public void run() {
        Socket client = null;
        DataInputStream input = null;
        DataOutputStream output = null;
        while(true) {
            try {
                client = new Socket(neighbor, port);
                input = new DataInputStream(client.getInputStream());
                output = new DataOutputStream(client.getOutputStream());
                String message;
                output.writeUTF(manager.getUpdateMessage(neighborId));
                output.flush();

                message = input.readUTF();
                if(message != null)
                    neighborId = manager.update(message, neighborId);

                TimeUnit.SECONDS.sleep(30);
                manager.removeFromBlacklist(neighborId);
                while(message != null) {
                    output.writeUTF(manager.getUpdateMessage(neighborId));
                    output.flush();

                    message = input.readUTF();
                    if(message != null)
                        neighborId = manager.update(message, neighborId);

                    TimeUnit.SECONDS.sleep(30);
                    //System.out.println("Cliente1");
                }
            } catch(InterruptedException e){
                try {
                    //System.out.println("Cliente2");
                    client.close();
                    input.close();
                    output.close();
                } catch (Exception ex){ex.printStackTrace();}
                return;
            }
            catch(IOException ex){
                //System.out.println("Cliente3");
                manager.addToBlacklist(neighborId);
                manager.eraseRoutes(neighborId);
                if(flag)
                    return;
            }
        }
    }
}
