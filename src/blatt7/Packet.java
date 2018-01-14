package blatt7;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.Adler32;
import javax.xml.bind.DatatypeConverter;

public class Packet {

	/* STRUCTURE:
	 * 8 X CHECKSUM | HEADER | PAYLOAD
	 * HEADER: 0|0|0|0|0|SEQ|ACK|ISACK
	 */
	private int seq;
	private int ack;
	private int end;
	private long checksum; 	//32 characters, 16 bytes
	private int isAck;		// 0 false, 1 true
	private byte[] payload;
	private byte[] packetBytes;
	private static final byte HEADERSIZE = 1 + 8;
	/**
	 * Construct a packet based on the indicated parameters
	 * @param seq
	 * @param ack
	 * @param payload
	 * @param isAck
	 */
	public Packet(int seq, int ack, byte[] payload, int isAck, int end) {
		this.seq = seq;
		this.ack = ack;
		this.payload = payload;
		this.isAck = isAck;
		this.end = end;
		packetBytes = new byte[payload.length + HEADERSIZE];
		
		//Create the header bits
		byte headerbits = (byte) (end*8 + seq*4 + ack*2 + isAck); 
		
		//Write header bits
		packetBytes[8] = headerbits;
		
		//Write payload
		for (int i = 0; i < payload.length; i++ ) {
			packetBytes[i + HEADERSIZE] = payload[i];
		}
		
		//Create the checksum over the pseudopacket (excluding the checksum field)
		Adler32 adler = new Adler32();
		adler.update(packetBytes, 8, packetBytes.length - 8);
		checksum = adler.getValue();
		byte[] bchecksum = longToBytes(checksum);
		
		//Write checksum
		for (int i = 0; i < 8; i++) {
			packetBytes[i] = bchecksum[i];
		}
		
		

	}

	/**
	 * Reconstruct a packet based on the bytes received through a network.
	 * @param bytes
	 */
	public Packet(byte[] bytes) {
		
		//Assign the bytes
		packetBytes = bytes;
		
		//Get the parameters
		end = getBit(bytes, 4 + 8*8);
		seq = getBit(bytes, 5 + 8*8);
		ack = getBit(bytes, 6 + 8*8);
		isAck = getBit(bytes, 7 + 8*8);
		byte[] bchecksum = new byte[8];
		for (int i = 0; i < 8; i ++) {
			bchecksum[i] = bytes[i];
		}
		checksum = bytesToLong(bchecksum);
		byte[] payload = new byte[bytes.length - HEADERSIZE]; 
		for (int i = 0; i < payload.length; i++) {
			payload[i] = bytes[i+HEADERSIZE];
		}
	}


	public byte[] getBytes(){
		return packetBytes;
	}
	
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Get packet length
	 * @return the length of the packet in bytes
	 */
	public int length() {
		return packetBytes.length;
	}

	public int getSeq() {
		return seq;
	}

	public int getAck() {
		return ack;
	}
	
	public boolean isEnd() {
		return end == 1;
	}

	public boolean checkChecksum() {
		Adler32 adler = new Adler32();
		adler.update(packetBytes, 8, packetBytes.length - 8);
		return adler.getValue() == checksum;
	}

	public boolean isAck() {
		return isAck == 1;
	}

	/**
	 * Taken from http://www.herongyang.com/Java/Bit-String-Get-Bit-from-Byte-Array.html
	 * @param data
	 * @param pos
	 * @return
	 */
	private static int getBit(byte[] data, int pos) {
		int posByte = pos/8; 
		int posBit = pos%8;
		byte valByte = data[posByte];
		int valInt = valByte>>(8-(posBit+1)) & 0x0001;
		return valInt;
	}



	/**
	 * Taken from https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
	 * @param x
	 * @return
	 */
	private byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}
	public long bytesToLong(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(bytes);
		buffer.flip();
		return buffer.getLong();
	}

}
