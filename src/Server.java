import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
    private int listeningSocket;
    private Map<String, List<String>> routes;
    private Manager manager;

    public Server(int listeningSocket, Map<String, List<String>> routes, Manager manager) {
        this.listeningSocket = listeningSocket;
        this.routes = routes;
        this.manager = manager;
    }

    private String update(String newRoute, String FromAS){
        for(Map.Entry<String, List<String>> entry : manager.getRoutes().entrySet()){
            String key = entry.getKey();
            for(String route : entry.getValue()){
                if(route.contains(manager.getId() + "-" + FromAS)){
                    manager.getRoutes().get(key).remove(route);
                }
            }
        }
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
            for(Map.Entry<String, List<String>> entry : manager.getRoutes().entrySet()) {
                String key = entry.getKey();
                for(String value : entry.getValue()) {
                    if(value.contains(AS)){
                        manager.getRoutes().get(key).remove(value);
                    }
                }
            }
        }
    }

    public String getUpdateMessage(String AS){
        Iterator<Map.Entry<String, List<String>>> iterator = manager.getRoutes().entrySet().iterator();
        String message = manager.getId() + "*";
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> currEntry = iterator.next();
            List<String> routeList = currEntry.getValue();

            String currentRoute = "";
            if(!routeList.isEmpty()) {
                if (!routeList.get(0).contains(AS))
                    currentRoute = routeList.get(0);

                for (int i = 1; i < routeList.size(); i++) {
                    if ((currentRoute.equals("") && !routeList.get(i).contains(AS)) ||
                            (!routeList.get(i).contains(AS) && routeList.get(i).split("-").length < currentRoute.split("-").length)) {
                        currentRoute = routeList.get(i);
                    }
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
        String fromAS = "-------";
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        BufferedReader dataInputStream = null;
        PrintStream printStream = null;
        while(true) {
            try {
                serverSocket = new ServerSocket();
                try {
                    serverSocket.bind(new InetSocketAddress(listeningSocket));
                }catch (Exception e){}
                try {
                    clientSocket = serverSocket.accept();
                }catch (Exception e){}
                String input = "";
                dataInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                printStream = new PrintStream(clientSocket.getOutputStream());
                manager.removeFromBlacklist(fromAS);
                while(input != null) {
                    //Receive and update
                    input = dataInputStream.readLine();
                    if(input == null)
                        break;

                    fromAS = this.update(input, fromAS);

                    //Response
                    printStream.println(getUpdateMessage(fromAS));
                    printStream.flush();

                    TimeUnit.SECONDS.sleep(30);
                }
                manager.addToBlacklist(fromAS);
                eraseRoutes(fromAS);
            } catch (IOException e) {e.printStackTrace();
            } catch(InterruptedException e){
                try {
                    serverSocket.close();
                    clientSocket.close();
                    dataInputStream.close();
                    printStream.close();
                }catch (Exception ex){e.printStackTrace();}
                break;
            }
        }
    }

}
