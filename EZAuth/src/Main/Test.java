package Main;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.NoSuchPaddingException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.macasaet.fernet.Key;

import NetworkConnections.NetworkConnector;

public class Test {
	public static void main(String[] args) {
		try {
			new Test();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Test() throws NoSuchAlgorithmException {
		Key accessKey = Key.generateKey();
		AccessManager mgr=new AccessManager(accessKey);
		System.out.print("Test 1 -> Generate Connection Key : ");
		KeyPair keys=mgr.GenerateConnectionKey();
		System.out.println(keys.toString());
		UserManager usrmgr = new UserManager(accessKey, accessKey);
		User testUser=new User("Test","This is the password",usrmgr);
		testUser.setMyPublicKey(keys.getPublic());
		testUser.setPrivateKey(keys.getPrivate());
		System.out.print("Test 2 -> Encrypt using RSA : ");
		String testCipher = null;
		try {
			testCipher=mgr.doEncryptRSA(accessKey.serialise(), testUser);
			System.out.println(testCipher);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print("Test 3 -> Decrypt using RSA : ");
		String out=null;
		try {
			out=mgr.doDecryptRSA(testCipher, testUser);
			System.out.println(out);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print("Test 4 -> Test JSON : ");
		JSONObject jo = new JSONObject();
		jo.put("Request", "CREATE USER");
		try {
			mgr.executeJSON(jo.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print("Test 5 -> Re-decrypt password with decrypted Token : ");
		Key outKey = new Key(out);
		String outPass=testUser.decryptPass(accessKey, outKey);
		System.out.println(outPass);
		System.out.println("Done, Starting network...");
		try {
			new NetworkConnector(6066).start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}