package blatt7;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
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
	
	enum State {
		WAIT_SEND_0, WAIT_ACK_0, WAIT_SEND_1, WAIT_ACK_1;
	};

	enum Condition {


	};

	private State currentState;
	private Transition[][] transition;

	public Sender() throws SocketException, UnknownHostException {
		DatagramSocket serverSocket = new DatagramSocket(PORT);
		remoteIP = Inet4Address.getByName(IP);
	}

	public static void main(String[] args) {

		Sender sender;
		try {
			sender = new Sender();
			while(true) {

				sender.waitSend(0);
				sender.waitAck(0);
				sender.waitSend(1);
				sender.waitAck(1);
			}

		} catch (SocketException | UnknownHostException e) {System.out.println("Something went wrong.");}
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
		Packet packet = getRemoteAck();
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

	private Packet getRemoteAck() {
		// socket read packet
	}

	private void sendPacket(Packet packet) {
		// socket send packet
	}
	
	private void loadFileBytes() throws IOException {
		fileBytes = Files.readAllBytes(Paths.get(FILE));
	}
	
	private void closeFile() throws IOException {
		this.reader.close();
	}
	
	private byte[] readChunk() {
		byte[] chunk = fileBytes[bytePos, bytesPos + CHUNKSIZE];
	}
	
	private void openFile(String filename) throws FileNotFoundException {
		File file = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(file));
		this.reader = br;
	}

}
