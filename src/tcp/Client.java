package tcp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class Client {

    private static final int BUFFER_SIZE = 1024;
	private static final int TIME_OUT = 2000;
	private static final int PORT_NUMBER = 26433;
	private static final double ALPHA = 0.875; // from Jacobson’s algorithm
	private static Socket socket;
    private static String file_name;
    private static BufferedReader stdin;
    private static FileOutputStream file_writer;
    private static PrintStream os;
    private static int file_length, current_pos=0, bytes_read, bytes_read_total, segment_id=0;
    private static double rtt_start, rtt_end, rtt_total, bandwidth_estimated, bandwidth_total, rtt_estimated_avg, 
    bandwidth_estimated_avg, rtt_estimated = 0;

    public static void main(String[] args) throws IOException {
        try {
            socket = new Socket("localhost", PORT_NUMBER);
            stdin = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            System.err.println("Cannot connect to the server, try again later.");
            System.exit(1);
        }
        System.out.println("Hi there, please provide a command (send).");
        os = new PrintStream(socket.getOutputStream());
        try {
        	if (stdin.readLine().contains("send")) {
        		os.println("send");
                try {
                    System.err.print("Enter file path (ie. C:/data.txt): ");
                    file_name = stdin.readLine();
                    
                    // setup input streams for sending data and file byte array
                    File myFile = new File(file_name);
                    
                    // setup and begin writing to our output file that will have transfer statistics and delays
            		file_writer = new FileOutputStream("tcp_client_output_"+ myFile.getName()+".txt");
            		file_writer.write(String.format("%n   Bytes sent (%d)   |   Avg RTT (ms)   |   Estimated RTT (ms)   |   Avg Bandwidth (KB/s)   |   Estimated Bandwidth (KB/s)   %n"
            				,myFile.length()).getBytes());
            		file_writer.write(String.format("%n---------------------------------------------------------------------------------------------------------------------------------------%n").getBytes());
                    
            		byte[] file_bytes = new byte[BUFFER_SIZE];
                    FileInputStream file_is = new FileInputStream(myFile);
                    BufferedInputStream buffered_is = new BufferedInputStream(file_is);
                    DataInputStream data_is = new DataInputStream(buffered_is);
                    file_length = (int) myFile.length();

                    OutputStream os = socket.getOutputStream();

                    // send file name, length and file bytes array to server
                    DataOutputStream dos = new DataOutputStream(os);
                    dos.writeUTF(myFile.getName());
                    dos.writeLong(file_length);
                    
                    while (current_pos < file_length) {
                    	// dynamically update timeout time based on our connection speeds
                    	socket.setSoTimeout(TIME_OUT * (int) rtt_estimated);
                    	
                    	bytes_read = data_is.read(file_bytes);
                    	bytes_read_total += bytes_read;
                    	System.out.println("\n~~~ Sending segment "+segment_id+" with " + bytes_read + " byte payload (/"+file_length+").");
                    	current_pos = current_pos + bytes_read;
                    	
                    	// begin timing and send file bytes to server
                    	rtt_start = System.currentTimeMillis();
                    	dos.write(file_bytes, 0, bytes_read);
                    	dos.flush();
	                    rtt_end = System.currentTimeMillis() - rtt_start;
	                    segment_id++;
	                    
	                    // Calculate estimations and average stats
	        			rtt_estimated = ALPHA * rtt_estimated + (1 - ALPHA) * (rtt_end); // Using the  TCP estimation algorithm
	        			rtt_total += rtt_end;
	        			rtt_estimated_avg += rtt_estimated;
	        			bandwidth_total = ((bytes_read_total / 1000.0) / (rtt_total / 1000.0) );
	        			System.out.println("bw_total "+ bandwidth_total + " - bytes_read_total "+ file_length + " - rtt_total "+ rtt_total);
	        			bandwidth_estimated = (bytes_read / 1000.0) / (rtt_estimated / 1000.0);
	        			bandwidth_estimated_avg += bandwidth_estimated;
	        			
	        			System.out.printf("%nBytes sent: %d bytes   |   Avg RTT: %.2fms   |   Estimated RTT: %.2fms   |   Avg BW: %.2fKB/s   |   Estimated BW: %.2fKB/s",
	        					bytes_read, rtt_total / (segment_id+1),	rtt_estimated, bandwidth_total, bandwidth_estimated );
	
	        			// 	write upload statistics to output file
	        			String output_str = String.format("%n   %12d         |   %8.2f       |   %15.2f        |   %17.2f        |   %20.2f   ",
	        					bytes_read, rtt_total / (segment_id+1),	rtt_estimated, bandwidth_total, bandwidth_estimated);
	        			file_writer.write(output_str.getBytes());
                    }
                } catch (Exception e) {
                    System.err.println("File does not exist!");
                }
                String file_complete_str = String.format("%n%n Total Avg Estimated RTT: %.2fms   |   Total Avg Estimated BW: %.2fKB/s %n%n*** File transfer complete...",
        				rtt_estimated_avg / (segment_id + 1) , bandwidth_estimated_avg / (segment_id + 1)); 
        		file_writer.write(file_complete_str.getBytes());
        		System.out.printf(file_complete_str);
        	}
        } catch (Exception e) {
            System.err.println("not valid input");
        }
        socket.close();
    }
}