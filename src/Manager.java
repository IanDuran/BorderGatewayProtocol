import com.sun.deploy.util.StringUtils;

import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Manager {

    private String id;
    private List<String> knownNetworks;
    private Map<String, Integer> neighbors;
    private int listeningSocket;
    private Map<String, List<String>> routes;

    public Manager(String id, List<String> knownNetworks, Map<String,Integer> neighbors, int listeningSocket){
        this.id = id;
        this.knownNetworks = knownNetworks;
        this.neighbors = neighbors;
        this.listeningSocket = listeningSocket;
        routes = new Hashtable<>();
    }

    public String getId() {
        return id;
    }

    public List<String> getKnownNetworks() {
        return knownNetworks;
    }

    public Map<String, Integer> getNeighbors() {
        return neighbors;
    }

    public int getListeningSocket() {
        return listeningSocket;
    }

    public Map<String, List<String>> getRoutes() {
        return routes;
    }

    public void addRoute(String subnet, String route){
        boolean notInList = true;
        List<String> subnetRoutes = routes.get(subnet);
        if(subnetRoutes != null) {
            for (String r : subnetRoutes) {
                if (r.contains(route)) {
                    notInList = false;
                    break;
                }
            }
            if (notInList) {
                subnetRoutes.add(route);
                routes.put(subnet, subnetRoutes);
            }
        }
        else {
            List<String> list = new LinkedList<>();
            list.add(route);
            routes.put(subnet, list);
        }
    }

    public void printRoutes(){
        for(Map.Entry<String, List<String>> entry : routes.entrySet()){
            String key = entry.getKey();
            List<String> routeList = entry.getValue();
            int shortestRoute = 1000;
            int bestRoute = 0;
            for(int i = 0; i < routeList.size(); i++){
                int count = routeList.get(i).length() - routeList.get(i).replace("-", "").length();
                if(count < shortestRoute) {
                    shortestRoute = count;
                    bestRoute = i;
                }
            }
            for(int i = 0; i < routeList.size(); i++){
                if(i == bestRoute)
                    System.out.println("Red * " + key + ": " + routeList.get(i));
                else
                    System.out.println("Red " + key + ": " + routeList.get(i));
            }
        }
    }

    public void run(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;

        while (true){
            System.out.print(id + ": ");
            try {
                input = reader.readLine();
            } catch (Exception e) {
                input = "";
                e.printStackTrace();
            }
            if(input.equals("start")){
                for(String s : knownNetworks){
                    addRoute(s,id);
                }
                for(Map.Entry<String, Integer> entry : neighbors.entrySet()){
                    Thread t = new Thread( new Client(entry.getKey(), entry.getValue(), this));
                    t.start();
                }
            }
            else if(input.equals("stop")){

            }
            else if(input.contains("add")){

            }
            else if(input.equals("show routes")){
                printRoutes();
            }
            else if(input.equals("exit")){
                break;
            }
            else{
                System.out.println("Invalid input");
            }
        }
    }

}
