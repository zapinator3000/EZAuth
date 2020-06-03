package NetworkConnections;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class NetworkConnector extends Thread {
	private ServerSocket socketServer;
	public NetworkConnector(int port) throws IOException {
		socketServer = new ServerSocket(port);
		socketServer.setSoTimeout(10000);
		System.out.println("Network Connections Manager has been created");
	}

	public void run() {
		System.out.println("Network Connections Manager has been started");
		while(true) {
			try {
				System.out.println("Waiting for you new connections: "+socketServer.getLocalPort());
				Socket server = socketServer.accept();
				System.out.println("Connected to: "+server.getRemoteSocketAddress());
				DataInputStream input = new DataInputStream(server.getInputStream());
				String incomingData=input.readUTF();
				
			}catch (SocketTimeoutException s) {
				System.out.println("The server has timed out!");
			}catch(IOException exp) {
				System.err.println("Woah, something really bad has happened");
				exp.printStackTrace();
				break;
			}
		}
	}

}
