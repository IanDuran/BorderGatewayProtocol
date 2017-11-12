

import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;

public class Manager {

    private String id;
    private List<String> knownNetworks;
    private Map<String, List<Integer>> neighbors;
    private List<Integer> listeningSockets;
    private Map<String, List<String>> routes;
    private List<Thread> threads;
    private List<String> blacklist;
    private Client[] clients;
    private Server[] servers;

    public Semaphore lock = new Semaphore(1);

    public Manager(String id, List<String> knownNetworks, Map<String,List<Integer>> neighbors, List<Integer> listeningSockets){
        this.id = id;
        this.knownNetworks = knownNetworks;
        this.neighbors = neighbors;
        this.listeningSockets = listeningSockets;
        routes = new Hashtable<>();
        threads = new LinkedList<>();
        blacklist = new LinkedList<>();
        int x=0;
        for( Map.Entry<String, List<Integer>> entry : neighbors.entrySet()){
            x += entry.getValue().size();
        }
        clients = new Client[x];
        servers = new Server[listeningSockets.size()];

    }

    public String getId() {
        return id;
    }

    public List<String> getKnownNetworks() {
        return knownNetworks;
    }

    public Map<String, List<Integer>> getNeighbors() {
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
        if(!blacklist.isEmpty()) {
            Iterator<String> iter = blacklist.iterator();
            while (iter.hasNext()) {
                String str = iter.next();

                if (str.equals(As)) {
                    iter.remove();
                }
            }
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
        synchronized (routes) {
            for (Map.Entry<String, List<String>> entry : routes.entrySet()) {
                String key = entry.getKey();
                List<String> routeList = entry.getValue();
                int shortestRoute = 1000;
                int bestRoute = 0;
                for (int i = 0; i < routeList.size(); i++) {
                    int count = routeList.get(i).length() - routeList.get(i).replace("-", "").length();
                    if (count < shortestRoute) {
                        shortestRoute = count;
                        bestRoute = i;
                    }
                }
                for (int i = 0; i < routeList.size(); i++) {
                    if (i == bestRoute)
                        System.out.println("Red * " + key + ": " + routeList.get(i));
                    else
                        System.out.println("Red " + key + ": " + routeList.get(i));
                }
            }
        }
    }

    public synchronized void eraseRoutes(String AS) {
        if (!AS.equals("")) {
            for (Map.Entry<String, List<String>> entry : routes.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue()) {
                    if (value.contains(AS)) {
                        routes.get(key).remove(value);
                    }
                }
            }
        }
    }

    public synchronized String getUpdateMessage(String AS) {
        Iterator<Map.Entry<String, List<String>>> iterator = routes.entrySet().iterator();
        String message = id + "*";
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> currEntry = iterator.next();
            List<String> routeList = currEntry.getValue();

            String currentRoute = "";
            if (!routeList.isEmpty()) {
                if (!routeList.get(0).contains(AS))
                    currentRoute = routeList.get(0);

                for (int i = 1; i < routeList.size(); i++) {
                    if ((currentRoute.equals("") && !routeList.get(i).contains(AS)) ||
                            (!routeList.get(i).contains(AS) && routeList.get(i).split("-").length < currentRoute.split("-").length)) {
                        currentRoute = routeList.get(i);
                    }
                }
            }
            if (!currentRoute.equals(""))
                message += currEntry.getKey() + ":" + currentRoute + ",";
        }

        message = message.substring(0, message.length() - 1);
        return message;
    }

    public synchronized String update(String newRoute, String FromAS) {
        try {
            for (Map.Entry<String, List<String>> entry : routes.entrySet()) {
                String key = entry.getKey();
                for (String route : entry.getValue()) {
                    if (route.contains(id + "-" + FromAS)) {
                        routes.get(key).remove(route);
                    }
                }
            }
            String as = newRoute.substring(0, newRoute.indexOf("*"));
            String message = newRoute.substring(newRoute.indexOf("*") + 1);
            String[] routes = message.split(",");
            for (String route : routes) {
                int position = route.indexOf(":");
                addRoute(route.substring(0, position), id + "-" + route.substring(position + 1));
            }
            return as;
        }catch (StringIndexOutOfBoundsException e){return "--------";}
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
                int x=0;
                for(Map.Entry<String, List<Integer>> entry : neighbors.entrySet()){
                    for(Integer i : entry.getValue()){
                        Client c = new Client(entry.getKey(), i, this);
                        clients[x] = c;
                        Thread t = new Thread(c);
                        t.start();
                        threads.add(t);
                        x++;
                    }
                }
                x=0;
                for(Integer i : listeningSockets) {
                    Server s = new Server(i, routes, this);
                    servers[x] = s;
                    Thread t = new Thread(s);
                    t.start();
                    threads.add(t);
                    x++;
                }
            }
            else if(input.equals("stop")){
                for(Server s : servers)
                    s.closeSocket();
                for(Client c : clients)
                    c.setFlag(true);
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
                    if(!knownNetworks.contains(subnet)) {
                        knownNetworks.add(subnet);
                        addRoute(subnet, id);
                    }
                } catch (Exception e){e.printStackTrace();}
            }
            else if(input.equals("show routes")){
                printRoutes();
            }
            else if(input.equals("exit")){
                for(Server s : servers)
                    s.closeSocket();
                for(Client c : clients)
                    c.setFlag(true);
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
