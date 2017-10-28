import java.io.*;
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
        neighborId = "";
    }

    public String getUpdateMessage(){
        String message = manager.getId() + "*";
        if(!neighborId.equals("")) {
            Iterator<Map.Entry<String, List<String>>> iterator = manager.getRoutes().entrySet().iterator();
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
                if (!currentRoute.equals(""))
                    message += currEntry.getKey() + ":" + currentRoute + ",";
            }
        }
        else{
            List<String> networks = manager.getKnownNetworks();
            for(String network : networks){
                message += network + ":";
                List<String> routeList = manager.getRoutes().get(network);
                for(String route : routeList){
                    if(!route.contains("-")) {
                        message += route + ",";
                        break;
                    }
                }
            }
        }
        message = message.substring(0, message.length() - 1);
        return message;
    }

    private String update(String newRoute){
        String as = newRoute.substring(0,newRoute.indexOf("*"));
        String message = newRoute.substring(newRoute.indexOf("*") + 1);
        String[] routes = message.split(",");
        for(String route : routes){
            int position = route.indexOf(":");
            manager.addRoute(route.substring(0,position), manager.getId() + "-" + route.substring(position + 1));
        }
        return as;
    }

    private void eraseRoutes(String AS){
        if(!AS.equals("")) {
            for(Map.Entry<String, List<String>> entry : manager.getRoutes().entrySet()){
                String key = entry.getKey();
                for(String route : entry.getValue()){
                    if(route.contains(AS)){
                        manager.getRoutes().remove(key, route);
                    }
                }
                if(entry.getValue().isEmpty()){
                    manager.getRoutes().remove(key);
                }
            }
        }
    }

    @Override
    public void run() {
        Socket client = null;
        BufferedReader input = null;
        PrintStream output = null;
        while(true) {
            try {
                client = new Socket(neighbor, port);
                input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                output = new PrintStream(client.getOutputStream());
                String message = "";
                while(message != null) {
                    output.println(getUpdateMessage());
                    output.flush();

                    message = input.readLine();
                    //System.out.println(message);
                    if(message != null)
                        neighborId = update(message);

                    //manager.printRoutes();

                    TimeUnit.SECONDS.sleep(30);
                }
                eraseRoutes(neighborId);
            } catch(InterruptedException e){
                try {
                    client.close();
                    input.close();
                    output.close();
                    break;
                } catch (Exception ex){ex.printStackTrace();}
            }
            catch(IOException ex){}
        }
    }
}
