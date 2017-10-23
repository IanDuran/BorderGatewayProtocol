import java.io.DataInputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable{

    private String neighbor;
    private Integer port;
    private Manager manager;

    public Client(String neighbor, Integer port, Manager manager){
        this.neighbor = neighbor;
        this.port = port;
        this.manager = manager;
    }

    public String getUpdateMessage(){
        String message = manager.getId() + "*";
        int x = manager.getRoutes().size();

        for(Map.Entry<String, List<String>> entry : manager.getRoutes().entrySet()){
            x--;
            String key = entry.getKey();
            List<String> routeList = entry.getValue();
            int shortestRoute = Integer.MAX_VALUE;
            int bestRoute = 0;
            for(int i = 0; i < routeList.size(); i++){
                int count = routeList.get(i).length() - routeList.get(i).replace("-", "").length();
                if(count < shortestRoute) {
                    shortestRoute = count;
                    bestRoute = i;
                }
            }
            message += key + ":" + routeList.get(bestRoute) + "";
            if(x != 0)
                message += ",";
        }
        return message;
    }

    @Override
    public void run() {
        Socket client = null;
        DataInputStream input = null;
        PrintStream output = null;
        try {
            client = new Socket(neighbor, port);
            output = new PrintStream(client.getOutputStream());
            while(true) {
                output.print(getUpdateMessage());
                output.flush();
                TimeUnit.SECONDS.sleep(30);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        finally {
            try {
                output.close();
                input.close();
                client.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
