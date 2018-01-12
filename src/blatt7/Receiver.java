package blatt7;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import blatt7.Packet;


import java.io.IOException;

public class Receiver {
	
	public static final String IP = "localhost";
	public static final int PORT = 8001;
	public static final int CHUNKSIZE = 1000; //Kb
	public static final int TIME_LIMIT = 2000;
	private BufferedReader reader;
	private DatagramSocket socket;
	private byte[] fileBytes;
	private State currentState; 
	private Transition[][] transition;
	private InetAddress remoteIP;

	private Packet packet = null; 
	
	
	enum State {
		WAIT_0, WAIT_1
	};
	enum Condition {
		SEQ_0, SEQ_1, CORRUPT;
	}; 
	
	
	public Receiver()  {
		try {
			socket = new DatagramSocket(PORT);
			socket.setSoTimeout(TIME_LIMIT);
			remoteIP = Inet4Address.getByName(IP);
		} catch (SocketException | UnknownHostException e) {
			System.out.println("Connection error!");
		}
		
		transition = new Transition[State.values().length][Condition.values().length];
		
		/* Conditions that allow the state machine to advance to the next state */
		transition[State.WAIT_0.ordinal()]
				[Condition.SEQ_0.ordinal()] = new SendACK0();
		transition[State.WAIT_1.ordinal()]
				[Condition.SEQ_1.ordinal()] = new SendACK1();
		
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
			return State.WAIT_1;
		}
	}
	
	class SendACK1 extends Transition {
		@Override
		public State execute() throws IOException{
			sendACK(1);
			return State.WAIT_0;
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
		if(!packet.checkChecksum()){
			return Condition.CORRUPT;
		}else if (packet.getSeq() == 0) {
			return Condition.SEQ_0;
		} else {
			return Condition.SEQ_1;
		}
	}
	
	private void sendACK(int i) throws IOException {
		
		byte in[] = new byte[0];
		Packet ack = new Packet(i, i, null, 1);
		DatagramPacket p = new DatagramPacket(ack.getBytes(), ack.length());
		socket.send(p);
	}
	
	private void receivePacket() throws IOException {
		byte buffer[] = new byte[1024];
		boolean success = false; 
		while(!success) {
			DatagramPacket p = new DatagramPacket(buffer, buffer.length);
			try {
				socket.receive(p);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			packet = new Packet(p.getData());
			File file = new File("C:\\Users\\Melanie\\Desktop\\Packet.txt");
			try (FileOutputStream fos = new FileOutputStream("myfile.txt", true)) {
				   fos.write(packet.getBytes());
				   fos.close();
				}
			success = processCondition(getCondition(packet));
		}
		
	}

}
