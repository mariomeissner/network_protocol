package blatt7;

import java.io.UnsupportedEncodingException;

public class Packet {
	
	private int seq;
	private int ack;
	private long checksum;
	private int isAck;
	private byte[] payload;
	private byte[] packetBytes;
	
	public Packet(int seq, int ack, byte[] payload, int isAck) {
		this.seq = seq;
		this.ack = ack;
		this.payload = payload;
		this.isAck = isAck;
	}
	
	public Packet(byte[] bytes) {
	try {
	String b = new String(bytes, "UTF-8");
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	}		//TODO: get info from the bytes
	seq = getBit(bytes, 0);
	ack = getBit(bytes, 1);
	isAck = getBit(bytes, 2);
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
	
	public boolean checkChecksum() {
		//TODO: check the checksum
		//gibt entweder 1(richtig), oder 0 (falsch)
		return false;
	}
	
	public boolean isAck() {
		if(this.getAck() == 0){
			return false;
		}else{
		return true;
		}
	}
	private static int getBit(byte[] data, int pos) {
	      int posByte = pos/8; 
	      int posBit = pos%8;
	      byte valByte = data[posByte];
	      int valInt = valByte>>(8-(posBit+1)) & 0x0001;
	      return valInt;
	   }
	
}
