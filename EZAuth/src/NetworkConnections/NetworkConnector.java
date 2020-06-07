package NetworkConnections;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import Main.AccessManager;

public class NetworkConnector extends Thread {
	private ServerSocket socketServer;
	private AccessManager accessManager;

	public NetworkConnector(int port, AccessManager acm) throws IOException {
		socketServer = new ServerSocket(port);
		socketServer.setSoTimeout(10000);
		this.accessManager = acm;
		System.out.println("Network Connections Manager has been created");
	}

	public void run() {
		System.out.println("Network Connections Manager has been started");
		while (true) {
			try {
				System.out.println("Waiting for you new connections: " + socketServer.getLocalPort());
				Socket server = socketServer.accept();
				SocketAddress remoteIp=server.getRemoteSocketAddress();
				System.out.println("Connected to: " +remoteIp );
				DataInputStream input = new DataInputStream(server.getInputStream());
				String incomingData = input.readUTF();
				JSONObject out=null;
				Object Ob = null;
				try {
					Ob = new JSONParser().parse(incomingData);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				JSONObject jsonOb = (JSONObject) Ob;
				if (jsonOb.get("Request") == "CONNECTION") {
					 out=this.accessManager.executeJSON(incomingData,remoteIp);
				} else {
					String data=this.accessManager.decrypt(remoteIp, incomingData);
					 out = this.accessManager.executeJSON(data,remoteIp);
				}
				DataOutputStream outStream = new DataOutputStream(server.getOutputStream());
				outStream.writeUTF(JSONObject.toJSONString(out));
			} catch (SocketTimeoutException s) {
				System.out.println("The server has timed out!");
			} catch (IOException exp) {
				System.err.println("Woah, something really bad has happened");
				exp.printStackTrace();
				break;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
