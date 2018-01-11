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
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Sender {

	public static final String IP = "localhost";
	public static final int PORT = 5001; 
	public static final int CHUNKSIZE =  1000; //Kb

	private InetAddress remoteIP; 
	private BufferedReader reader;
	private byte[] fileBytes;
	private int bytePos = 0;
	private State currentState;
	private Transition[][] transition;
	private DatagramSocket socket;
	private long timer_start;
	private int bytesPos = 0;
	private int numChunks;
	
	enum State {
		WAIT_SEND_0, WAIT_ACK_0, WAIT_SEND_1, WAIT_ACK_1;
	};

	enum Condition {
		LOCAL_SEQ_0, LOCAL_SEQ_1, ACK_0, ACK_1
	};

	public Sender() throws SocketException, UnknownHostException {
		socket = new DatagramSocket(PORT);
		remoteIP = Inet4Address.getByName(IP);
		transition = new Transition[State.values().length]
				[Condition.values().length];
		transition[State.WAIT_ACK_0.ordinal()]
				[Condition.ACK_0.ordinal()] = new SendPacketOne();
		transition[State.WAIT_ACK_1.ordinal()]
				[Condition.ACK_1.ordinal()] = new SendPacketZero();
		transition[State.WAIT_SEND_0.ordinal()]
				[Condition.LOCAL_SEQ_0.ordinal()] = new ReadAckZero();
		transition[State.WAIT_SEND_1.ordinal()]
				[Condition.LOCAL_SEQ_1.ordinal()] = new ReadAckOne();
		
	}

	public static void main(String[] args) {
		
		try {
			
			
			
		} catch (IOException e) {}
		
		
		
	}

	abstract class Transition {
		abstract public State execute(Packet packet) throws IOException;
	}

	class SendPacketZero extends Transition {
		@Override
		public State execute(Packet packet) throws IOException {
			socketSendPacket(packet);
			return State.WAIT_ACK_0;
		}
	}

	class SendPacketOne extends Transition {
		@Override
		public State execute(Packet packet) throws IOException {
			socketSendPacket(packet);
			return State.WAIT_ACK_1;
		}
	}

	class ReadAckZero extends Transition {
		@Override
		public State execute(Packet packet) throws IOException {
			return State.WAIT_SEND_1;
		}
	}

	class ReadAckOne extends Transition {
		@Override
		public State execute(Packet packet) throws IOException {
			return State.WAIT_SEND_0;
		}
	}
	
	public void sendFile(String path) throws IOException {
		
		/* Read the file and chunk it up */
		loadFileBytes("test.txt");
		
		/* Send packets in a loop */
		for (int i = 0; i < numChunks; i++) {
			sendPacket(packet);
			getRemotePacket();
		}
		/* Finish */
		
	}
	
	
	/**
	 * Processes a condition and returns if the status changed or not.
	 * @param packet that will be used to evaluate the condition
	 * @return status changed
	 * @throws IOException 
	 */
	private boolean processCondition(Packet packet) throws IOException {
		Condition cond = getCondition(packet);
		Transition trans = transition[currentState.ordinal()]
				[cond.ordinal()];
		if(trans != null) {
			currentState = trans.execute(packet);
			return true;
		} else {
			return false;
		}
	}
	
	private Condition getCondition(Packet packet) {
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

	//TODO: Do we need to return the packet?
	/**
	 * Reads a new packet and validates it. 
	 * Will keep reading packets until received packet is a valid condition.
	 * @return the packet we read
	 * @throws IOException
	 */
	public void getResponse() throws IOException {
		boolean success = false;
		while (!success) {
			byte[] buffer = new byte[1024];
			DatagramPacket datagram = new DatagramPacket(buffer, buffer.length); 
			socket.receive(datagram);
			Packet packet = new Packet(datagram.getData());
			success = processCondition(packet);
		}
	}

	/**
	 * Creates a new packet, and tries to send it. 
	 * Will retry until transition is successsful.
	 * @throws IOException
	 */
	public void send() throws IOException {
		boolean success = false;
		while(!success) {
			Packet packet = new Packet(getNextChunk());
			success = processCondition(packet);
		}
	}
	
	private void socketSendPacket(Packet packet) throws IOException {
		
		// socket send packet
		socket.send(new DatagramPacket(packet.getBytes(), packet.length()));
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
	
	private void openFile(String filename) throws FileNotFoundException {
		File file = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(file));
		this.reader = br;
	}

}
