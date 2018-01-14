package blatt7;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Adler32;
import javax.xml.bind.DatatypeConverter;

public class Packet {

	/* STRUCTURE:
	 * HEADER | 2 X PAYLOAD LENGTH | 8 X CHECKSUM | PAYLOAD
	 */
	private int seq;
	private int ack;
	private long checksum; //32 characters, 16 bytes
	private int isAck;
	private byte[] payload;
	private byte[] packetBytes;
	//TODO: Add the payload length in the constructors
	private short payloadLength;

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
		ByteBuffer checksumContent = ByteBuffer.allocate(payload.length + 2);
		checksumContent.put(header);
		checksumContent.putInt(payloadLength);
		checksumContent.put(payload);
		adler.update(checksumContent);
		checksum = adler.getValue();

		//Create the packet bytes
		ByteBuffer buffer = ByteBuffer.allocate(payload.length + 11);
		buffer.put(header);
		buffer.putShort(payloadLength);
		buffer.putLong(checksum);
		buffer.put(payload);
		packetBytes = buffer.array();


	}

	/**
	 * Reconstruct a packet based on the bytes received through a network.
	 * @param bytes
	 */
	public Packet(byte[] bytes) {
		try {
			String b = new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		seq = getBit(bytes, 0);
		ack = getBit(bytes, 1);
		isAck = getBit(bytes, 2);
		Adler32 adler = new Adler32();
		//Checksum starts at 3 and is 8 bytes long
		adler.update(packetBytes, 3, 8);
		checksum = adler.getValue();
		byte[] payload = new byte[bytes.length - 11]; 
		for (int i = 0; i < payload.length; i++) {
			payload[i] = bytes[i+11];
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
		if(this.getAck() == 0){
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
