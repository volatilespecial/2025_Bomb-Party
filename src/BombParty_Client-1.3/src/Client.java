
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Client
	 * @version 1.3
	 * Client main
	 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Client {


    public static void main(String[] args){


        Socket socket = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        BlockingQueue<String> messageQueue;
        Scanner scanner = null;

        try {
        	
        	/* Connexion au serveur */
            socket = new Socket("localhost", 12345);
            
            System.out.println("Bomb Party - Client 1.3. Connection established.");

            inputStreamReader = new InputStreamReader(socket.getInputStream());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

            bufferedReader = new BufferedReader(inputStreamReader);
            bufferedWriter = new BufferedWriter(outputStreamWriter);

            messageQueue = new LinkedBlockingQueue<String>();

            scanner = new Scanner(System.in);

            ClientStats clientStats = new ClientStats();
            
            /* Création des objets dans des threads */
            Thread listener = new Thread(new Listener(bufferedReader, clientStats, messageQueue));
            Thread sender = new Thread(new Sender(bufferedWriter, messageQueue, clientStats));
            Thread input = new Thread(new Input(clientStats, messageQueue, scanner));

            clientStats.setRunThreads(true);

            listener.start();
            sender.start();
            input.start();
            
            System.out.println("Entrez votre pseudo et le mode (\"J\" pour Joueur ou \"S\" pour spectateur)");
            System.out.println("Exemple : BOB J");

            while(clientStats.getRunThreads()){}
            
            /* Si runThreads est false, arrêt des threads et déconnexion du serveur */
            if(!clientStats.getRunThreads()) {
                listener.interrupt();
                sender.interrupt();
                input.interrupt();
                if(scanner != null) scanner.close();
                if(socket != null) socket.close();
                if(inputStreamReader != null) inputStreamReader.close();
                if(outputStreamWriter != null) outputStreamWriter.close();
                if(bufferedReader != null) bufferedReader.close();
                if(bufferedWriter != null) bufferedWriter.close();
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}