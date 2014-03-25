package udp;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;

public class Receiver {
	private static final int BUFFER_SIZE = 2024;
	private byte[] buffer;
	private Message message;
	private DatagramSocket socket;
	private String filename, init_string;
	private FileOutputStream file_os;
	private DatagramPacket init_packet, received_packet;
	private int bytes_received, bytes_to_receive, segmentID_expected;

	public Receiver(DatagramSocket socket) throws IOException {
		this.socket = socket;
	}

	public void recieveFile() throws IOException {
		buffer = new byte[BUFFER_SIZE];
		System.out.println("*** Ready to receive file on port: "+ socket.getLocalPort() + " ***");

		// get the file name and byte size of file name from the first packet received
		init_packet = receivePacket();
		init_string = "udp_client_upload_success_"+ new String(init_packet.getData(), 0, init_packet.getLength());
		StringTokenizer t = new StringTokenizer(init_string, "::");
		filename = t.nextToken();
		bytes_to_receive = new Integer(t.nextToken()).intValue();

		System.out.println("*** The file will be saved as: " + filename	+ " ***");
		System.out.println("*** Expecting to receive: " + bytes_to_receive	+ " bytes ***");

		file_os = new FileOutputStream(filename);

		// First checks that there is still more data to receive
		while (bytes_received < bytes_to_receive) {
			message = new Message();
			int addedBytes = 0;
			
			//  error check on received packets and catch missing ACK sent to sender
			do {
				received_packet = receivePacket();
				addedBytes = received_packet.getLength();
				try {
					message = (Message) deserialize(received_packet.getData());
				} catch (ClassNotFoundException ex) {
					System.out.println("*** Message packet failed. ***");
				}
			} while (message.getSegmentID() != segmentID_expected);

			segmentID_expected++;

			// handles the last byte segmentID size .getBytesToWrite()
			file_os.write(message.getPacket(), 0, message.getBytesToWrite());
			System.out.println("Received segmentID " + message.getSegmentID() + " | file data: " + message.getPacket().length * message.getSegmentID());
			bytes_received = bytes_received + message.getPacket().length;

			// Send ACK message (which is the segment id)
			String ACK = Integer.toString(message.getSegmentID());
			send(init_packet.getAddress(), init_packet.getPort(), (ACK).getBytes());
		}
		System.out.println("File transfer complete.");
		file_os.close();
	}

	private DatagramPacket receivePacket() throws IOException {
		buffer = new byte[BUFFER_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		return packet;
	}

	private void send(InetAddress receive, int port, byte[] message)
			throws IOException {
		DatagramPacket packet = new DatagramPacket(message, message.length,
				receive, port);
		socket.send(packet);
	}

	private Object deserialize(byte[] bytes) throws IOException,
			ClassNotFoundException {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		Object readObject = (Message) objectStream.readObject();
		return readObject;
	}
}