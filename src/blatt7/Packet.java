package blatt7;

public class Packet {
	
	private int seq;
	private int ack;
	private long checksum;
	private boolean isAck;
	private String data;
	
	public Packet(int seq, int ack, String data, boolean isAck) {
		this.seq = seq;
		this.ack = ack;
		this.data = data;
		//TODO: make checksum
	}
	
	public Packet(Byte[] bytes) {
		//TODO: get info from the bytes
	}
	
	public Byte[] getBytes(){
		//TODO: return the byte array
		return null;
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
