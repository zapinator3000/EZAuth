import java.util.ArrayList;

import com.macasaet.fernet.Key;

/*
 * Manages User logins and encryption. Handles Re-encryption methods provided
 * rolling keys
 * @Author Zackery Painter
 */
public class UserManager {
	private Key currentKey;
	private ArrayList<User> users;
	private Key accessKey;

	public UserManager(Key accessKey, Key currentKey) {
		// TODO Auto-generated constructor stub
		this.setAccessKey(accessKey);
		this.users = new ArrayList<User>();
		this.currentKey = currentKey;
	}

	public boolean login(String username, String password) {

		return false;
	}

	public void createUser(String username, String password) {
		users.add(new User(username, password, this));

	}

	public boolean reEncrypt(Key newKey) {
		System.out.println("Running re-encrypt");
		for (User user : users) {
			String p = user.decryptPass(this.accessKey, this.currentKey);
			// System.out.println("Password: "+p);
			this.currentKey = newKey;
			user.setPassword(p);
		}
		return true;
	}

	public Key getCurrentKey() {
		return currentKey;
	}

	public void setCurrentKey(Key currentKey) {
		this.currentKey = currentKey;
	}

	public Key getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(Key accessKey) {
		this.accessKey = accessKey;
	}

	public String getPass(Key accessKey, String user) {
		if (accessKey.equals(this.accessKey)) {
			User us = searchForUser(user);
			return us.decryptPass(this.accessKey, this.currentKey);
		} else {
			System.out.println("Fail");
			return "Fail";
		}
	}

	public User searchForUser(String username) {
		for (User user : users) {
			// System.out.println(user.getUsername());

			if (user.getUsername().equals(username)) {
				return user;
			}
		}
		// System.out.println("Fail");
		return null;
	}
}
