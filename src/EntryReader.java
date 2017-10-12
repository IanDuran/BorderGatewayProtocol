import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

public class EntryReader {
    private List<String> knownNetworks;
    private int listeningSocket;

    public EntryReader(String path){
        this.knownNetworks = new LinkedList<>();
        this.listeningSocket = 0;
        try{
            File entryFile = new File(path);
            FileReader fileReader = new FileReader(entryFile);
            BufferedReader reader = new BufferedReader(fileReader);
            String currentLine;
            while((currentLine = reader.readLine()) != null){
                if(currentLine.contains("#R")){
                    while(!(currentLine = reader.readLine()).contains("#"))
                        this.knownNetworks.add(currentLine);
                }
                if(currentLine.contains("#V")){
                    while(!(currentLine = reader.readLine()).contains("#")) {
                        //Vecino y puerto
                    }
                }
                if(currentLine.contains("#E")){
                    this.listeningSocket = Integer.parseInt(reader.readLine());
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public List<String> getKnownNetworks() {
        return knownNetworks;
    }

    public int getListeningSocket() {
        return listeningSocket;
    }

    public static void main(String... args) {
        EntryReader entryReader = new EntryReader("resources/InitialParameters.txt");
        List neighbors = entryReader.getKnownNetworks();
        for (int i = 0; i < neighbors.size(); i++) {
            System.out.println(neighbors.get(i));
        }
        System.out.println("Listening socket: " + entryReader.getListeningSocket());
    }
}
