import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class EntryReader {
    private String id;
    private List<String> knownNetworks;
    private Map<String, List<Integer>> neighbors;
    private List<Integer> listeningSockets;

    public EntryReader(String path){
        this.knownNetworks = new LinkedList<>();
        this.neighbors = new Hashtable<>();
        this.listeningSockets = new LinkedList<>();
        try{
            File entryFile = new File(path);
            FileReader fileReader = new FileReader(entryFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String currentLine;
            if((currentLine = reader.readLine()) != null)
                id = currentLine;
            while((currentLine = reader.readLine()) != null){
                if(currentLine.contains("#R")){
                    while(!(currentLine = reader.readLine()).contains("#"))
                        this.knownNetworks.add(currentLine);
                }
                if(currentLine.contains("#V")){
                    String[] information = null;
                    while(!((currentLine = reader.readLine()).contains("#"))) {
                        information = currentLine.split(":");
                        List<Integer> list = neighbors.get(information[0]);
                        if(list != null) {
                            list.add(Integer.parseInt(information[1]));
                            neighbors.put(information[0], list);
                        }
                        else {
                            list = new LinkedList<>();
                            list.add(Integer.parseInt(information[1]));
                            neighbors.put(information[0] , list);
                        }
                    }
                }
                if(currentLine.contains("#E")){
                    while((currentLine = reader.readLine()) != null)
                        this.listeningSockets.add(Integer.parseInt(currentLine));
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public String getId() {
        return id;
    }

    public List<String> getKnownNetworks() {
        return knownNetworks;
    }

    public List<Integer> getListeningSocket() {
        return listeningSockets;
    }

    public Map<String, List<Integer>> getNeighbors() {
        return neighbors;
    }

    public static void main(String... args) {
        EntryReader entryReader = new EntryReader("resources/InitialParameters.txt");
        List knownNetworks = entryReader.getKnownNetworks();

        System.out.println("AS id:");
        System.out.println(entryReader.getId());
        System.out.println("Known networks:");
        for (int i = 0; i < knownNetworks.size(); i++) {
            System.out.println(knownNetworks.get(i));
        }
        System.out.println();
        System.out.println("Neighbors:");
        Set<Map.Entry<String, List<Integer>>> neighbors = entryReader.getNeighbors().entrySet();
        Iterator<Map.Entry<String, List<Integer>>> iterator = neighbors.iterator();
        while(iterator.hasNext()){
            Map.Entry currEntry = iterator.next();
            System.out.println("Neighbor: " + currEntry.getKey());
            System.out.println("Port: " + currEntry.getValue());
            System.out.println();
        }
        System.out.println("Listening socket: " + entryReader.getListeningSocket());

        Manager manager = new Manager(entryReader.getId(), entryReader.getKnownNetworks(), entryReader.getNeighbors(), entryReader.getListeningSocket());
        manager.run();
    }
}
