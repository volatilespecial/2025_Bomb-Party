
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Server
	 * @version 1.3
	 * GameManager : gestion de la partie
	 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameManager {
    
    private static List<ClientHandler> players = new ArrayList<>();
    private static List<ClientHandler> viewers = new ArrayList<>();
    private static List<String> words;
    private static ClientHandler currentPlayer;
    private static int defaultTimer = 5;
    private static int minTimer = 10;
    private static int maxTimer = 30;
    private static int bombTimer;
    private static Timer bomb;

    private static String currentLetters;

    private static boolean firstRound = true;

    private static Map<ClientHandler, Boolean> aliveMap;

    private static long bombStartTime;
    private static long bombDelay;

    private static boolean gameOver = false;

    private ServerStats serverStats;
    
    /* Démarre la partie */
    public void startGame(ArrayList<ClientHandler> connectedPlayers, ServerStats serverStats) throws IOException{
        //clients = new ArrayList<>(connectedPlayers);
        aliveMap = new HashMap<ClientHandler, Boolean>();
        this.serverStats = serverStats;
        
        /* Sépare la liste ClientHandler en deux listes : players et viewers */
        for(ClientHandler client: connectedPlayers){
            if(client.getClientMode() ==  'J'){
                players.add(client);
                aliveMap.put(client, false);
            } else if (client.getClientMode() == 'S'){
                viewers.add(client);
            }
        }
        /* Le dernier joueur commence */
        Collections.reverse(players);
        getWordsFile();
        currentPlayer = players.get(0);
        if (!gameOver){
            beginRound();
        }
    }

    /* Commence un nouveau tour */
    public void beginRound() throws IOException{

        checkEndGame();

        if (!gameOver){

            if(!firstRound){
                checkAliveMap();
            } else {
                firstRound = false;
            }
            currentLetters = generateLetterSequence(words);
            resetBomb(-1);
            broadcastMessage("ROUND " + currentLetters + " " + currentPlayer.getClientName());
        }
    }

    /* Continue si la bombe n'a pas explosé */
    public void nextRound() throws IOException{
        checkEndGame();
        if (!gameOver){
            currentPlayer = getNextPlayer(currentPlayer);
            if(currentPlayer != null) {
            	currentLetters = generateLetterSequence(words);
                broadcastMessage("ROUND " + currentLetters + " " + currentPlayer.getClientName());
            } 
        }
        
    }

    /* Reset le timer de la bomb. Si -1, temps random */
    public synchronized void resetBomb(int seconds) {

        if (!gameOver){
            if(seconds != -1){
                bombTimer = seconds;
            } else {
                Random random = new Random();
                bombTimer = random.nextInt((maxTimer - minTimer) + 1 ) + minTimer;
            }
           
            /* Annule la bombe */
            if(bomb != null){
                bomb.cancel();
            }
            
            /* Relance la bombe */
            bombDelay = bombTimer * 1000l;
            bombStartTime = System.currentTimeMillis();
    
            bomb = new Timer();
            TimerTask task = new TimerTask() {
    
                @Override
                public void run() {
                    try {
                        currentPlayer.sendError(33);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                        	currentPlayer = getNextPlayer(currentPlayer);
                            System.out.println("Current player : " + currentPlayer.getClientName());
                            if(currentPlayer != null) {
                            	beginRound();
                            }
							
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                }
                
            };
    
            bomb.schedule(task, (long)bombDelay);
        } 
    }

    /* Gestion de l'entrée du client */
    public synchronized void handleGameInput(ClientHandler client, String input) throws IOException{
        if(!gameOver){
            if (!players.contains(client)) return;

            if(input.startsWith("DEADP")){
                handleDead(client);
                return;
            }

            System.out.println("received " + input);
            if(input.startsWith("SENDW")){
                String[] parts = input.split(" ", 2);
                
                if(parts.length < 2){         
                    client.sendError(10);
                    return;
                }
            } else {
                client.sendError(11);
                return;
            }

            String word = input.split(" ", 2)[1].trim();

            if(!isClientTurn(client)) {
                client.sendError(35);
                return;
            }

            aliveMap.put(client, true);

            if(checkWordAgainstDict(client, word)){
                long now = System.currentTimeMillis();
                long elapsedTime = now - bombStartTime;
                long remaining = bombDelay - elapsedTime;
                if (remaining <= defaultTimer * 1000){
                    resetBomb(defaultTimer);
                }
                nextRound();
            }
        }
    }

    /* Gestion des clients morts */
    private synchronized void handleDead(ClientHandler client) throws IOException{
        if(!gameOver) {
            if(client.getClientMode() == 'S'){
                client.sendError(36);
            } else {
                broadcastMessage("DEADP " + client.getClientName());
                if(players.contains(client)){
                    players.remove(client);
                    viewers.add(client);
                    aliveMap.remove(client);
                }
                checkEndGame();
            }
        }
    }

    /* Verifie si la partie est terminée */
    private void checkEndGame() throws IOException{
        if(players.size() == 1){
            broadcastMessage("GOVER " + players.get(0).getClientName());
            for(ClientHandler client : new ArrayList<>(players)) {
                if(client != null)
                    client.closeEverything(client.socket, client.bufferedReader, client.bufferedWriter);
            }
            for(ClientHandler client : new ArrayList<>(viewers)) {
                if(client != null)
                    client.closeEverything(client.socket, client.bufferedReader, client.bufferedWriter);
            }
            gameOver = true;
            serverStats.setIsServerRunning(false);
        }
    }
    
    /* Vérifie la connectivité des joueurs qui n'ont pas répondu à leur tour */
    private void checkAliveMap() throws IOException{

        if(!gameOver) {
            ClientHandler previousPlayer;
            int index = players.indexOf(currentPlayer);
            if(index == -1) return;
            if(index == 0){
                previousPlayer = players.get(players.size() - 1);
            } else {
                previousPlayer = players.get(index - 1);
            }
            //System.out.println("Check alive map : " + previousPlayer.getClientName());
            Boolean wasAlive = aliveMap.getOrDefault(previousPlayer, false);
            if(wasAlive){
                aliveMap.put(previousPlayer, false);
            } else {
                try {
                    previousPlayer.sendMessage("ALIVE");
                } catch (IOException e) {
                    /* Si l’écriture échoue, retire le client */
                    removeClient(previousPlayer);
                    return;
                }

                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

                scheduler.schedule(() -> {
                    if(!aliveMap.getOrDefault(previousPlayer, false)) {
                    	try {
                            removeClient(previousPlayer);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    } else {
                        aliveMap.put(previousPlayer, false);
                    }
                }, 5, TimeUnit.SECONDS);
            }
        }
        
    }
    
    /* Retire le client */
    private void removeClient(ClientHandler client) throws IOException {
        client.closeEverything(client.socket, client.bufferedReader, client.bufferedWriter);
    }
    
    /* Retire le client */
    public synchronized void removeClientFromGame(ClientHandler client) {
    	/* Client est retiré de players et viewers */
        viewers.remove(client);
        if (!gameOver && client == currentPlayer) {
            if (!players.isEmpty()) {
                try {
                	currentPlayer = getNextPlayer(client);
                	if (players.remove(client)) {
                        aliveMap.remove(client);
                        System.out.println("Removed player " + client.getClientName() + " from players (disconnected).");
                    }
                	if(currentPlayer != null) {
                		beginRound();
                	}
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
        	if (players.remove(client)) {
                aliveMap.remove(client);
                System.out.println("Removed player " + client.getClientName() + " from players (disconnected).");
            }
        }
    }


    /* Client toujours connecté */
    protected void isStillAlive(ClientHandler client){
        if(!gameOver) {
            aliveMap.put(client, true);
        }
    }

    /* Vérifie si le mot envoyé par le client est correct et est dans le dictionnaire */
    private static boolean checkWordAgainstDict(ClientHandler player, String word) throws IOException{
        String formatedWord = (Normalizer.normalize(word, Normalizer.Form.NFD).replaceAll("\\p{M}", "")).toUpperCase();

        if (formatedWord.contains(currentLetters) && words.contains(formatedWord)) {
            broadcastMessage("SENDW " + formatedWord + " C");
            currentLetters = generateLetterSequence(words);
            return true;
        } else {
            broadcastMessage("SENDW " + formatedWord + " I");
            return false;
        }
    }

    /* Vérifie si c'est au joueur actuel de jouer */
    private static boolean isClientTurn(ClientHandler client){
        if(!gameOver) {
            if(currentPlayer != client){
                return false;
            }
            aliveMap.put(client, true);
            //client.setAlive(true);
            return true;
        }
        return false;
        
    }

    /* Récupère les mots et les formate (sans accent, majuscule) */
    private static void getWordsFile(){
        try {
            words = Files.readAllLines(Paths.get("src/french"));
            for(int i = 0; i<words.size(); ++i){
                words.set(i, (Normalizer.normalize(words.get(i), Normalizer.Form.NFD).replaceAll("\\p{M}", "")).toUpperCase());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }   
    }

    /* Génère une suite de lettre */
    public static String generateLetterSequence(List<String> words) throws IOException{
        Random random = new Random();

        while(true){
            String word = words.get(random.nextInt(words.size())).toUpperCase();
            word = word.replaceAll("[^A-Z]", "");

            if(word.length() >= 3){
                int start = random.nextInt(word.length() - 2);
                int len = 2 + random.nextInt(2);
                if (start + len <= word.length()){
                    return word.substring(start, start + len);
                }
            }
        }
    }

    /* Envoi un message à tout le monde */
    private static void broadcastMessage(String message) throws IOException{
        if(!gameOver) {
            for(ClientHandler client : players){
                client.sendMessage(message);
            }
            for(ClientHandler client : viewers){
                client.sendMessage(message);
            }
        }
    }

    /* Récupère le prochain joueur */
    private ClientHandler getNextPlayer(ClientHandler currentPlayer) throws IOException{
    	if(players.isEmpty()) {
    		checkEndGame();
    		return null;
    	}
	    int index = players.indexOf(currentPlayer);
	    if(index == -1) {
	    	System.out.println("Player was not found in list.");
	    	return players.get(0);
	    }
	    
	    if(index == players.size() - 1){
	        index = 0;
	    } else {
	        index += 1;
	    }
	    System.out.println("Next player : " + players.get(index).getClientName());
	    return players.get(index);
	}

}