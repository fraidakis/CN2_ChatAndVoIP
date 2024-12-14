// cd "c:\Users\Lenovo Ideapad 5 Pro\OneDrive - Αριστοτέλειο Πανεπιστήμιο Θεσσαλονίκης\Επιφάνεια εργασίας\Δίκτυα ΙΙ\Project\Code\CN2_AUTH_ChatAndVoIP\src\main\java" ; if ($?) { javac com\cn2\communication\App.java } ; if ($?) { java com.cn2.communication.App }

/** 
 *! Instructions:
 ** 1. Replace the IP address in the main method with the actual IP address of the remote machine.
 * In windows, you can find the IP address by running the command "ipconfig" in the command prompt.
 * It is the value of the "IPv4 Address" field. Usually, it is something like "192.168.1.100" (where the last octet can be 0-255).
 * 
 ** 2. To run the program, cd ./Code/src/main/java and run the following command:
 ** 	   javac com/cn2/communication/App.java ; java com.cn2.communication.App
 */

package com.cn2.communication; // Define the package where the class is located

import java.io.*; // Import the java.io package to use the classes for input/output operations
import java.net.*; // Import the java.net package to use the classes for networking operations
import java.lang.Thread; // Import the Thread class from the java.lang package to create a new thread
import javax.sound.sampled.*;

import javax.swing.JFrame; // Import the JFrame class from the javax.swing package to create the app's window
import javax.swing.JOptionPane; // Import the JOptionPane class from the javax.swing package to create dialog boxes
import javax.swing.JTextField; // Import the JTextField class from the javax.swing package to create the text field
import javax.swing.text.DefaultCaret; // Import the DefaultCaret class from the javax.swing.text package to enable auto-scrolling
import javax.swing.JButton; // Import the JButton class from the javax.swing package to create the button
import javax.swing.JTextArea; // Import the JTextArea class from the javax.swing package to create the text area
import javax.swing.JScrollPane; // Import the JScrollPane class from the javax.swing package to create the scroll pane

import java.awt.*; // Import the java.awt package to use the classes for the GUI components
import java.awt.event.*; // Import the java.awt.event package to use the classes for the events


/**
 * The App class is the main class of the application. It creates the app's
 * window and initializes the GUI components. It also listens for the events
 * that are triggered by the user's actions (e.g., clicking a button) and
 * handles them accordingly.
 */
public class App extends Frame implements WindowListener, ActionListener {

	/**
	 ** Definition of the app's fields
	 */

	private static App instance; // The instance of the App class

	// GUI related fields
	static TextField inputTextField; // The text field where the user types the message
	static JTextArea textArea; // The text area where the messages are displayed
	static JFrame frame; // The app's window
	static JButton sendButton; // The button that sends the message
	static JTextField meesageTextField; // ???? Never used. Instead we have inputTextField...)
	public static Color gray; // The color of the GUI components
	final static String newline = "\n";
	static JButton callButton; // The button that initiates the call

	private static JButton acceptButton; // Button to accept call
	private static JButton rejectButton; // Button to reject call

	// !Please define and initialize your variables here...

	// Network related fields
	private static DatagramSocket chatSocket; // Socket for chat messages
	private static DatagramSocket voiceSocket; // Socket for voice data
	private static InetAddress remoteIP; // Target IP address
	private static int remoteChatPort = 8000; // Target chat port
	private static int remoteVoicePort = 8001; // Target voice port
	private static int localChatPort = 8002; // Local chat port
	private static int localVoicePort = 8003; // Local voice port
	private static byte[] buffer = new byte[1024]; // Buffer for receiving data

	// Audio related fields
	private static AudioFormat audioFormat; // Audio format for voice data
	private static TargetDataLine microphone; // Microphone for recording voice
	private static SourceDataLine speakers; // Speakers for playing voice
	private static boolean isCallActive = false; // Flag for call status
	private static long callStartTime; // Time when the call started
	private static long callEndTime; // Time when the call ended
	private static Thread receiveVoiceThread; // Thread for receiving voice data
	private static Thread sendVoiceThread; // Thread for sending voice data
	private static Thread receiveChatThread; // Thread for receiving chat messages
	private static Clip incomingCallClip; // Clip for incoming call sound

