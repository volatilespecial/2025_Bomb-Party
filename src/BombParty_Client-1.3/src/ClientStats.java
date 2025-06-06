
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Client
	 * @version 1.3
	 * ClientStats : contient les informations concernant le client
	 */

public class ClientStats {
    
	/* Les différents états */
	public enum clientState {
        WAITING_NAMEP,
        WAITING_START,
        IN_GAME,
        VIEWER
    };

    private volatile clientState state = clientState.WAITING_NAMEP;
    private volatile int lives = 3;
    private volatile boolean myTurn;
    private volatile String username = null;
    private volatile String mode = null;
    private volatile boolean runThreads;

    public int getLives(){
        return lives;
    }

    public boolean getMyTurn(){
        return myTurn;
    }

    public clientState getState(){
        return state;
    }

    public String getUsername(){
        return username;
    }

    public String getMode(){
        return mode;
    }

    public Boolean getRunThreads(){
        return runThreads;
    }

    public void setMyTurn(boolean newValue){
        myTurn = newValue;
    }

    public void setState(clientState newState){
        state = newState;
    }

    public void setUsername(String newUsername){
        username = newUsername;
    }

    public void setMode(String newMode){
        mode = newMode;
    }

    public void setRunThreads(boolean newValue){
        runThreads = newValue;
    }

    public void removeLife(){
        lives--;
    }

    public Boolean isUsernameEmpty() {
        return username.isEmpty();
    }




    
}
