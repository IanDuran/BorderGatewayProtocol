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
    private String neighborId;

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
            if(!routeList.get(bestRoute).contains(neighborId)) {
                message += key + ":" + routeList.get(bestRoute) + "";
                if (x != 0)
                    message += ",";
            }
        }
        return message;
    }

    @Override
    public void run() {
        Socket client = null;
        DataInputStream input = null;
        PrintStream output = null;
        try {
            while (true) {
                try {
                    client = new Socket(neighbor, port);
                    output = new PrintStream(client.getOutputStream());
                    input = new DataInputStream(client.getInputStream());
                    String message;
                    while (true) {
                        output.print(getUpdateMessage());
                        message = input.readUTF();
                        neighborId = message.substring(0,message.indexOf("*"));
                        message = message.substring(message.indexOf("*"));
                        String[] routes = message.split(",");
                        for(String route : routes){
                            int position = route.indexOf(":");
                            manager.addRoute(route.substring(0,position), route.substring(position + 1));
                        }
                        TimeUnit.SECONDS.sleep(30);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){e.printStackTrace();}
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
