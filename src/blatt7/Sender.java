package blatt7;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

import blatt7.Receiver.State;
import blatt7.Receiver.Transition;

public class Sender {
	
	public static void main(String[] args) {
		Sender sender = new Sender();
		sender.startTransmission("./java.jpg");
	}
	
	public static final String TARGET = "217.255.167.49";
	public static final int HOME_PORT = 5001;
	public static final int TARGET_PORT = 5002;
	public static final int CHUNKSIZE =  20; //Kb
	public static final int TIME_LIMIT = 2000;
	public static final int MAXPACKETSIZE = 5000;
	public static final int MAX_FILESIZE = 50000;
	public static final double CHANCE_LOST = 0.05;
	public static final double CHANCE_DUPLICATE = 0.05;
	public static final double CHANCE_MODIFIED = 0.05;
	
	private InetSocketAddress target_address;
	private byte[] fileBytes;
	private State currentState;
	private Transition[][] transition;
	private DatagramSocket socket;
	private int bytesPos = 0;
	private int numChunks;
	private Packet currentPacket;
	private Packet oldPacket; //For unreliable
	enum State {
		WAIT_SEND_0, WAIT_ACK_0, WAIT_SEND_1, WAIT_ACK_1;
	};

	enum Condition {
		LOCAL_SEQ_0, LOCAL_SEQ_1, ACK_0, ACK_1, CORRUPT;
	};

	public Sender() {
		try {
			socket = new DatagramSocket(HOME_PORT);
			socket.setSoTimeout(TIME_LIMIT);
			target_address = new InetSocketAddress(TARGET, TARGET_PORT);
		} catch (SocketException e) {
			System.out.println("Connection error!");
			return;
		}
		
		transition = new Transition[State.values().length]
				[Condition.values().length];
		
		/* Conditions that allow the state machine to advance to the next state */
		transition[State.WAIT_ACK_0.ordinal()]
				[Condition.ACK_0.ordinal()] = new ReadAckZero();
		transition[State.WAIT_ACK_1.ordinal()]
				[Condition.ACK_1.ordinal()] = new ReadAckOne();
		transition[State.WAIT_SEND_0.ordinal()]
				[Condition.LOCAL_SEQ_0.ordinal()] = new SendPacketZero();
		transition[State.WAIT_SEND_1.ordinal()]
				[Condition.LOCAL_SEQ_1.ordinal()] = new SendPacketOne();
		
		/* Conditions that make the state machine repeat its last state */
		transition[State.WAIT_ACK_0.ordinal()]
				[Condition.ACK_1.ordinal()] = new SendPacketZero();
		transition[State.WAIT_ACK_1.ordinal()]
				[Condition.ACK_0.ordinal()] = new SendPacketOne();
		transition[State.WAIT_ACK_0.ordinal()]
				[Condition.CORRUPT.ordinal()] = new SendPacketZero();
		transition[State.WAIT_ACK_1.ordinal()]
				[Condition.CORRUPT.ordinal()] = new SendPacketOne();
		
	}

	abstract class Transition {
		abstract public State execute() throws IOException;
	}

	class SendPacketZero extends Transition {
		@Override
		public State execute() throws IOException {
			socketSendCurrentPacket();
			System.out.println("Sender: Transition: SendPacketZero");
			return State.WAIT_ACK_0;
		}
	}

	class SendPacketOne extends Transition {
		@Override
		public State execute() throws IOException {
			socketSendCurrentPacket();
			System.out.println("Sender: Transition: SendPacketOne");
			return State.WAIT_ACK_1;
		}
	}

	class ReadAckZero extends Transition {
		@Override
		public State execute() throws IOException {
			setPacket(1);
			System.out.println("Sender: Transition: ReadAckZero");
			return State.WAIT_SEND_1;
		}
	}

