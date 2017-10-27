import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Iterator;
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
        neighborId = "-------------------";
    }

    public String getUpdateMessage(){
        Iterator<Map.Entry<String, List<String>>> iterator = manager.getRoutes().entrySet().iterator();
        String message = manager.getId() + "*";
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> currEntry = iterator.next();
            List<String> routeList = currEntry.getValue();

            String currentRoute = "";
            if (!routeList.get(0).contains(neighborId))
                currentRoute = routeList.get(0);

            for (int i = 1; i < routeList.size(); i++) {
                if ((currentRoute.equals("") && !routeList.get(i).contains(neighborId)) ||
                        (!routeList.get(i).contains(neighborId) && routeList.get(i).split("-").length < currentRoute.split("-").length)) {
                    currentRoute = routeList.get(i);
                }
            }
            if(!currentRoute.equals(""))
                message += currEntry.getKey() + ":" + currentRoute + ",";
        }

        message = message.substring(0, message.length() - 1);
        return message;
    }

    @Override
    public void run() {
        Socket client = null;
        BufferedReader input = null;
        PrintStream output = null;
        try {
            while (true) {
                try {
                    client = new Socket("localhost", port);
                    output = new PrintStream(client.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String message;
                    while (true) {
                        output.println(getUpdateMessage());
                        output.flush();
                        message = input.readLine();

                        neighborId = message.substring(0,message.indexOf("*"));
                        message = message.substring(message.indexOf("*") + 1);
                        String[] routes = message.split(",");
                        for(String route : routes){
                            int position = route.indexOf(":");
                            manager.addRoute(route.substring(0,position), manager.getId() + "-" + route.substring(position + 1));
                        }

                        TimeUnit.SECONDS.sleep(30);
                    }
                } catch (Exception e) {TimeUnit.SECONDS.sleep(10);}
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