	// Connection and security related fields
	private static boolean isConnected = false; // Flag for connection status (connected or not)
	private static boolean isInitiator = true; // Flag for connection initiator (who initiates the connection)
	private static SecurityModule securityModule; // Security module for encryption and decryption of messages
	private static final String[] CONNECTION_STATES = {
		"PUBLIC_KEY:", 
		"SYMMETRIC_KEY:", 
		"SECURE_CONNECTION_ESTABLISHED", 
		"CALL_REQUEST", 
		"CALL_ACCEPT", 
		"CALL_REJECT", 
		"END_CALL", 
		"DISCONNECT"
	};
	

	/**
	 * The constructor of the App class. It initializes the app's window and the GUI
	 */
	public App(String title) {


		/*
		 * 1. Defining the components of the GUI
		 */

		// Setting up the characteristics of the frame
		super(title);
		gray = new Color(254, 254, 254);
		setBackground(gray); // Setting the background color of the frame
		setLayout(new FlowLayout()); // Setting the layout of the frame
		addWindowListener(this); // Adding the WindowListener to the frame

		// Setting up the TextField (where the user types the message).
		inputTextField = new TextField();
		inputTextField.setColumns(20);

		// Setting up the TextArea (where the messages will be displayed).
		textArea = new JTextArea(10, 40);
		textArea.setLineWrap(true); // The text will wrap to the next line if it exceeds the width of the text area
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false); // The user cannot edit the displayed messages
		JScrollPane scrollPane = new JScrollPane(textArea); // Adding a scroll pane to the text area
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Setting the vertical scroll bar
																						// to always be visible

