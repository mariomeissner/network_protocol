package blatt7;

public class Packet {
	
	private int seq;
	private int ack;
	private long checksum;
	private boolean isAck;
	private String data;
	private byte[] bytes;
	
	public Packet(int seq, int ack, String data, boolean isAck) {
		this.seq = seq;
		this.ack = ack;
		this.data = data;
		this.bytes = null; //TODO: Create the byte array
		//TODO: make checksum
	}
	
	public Packet(byte[] bytes) {
		//TODO: get info from the bytes
	}
	
	public byte[] getBytes(){
		//TODO: return the byte array
		return null;
	}
	
	public int length() {
		return bytes.length;
	}
	
	public int getSeq() {
		return seq;
	}
	
	public int getAck() {
		return ack;
	}
	
	public long getChecksum() {
		return checksum;
	}
	
	public boolean isAck() {
		return isAck;
	}
	
}
