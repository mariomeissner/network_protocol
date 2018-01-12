package blatt7;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Sender {

	public static final String IP = "localhost";
	public static final int PORT = 5001; 
	public static final int CHUNKSIZE =  1000; //Kb
	public static final int TIME_LIMIT = 2000;
	private InetAddress remoteIP; 
	private BufferedReader reader;
	private byte[] fileBytes;
	private State currentState;
	private Transition[][] transition;
	private DatagramSocket socket;
	private int bytesPos = 0;
	private int numChunks;
	private Packet currentPacket;
	
	enum State {
		WAIT_SEND_0, WAIT_ACK_0, WAIT_SEND_1, WAIT_ACK_1;
	};

	enum Condition {
		LOCAL_SEQ_0, LOCAL_SEQ_1, ACK_0, ACK_1, CORRUPT;
	};

	public Sender() {
		try {
			socket = new DatagramSocket(PORT);
			socket.setSoTimeout(TIME_LIMIT);
			remoteIP = Inet4Address.getByName(IP);
		} catch (SocketException | UnknownHostException e) {
			System.out.println("Connection error!");
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
			setPacket(0);
			socketSendCurrentPacket();
			return State.WAIT_ACK_0;
		}
	}

	class SendPacketOne extends Transition {
		@Override
		public State execute() throws IOException {
			
			socketSendCurrentPacket();
			return State.WAIT_ACK_1;
		}
	}

	class ReadAckZero extends Transition {
		@Override
		public State execute() throws IOException {
			setPacket(1);
			return State.WAIT_SEND_1;
		}
	}

	class ReadAckOne extends Transition {
		@Override
		public State execute() throws IOException {
			setPacket(0);
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
		Transition trans = transition[currentState.ordinal()]
				[cond.ordinal()];
		if(trans != null) {
			currentState = trans.execute();
			return true;
		} else {
			return false;
		}
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
			byte[] buffer = new byte[1024];
			DatagramPacket datagram = new DatagramPacket(buffer, buffer.length); 
			try { socket.receive(datagram); } catch (SocketTimeoutException e) {
				//We do as if the packet was corrupt because we just want to send it again
				processCondition(Condition.CORRUPT);
			}
			Packet packet = new Packet(datagram.getData());
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
			System.out.println("currentPacket is not a valid condition, aborting!");
		}
	}
	
	public void startTransmission(String filepath) {
		currentState = State.WAIT_SEND_0;
		setPacket(0);
		try {
			loadFileBytes(filepath);
		} catch (IOException e) {
			System.out.println("Error loading the file");
			return;
		}
		
		try {
			for (int i = 0; i < numChunks; i++) {
				send();
				getResponse();
			}
		} catch (IOException e) {
			System.out.println("Error while sending data!");
			return;
		}
		
		System.out.println("Finished transmisison");
		
	}
	
	private void socketSendCurrentPacket() throws IOException {
		DatagramPacket datagram = new DatagramPacket(currentPacket.getBytes(), currentPacket.length(), remoteIP, PORT);
		socket.send(datagram);
	}
	
	private void setPacket(int seq) {
		if (seq > 1 || seq < 0) throw new IndexOutOfBoundsException();
		currentPacket = new Packet(seq, 0, getNextChunk(), false);
	}
	
	private void loadFileBytes(String path) throws IOException {
		fileBytes = Files.readAllBytes(Paths.get(path));
		reader.close();
		numChunks = (int) Math.ceil(fileBytes.length / CHUNKSIZE);
	}
		
	private byte[] getNextChunk() {
		byte[] chunk = new byte[CHUNKSIZE];
		for (int i = 0; i<CHUNKSIZE; i++) {
			chunk[i] = fileBytes[i + bytesPos];
		}
		bytesPos += CHUNKSIZE;
		return chunk;
	}
}
