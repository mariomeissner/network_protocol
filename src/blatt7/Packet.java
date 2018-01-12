package blatt7;

public class Packet {
	
	private int seq;
	private int ack;
	private long checksum;
	private boolean isAck;
	private byte[] payload;
	private byte[] packetBytes;
	
	public Packet(int seq, int ack, byte[] payload, boolean isAck) {
		this.seq = seq;
		this.ack = ack;
		this.payload = payload;
		this.payload = null; //TODO: Create the byte array
		//TODO: make checksum
	}
	
	public Packet(byte[] bytes) {
		//TODO: get info from the bytes
	}
	
	public byte[] getBytes(){
		//TODO: return the byte array
		return null;
	}
	
	/**
	 * Get packet length
	 * @return the length of the packet in bytes
	 */
	public int length() {
		return payload.length;
	}
	
	public int getSeq() {
		return seq;
	}
	
	public int getAck() {
		return ack;
	}
	
	public long checkChecksum() {
		//TODO: check the checksum
		return checksum;
	}
	
	public boolean isAck() {
		return isAck;
	}
	
}
