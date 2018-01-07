package blatt7;

public class Main {

	
	public static void main(String[] args) {
			
		Sender sender = new Sender("0.0.0.0");
		
		while(true) {
					
			sender.waitSend(0);
			sender.waitAck(0);
			sender.waitSend(1);
			sender.waitAck(1);
			
		}
	}
}
