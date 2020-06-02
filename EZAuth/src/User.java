
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;

public class User {
	private Token password;
	private String username;
	private int uniqueId;
	private UserManager myUserManager;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private PublicKey myPublicKey;
	private final Validator<String> validator = new StringValidator() {
	};
	public User(String user, String password, UserManager manager) {
		this.myUserManager = manager;
		this.password = this.encryptPass(password);
		this.setUsername(user);
	}

	public void setPassword(String pass) {
		this.password = this.encryptPass(pass);
	}

	public void setUsername(String user) {
		this.username = user;
	}

	public Token encryptPass(String pass) {
		return Token.generate(this.myUserManager.getCurrentKey(), pass);
	}

	public String decryptPass(Key accessKey, Key keyToDecrypt) {
		if (this.myUserManager.getAccessKey().equals(accessKey)) {
			return validator.validateAndDecrypt(keyToDecrypt, this.password);
		} else {
			if (EZAuthMain.logLevel == 2) {
				System.err.println("Invalid accessKey was given");
			}
			return "INVALID";
		}
	}

	public void setMyPublicKey(PublicKey pubKey) {
		this.myPublicKey=pubKey;
	}
	public void setPrivateKey(PrivateKey privKey) {
		this.privateKey=privKey;
	}
	public PrivateKey getPrivateKey() {
		return this.privateKey;
	}
	
	public PublicKey getPublicKey() {
		return this.publicKey;
	}
	public PublicKey getMyPublicKey() {
		return this.myPublicKey;
	}
	public String getUsername() {
		return this.username;
	}
}
