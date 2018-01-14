package blatt7;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import blatt7.Packet;
import java.io.IOException;

public class Receiver {

	public static final String TARGET = "localhost";
	public static final int HOME_PORT = 5002;
	public static final int TARGET_PORT = 5001;
	private DatagramSocket socket;
	private ByteBuffer fileBytes;
	private State currentState; 
	private Transition[][] transition;
	private InetAddress remoteIP;

	private Packet packet = null;
	private InetSocketAddress target_address; 


	enum State {
		WAIT_0, WAIT_1, FINISHED;
	};
	enum Condition {
		SEQ_0, SEQ_1, CORRUPT, END;
	}; 


	public Receiver()  {
		try {
			socket = new DatagramSocket(HOME_PORT);
			socket.setSoTimeout(Sender.TIME_LIMIT);
			target_address = new InetSocketAddress(TARGET, TARGET_PORT);
		} catch (SocketException e) {
			System.out.println("Connection error!");
		}
		fileBytes = ByteBuffer.allocate(Sender.MAX_FILESIZE);
		transition = new Transition[State.values().length][Condition.values().length];

		/* Conditions that allow the state machine to advance to the next state */
		transition[State.WAIT_0.ordinal()]
				[Condition.SEQ_0.ordinal()] = new SendACK0();
		transition[State.WAIT_1.ordinal()]
				[Condition.SEQ_1.ordinal()] = new SendACK1();
		
		/* Conditions that finish the transmission */
		transition[State.WAIT_0.ordinal()]
				[Condition.END.ordinal()] = new End();
		transition[State.WAIT_1.ordinal()]
				[Condition.END.ordinal()] = new End();
		
		/* Conditions that make the state machine repeat its last state */
		transition[State.WAIT_1.ordinal()]
				[Condition.SEQ_0.ordinal()] = new SendACK0();
		transition[State.WAIT_0.ordinal()]
				[Condition.SEQ_1.ordinal()] = new SendACK1();
		transition[State.WAIT_1.ordinal()]
				[Condition.CORRUPT.ordinal()] = new SendACK0();
		transition[State.WAIT_0.ordinal()]
				[Condition.CORRUPT.ordinal()] = new SendACK1();
	}

	abstract class Transition {
		abstract public State execute() throws IOException;
	}

	class SendACK0 extends Transition {
		@Override
		public State execute() throws IOException{
			sendACK(0);
			System.out.println("Receiver: Transition: Send Ack0");
			return State.WAIT_1;
		}
	}

	class SendACK1 extends Transition {
		@Override
		public State execute() throws IOException{
			sendACK(1);
			System.out.println("Receiver: Transition: Send Ack1");
			return State.WAIT_0;
		}
	}
	
	class End extends Transition {
		@Override
		public State execute() throws IOException{
			return State.FINISHED;
		}
	}
	/**
	 * Processes a condition and returns if the status changed or not.
	 * @param packet that will be used to evaluate the condition
	 * @return status changed
	 * @throws IOException 
	 */
	private boolean processCondition(Condition cond) throws IOException {
		State oldState = currentState;
		Transition trans = transition[currentState.ordinal()]
				[cond.ordinal()];
		if(trans != null) {
			currentState = trans.execute();
		}
		return (!oldState.equals(currentState));
	}
	private Condition getCondition(Packet packet) {
		if(!packet.checkChecksum()){
			return Condition.CORRUPT;
		} else if (packet.isEnd()){
			return Condition.END;
		} else if (packet.getSeq() == 0) {
			return Condition.SEQ_0;
		} else {
			return Condition.SEQ_1;
		}
	}

	/**
	 * Sends an ack
	 * @param int, that shows if it is ACK 1 or 0
	 * @throws IOException
	 */
	private void sendACK(int i) throws IOException {

		byte[] dummypayload = {0};
		Packet ack = new Packet(i, i, dummypayload, 1, 0);
		DatagramPacket p = new DatagramPacket(ack.getBytes(), ack.length());
		p.setSocketAddress(target_address);
		
		socket.send(p);
	}

	private void receivePacket() throws IOException {
		byte buffer[] = new byte[Sender.MAXPACKETSIZE];
		boolean success = false; 
		while(!success) {
			DatagramPacket p = new DatagramPacket(buffer, buffer.length);
			try {
				socket.receive(p);
			} catch (SocketTimeoutException e) {
				System.out.println("Receiver: Timeout");
				//Contents will be corrupt so we will resend
			}
			packet = new Packet(p.getData());
			success = processCondition(getCondition(packet));
			
			/* we dont need this
			if (packet.getSeq() == currentACK) {
				sendACK(currentACK); 
			} else {
				if (currentACK == ACK0){
					currentACK = ACK1;
					sendACK(currentACK);
				}else{
					currentACK = ACK0;
					sendACK(currentACK);
				}
			}
			*/
		}
		fileBytes.put(packet.getBytes());
	}

	private void writeFile(String filepath) throws FileNotFoundException, IOException {
		File file = new File(filepath);
		try (FileOutputStream fos = new FileOutputStream(file, true)) {
			fos.write(fileBytes.array());
			fos.close();
		}
	}
	
	public void startTransmission(String filepath){
		currentState = State.WAIT_0;
		try {
			while(!currentState.equals(State.FINISHED)) {
				receivePacket();
			}
		} catch (IOException e) {System.out.println("Receiver: Error while receiving data");}
		try {writeFile(filepath);} catch (IOException e) {
			System.out.println("Receiver: Error writing file");
		}
		System.out.println("Receiver: Finished transmission");

	}

}
