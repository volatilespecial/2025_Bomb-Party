
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Server
	 * @version 1.3
	 * ClientHandler : gestion du client et de ses entrées
	 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    protected Socket socket;
    protected BufferedReader bufferedReader;
    protected BufferedWriter bufferedWriter;
    private String clientUsername;
    private char mode;
    public enum ClientState {
        WAITING_NAMEP,
        WAITING_START,
        IN_GAME
    };
    private ClientState state = ClientState.WAITING_NAMEP;

    private boolean Alive = false;

    private ServerStats serverStats;

    private static GameManager gameManager = null;
    
    private boolean firstConnection = true;

    protected static AliveChecker aliveChecker;

    public ClientHandler(Socket socket, ServerStats serverStats){
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.serverStats = serverStats;
        } catch (IOException e){
        }
    }

    @Override
    public void run() {
        String fromClient;
        while(serverStats.getIsServerRunning()) {
            try{
            	if(firstConnection) {
                    this.sendPlayersList("self");
                    firstConnection = false;
                }
            	
            	/* Récupère ce que le client a envoyé */
                fromClient = bufferedReader.readLine();
                if(fromClient == null){
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
                
                /* Si l'état est IN_GAME */
                if(state == ClientState.IN_GAME){
                	/* Si ALIVE, met ce client en alive */
                    if(fromClient.startsWith("ALIVE")){
                        gameManager.isStillAlive(this);
                    } else {
                        gameManager.handleGameInput(this, fromClient);
                    }
                } 
                /* Si l'état est WAITING_NAMEP */
                else if (state == ClientState.WAITING_NAMEP){
                    if(fromClient.startsWith("NAMEP")) {
                        handleNamepCommand(fromClient);
                    } else if (fromClient.startsWith("ALIVE")) {
                        this.setAlive(true);
                    } else {
                        sendError(11);
                    }
                } 
                /* Si l'état est WAITING_START */
                else if (state == ClientState.WAITING_START) {
                    if(fromClient.startsWith("START")) {
                        handleStartCommand(fromClient);
                    } else if (fromClient.startsWith("ALIVE")) {
                        this.setAlive(true);
                    } else {
                        sendError(11);
                    }
                } else {
                    sendError(11);
                }

            } catch (IOException e){

                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
        return;
    }

    /* Vérifie si le pseudo est valide */
    public int isUsernameValid(String username){
        if (!username.matches("[a-zA-Z0-9]{1,10}"))
            return 21;
        for(ClientHandler client :  clientHandlers){
            if(client.clientUsername != null && client.clientUsername.equals(username))
                return 22;
        }
        return 0;
    }

    /* Gestion de NAMEP */
    private void handleNamepCommand(String command) throws IOException{

        if(state != ClientState.WAITING_NAMEP){
            sendError(01);
        }else{
            int retv;
            String[] parts = command.split(" ");
            if(parts.length != 3){
                sendError(10);
            } else if (!parts[0].equals("NAMEP")){
                sendError(11);
            } else if((retv = isUsernameValid(parts[1])) != 0){
                sendError(retv);
            } else if (!parts[2].equals("J") && !parts[2].equals("S")){
                sendError(23);
            } else if (parts[2].equals("J") && clientHandlers.size() >= 16) {
                sendError(02);
            } else {
                this.clientUsername = parts[1];
                this.mode = parts[2].charAt(0);
                this.state = ClientState.WAITING_START;
                this.setAlive(true);
                clientHandlers.add(this);
                broadcastMessage("NAMEP " + clientUsername + " " + mode);
                this.sendPlayersList("all");
            }
        }
    }

    /* Gestion de START */
    private void handleStartCommand(String command) throws IOException {
    	if (this.state != ClientState.WAITING_START){
            sendError(01);
        }
        else if(this != clientHandlers.get(0)){
            sendError(32);
        }else if(clientHandlers.size() < 2){
            sendError(31);
        }else{
        	aliveChecker.shutdown();
            for(ClientHandler client : clientHandlers){
                client.state = ClientState.IN_GAME;
                client.setAlive(true);
            }
            broadcastMessage("START");
            gameManager = new GameManager();
            gameManager.startGame(clientHandlers, serverStats);
            //GameManager.startGame(clientHandlers, serverStats);
        }
    }

    /* Envoi des erreurs */
    protected void sendError(int errorcode){
    	//System.out.println("Error code : " + errorcode);
        if(bufferedWriter != null){
            try {
				bufferedWriter.write("ERROR " + errorcode);
				bufferedWriter.newLine();
	            bufferedWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				closeEverything(this.socket, this.bufferedReader, this.bufferedWriter);
			}
            
        }
    }

    /* Envoi un message au client */
    protected void sendMessage(String message) throws IOException {
        if(bufferedWriter != null){
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
    }

    /* Envoi d'un message à tout les clients identifiés */
    public void broadcastMessage(String message){
        for(ClientHandler client : clientHandlers){
            try {
                client.bufferedWriter.write(message);
                client.bufferedWriter.newLine();
                client.bufferedWriter.flush();
            } catch (Exception e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    /* Retire un client */
    public void removeClientHandler() {
        clientHandlers.remove(this);
    }

    /* Déconnexion du client */
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClientHandler();
        try {
        	/* Retire du jeu si en partie */
        	if(gameManager != null) gameManager.removeClientFromGame(this);
            if(socket != null) socket.close();
            if(bufferedReader != null) bufferedReader.close();
            if(bufferedWriter != null) bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /* Envoi la liste des joueurs aux clients identifiés */
    protected void sendPlayersList(String toWho) throws IOException {
        String players = "";
        for(ClientHandler client : clientHandlers){
            if(client.mode == 'J'){
                if(players.equalsIgnoreCase("")){
                    players = players + client.clientUsername;
                } else {
                    players = players + "-" + client.clientUsername;
                }
                
            }
        }
        players = "PLYRS [" + players + "]";
        if(toWho == "self") {
            sendMessage(players);
        } else {
            broadcastMessage(players);
        }
    }

    public char getClientMode(){
        return mode;
    }

    public void setClientMode(char newMode){
        mode = newMode;
    }

    public String getClientName(){
        return clientUsername;
    }
    
    public void setAlive(boolean state){
        Alive = state;
    } 
    
    public boolean getAlive(){
        return Alive;
    }
}
