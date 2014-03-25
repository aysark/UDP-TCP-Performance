Comparing UDP and TCP architectures in delay and bandwidth.
===================

Free to use and modify.

Built a basic UDP and TCP architecture network resource measurement and estimation scheme.  The measurands were namely the bandwidth that is the data amount over the time it takes to send it.  And the roundtrip time (RTT) which is the time it takes to send one segment or packet and receive an acknowledgement from the server.  The scenario was designed so that the client sends the file to the server, as in the client is uploading to the server.

How it Works
============

UDP Application
---------------
The UDP application starts at the Server class, which sets up the necessary initial packet that is to be received to initiate a connection.  This allows us to capture the connection data (IP and port) to be able to open a port for that DatagramSocket.  For each DatagramSocket is a client which is handled by a thread.  The thread instantiates a Receiver object which handles file receiving from a client.

The Receiver first expects an initial packet that contains the file name and size.  This allows us to create the necessary output streams and buffers.  Next we continuously loop while keeping track of the bytes received from incoming packets.  Packets are deserialized onto an abstracted object called a Message that serves as a container for the packet data, size and segment ID.  This is done until we have reached the bytes received to be greater than file length.
In the Client end, once we get the send command from the user, an initial packet is sent to the Server regarding the action of the user and our connection details (IP and port).  Next we call our Sender.java class to handle sending of the file.  In the Sender class, after setting up the streams, we begin by keeping track of our position in the file using current_pos and continuously loop until current_pos is greater than the file length.  We set out packet timeout time to be 2 seconds * the connections estimated delay time so far.  This follows the original TCP RTT time algorithm.  Next we read a fix amount of data (based on our buffer size) from the file to be sent and create a Message object that contains a segment id, the data and the length of the data.  We send a packet containing this Message object and begin our timing of RTT.  Once we get a reply (ACK), we take our end time for RTT.  If it so happens that we time out and not get a reply, then we know that the packet has been lost but we do not do anything and just continue with the next packet- we do not retransmit.  

We calculated our bandwidth by taking the total bytes read from the file so far over the total delay time so far.  The estimated bandwidth- by taking the bytes read in that packet instance over the estimated RTT.  The estimated RTT using the original TCP RTT estimated algorithm (discussed further below). 

TCP Application
---------------
The TCP application builds on previous assignments Server-Client architecture and follows a concurrent model.  The main modifications were done on Client.java which sends file name and length info via a DataOutputStream- this was a very convenient class since it allows once to send Java primitive types and not just send bytes.  Next we segmented our data based a buffer size defaulted to 1024.  Each segment gets written out to the stream and flushed; this operation was timed as our RTT.  Since TCP sends a continuous stream of data, it was timed like this.


How to Run
=========
Both applications have been tested thoroughly using Windows 7 on Eclipse IDE.   If running without Eclipse, one will have to remove the ‘package’ line in the beginning of each java file before compiling.

To run the UDP application, first compile all java files and then run Server.java in one console.  And run Client.java in another console.  Next interact with Client.java by sending a ‘send *file path*’ command, for example: 
> send C:\Users\Z\a2\a2\test_files\small.txt

Please note the file path must be full path to the file.  This will begin the process of the client uploading the small.txt file to the server, you will see various outputs indicating the progress of the upload.  Only one file can be sent during a client instance, after the file is sent, you will need to end the process and start the client again.  You can run multiple client instances at the same time.
Once the file transfer is complete, two files are outputted by the client.  One is udp_client_upload_success_*file_name*.txt which contains all measurements and predictions.  And the other is udp_client_output_*file_name* which is the file the was uploaded on the server.

To run the TCP application, first compile all java files, and then run Server.java in one console.  And run Client.java in another console.  Next interact with Client.java by sending a ‘send’ command, for example: 
send

Then you will be prompted to give the file path of the file, the file path must be full path to the file.  Only one file can be sent during a client instance, after the file is sent, you will need to end the process and start the client again.  You can run multiple client instances at the same time.

Once the file transfer is complete, two files are outputted by the client.  One is tcp_client_upload_success_*file_name*.txt which contains all measurements and predictions.  And the other is tcp_client_output_*file_name* which is the file the was uploaded on the server.
