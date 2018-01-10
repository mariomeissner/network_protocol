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
	public static final String FILE = ".\test.txt";
	public static final int CHUNKSIZE =  1000; //Kb

	private InetAddress remoteIP; 
	private BufferedReader reader;
	private byte[] fileBytes;
	private int bytePos = 0;
	private State currentState;
	private Transition[][] transition;
	private DatagramSocket socket;
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

	}

	abstract class Transition {
		abstract public State execute(Packet packet) throws IOException;
	}

	class SendPacketZero extends Transition {
		@Override
		public State execute(Packet packet) throws IOException {
			sendPacket(packet);
			return State.WAIT_ACK_0;
		}
	}

	class SendPacketOne extends Transition {
		@Override
		public State execute(Packet packet) throws IOException {
			sendPacket(packet);
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

	private String getData() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Data of the new packet: ");
		String data;
		data = scanner.next();
		scanner.close();
		return data;
	}

	private Packet getRemotePacket() throws IOException {
		// socket read packet
		byte[] buffer = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length); 
		socket.receive(packet);
		return new Packet(packet.getData());
	}

	private void sendPacket(Packet packet) throws IOException {
		// socket send packet
		socket.send(new DatagramPacket(packet.getBytes(), packet.length()));
		
	}
	
	private void loadFileBytes() throws IOException {
		fileBytes = Files.readAllBytes(Paths.get(FILE));
	}
	
	private void closeFile() throws IOException {
		this.reader.close();
	}
	
	private byte[] readChunk() {
		//byte[] chunk = fileBytes[bytePos, bytesPos + CHUNKSIZE];
		return null;
	}
	
	private void openFile(String filename) throws FileNotFoundException {
		File file = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(file));
		this.reader = br;
	}

}
