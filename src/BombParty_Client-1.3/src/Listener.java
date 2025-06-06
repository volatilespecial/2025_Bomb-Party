
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Client
	 * @version 1.3
	 * Listener : écoute ce qu'envoi le serveur
	 */

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

//import Files.Clientside.ClientStats.clientState;

public class Listener implements Runnable {

    private static Map<Integer, String> errorMap = new HashMap<Integer, String>();
    private BufferedReader bufferedReader;
    private ClientStats clientStats;
    private BlockingQueue<String> messageQueue;

    public Listener(BufferedReader bufferedReader, ClientStats clientStats, BlockingQueue<String> messageQueue) {

        this.bufferedReader = bufferedReader;
        this.clientStats = clientStats;
        this.messageQueue = messageQueue;

        errorMap.put(00, "Erreur serveur");
        errorMap.put(01, "Identification requise");
        errorMap.put(02, "Partie remplie, mode spectacteur possible");
        errorMap.put(10, "Argument invalide");
        errorMap.put(11, "Commande invalide");
        errorMap.put(21, "Pseudo non autorisÃ©");
        errorMap.put(22, "Pseudo dÃ©jÃ  pris");
        errorMap.put(23, "Mode non reconnu");
        errorMap.put(31, "Pas assez de joueurs (min. 2)");
        errorMap.put(32, "Seul le gamemaster peut lancer la partie");
        errorMap.put(33, "KABOOM! -1 vie...");
        errorMap.put(34, "ParamÃ¨tre 'correct' invalide");
        errorMap.put(35, "Pas votre tour");
        errorMap.put(36, "Non joueur : spectateur");
    }

    @Override
    public void run() {
        try {
            String parts[] = null;
            String line;
            /* Si le thread est toujours actif */
            while(clientStats.getRunThreads()){
            	
            	/* Récupère ce que le serveur a envoyé */
            	line = bufferedReader.readLine();
                if (line == null) break;
                
                /* Séparation en plusieurs partie */
                parts = line.split(" ");
                
                /* Gestion de ERROR */
                if (parts[0].equalsIgnoreCase("ERROR")) {
                    handleErrors(Integer.parseInt(parts[1]));
                    continue;
                }
                
                /* Gestion de ALIVE */
                if (parts[0].equalsIgnoreCase("ALIVE")) {
                    responseAlive();
                    continue;
                }
                
                /* Gestion de PLAYERS */
                if (parts[0].equalsIgnoreCase("PLYRS")) {
                    System.out.println("Players : " + parts[1]);
                    continue;
                }
                
                /* Si état WAITING_NAMEP, vérification si le NAMEP reçu est le notre
                 * et met à l'état WAITING_START */
                if (clientStats.getState() == ClientStats.clientState.WAITING_NAMEP) { 
                    if (parts.length == 3) {
                        if (parts[0].equalsIgnoreCase("NAMEP") && !clientStats.isUsernameEmpty()) {
                            if(parts[1].equalsIgnoreCase(clientStats.getUsername())) {
                                clientStats.setState(ClientStats.clientState.WAITING_START);
                                continue;
                            }
                        }
                    }
                }
                
                /* Gestion de START */
                if (clientStats.getState() == ClientStats.clientState.WAITING_START) {
                    if (parts[0].equalsIgnoreCase("START")) {
                        handleStart();
                        continue;
                    }
                }

                if (clientStats.getState() == ClientStats.clientState.IN_GAME || clientStats.getState() == ClientStats.clientState.VIEWER) {
                    
                	/* Si aucune vie, envoi de DEADP et met le client en Spectateur */
                	if(clientStats.getLives() == 0 && clientStats.getState() == ClientStats.clientState.IN_GAME){
                        System.out.println("No more lives!");
                        messageQueue.put("DEADP");
                        clientStats.setState(ClientStats.clientState.VIEWER);
                        continue;
                    }
                	
                	/* Gestion de GOVER*/
                    if(parts[0].equalsIgnoreCase("GOVER")) {
                    	
                    	/* Si le client est un spectateur */
                        if (clientStats.getState() == ClientStats.clientState.VIEWER) {
                            System.out.println("End of game : " + parts[1] + " won!");
                            messageQueue.put("ENDGAME");
                            clientStats.setRunThreads(false);
                            break;
                        }
                        
                        /* Si le client a gagné. Variable RunThreads a false */
                        if(parts[1].equalsIgnoreCase(clientStats.getUsername())){
                            System.out.println("You won!");
                            messageQueue.put("ENDGAME");
                            clientStats.setRunThreads(false);
                            break;
                        }
                    }
                    
                    /* Si ce n'est pas au tour du client */
                    if(parts.length == 3){
                        if(parts[0].equalsIgnoreCase("ROUND") && !parts[2].equalsIgnoreCase(clientStats.getUsername())) {
                            System.out.println(parts[2] + "'s turn: " + parts[1]);
                            continue;
                        }
                        if(parts[0].equalsIgnoreCase("SENDW") && !clientStats.getMyTurn()) {
                            System.out.println(parts[1] + " - " + parts[2]);
                            continue;
                        }
                    }
                    
                    /* Si c'est au tour du client */
                    if(clientStats.getState() == ClientStats.clientState.IN_GAME) {
                        if(clientStats.getLives() > 0) {
                            if(parts.length == 3){
                            	
                            	/* Varible myTurn à true */
                                if(parts[0].equalsIgnoreCase("ROUND") && parts[2].equalsIgnoreCase(clientStats.getUsername())){
                                    clientStats.setMyTurn(true);
                                    System.out.println("Your turn: " + parts[1]);
                                    continue;
                                }
                                if(parts[0].equalsIgnoreCase("SENDW") && clientStats.getMyTurn()){
                                    if(parts[2].equalsIgnoreCase("C")){
                                        System.out.println(parts[1] + " - Correct!");
                                        clientStats.setMyTurn(false);
                                        continue;
                                    } else {
                                        System.out.println(parts[1] + " - Incorrect! Try again!");
                                        continue;
                                    }
                                }
                            }
                            
                        }
                    }
                }
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    /* Gestion des erreurs. Si la bombe explose, myTurn à false */
    public void handleErrors(int errorCode){
        if(errorCode ==  33){
            clientStats.setMyTurn(false);
            clientStats.removeLife();
        }
        String errorMessage = errorMap.getOrDefault(errorCode, "");
        System.out.println("ERR" + errorCode + " : " + errorMessage);
    }

    /* Gestion de START */
    public void handleStart(){
        if (clientStats.getMode().equalsIgnoreCase("J")) {
            clientStats.setState(ClientStats.clientState.IN_GAME);
        } else {
            clientStats.setState(ClientStats.clientState.VIEWER);
        }
    }

    /* Gestion de ALIVE : met dans la queue de messages à envoyer au serveur */
    private void responseAlive() throws InterruptedException{
        messageQueue.put("ALIVE");
    }
    
}