	class ReadAckOne extends Transition {
		@Override
		public State execute() throws IOException {
			setPacket(0);
			System.out.println("Sender: Transition: ReadAckOne");
			return State.WAIT_SEND_0;
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
		if (!packet.checkChecksum()) {
			return Condition.CORRUPT;
		}
		if (packet.isAck()) {
			if (packet.getAck() == 0) {
				return Condition.ACK_0;
			} else {
				return Condition.ACK_1;
			}
		} else {
			if (packet.getSeq() == 0) {
				return Condition.LOCAL_SEQ_0;
			} else {
				return Condition.LOCAL_SEQ_1;
			}
		}
	}

	/**
	 * Reads a new packet and validates it. 
	 * Will keep reading packets until received packet is a valid condition.
	 * @return the packet we read
	 * @throws IOException
	 */
	private void getResponse() throws IOException {
		boolean success = false;
		while (!success) {
			byte[] buffer = new byte[MAXPACKETSIZE];
			DatagramPacket datagram = new DatagramPacket(buffer, buffer.length); 
			try { socket.receive(datagram); } catch (SocketTimeoutException e) {
				System.out.println("Sender: Timeout");
				//Contents will be corrupt so we will resend
			}
			Packet packet = new Packet(datagram.getData(), datagram.getLength());
			success = processCondition(getCondition(packet));
		}
	}

	/**
	 * Creates a new packet, and tries to send it. 
	 * Will retry until transition is successsful.
	 * @throws IOException
	 */
	private void send() throws IOException {
		if(!processCondition(getCondition(currentPacket))) {
			System.out.println("Sender: currentPacket is not a valid condition, aborting!");
		}
	}
	
	public void startTransmission(String filepath) {
		currentState = State.WAIT_SEND_0;
		try {
			loadFileBytes(filepath);
		} catch (IOException e) {
			System.out.println("Sender: Error loading the file");
			return;
		}
		setPacket(0);
		try {
			for (int i = 0; i < numChunks; i++) {
				send();
				getResponse();
			}
		} catch (IOException e) {
			System.out.println("Sender: Error while sending data!");
			return;
		}
		
		System.out.println("Sender: Finished transmisison");
	}
	
	private void socketSendCurrentPacket() throws IOException {
		Packet candidate = unreliable(currentPacket);
		if (candidate == null) return; //lost paacket
		DatagramPacket datagram = new DatagramPacket(candidate.getBytes(), candidate.length());
		datagram.setSocketAddress(target_address);
		socket.send(datagram);
	}
	
	private void setPacket(int seq) {
		if (seq > 1 || seq < 0) throw new IndexOutOfBoundsException();
		int end = 0;
		byte[] chunk = getNextChunk();
		if (bytesPos >= fileBytes.length) {
			end = 1;
		}
		currentPacket = new Packet(seq, 0, chunk, 0, end);
	}
	
	private void loadFileBytes(String path) throws IOException {
		fileBytes = Files.readAllBytes(Paths.get(path));
		numChunks = (int) Math.ceil((double) fileBytes.length / CHUNKSIZE);
	}
		
	private byte[] getNextChunk() {
		byte[] chunk;
		if (bytesPos + CHUNKSIZE > fileBytes.length) {
			chunk = new byte[fileBytes.length - bytesPos];
		} else {
			chunk = new byte[CHUNKSIZE];
		}
		for (int i = 0; i<chunk.length; i++) {
			chunk[i] = fileBytes[i + bytesPos];
		}
		bytesPos += CHUNKSIZE;
		return chunk;
	}
	
	private Packet unreliable(Packet packet) {
		double rand = Math.random();
		if (rand < CHANCE_DUPLICATE) {
			return oldPacket;
		} 
		rand = Math.random();
		if (rand < CHANCE_LOST) {
			return null;
		}
		rand = Math.random();
		if (rand < CHANCE_MODIFIED) {
			byte[] data = packet.getBytes();
			//Overwrite something random in the first 20 bytes
			data[(int) rand * 20] = 0;
			return new Packet(data, data.length);
		} 
		oldPacket = currentPacket;
		return packet;
	}
}
