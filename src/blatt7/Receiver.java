package blatt7;


public class Receiver {
	
	enum State {
		WAIT_0, WAIT_1
	};
	enum Msg {
		// not sure yet 
	}; 
	private State currentState; 
	private Transition[][] transition; 
	
	public Receiver() {
		
	}
	
	
	abstract class Transition {
		abstract public State execute();
	}
	
	class sendACK0 extends Transition {
		@Override
		public State execute() {
			return State.WAIT_1;
		}
	}
	
	class sendACK1 extends Transition {
		@Override
		public State execute() {
			return State.WAIT_0;
		}
	}
	
	public void wait(int seq) {
		
	}
	
	private String getSeq(Packet p) {
		//extract Seq Number 
		return null;
	}

}
