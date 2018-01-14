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
	 */
	private int seq;
	private int ack;
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
	public Packet(int seq, int ack, byte[] payload, int isAck) {
		this.seq = seq;
		this.ack = ack;
		this.payload = payload;
		this.isAck = isAck;

		//Create the header
		Byte header = Byte.parseByte(seq + ack + isAck + "00000", 2);

		//Create the checksum over the pseudopacket (excluding the checksum field
		Adler32 adler = new Adler32();
		ByteBuffer checksumContent = ByteBuffer.allocate(payload.length + 1);
		checksumContent.put(header);
		checksumContent.put(payload);
		adler.update(checksumContent);
		checksum = adler.getValue();

		//Create the packet bytes
		ByteBuffer buffer = ByteBuffer.allocate(payload.length + HEADERSIZE);
		buffer.putLong(checksum);
		buffer.put(header);
		buffer.put(payload);
		packetBytes = buffer.array();

	}

	/**
	 * Reconstruct a packet based on the bytes received through a network.
	 * @param bytes
	 */
	public Packet(byte[] bytes) {
		
		//TODO: Do we really need this?
		try {
			String b = new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		
		seq = getBit(bytes, 0);
		ack = getBit(bytes, 1);
		isAck = getBit(bytes, 2);
		Adler32 adler = new Adler32();
		adler.update(bytes, 8, bytes.length - 8);
		checksum = adler.getValue();
		byte[] payload = new byte[bytes.length - HEADERSIZE]; 
		for (int i = 0; i < payload.length; i++) {
			payload[i] = bytes[i+HEADERSIZE];
		}

	}


	public byte[] getBytes(){
		return packetBytes;
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

	public boolean checkChecksum() {
		Adler32 adler = new Adler32();
		adler.update(packetBytes);
		return adler.getValue() == checksum;
	}

	public boolean isAck() {
		if(isAck == 0){
			return false;
		}else{
			return true;
		}
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
