
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Server
	 * @version 1.3
	 * Server main
	 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    
    private ServerSocket serverSocket;
    private volatile boolean serverRunning = true;
    private static ServerStats serverStats;
    private ArrayList<Thread> arrayThreads;
    private AliveChecker aliveChecker;
    private Thread aliveCheckerThread;
    
        public Server(ServerSocket serverSocket){
            this.serverSocket = serverSocket;
        }
    
        public void startServer(ServerStats serverStats) { 
            try {
            	
            	/* Object aliveChecker qui va vérifier la connectivité */
            	aliveChecker = new AliveChecker(ClientHandler.clientHandlers);
            	/* Nouveau thread pour l'objet aliveChecker */
                aliveCheckerThread = new Thread(aliveChecker);
                aliveCheckerThread.start();
                /* Passe l'objet en référence dans la classe ClientHandler */
                ClientHandler.aliveChecker = aliveChecker;
                arrayThreads = new ArrayList<Thread>();
                Thread thread = null;
                while(serverStats.getIsServerRunning()) {
                    try {
                    	
                    	/* Attend et accepte un nouveau client */
                        Socket socket = serverSocket.accept();
                        System.out.println("A new client has connected!");
                        
                        /* Objet pour ce client */
                        ClientHandler clientHandler = new ClientHandler(socket, serverStats);
                        
                        /* Nouveau thread */
                        thread = new Thread(clientHandler);
                        arrayThreads.add(thread);
                        thread.start();
                    } catch (java.net.SocketTimeoutException e) {
                    	/* Timeout de accept après 1000 millisecondes */
                        if(!serverStats.getIsServerRunning()) break;
                    }
                    
                }
            } catch (IOException e) {
                if (serverRunning) e.printStackTrace();
            } finally {
            	for(Thread thread : arrayThreads){
                    if(thread.isAlive()) thread.interrupt();
                }
                if(aliveCheckerThread.isAlive()){
                    aliveChecker.shutdown();
                    aliveCheckerThread.interrupt();
                }
                stopServer();
            }
        }
    
        /* Arrêt du serveur */
        public void stopServer() {
            serverRunning = false;
            closeServerSocket();
        }
    
        public void closeServerSocket() {
            try {
                if(serverSocket != null && !serverSocket.isClosed()) 
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        public static void main(String[] args) throws IOException {
            
        	/* Création du socket */
            ServerSocket serverSocket = new ServerSocket(12345);
            /* Accepte des clients seulement pendant 1 seconde pour empêcher de bloquer */
            serverSocket.setSoTimeout(1000);
            /* Création du serveur */
            Server server = new Server(serverSocket);
            System.out.println("Bomb Party - Server 1.3. Server launched.");
            serverStats = new ServerStats();
            server.startServer(serverStats);
            System.exit(0);
        }
}


