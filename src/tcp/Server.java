package tcp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
	private final static int PORT_NUMBER = 26433; // set port number to 2000 + student id last digits/2
	private final static int BUFFER_SIZE = 2024;
    private static ServerSocket server_socket;
    private static Socket client_socket = null;

    public static void main(String[] args) throws IOException {
        try {
            server_socket = new ServerSocket(PORT_NUMBER);
            System.out.println("Server started.");
        } catch (Exception e) {
            System.err.println("Port already in use.");
            System.exit(1);
        }

        while (true) {
            try {
                client_socket = server_socket.accept();
                System.out.println("Accepted connection : " + client_socket);
                Thread t = new Thread(new ConnectionRequestHandler(client_socket));
                t.start();
            } catch (Exception e) {
                System.err.println("Error in connection attempt.");
            }
        }
    }
    
    // We use 'implements Runnable' over 'extends Thread' because we are not overriding/specializing any thread behaviour
    public static class ConnectionRequestHandler implements Runnable {
    	private Socket client_socket;
        private BufferedReader in = null;

        public ConnectionRequestHandler(Socket client) {
            this.client_socket = client;
        }
    	   
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(
                        client_socket.getInputStream()));
                String clientSelection;
                while ((clientSelection = in.readLine()) != null) {
                	// wait till send file command is given by client
                    if (clientSelection.equalsIgnoreCase("send")) {
                    	System.out.println("~~ Received send command from client...");
                    	try {
            	            int bytesRead = 0;
            	            int bytesRead_total = 0;
            	            int segmentID = 0;
            	            DataInputStream clientData = new DataInputStream(client_socket.getInputStream());
            	            
            	            String fileName = clientData.readUTF();
            	            
            	            // we write the sent file to another file to demonstrate capability of client successful upload
            	            OutputStream output = new FileOutputStream(("tcp_client_upload_success_" + fileName));
            	            long file_length = clientData.readLong();
            	            byte[] buffer = new byte[BUFFER_SIZE];
            	            
            	            // keep reading client file data by batched segments based on our buffer
            	            while (bytesRead_total < file_length) {
            	            	bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, file_length));
            	            	bytesRead_total += bytesRead;
            	                System.out.println(segmentID + " : "+bytesRead+" / "+file_length+" bytes");
            	            	output.write(buffer, 0, bytesRead);
            	                segmentID++;
            	            }

            	            output.close();
            	            clientData.close();
            	            System.out.println("File "+fileName+" received from client.");
            	        } catch (IOException ex) {
            	            System.err.println("Client error. Connection closed.");
            	        }
                    }
                    in.close();
                    break;
                }

            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }
}