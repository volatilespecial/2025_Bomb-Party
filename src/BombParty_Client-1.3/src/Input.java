
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Client
	 * @version 1.3
	 * Input : lit l'entrée de l'utilisateur
	 */

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

public class Input implements Runnable {

    private ClientStats clientStats;
    BlockingQueue<String> messageQueue = null;
    public Scanner scanner = null;

    public Input(ClientStats clientStats, BlockingQueue<String> messageQueue, Scanner scanner) {
        this.clientStats = clientStats;
        this.messageQueue = messageQueue;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        try {
        	
        	/* Si le thread est toujours actif */
            while(clientStats.getRunThreads()) {
            	
            	/* Entrée de l'utilisateur */
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("ENDGAME4ME")) {
                    clientStats.setRunThreads(false);
                    break;
                }
                
                /* Met dans la queue des messages */
                messageQueue.put(line);
                
                /* Vérification si c'est NAMEP */
                String[] parts = line.split(" ");
                if (parts.length == 2 && clientStats.getState() == ClientStats.clientState.WAITING_NAMEP) {
                	clientStats.setUsername(parts[0]);
                    clientStats.setMode(parts[1]);
                }
                
            }
            return;
        } catch (InterruptedException e) {
            //
        }
    }
    
}
