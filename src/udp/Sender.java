package udp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class Sender {
	private static final int BUFFER_SIZE = 1024;
	private static final int TIME_OUT = 2000; // our t seconds that we timeout for at start
	private static final double ALPHA = 0.875; // from Jacobson’s algorithm
	private int segment_id;
	private byte[] received_bytes, sent_bytes;
	private FileInputStream file_reader;
	private DatagramSocket datagram_socket;
	private FileOutputStream file_writer;
	private int file_length, current_pos, bytes_read, bytes_read_total;
	private double rtt_start, rtt_end, rtt_total, bandwidth_estimated, bandwidth_total, rtt_estimated_avg, bandwidth_estimated_avg,
			rtt_estimated = 0;

	public Sender(DatagramSocket socket, DatagramPacket initPacket)
			throws IOException {
		received_bytes = new byte[BUFFER_SIZE]; // buffer used for file data
		sent_bytes = new byte[100]; // buffer mainly used for sending our ACK messages
		datagram_socket = socket;
		// setup DatagramSocket with correct Inetaddress and port of receiver
		datagram_socket.connect(initPacket.getAddress(), initPacket.getPort());
		segment_id = 0;
	}

	public void sendFile(File theFile) throws IOException {
		bytes_read_total = 0;
		file_reader = new FileInputStream(theFile);
		file_length = file_reader.available();
		
		// setup and begin writing to our output file that will have transfer statistics and delays
		file_writer = new FileOutputStream("udp_client_output_"+ theFile.getName()+".txt");
		file_writer.write(String.format("%nSegID   |   Bytes sent (%d)   |   Avg RTT (ms)   |   Estimated RTT (ms)   |   Avg Bandwidth (KB/s)   |   Estimated Bandwidth (KB/s)   %n",file_length).getBytes());
		file_writer.write(String.format("%n---------------------------------------------------------------------------------------------------------------------------------------%n").getBytes());
		
		System.out.println("*** Filename: " + theFile.getName() + " ***");
		System.out.println("*** Bytes to send: " + file_length + " ***");

		// Send file input (name and length) to server
		byte[] file_info_message =(theFile.getName() + "::" + file_length).getBytes(); 
		DatagramPacket file_info_packet = new DatagramPacket(file_info_message, file_info_message.length);
		datagram_socket.send(file_info_packet);
		
		DatagramPacket reply = new DatagramPacket(sent_bytes, sent_bytes.length);
		
		// controls when send operation is completed
		int packetsLost = 0;
		while (current_pos < file_length) {
			boolean receiveACK = false;
			
			// dynamically update timeout time based on our connection speeds
			datagram_socket.setSoTimeout(TIME_OUT * (int) rtt_estimated); 

			// store a batch of file data and create our Message obj
			bytes_read = file_reader.read(received_bytes);
			bytes_read_total += bytes_read;
			Message message = new Message(segment_id, received_bytes, bytes_read);
			System.out.println("\n~~~ Sending segment " + message.getSegmentID() + " with " + bytes_read + " byte payload.");
			current_pos = current_pos + bytes_read;
			
			byte[] message_serialized = serialize(message);
			
			DatagramPacket packet = new DatagramPacket(message_serialized, message_serialized.length);
			rtt_start = System.currentTimeMillis();
			datagram_socket.send(packet);
			// controls ACK messages from receiver
			// handle ACK of sent message object, timeout of 2 seconds.
			while (!receiveACK) {
				try {
					datagram_socket.receive(reply);
					rtt_end = System.currentTimeMillis() - rtt_start;
					System.out.println("\n+++ Received ACK to segment" + new String(reply.getData(), 0, reply.getLength()) + " RTT time: " + rtt_end + "ms");
					segment_id++;
					receiveACK = true;
				} catch (SocketTimeoutException e) { 
					// we've waited the timeout and no ACK received, assume packet is lost, send next packet
					rtt_end = TIME_OUT * (int) rtt_estimated;
					System.out.println("!!! Packet (segment" + new String(reply.getData(), 0, reply.getLength()) + ") has been lost");
					segment_id++;
					packetsLost++;
					receiveACK = true;
				}
			}
			// Calculate estimations and average stats
			rtt_estimated = ALPHA * rtt_estimated + (1 - ALPHA) * (rtt_end); // Using the  TCP estimation algorithm
			rtt_total += rtt_end;
			rtt_estimated_avg += rtt_estimated;
			bandwidth_total = ((bytes_read_total / 1000.0) / (rtt_total / 1000.0) );
			bandwidth_estimated = (bytes_read / 1000.0) / (rtt_estimated / 1000.0);
			bandwidth_estimated_avg += bandwidth_estimated;
						
			System.out.printf("%nBytes sent: %dbytes   |   Avg RTT: %.2fms   |   Estimated RTT: %.2fms   |   Avg BW: %.2fKB/s   |   Estimated BW: %.2fKB/s", 
					bytes_read, rtt_total / (segment_id + 1),	rtt_estimated, bandwidth_total, bandwidth_estimated );
			
			// write upload statistics to output file
			String output_str = String
					.format("%n%d       |   %12d         |   %8.2f       |   %15.2f        |   %17.2f        |   %20.2f   ",message.getSegmentID(),
							bytes_read, rtt_total / (segment_id + 1),	rtt_estimated, bandwidth_total, bandwidth_estimated);
			file_writer.write(output_str.getBytes());
			
		}
		String file_complete_str = String.format("%n%n Total Avg Estimated RTT: %.2fms   |   Total Avg Estimated BW: %.2fKB/s %n%n*** File transfer complete...",
				rtt_estimated_avg / (segment_id + 1) , bandwidth_estimated_avg / (segment_id + 1)); 
		file_writer.write(file_complete_str.getBytes());
		System.out.printf(file_complete_str);
		
		// Notify user of any lost packets
		if (packetsLost > 0) {
			file_complete_str = String.format("%n*** File may be corrupt, due to loss of %d packets.",packetsLost);
			System.out.println(file_complete_str);
			file_writer.write(file_complete_str.getBytes());
		}
	}

	public byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(obj);
		objectStream.flush();
		return byteStream.toByteArray();
	}
}