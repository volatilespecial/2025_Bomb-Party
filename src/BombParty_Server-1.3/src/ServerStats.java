
	/**
	 * @author Anparasan ANPUKKODY
	 * Bomb Party - Server
	 * @version 1.3
	 * ServerStats : serveur d�marr� ou non
	 */

public class ServerStats {

    private volatile boolean isServerRunning = true;

    public void setIsServerRunning(boolean newValue) {
        isServerRunning = newValue;
    }

    public boolean getIsServerRunning() {
        return isServerRunning;
    }
}
