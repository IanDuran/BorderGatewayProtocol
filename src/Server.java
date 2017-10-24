import java.io.DataInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

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

    private void update(String newRoute){
        String[] info = newRoute.split(":");
        String fromAS =info[0].split("\\*")[0];
        String ip = info[0].split("\\*")[1];//ip y AS que lo mando/
        String route = manager.getId() + "-" + info[1];

        try {
            mapSemaphore.acquire();
            if(routes.get(ip) == null){
                List<String> routesList = new LinkedList<>();
                routesList.add(route);
                routes.put(ip, routesList);
            }else{
                routes.get(ip).add(route);
            }
            mapSemaphore.release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try(Socket clientSocket = this.serverSocket.accept();
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
            PrintStream printStream = new PrintStream(clientSocket.getOutputStream())
        ){
            this.update(dataInputStream.readUTF());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
