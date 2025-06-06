
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Server
	 * @version 1.3
	 * AliveChecker : gestion de la connectivité des clients avant la partie
	 */

import java.util.ArrayList;

public class AliveChecker implements Runnable{

    private static ArrayList<ClientHandler> clientHandlers;
    private volatile boolean gameNotStarted = true;

    public AliveChecker(ArrayList<ClientHandler> clientHandlers) {
        AliveChecker.clientHandlers = clientHandlers;
    }

    protected void shutdown() {
        gameNotStarted = false;
    }

    @Override
    public void run() {
        try {
            while(gameNotStarted){
            	
            	/* Attend 3 secondes avant d'envoyer ALIVE */
                Thread.sleep(3000);
                for(ClientHandler client : clientHandlers){
                    //System.out.println("Sending alive to " + client.clientUsername);
                    client.setAlive(false);
                    client.sendMessage("ALIVE");
                }
                
                /* Attend 5 secondes pour voir les réponses */
                Thread.sleep(5000);
                
                /* Ajoute a toRemove les clients a retirer */
                ArrayList<ClientHandler> toRemove = new ArrayList<ClientHandler>();
                for(ClientHandler client : clientHandlers) {
                    if(!client.getAlive()) {
                        toRemove.add(client);
                    }
                }

                /* Retire les clients inactifs */
                for(ClientHandler client : toRemove) {
                    client.closeEverything(client.socket, client.bufferedReader, client.bufferedWriter);
                }
                
                /* Envoi la nouvelle liste de client */
                if(toRemove.size() > 0){
                	if (clientHandlers.size() > 0) {
                		clientHandlers.get(0).sendPlayersList("all");
                	}   
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
