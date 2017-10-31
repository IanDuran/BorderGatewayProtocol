

import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Manager {

    private String id;
    private List<String> knownNetworks;
    private Map<String, Integer> neighbors;
    private List<Integer> listeningSockets;
    private Map<String, List<String>> routes;
    private List<Thread> threads;
    private List<String> blacklist;

    public Manager(String id, List<String> knownNetworks, Map<String,Integer> neighbors, List<Integer> listeningSockets){
        this.id = id;
        this.knownNetworks = knownNetworks;
        this.neighbors = neighbors;
        this.listeningSockets = listeningSockets;
        routes = new Hashtable<>();
        threads = new LinkedList<>();
        blacklist = new LinkedList<>();
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

    public List<Integer> getListeningSocket() {
        return listeningSockets;
    }

    public Map<String, List<String>> getRoutes() {
        return routes;
    }

    public void addToBlacklist(String as){
        if(!blacklist.contains(as))
            blacklist.add(as);
    }

    public synchronized void removeFromBlacklist(String As){
        Iterator<String> iter = blacklist.iterator();

        while (iter.hasNext()) {
            String str = iter.next();

            if (str.equals(As))
                iter.remove();
        }
    }

    public synchronized void addRoute(String subnet, String route){
        for(String as : blacklist){
            if(route.contains(as))
                return;
        }
        //System.out.println(route);
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
                    threads.add(t);
                }
                for(Integer i : listeningSockets) {
                    Thread t = new Thread(new Server(i, routes, this));
                    t.start();
                    threads.add(t);
                }
            }
            else if(input.equals("stop")){
                for(Thread t : threads){
                    if(t.isAlive()){
                        t.interrupt();
                    }
                }
                routes.clear();
            }
            else if(input.contains("add")){
                try {
                    System.out.print("Enter the new network: ");
                    String subnet = reader.readLine();
                    addRoute(subnet, id);
                } catch (Exception e){e.printStackTrace();}
            }
            else if(input.equals("show routes")){
                printRoutes();
            }
            else if(input.equals("exit")){
                for(Thread t : threads){
                    if(t.isAlive()){
                        t.interrupt();
                    }
                }
                break;
            }
            else{
                System.out.println("Invalid input");
            }
        }
    }

}
