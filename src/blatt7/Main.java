package blatt7;

public class Main {

	public static void main(String[] args) {
		
		Sender sender = new Sender();
		Receiver receiver = new Receiver();
		
		sender.startTransmission("./testsend.txt");
		receiver.startTransmission("./testreceive.txt");
		
		
	} 
	
	
	
}
