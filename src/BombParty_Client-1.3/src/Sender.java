
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Client
	 * @version 1.3
	 * Sender : envoi tout les messages au serveur
	 */


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;


public class Sender implements Runnable {

    private BufferedWriter bufferedWriter;
    private BlockingQueue<String> messageQueue;
    private ClientStats clientStats;
    
    public Sender(BufferedWriter bufferedWriter, BlockingQueue<String> messageQueue, ClientStats clientStats){
        this.bufferedWriter = bufferedWriter;
        this.messageQueue = messageQueue;
        this.clientStats = clientStats;
    }   

    @Override
    public void run(){
        try {
        	
        	/* Si le thread est toujours actif */
            while(clientStats.getRunThreads()){
            	
            	/* Attend qu'un message est disponible dans la queue */
                String message = messageQueue.take();
                
                /* Si ENDGAME4ME, sorte de la boucle */
                if(message.equalsIgnoreCase("ENDGAME4ME")){
                    break;
                }
                
                /* Si l'état est WAITING_NAMEP, envoi le message avec NAMEP */
                else if(clientStats.getState() == ClientStats.clientState.WAITING_NAMEP && !message.equalsIgnoreCase("ALIVE")) {
                    message = "NAMEP " + message;
                }
                
                /* Si l'état est IN_GAME, envoi le message avec SENDW */
                else if(clientStats.getState() == ClientStats.clientState.IN_GAME && !message.equalsIgnoreCase("ALIVE")){
                    message = "SENDW " + message;
                }
                
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
            return;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}
