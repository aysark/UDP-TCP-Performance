package udp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;

public class Client {
	private static byte[] buffer;
	private static final int BUFFER_SIZE = 1024;
	public static final int PORT_NUMBER = 26433; // set port number to 2000 +
													// student id last digits/2
	private static DatagramSocket socket;
	private static BufferedReader stdin;
	private static StringTokenizer userInput;
	private static DatagramPacket initPacket, packet;

	public static void main(String[] args) throws IOException {
		socket = new DatagramSocket();
		InetAddress address = InetAddress.getByName("localhost");

		buffer = new byte[BUFFER_SIZE];

		stdin = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("COMMANDS: send *file path*");
		System.out.println("\t  example: send C:/data.txt");
		System.out.print("udp> ");
		String selectedAction = stdin.readLine();

		userInput = new StringTokenizer(selectedAction);
		
		try {
			if (userInput.nextToken().equalsIgnoreCase("send")) {
				// send 'Send' command to server
				packet = new DatagramPacket((selectedAction).getBytes(),(selectedAction).getBytes().length, address,PORT_NUMBER);
				socket.send(packet);

				File theFile = new File(userInput.nextToken());

				initPacket = receivePacket();

				// create object to handle out going file
				Sender fileHandler = new Sender(socket, initPacket);
				fileHandler.sendFile(theFile);
			}
		} catch (Exception e) {
			System.err.println("Not valid input " + e.toString());
		}
		socket.close();
	}

	private static DatagramPacket receivePacket() throws IOException {
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		return packet;
	}
}