		// Enable auto-scrolling
		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		// Setting up the buttons
		sendButton = new JButton("Send");
		callButton = new JButton("Call");
		acceptButton = new JButton("Accept"); // Button to accept call request
		rejectButton = new JButton("Reject"); // Button to reject call request

		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(acceptButton);
		add(rejectButton);

		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this); // The ActionListener will listen for the "Send" button to be clicked
		sendButton.setBackground(Color.CYAN); // Setting the background color of the "Send" button

		callButton.addActionListener(this); // The ActionListener will listen for the "Call" button to be clicked
		callButton.setBackground(Color.GREEN); // Setting the background color of the "Call" button

		acceptButton.addActionListener(this); // The ActionListener will listen for the "Accept" button to be clicked
		acceptButton.setVisible(false); // Initially hidden
		acceptButton.setBackground(Color.GREEN); // Setting the background color of the "Accept" button

		rejectButton.addActionListener(this); // The ActionListener will listen for the "Reject" button to be clicked
		rejectButton.setVisible(false); // Initially hidden
		rejectButton.setBackground(Color.RED); // Setting the background color of the "Reject" button

		inputTextField.addActionListener(e -> sendMessage()); // The ActionListener will listen for the "Enter" key to be pressed in the text field
		
		instance = this; // Set the global instance to the current instance
	}

	public static App getInstance() {
        return instance; // Return the global instance
    }

	/**
	 * The main method of the App class. It creates an instance of the App class and
	 * initializes the network sockets. It also starts the thread for receiving chat
	 * messages and continuously listens new messages.
	 */
	public static void main(String[] args) {

		// !Testing purposes only (two instances of the app on the same machine)
		try {
			DatagramSocket testSocket = new DatagramSocket(localChatPort);
			testSocket.close();
		} catch (Exception e) {
			int tempChatPort = localChatPort;
			int tempVoicePort = localVoicePort;

			localChatPort = remoteChatPort;
			localVoicePort = remoteVoicePort;

			remoteChatPort = tempChatPort;
			remoteVoicePort = tempVoicePort;
		}

		try {
			App app = new App("Chat & VoIP");
			app.setSize(500, 250);
			app.setVisible(true);

			chatSocket = new DatagramSocket(localChatPort); // Create a new DatagramSocket for chat messages
			voiceSocket = new DatagramSocket(localVoicePort); // Create a new DatagramSocket for voice data

			remoteIP = InetAddress.getLocalHost(); // Get the IP address of the remote machine
			audioFormat = new AudioFormat(8000.0f, 8, 1, true, false); // Set the audio format for voice data
			securityModule = new SecurityModule(); // Create a new security module for encryption and decryption


			app.establishConnection(); // Method to handle reconnection logic

			
			receiveChatThread = new Thread(() -> {
				/*
				 * 2. Listen for new messages
				 */
				while (true) {

					// !Be in a loop and listen for new messages

					try {
						// Prepare packet for receiving
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // Create a new DatagramPacket for receiving data
						chatSocket.receive(packet); // Receive data and store it in the packet

						// Convert received data to string and display
						String receivedMessage = new String(packet.getData(), packet.getOffset(), packet.getLength()); // Convert the received data to a string

						// Check if the message matches a connection state
						boolean isConnectionState = false;
						for (String state : CONNECTION_STATES) {
							if (receivedMessage.startsWith(state)) {
								isConnectionState = true;
								app.handleConnectionState(receivedMessage);
								break;
							}
						}

						// If it's not a connection message -> regular message
						if (!isConnectionState) {

							if (securityModule.isSecureConnectionEstablished()) {
								// Decrypt and display message
								String decryptedMessage = securityModule.decrypt(receivedMessage);
								
								if (decryptedMessage != null) {
									textArea.append("Received: " + decryptedMessage + newline);
								} 
								
								else {
									textArea.append("Failed to decrypt message" + newline);
								}
							} 
							
							else {
								textArea.append("Secure connection not established. Discarding message." + newline);
							}
						}

					} catch (IOException e) {
						e.printStackTrace(); // Print any exceptions that occur while receiving messages
					}

				}
			});

			receiveChatThread.start(); // Start the thread for receiving chat messages

		} catch (Exception e) {
			e.printStackTrace();

			// Make sure to clean up resources in case of error
			if (chatSocket != null && !chatSocket.isClosed()) {
				chatSocket.close();
			}
			if (voiceSocket != null && !voiceSocket.isClosed()) {
				voiceSocket.close();
			}
		}
	}

	/**
	 * The method that corresponds to the Action Listener. Whenever an action is
	 * performed (i.e., one of the buttons is clicked) this method is executed.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		/**
		 ** Check which button was clicked.
		 */
		if (e.getSource() == sendButton) {

			// The "Send" button was clicked
			sendMessage();

		} else if (e.getSource() == callButton) {

			// The "Call" button was clicked

			if (!isConnected) { // Check if the connection is established
				textArea.append("Connection not established. Cannot initiate call." + newline);
				return;
			}

			else if (!isCallActive) { // Check if a call is active
				// Send call request
				callButton.setEnabled(false); // Disable the button when the call starts
				callButton.setText("Calling...");
				callButton.setBackground(Color.orange);
				sendConnectionMessage("CALL_REQUEST");
				textArea.append("Call request sent." + newline);
			}

			else { // End call
				stopCall();
				sendConnectionMessage("END_CALL");
				textArea.append("Ended call." + newline);
			}
		}

		else if (e.getSource() == acceptButton) {
			acceptButton.setVisible(false);
			rejectButton.setVisible(false);

			if (incomingCallClip != null && incomingCallClip.isRunning()) {
				incomingCallClip.stop();
			}

			sendConnectionMessage("CALL_ACCEPT");
			textArea.append("Call accepted." + newline);
			callButton.setVisible(true);

			if (!isCallActive)
				startCall();

		}

		else if (e.getSource() == rejectButton) {
			acceptButton.setVisible(false);
			rejectButton.setVisible(false);

			if (incomingCallClip != null && incomingCallClip.isRunning()) {
				incomingCallClip.stop();
			}

			sendConnectionMessage("CALL_REJECT");
			textArea.append("Call rejected." + newline);
			callButton.setVisible(true);
		}

	}

	/**
	 * The method that sends the message to the remote machine.
	 */
	public void sendMessage() {
		if (!securityModule.isSecureConnectionEstablished()) {
			textArea.append("Secure connection not established. Cannot send message." + newline);
			return;
		}

		// Get the message from the text field
		String message = inputTextField.getText();

		// Encrypt the message
		String encryptedMessage = securityModule.encrypt(message);

		if (encryptedMessage == null) {
			textArea.append("Message encryption failed." + newline);
			return;
		}

		sendConnectionMessage(encryptedMessage);

		// Display the message in the text area
		textArea.append("Send: " + message + newline);
		// textArea.append("Send: " + securityModule.decrypt(message) + newline);

		// Clear the text field
		inputTextField.setText("");
	}

	private void startCall() {

		try {
			// Initialize audio devices
			DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat); 
			DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

			if (!AudioSystem.isLineSupported(micInfo) || !AudioSystem.isLineSupported(speakerInfo)) {
				textArea.append("Audio devices not supported" + newline);
				return;
			}

			microphone = (TargetDataLine) AudioSystem.getLine(micInfo); 
			speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo); 


			// Open and start audio devices with the specified audio format
			microphone.open(audioFormat);  
			speakers.open(audioFormat); 
			microphone.start(); 
			speakers.start();

			isCallActive = true;
			callStartTime = System.currentTimeMillis(); // Record the start time of the call
			callButton.setText("End Call");
			callButton.setBackground(Color.RED);

			// Start voice receiver thread
			receiveVoiceThread = new Thread(() -> {

				byte[] voiceBuffer = new byte[1024];
				while (isCallActive) {
					try {
						DatagramPacket packet = new DatagramPacket(voiceBuffer, voiceBuffer.length);
						voiceSocket.receive(packet);
						speakers.write(packet.getData(), 0, packet.getLength());
					} catch (IOException e) {
						if (isCallActive)
							e.printStackTrace();
					}
				}
			});

			// Start voice sender thread
			sendVoiceThread = new Thread(() -> {
				byte[] voiceBuffer = new byte[1024];
				while (isCallActive) {
					try {
						int count = microphone.read(voiceBuffer, 0, voiceBuffer.length);
						if (count > 0) {
							DatagramPacket packet = new DatagramPacket(voiceBuffer, count, remoteIP, remoteVoicePort);
							voiceSocket.send(packet);
						}
					} catch (IOException e) {
						if (isCallActive)
							e.printStackTrace();
					}
				}
			});

			receiveVoiceThread.start();
			sendVoiceThread.start();

		} catch (LineUnavailableException | SecurityException e) {
			e.printStackTrace();
		}
	}

	private void stopCall() {
		isCallActive = false;

		callEndTime = System.currentTimeMillis(); // Record the end time of the call
		// Calculate and display call duration
		long callDuration = (callEndTime - callStartTime) / 1000; // Duration in seconds
		String durationMessage = "Call duration: " + callDuration + " seconds";
		textArea.append(durationMessage + newline);

		callButton.setText("Call");
		callButton.setBackground(Color.GREEN);

		if (microphone != null) {
			microphone.stop();
			microphone.close();
		}
		if (speakers != null) {
			speakers.stop();
			speakers.close();
		}
	}

	// ***************************************   Security Methods   *************************************** //

	/**
	 * Initiate key exchange with the remote machine
	 */
	private void initiateKeyExchange() {
		if (securityModule.isSecureConnectionEstablished()) {
			textArea.append("Secure connection already established. Skipping key exchange." + newline);
			return;
		}

		String publicKey = securityModule.getPublicKey(); // Get RSA public key to send to the remote party
		sendConnectionMessage("PUBLIC_KEY:" + publicKey);
		textArea.append("Stage 1: Send local public key." + newline);
	}

	/**
	 * Handle public key exchange from remote party and initiate symmetric key
	 * exchange
	 */
	private void handlePublicKeyExchange(String remotePublicKey) {
		if (securityModule.setRemotePublicKey(remotePublicKey)) {
			textArea.append("Stage 2: Received public key from remote." + newline);
			// Send back encrypted symmetric key
			String encryptedSymmetricKey = securityModule.getEncryptedSymmetricKey(); // Encrypt symmetric key with
																						// remote party's public key

			if (encryptedSymmetricKey != null) {
				sendConnectionMessage("SYMMETRIC_KEY:" + encryptedSymmetricKey);
				textArea.append("Stage 3: Generating symmetric key and send it to remote." + newline);

			} else
				textArea.append("Failed to encrypt symmetric key." + newline);
		}
	}

	/**
	 * Handle symmetric key exchange from remote party
	 */
	private static boolean handleSymmetricKeyExchange(String encryptedSymmetricKey) {
		if (securityModule.setRemoteSymmetricKey(encryptedSymmetricKey)) {
			textArea.append("Stage 4: Received symmetric key." + newline);
			return true;
		}

		else {
			textArea.append("Failed to set symmetric key." + newline);
			return false;
		}
	}

	// ***************************************   Network Methods   *************************************** //

	/**
	 * Send a key exchange message
	 */
	private void sendConnectionMessage(String message) {
		try {
			// Convert the message to bytes
			byte[] data = message.getBytes();

			// Create a packet with the message and the remote IP and port
			DatagramPacket packet = new DatagramPacket(data, data.length, remoteIP, remoteChatPort);

			// Send the packet
			chatSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Play incoming call sound
	 */
	private void playIncomingCallSound() {
		try {
			File soundFile = new File("./../../../src/resources/ringtone.wav");
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);

			incomingCallClip = AudioSystem.getClip();
			incomingCallClip.open(audioStream);
			incomingCallClip.start(); // Play the sound
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Establish connection with the remote machine
	 */
	private void establishConnection() {
			// Establish connection
			while (!isConnected) {
				try {
					if (isInitiator) {
						
						textArea.append("---------------------------      Connection Status      ---------------------------" + newline + newline);
						textArea.append("Initiating connection..." + newline);
						sendConnectionMessage("PING"); // Send a PING message to the remote machine

						// Wait for PONG response
						DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
						chatSocket.setSoTimeout(100); // First person to open the app waits for 100ms and then becomes the responder
						chatSocket.receive(responsePacket);

						String response = new String(responsePacket.getData(), responsePacket.getOffset(), responsePacket.getLength());
						
						if ("PONG".equals(response)) {

							textArea.append("Received response from remote party." + newline);
							textArea.append("Connection established - Initiator" + newline + newline);
							isConnected = true; // Connection established

							textArea.append("---------------------------      Secure Connection      ---------------------------" + newline + newline);

							textArea.append("Starting secure connection..." + newline);
							initiateKeyExchange(); // Initiates key exchange with the remote machine
						}
					}

					else {
						// Listen for PING and respond with PONG
						DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
						chatSocket.receive(requestPacket);

						String receivedMessage = new String(requestPacket.getData(), requestPacket.getOffset(),
								requestPacket.getLength());
						if ("PING".equals(receivedMessage)) {

							textArea.append("Remote party connected, responding to connection request." + newline);

							sendConnectionMessage("PONG");

							textArea.append("Connection established - Responder" + newline + newline);
							isConnected = true;

							textArea.append(
									"---------------------------      Secure Connection      ---------------------------"
											+ newline + newline);

							textArea.append("Waiting for secure connection initialization by remote..." + newline);
						}
					}
				} catch (SocketTimeoutException e) {
					// Switch roles if no response after timeout
					if (isInitiator) {
						isInitiator = false; // No response, retrying as responder
						textArea.append("Waiting for remote party to connect." + newline);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

	}

	/**
	 * Handle connection state messages
	 */
	private void handleConnectionState(String receivedMessage) {

		// Handle key exchange messages
		if (receivedMessage.startsWith("PUBLIC_KEY:")) {
			handlePublicKeyExchange(receivedMessage.substring(11)); // As responder, send encrypted
																	// symmetric key
		} 
		
		else if (receivedMessage.startsWith("SYMMETRIC_KEY:")) {

			if (handleSymmetricKeyExchange(receivedMessage.substring(14))) {
				textArea.append("Secure connection established." + newline + newline);
				textArea.append("---------------------------      Chat      ---------------------------" + newline + newline);
				sendConnectionMessage("SECURE_CONNECTION_ESTABLISHED");
			}

			else
				textArea.append("Failed to establish secure connection." + newline);

		} 
		
		else if ("SECURE_CONNECTION_ESTABLISHED".equals(receivedMessage)) {
			textArea.append("Secure connection established." + newline + newline);
			textArea.append("---------------------------      Chat      ---------------------------" + newline + newline);
			securityModule.setSecureConnectionStatus(true);
		}

		else if ("CALL_REQUEST".equals(receivedMessage)) {

			textArea.append("Incoming call request..." + newline);

			new Thread(() -> playIncomingCallSound()).start(); // Thread to play incoming call sound (non-blocking)

			// Turn call button into accept/reject buttons
			acceptButton.setVisible(true);
			rejectButton.setVisible(true);
			callButton.setVisible(false);
		}

		else if ("CALL_ACCEPT".equals(receivedMessage)) {
			textArea.append("Call accepted by remote." + newline);
			App.getInstance().startCall();
			callButton.setEnabled(true);
			acceptButton.setVisible(false);
			rejectButton.setVisible(false);
		}

		else if ("CALL_REJECT".equals(receivedMessage)) {
			textArea.append("Call rejected by remote." + newline);
			callButton.setText("Call");
			callButton.setBackground(Color.GREEN);
			callButton.setEnabled(true); 
			acceptButton.setVisible(false);
			rejectButton.setVisible(false);
		}

		else if ("END_CALL".equals(receivedMessage)) {
			textArea.append("Call ended by remote." + newline);
			stopCall();
		}

		else if ("DISCONNECT".equals(receivedMessage)) {
			handleDisconnect();	
		}
								
	}		

	/**
	 * Handle disconnection
	 */
	private void handleDisconnect() {
		textArea.append("Connection lost. The other party has closed the application." + newline);
		isConnected = false;
		securityModule.setSecureConnectionStatus(false);
		isInitiator = true;
		if(isCallActive) {
			stopCall();
		}
	
		// Prompt the user to decide whether to close the app or not
		int choice = JOptionPane.showOptionDialog(
			null,
			"The other party has left. Would you like to close the application?",
			"Connection Lost",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			new String[]{"Close App", "Wait for new connection"},
			null
		);
	
		if (choice == JOptionPane.YES_OPTION) {
			App.getInstance().closeApplication();
		} else if (choice == JOptionPane.NO_OPTION) { // User chose to continue using the app
	
			// Prompt the user to keep or clear the chat history
			int historyChoice = JOptionPane.showOptionDialog(
				null,
				"Would you like to clear the chat history before continuing?",
				"Chat History",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				new String[]{"Clear Chat", "Keep Chat"},
				null
			);
	
			if (historyChoice == JOptionPane.YES_OPTION) {
				// Clear chat history
				textArea.setText("");
			}
	
			// Return to "waiting for connection" state
			textArea.append("Waiting for the remote party to reconnect..." + newline);
			establishConnection(); // Handle reconnection logic
		}
	}

	/**
	 * Close the application
	 */
	public void closeApplication() {
		System.out.println("Window is closing...");
		int choice = JOptionPane.showConfirmDialog(
				null, "Are you sure you want to exit?", "Confirm Exit", JOptionPane.YES_NO_OPTION);

		if (choice == JOptionPane.YES_OPTION) {
			sendConnectionMessage("DISCONNECT");
			dispose(); // Dispose the window
			System.exit(0); // Exit the application
		}
	}

	// ***************************************   GUI Methods   *************************************** //

	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the
	 * window).
	 */

	@Override
	public void windowActivated(WindowEvent e) {
		// This method is called when the window becomes the active window
		System.out.println("Window activated");
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// This method is called after the window has been closed
		System.out.println("Window closed");
	}

	@Override
	public void windowClosing(WindowEvent e) {
		closeApplication();
	}
	
	@Override
	public void windowDeactivated(WindowEvent e) {
		// This method is called when the window loses focus
		System.out.println("Window deactivated");
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// This method is called when the window is restored from a minimized state
		System.out.println("Window deiconified (restored)");
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// This method is called when the window is minimized
		System.out.println("Window iconified (minimized)");
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// This method is called when the window is first opened
		System.out.println("Window opened");
	}

}
