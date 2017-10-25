import java.io.DataInputStream;
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
    private Semaphore mapSemaphore;
    private Manager manager;

    public Server(int listeningSocket, Map<String, List<String>> routes, Semaphore mapSemaphore, Manager manager) {
        this.listeningSocket = listeningSocket;
        this.routes = routes;
        this.mapSemaphore = mapSemaphore;
        this.manager = manager;
        try {
            this.serverSocket = new ServerSocket(listeningSocket);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private String update(String newRoute){
        String[] info = newRoute.split(":");
        String fromAS = info[0].split("\\*")[0];
        String ip = info[0].split("\\*")[1];//ip y AS que lo mando/
        String route = manager.getId() + "-" + info[1];

        this.manager.addRoute(ip, route);
        return fromAS;
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
                 DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                 PrintStream printStream = new PrintStream(clientSocket.getOutputStream())
            ) {
                while(true) {
                    //Receive and update
                    fromAS = this.update(dataInputStream.readUTF());

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
                        message += currEntry.getKey() + ":" + currentRoute + ",";
                    }

                    message = message.substring(0, message.length() - 1);
                    printStream.print(message);
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
