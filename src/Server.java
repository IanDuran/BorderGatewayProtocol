import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
    private int listeningSocket;
    private ServerSocket serverSocket;
    private Map<String, List<String>> routes;
    private Manager manager;

    public Server(int listeningSocket, Map<String, List<String>> routes, Manager manager) {
        this.listeningSocket = listeningSocket;
        this.routes = routes;
        this.manager = manager;
        try {
            this.serverSocket = new ServerSocket(listeningSocket);
        }catch(Exception e){
            e.printStackTrace();
        }
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
            Iterator<Map.Entry<String, List<String>>> iterator = routes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<String>> currEntry = iterator.next();
                List<String> currentEntryRoutes = currEntry.getValue();
                for (int i = 0; i < currentEntryRoutes.size(); i++) {
                    if (currentEntryRoutes.get(i).contains(AS)) {
                        currentEntryRoutes.remove(i);
                        i--;
                    }
                }
                if(currentEntryRoutes.size() == 0){
                    routes.remove(currEntry.getKey(), currEntry.getValue());
                }
            }
        }
    }

    @Override
    public void run() {
        String fromAS = "";
        while(true) {
            try (Socket clientSocket = this.serverSocket.accept();
                 BufferedReader dataInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintStream printStream = new PrintStream(clientSocket.getOutputStream())
            ) {
                while(true) {
                    //Receive and update
                    String input = dataInputStream.readLine();
                    fromAS = this.update(input);

                    //Response
                    Iterator<Map.Entry<String, List<String>>> iterator = routes.entrySet().iterator();
                    String message = manager.getId() + "*";
                    while (iterator.hasNext()) {
                        Map.Entry<String, List<String>> currEntry = iterator.next();
                        List<String> routeList = currEntry.getValue();

                        String currentRoute = "";
                        if (!routeList.get(0).contains(fromAS))
                            currentRoute = routeList.get(0);

                        for (int i = 1; i < routeList.size(); i++) {
                            if ((currentRoute.equals("") && !routeList.get(i).contains(fromAS)) ||
                                    (!routeList.get(i).contains(fromAS) && routeList.get(i).split("-").length < currentRoute.split("-").length)) {
                                currentRoute = routeList.get(i);
                            }
                        }
                        if(!currentRoute.equals(""))
                            message += currEntry.getKey() + ":" + currentRoute + ",";
                    }

                    message = message.substring(0, message.length() - 1);
                    printStream.println(message);
                    printStream.flush();
                    TimeUnit.SECONDS.sleep(30);
                }
            } catch (IOException e) {
                //Llega aqui cuando la conexion muere, hacer algo
                this.eraseRoutes(fromAS);
                e.printStackTrace();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
