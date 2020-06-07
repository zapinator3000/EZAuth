package Main;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.StringValidator;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
	private JSONObject response;
	private HashMap<SocketAddress,PrivateKey> privateKeys;
	private HashMap<SocketAddress,PublicKey> publicKeys;
	private UserManager userManager;
	private HashMap<SocketAddress, Key> decryptKeys;
	
	public AccessManager(Key accessKey,UserManager usrmgr) {
		this.finalKey = accessKey;
		this.setServerTicks((long) 0);
		this.gameKeys = new HashMap<Key, Long>();
		this.setCurrentKey(Key.generateKey());
		this.userManager=usrmgr;
		this.decryptKeys= new HashMap<SocketAddress,Key>();
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
	/*
	 * Execute the JSON command and return with the response
	 * @param command
	 * @return a JSON response
	 */
	public JSONObject executeJSON(String command, SocketAddress ip) throws ParseException {
		Object Ob=new JSONParser().parse(command);
		JSONObject out= new JSONObject();
		JSONObject jsonOb=(JSONObject)Ob;
		String executeCommand=(String)jsonOb.get("Request");
		if(executeCommand.equals("LOGIN")) {
			Key key = new Key((String)jsonOb.get("ACCESS_KEY"));
			String username=(String)jsonOb.get("USERNAME");
			String password=(String)jsonOb.get("PASSWORD");
		}else if(executeCommand.equals("CREATE_USER")) {
			Key key = new Key((String)jsonOb.get("ACCESS_KEY"));
			String username=(String)jsonOb.get("USERNAME");
			String password=(String)jsonOb.get("PASSWORD");
			if(this.checkGameValidity(key)) {
				if(this.userManager.createUser(username, password)) {
					out.put("RESPONSE", 200);
				}else {
					out.put("RESPONSE",1000);
				}
			}else {
				out.put("RESPONSE", 401);
			}
			out.put("RESPONSE",200);
			
		}else if(executeCommand.equals("CONNECTION")) {
			PublicKey publicKey=(PublicKey)jsonOb.get("PUBLIC_KEY");
			this.publicKeys.put(ip, publicKey);
			KeyPair keys=null;
			try {
				 keys=this.GenerateConnectionKey();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.privateKeys.put(ip,keys.getPrivate());
			Key decryptionKey = this.generateKey();
			this.decryptKeys.put(ip,decryptionKey );
			out.put("RESPONSE", "200");
			out.put("PUBLIC_KEY", keys.getPublic());
			out.put("ACCESS_KEY", this.generateKey());
			out.put("CONNECTION_KEY",decryptionKey);
		
		}else {
			out.put("RESPONSE",501);
		}
		return out;
	}
	/*
	 * Encrypt RSA
	 * Based on https://niels.nu/blog/2016/java-rsa.html
	 * @param plainText
	 * @param user
	 * @return String
	 */
	public String doEncryptRSA(String plainText,String ip) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher encryptCipher = Cipher.getInstance("RSA");
		
		encryptCipher.init(Cipher.ENCRYPT_MODE, this.publicKeys.get(ip));
		byte[] cipherText = null ;
		try {
			cipherText= encryptCipher.doFinal(plainText.getBytes());
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Base64.getEncoder().encodeToString(cipherText);
	}
	/*
	 * Decrypt RSA
	 * Based on: https://niels.nu/blog/2016/java-rsa.html
	 * @param cipherText
	 * @param user
	 * @return string
	 */
	public  String doDecryptRSA(String cipherText, String ip) throws Exception {
	    byte[] bytes = Base64.getDecoder().decode(cipherText);

	    Cipher decriptCipher = Cipher.getInstance("RSA");
	    decriptCipher.init(Cipher.DECRYPT_MODE, this.privateKeys.get(ip));

	    return new String(decriptCipher.doFinal(bytes));
	}
	public KeyPair GenerateConnectionKey() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair pair = keyGen.generateKeyPair();
		return pair;
	}
	public String decrypt(SocketAddress remoteIp, String token) {
		Token t = Token.fromString(token);
		return t.validateAndDecrypt(this.decryptKeys.get(remoteIp),validator);
	}

}
