package udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public class Server {
	public static final int PORT_NUMBER = 26433; // set port number to 2000 +
													// student id last digits/2
	private static final int BUFFER_SIZE = 1024;
	private static DatagramSocket server;
	private static int clientID = 1;

	public static void main(String[] args) throws IOException {
		System.out.println("Server started.");

		ServerSocket socket = null;
		byte[] buffer = new byte[BUFFER_SIZE];

		try {
			server = new DatagramSocket(PORT_NUMBER);
		} catch (IOException e) {
			System.err.println("Could not listen on port: "
					+ socket.getLocalPort() + ".");
			System.exit(-1);
		}

		while (true) {
			try {
				DatagramPacket packet = new DatagramPacket(buffer,
						buffer.length);
				server.receive(packet);
				System.out.println("SERVER: Accepted connection - received"
						+ new String(packet.getData(), 0, packet.getLength()));
				// new socket created with random port for thread
				DatagramSocket threadSocket = new DatagramSocket();
				new Thread(new ClientConnection(threadSocket, packet,
						clientID++)).start();
			} catch (Exception e) {
				System.err.println("Error in connection attempt.");
			}
		}
	}

	public static class ClientConnection implements Runnable {
		private byte[] buffer;
		private int clientID;
		private DatagramSocket clientSocket;
		private DatagramPacket packet;

		public ClientConnection(DatagramSocket clientSocket,
				DatagramPacket packet, int clientID) {
			this.clientSocket = clientSocket;
			this.packet = packet;
			this.clientID = clientID;
		}

		public void run() {
			try {
				buffer = new byte[BUFFER_SIZE];
				System.out.println("THREAD: "
						+ new String(packet.getData(), 0, packet.getLength()));

				// sends a message gets the new port information to the client
				DatagramPacket packet = new DatagramPacket(("OK").getBytes(),
						("OK").getBytes().length, this.packet.getAddress(),
						this.packet.getPort());
				clientSocket.send(packet);

				// create Object to handle incoming file
				Receiver fr = new Receiver(clientSocket);
				fr.recieveFile();

			} catch (IOException ex) {
				System.out.println("IOException: " + ex);
			}
			System.out.println("*** Transfer for client " + clientID
					+ " complete. ***");
		}
	}
}