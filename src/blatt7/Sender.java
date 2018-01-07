package blatt7;

import java.util.Scanner;

public class Sender {
	
	enum State {
		WAIT_SEND_0, WAIT_ACK_0, WAIT_SEND_1, WAIT_ACK_1;
	};
	
	enum ??? {
		
	};
	
	private State currentState;
	private Transition[][] transition;
	
	public Sender(String IP) {
		//TODO: Create socket
	}
	
	abstract class Transition {
		abstract public State execute();
	}
	
	class SendPacketZero extends Transition {
		@Override
		public State execute() {
			
			return State.WAIT_ACK_0;
		}
	}
	
	class SendPacketOne extends Transition {
		@Override
		public State execute() {
			
			return State.WAIT_ACK_1;
		}
	}
	
	class ReadAck0 extends Transition {
		@Override
		public State execute() {
			
			return State.WAIT_SEND_1;
		}
	}
	
	class ReadAck1 extends Transition {
		@Override
		public State execute() {
			
			return State.WAIT_SEND_0;
		}
	}
	
	public void waitSend(int seq) {
		if (seq < 0 || seq > 1) throw new IndexOutOfBoundsException();
		String data = getData();
		Packet packet = new Packet(seq, 0, data, false);
		sendPacket(packet);
	}
		
	public void waitAck(int seq) {
		if (seq < 0 || seq > 1) throw new IndexOutOfBoundsException();
		Packet packet = getRemotePacket();
		while(packet.isAck)...
	}
	
	private String getData() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Data of the new packet: ");
		String data;
		data = scanner.next();
		scanner.close();
		return data;
	}
	
	private Packet getRemotePacket() {
		// socket read packet
	}
	
	private void sendPacket(Packet packet) {
		// socket send packet
	}
	
}
