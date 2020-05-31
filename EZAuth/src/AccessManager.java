import java.util.ArrayList;
import java.util.HashMap;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

/*
 * This class handles external Access, including rolling keys
 * @Author Zackery Painter
 * 
 */
public class AccessManager {
	private final Validator<String> validator = new StringValidator() {
	};
	public static int EXPIRATION_COUNT = 30000;
	private long serverTicks;
	private HashMap<Key, Long> gameKeys;
	private Key currentKey;
	private Key finalKey;

	public AccessManager(Key accessKey) {
		this.finalKey = accessKey;
		this.setServerTicks((long) 0);
		this.gameKeys = new HashMap<Key, Long>();
		this.setCurrentKey(Key.generateKey());
	}

	/*
	 * Update the ticks
	 */
	public void update() {
		this.setServerTicks((long) (this.getServerTicks() + 1));

	}

	/*
	 * If the given key does not exist, return false If the key is expired, reject
	 * the key and return false If the key is valid return true and accept it
	 * 
	 * @param key
	 */
	public boolean checkGameValidity(Key key) {
		if (gameKeys.containsKey(key)) {
			long creationTime = gameKeys.get(key);
			long offset = EXPIRATION_COUNT + creationTime;
			if (offset > getServerTicks()) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public Key generateKey() {
		Key key = Key.generateKey();
		this.gameKeys.put(key, getServerTicks());
		return key;
	}

	public long getServerTicks() {
		return serverTicks;
	}

	public void setServerTicks(Long serverTicks) {
		this.serverTicks = serverTicks;
	}

	public void changeKey(Key accessKey) {
		if (!accessKey.equals(this.finalKey)) {
			System.err.println("Correct key not provided!");
		} else {
			this.setCurrentKey(Key.generateKey());
		}
	}

	public Key getCurrentKey(Key accessKey) {
		if (accessKey.equals(this.finalKey)) {
			return currentKey;
		} else {
			return Key.generateKey();
		}
	}

	public void setCurrentKey(Key currentKey) {
		this.currentKey = currentKey;
	}

}
