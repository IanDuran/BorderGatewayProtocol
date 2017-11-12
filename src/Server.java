import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
    private int listeningSocket;
    private Map<String, List<String>> routes;
    private Manager manager;
    private ServerSocket serverSocket;

    public Server(int listeningSocket, Map<String, List<String>> routes, Manager manager) {
        this.listeningSocket = listeningSocket;
        this.routes = routes;
        this.manager = manager;
    }

    public void closeSocket(){
        try {
            serverSocket.close();
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void run() {
        String fromAS = "--------";
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(listeningSocket));
            Socket clientSocket = serverSocket.accept();
            BufferedReader dataInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintStream printStream = new PrintStream(clientSocket.getOutputStream());
            while (true) {
                try {
                    String input;
                    //Receive and update
                    input = dataInputStream.readLine();
                    if (input == null)
                        break;

                    try {
                        fromAS = input.substring(0, input.indexOf('*'));
                    }catch (StringIndexOutOfBoundsException e){fromAS = "--------";}

                    fromAS = manager.update(input, fromAS);

                    //Response
                    printStream.println(manager.getUpdateMessage(fromAS));
                    printStream.flush();

                    TimeUnit.SECONDS.sleep(5);
                    manager.removeFromBlacklist(fromAS);
                    while (input != null) {
                        //Receive and update
                        input = dataInputStream.readLine();
                        if (input == null)
                            break;

                        try {
                            fromAS = input.substring(0, input.indexOf('*'));
                        }catch (StringIndexOutOfBoundsException e){fromAS = "--------";}

                        fromAS = manager.update(input, fromAS);

                        //Response
                        printStream.println(manager.getUpdateMessage(fromAS));
                        printStream.flush();

                        TimeUnit.SECONDS.sleep(5);
                        //System.out.println("Servidor1");
                    }
                    //System.out.println("Servidor2");
                    manager.addToBlacklist(fromAS);
                    manager.eraseRoutes(fromAS);
                    clientSocket = serverSocket.accept();
                    dataInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    printStream = new PrintStream(clientSocket.getOutputStream());
                } catch (InterruptedException e) {
                    try {
                        //System.out.println("Servidor3");
                        if(!serverSocket.isClosed())
                            serverSocket.close();
                        clientSocket.close();
                        dataInputStream.close();
                        printStream.close();
                    } catch (Exception ex) {
                        e.printStackTrace();
                    }
                    return;
                }
                catch (SocketException e) {
                    try {
                        //System.out.println("Servidor3");
                        if(!serverSocket.isClosed())
                            serverSocket.close();
                        clientSocket.close();
                        dataInputStream.close();
                        printStream.close();
                    } catch (Exception ex) {
                        e.printStackTrace();
                    }
                    return;
                }
            }
        }catch (SocketException ex){return;}
        catch (Exception e){e.printStackTrace();}
    }

}
