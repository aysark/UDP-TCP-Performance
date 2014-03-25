package udp;

import java.io.Serializable;

public class Message implements Serializable {

    private int segmentID;
    private byte[] packet;
    private int bytesToWrite;

    public Message(){}

    public Message(int segmentID, byte[] packet, int bytesToWrite) {
        this.segmentID = segmentID;
        this.packet = packet;
        this.bytesToWrite = bytesToWrite;
    }

    public int getBytesToWrite() {
        return bytesToWrite;
    }

    public int getSegmentID() {
        return segmentID;
    }

    public byte[] getPacket() {
        return packet;
    }
}