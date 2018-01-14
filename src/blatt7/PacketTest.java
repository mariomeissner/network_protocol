package blatt7;

import static org.junit.Assert.*;

import org.junit.Test;

public class PacketTest {

	@Test
	public void checksum() {
		byte[] test = {1};
		int bit = getBit(test, 7);
		assertTrue(bit == 1);
		bit = getBit(test, 0);
		assertTrue(bit == 0);
		
		byte[] payload = {1, 2, 3, 4};
		Packet packet = new Packet(1, 1, payload, 0);
		Packet packet2 = new Packet(0, 0, payload, 1);
		
		Packet received = new Packet(packet.getBytes());
		Packet received2 = new Packet(packet2.getBytes());
		
		assertTrue(packet.getSeq() == received.getSeq());
		assertTrue(packet2.getAck() == received2.getAck());
		
		assertTrue(packet.checkChecksum());
		assertTrue(packet2.checkChecksum());
		assertTrue(received.checkChecksum());
		assertTrue(received2.checkChecksum());
		
	}
	
	private static int getBit(byte[] data, int pos) {
		int posByte = pos/8; 
		int posBit = pos%8;
		byte valByte = data[posByte];
		int valInt = valByte>>(8-(posBit+1)) & 0x0001;
		return valInt;
	}

}
