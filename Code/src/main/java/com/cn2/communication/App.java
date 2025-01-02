/**
 *! Instructions:
 ** 1. Replace the IP address in the main method with the actual IP address of the remote machine (line 115).
 * In Windows, you can find the IP address by running the command "ipconfig" in the command prompt.
 * It is the value of the "IPv4 Address" field. Usually, it is something like "192.168.1.100" (where the last octet can be 0-255).
 * 
 ** 2. To run the program, cd ./Code/src/main/java and run the following command:
 ** 	   javac com/cn2/communication/App.java ; java com.cn2.communication.App
 *
 ** 3. In function playIncomingCallSound(), replace the path to the ringtone.wav file with the actual path on your machine (line 920).
 */

package com.cn2.communication; // Define the package where the class is located

import java.io.*; // Import the java.io package to use the classes for input/output operations
import java.net.*; // Import the java.net package to use the classes for networking operations
import java.util.Arrays;
import java.lang.Thread; // Import the Thread class from the java.lang package to create a new thread
import javax.sound.sampled.*;

import javax.swing.*;
import javax.swing.text.DefaultCaret; // Import the DefaultCaret class from the javax.swing.text package to enable auto-scrolling
import java.awt.*; // Import the java.awt package to use the classes for the GUI components
import java.awt.event.*; // Import the java.awt.event package to use the classes for the events

/**
 * The App class is the main class of the application. It creates the app's
 * window and initializes the GUI components. It also listens for the events
 * that are triggered by the user's actions (e.g., clicking a button) and
 * handles them accordingly.
 */
public class App extends Frame implements WindowListener, ActionListener {

	// *************************************** Field Definitions *************************************** //

	private static App instance; // The instance of the App class

	// GUI related fields
	private static JTextField inputTextField; // The text field where the user types the message
	private static JTextArea textArea; // The text area where the messages are displayed
	private static JButton sendButton; // The button that sends the message
	private static JButton callButton; // The button that initiates the call
	private static JButton acceptButton; // Button to accept call
	private static JButton rejectButton; // Button to reject call
	final static String newline = "\n";

	// Network related fields
	private static DatagramSocket chatSocket; // Socket for chat messages
	private static DatagramSocket voiceSocket; // Socket for voice data
	private static ServerSocket serverSocket; // Server socket for TCP connection
	private static InetAddress remoteIP; // Target IP address
	private static int REMOTE_CHAT_PORT = 8000; // Target chat port
	private static int REMOTE_VOICE_PORT = 8001; // Target voice port
	private static int LOCAL_CHAT_PORT = 8002; // Local chat port
	private static int LOCAL_VOICE_PORT = 8003; // Local voice port
	private static byte[] BUFFER = new byte[1024]; // Buffer for receiving data

	// Audio related fields
	private static AudioFormat audioFormat; // Audio format for voice data
	private static TargetDataLine microphone; // Microphone for recording voice
	private static SourceDataLine speakers; // Speakers for playing voice
	private static boolean isCallActive = false; // Flag for call status
	private static Thread receiveVoiceThread; // Thread for receiving voice data
	private static Thread sendVoiceThread; // Thread for sending voice data
	private static long callStartTime; // Time when the call started
	private static long callEndTime; // Time when the call ended
	private static Clip incomingCallClip; // Clip for incoming call sound

	// Connection and security related fields
	private static boolean isConnected = false; // Flag for connection status (connected or not)
	private static boolean isInitiator = true; // Flag for connection initiator (who initiates the connection)
	private static SecurityModule securityModule; // Security module for encryption and decryption of messages
	private String selectedProtocol; // Default πρωτόκολλο
	private static boolean isProtocolSelected = false; // Flag for protocol selection

	private static final String[] CONNECTION_STATES = {
			"CALL_REQUEST", "CALL_ACCEPT", "CALL_REJECT", "END_CALL", "DISCONNECT"
	};

	// *************************************** Constructor *************************************** //

	/**
	 * The constructor of the App class. It initializes the app's window and the GUI
	 */
	public App(String title) {
		super(title);
		setUpGUI(); // Set up the GUI components of the application
		instance = this; // Set the global instance to the current instance
		        // Set the Look and Feel (optional)
				try {
					UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); 
				} catch (Exception e) {
					e.printStackTrace();
				}
		
	}

	public static App getInstance() {
		return instance; // Return the global instance
	}

	// *************************************** Main Method *************************************** //

	/**
	 * The main method of the App class. It creates an instance of the App class and
	 * initializes the network sockets. It also starts the thread for receiving chat
	 * messages and continuously listens new messages.
	 */
	public static void main(String[] args) {
		try {
			App app = new App("Chat & VoIP");
			app.setSize(500, 250);
			app.setVisible(true);

			String remoteIP = null; // !Replace ``null`` with the actual IP address of the remote machine, eg "192.168.1.100"
			initializeNetworkComponents(remoteIP);
			
			app.establishConnection();

		} catch (Exception e) {
			e.printStackTrace();
			cleanupSockets();
		}
	}

	// *************************************** Initialization & Setup *************************************** //
	// Methods for setting up the application environment, network, and GUI.

	/**
	 * Initializes the network components of the application.
	 */
	private static void initializeNetworkComponents(String IP) throws Exception {

		checkPortAvailability();

		chatSocket = new DatagramSocket(LOCAL_CHAT_PORT); // Create a new chat socket
		voiceSocket = new DatagramSocket(LOCAL_VOICE_PORT); // Create a new voice socket

		if (IP == null) {
			remoteIP = InetAddress.getLocalHost(); // Get the local IP address
		} else {
			remoteIP = InetAddress.getByName(IP); // Get the remote IP address
		}

		audioFormat = new AudioFormat(8000.0f, 8, 1, true, false); // Set the audio format for voice data
		securityModule = new SecurityModule(); // Initialize the security module
	}

	// !Testing purposes only (two instances of the app on the same machine)
	private static void checkPortAvailability() {
		try (DatagramSocket testSocket = new DatagramSocket(LOCAL_CHAT_PORT)) {
			// Test socket closes immediately, no action needed
		} catch (Exception e) {
			swapPorts();
		}
	}

	/**
	 * Swaps the local and remote ports for testing purposes.
	 */
	private static void swapPorts() {
		int tempChatPort = LOCAL_CHAT_PORT;
		int tempVoicePort = LOCAL_VOICE_PORT;

		LOCAL_CHAT_PORT = REMOTE_CHAT_PORT;
		LOCAL_VOICE_PORT = REMOTE_VOICE_PORT;
		REMOTE_CHAT_PORT = tempChatPort;
		REMOTE_VOICE_PORT = tempVoicePort;
		}
	

	/**
	 * Handles cleanup of sockets during exceptions or application exit.
	 */
	private static void cleanupSockets() {
		try {
			if (chatSocket != null && !chatSocket.isClosed())
				chatSocket.close();
			if (voiceSocket != null && !voiceSocket.isClosed())
				voiceSocket.close();
		} catch (Exception ignored) {
		}
	}

	/**
	 * Sets up the GUI components of the application.
	 */
	private void setUpGUI() {
		/*
		 * 1. Defining the components of the GUI
		 */

		// Setting up the characteristics of the frame
        // Setting up the characteristics of the frame
        setBackground(Color.LIGHT_GRAY); // Set a lighter background color
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Add some spacing
		// setLayout(new FlowLayout()); // Setting the layout of the frame
		addWindowListener(this); // Adding the WindowListener to the frame

		// Setting up the TextField (where the user types the message).
        inputTextField = new JTextField(20); // Create a JTextField with 20 columns

		// Setting up the TextArea (where the messages will be displayed).
		textArea = new JTextArea(10, 40);
		textArea.setLineWrap(true); // The text will wrap to the next line if it exceeds the width of the text area
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false); // The user cannot edit the displayed messages
		JScrollPane scrollPane = new JScrollPane(textArea); // Adding a scroll pane to the text area
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Setting the vertical scroll bar to always be visible

		// Enable auto-scrolling
		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		// Setting up the buttons
		sendButton = createButton("Send", Color.CYAN);
		callButton = createButton("Call", Color.GREEN);
		acceptButton = createButton("Accept", Color.GREEN, false);
		rejectButton = createButton("Reject", Color.RED, false);

		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(acceptButton);
		add(rejectButton);

		inputTextField.addActionListener(e -> {
			if ("UDP".equals(selectedProtocol)) {
				sendMessageWithUDP();
			} else {
				sendMessageWithTCP();
			}
		});
	}

	/**
	 * Creates a button with the specified text and color.
	 * 
	 * @param text  The text to display on the button.
	 * @param color The background color of the button.
	 * @return The created button.
	 */
	private JButton createButton(String text, Color color) {
		return createButton(text, color, true); // Call the overloaded method with the default visibility
	}

	/**
	 * Creates a button with the specified text, color, and visibility.
	 * 
	 * @param text      The text to display on the button.
	 * @param color     The background color of the button.
	 * @param isVisible The visibility of the button.
	 * @return
	 */
	private JButton createButton(String text, Color color, boolean isVisible) {
		JButton button = new JButton(text); // Create a new button with the specified text
		button.setBackground(color); // Set the background color of the button
		button.setVisible(isVisible); // Set the visibility of the button
		button.addActionListener(this); // Add an action listener to the button
		return button;
	}

	/**
	 * Displays a dialog box for selecting the communication protocol.
	 * 
	 * @return A string indicating the selected protocol ("UDP" or "TCP").
	 */
	private static String setupProtocolSelector() {
		String[] options = { "UDP", "TCP" };
		int choice = JOptionPane.showOptionDialog(
				null,
				"Select the protocol you want to use:",
				"Protocol Selector",
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.INFORMATION_MESSAGE,
				null,
				options,
				options[0]);
		return (choice == 1) ? "TCP" : "UDP";
	}

	/**
	 * Returns the selected protocol.
	 * 
	 * @return The selected protocol ("UDP" or "TCP").
	 */
	public String getSelectedProtocol() {
		return this.selectedProtocol;
	}

	// *************************************** Connection Methods *************************************** //

	/**
	 * Sends a connection-related message (e.g., PING, PONG) to the remote party.
	 * The method uses the currently selected protocol (UDP or TCP).
	 * 
	 * @param message The connection-related message to send.
	 */
	private void sendConnectionMessage(String message) {
		if (!isProtocolSelected || App.getInstance().getSelectedProtocol().equals("UDP")) {
			sendMessageWithUDP(message); // Use UDP for message transmission
		} else if ("TCP".equals(selectedProtocol)) {
			sendMessageWithTCP(message); // Use TCP for message transmission
		}
	}

	/**
	 * Establishes a connection with the remote party using the selected protocol.
	 * Handles initial handshake, secure key exchange, and protocol setup.
	 */
	private void establishConnection() {
		while (!isConnected) { // Loop until the connection is established
			try {
				if (isInitiator) { // If this instance initiates the connection
					textArea.append("--------------------------- Connection Status ---------------------------\n\n");
					textArea.append("Initiating connection...\n");

					// Send a PING message to the remote machine to check availability
					sendConnectionMessage("PING");

					// Wait for a response from the remote party
					DatagramPacket responsePacket = new DatagramPacket(BUFFER, BUFFER.length);
					chatSocket.setSoTimeout(100); // First person to open the app waits for 100ms and then becomes the responder
					chatSocket.receive(responsePacket);

					String response = new String(responsePacket.getData(), responsePacket.getOffset(),
							responsePacket.getLength());

					if ("PONG".equals(response)) { // If the response is PONG, connection established
						textArea.append("Received response from remote party.\n");
						textArea.append("Connection established - Initiator\n\n");
						isConnected = true;

						// Start secure key exchange
						textArea.append(
								"--------------------------- Secure Connection ---------------------------\n\n");
						textArea.append("Starting secure connection...\n");
						initiateKeyExchange();
					}
				} else { // If this instance waits for the connection
					// Wait for a PING message from the remote party
					DatagramPacket requestPacket = new DatagramPacket(BUFFER, BUFFER.length);
					chatSocket.receive(requestPacket);

					String receivedMessage = new String(requestPacket.getData(), requestPacket.getOffset(),
							requestPacket.getLength());

					if ("PING".equals(receivedMessage)) { // If PING is received, respond with PONG
						textArea.append("Remote party connected, responding to connection request.\n");
						sendConnectionMessage("PONG");
						textArea.append("Connection established - Responder\n\n");
						isConnected = true;

						// Wait for secure key exchange to start
						textArea.append(
								"--------------------------- Secure Connection ---------------------------\n\n");
						textArea.append("Waiting for secure connection initialization by remote...\n");
					}
				}

				// Manage secure key exchange (public/private keys and symmetric key)
				while (!securityModule.isSecureConnectionEstablished()) {
					DatagramPacket keyPacket = new DatagramPacket(BUFFER, BUFFER.length);
					chatSocket.receive(keyPacket);

					String keyMessage = new String(keyPacket.getData(), keyPacket.getOffset(), keyPacket.getLength());

					if (keyMessage.startsWith("PUBLIC_KEY:")) { // If the message contains a public key
						String remotePublicKey = keyMessage.split(":")[1];
						handlePublicKeyExchange(remotePublicKey); // Respond with SYMMETRIC_KEY
					} else if (keyMessage.startsWith("SYMMETRIC_KEY:")) { // If the message contains a symmetric key
						String encryptedSymmetricKey = keyMessage.split(":")[1];
						if (handleSymmetricKeyExchange(encryptedSymmetricKey)) { // Decrypt the symmetric key
							textArea.append("Secure connection established successfully!\n");
							sendConnectionMessage("SECURE_CONNECTION_ESTABLISHED"); // Inform the remote party
							securityModule.setSecureConnectionStatus(true); // Mark the connection as secure
						} else {
							textArea.append("Failed to establish secure connection.\n");
						}
					} else if ("SECURE_CONNECTION_ESTABLISHED".equals(keyMessage)) {
						textArea.append("Secure connection established.\n\n");
						securityModule.setSecureConnectionStatus(true); // Mark the connection as secure
					}
				}
			} catch (SocketTimeoutException e) {
				// If no response is received, switch roles from initiator to responder
				if (isInitiator) {
					isInitiator = false;
					textArea.append("Waiting for remote party to connect.\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Reset socket timeout to infinite after connection is established
		try {
			chatSocket.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		// If this instance is the initiator of the connection
		if (isInitiator) {
			textArea.append("Selecting protocol...\n"); // Inform the user that protocol selection is in progress
			this.selectedProtocol = setupProtocolSelector(); // Display the protocol selector dialog and store the user's choice
			textArea.append("Selected protocol: " + this.selectedProtocol + "\n"); // Log the selected protocol
			sendConnectionMessage("PROTOCOL:" + this.selectedProtocol); // Inform the remote party of the chosen
																		// protocol
			isProtocolSelected = true; // Mark protocol selection as complete
		} else {
			// If this instance is waiting for the other party to select the protocol
			textArea.append("Waiting for protocol selection...\n"); // Inform the user that the app is waiting for protocol info
			DatagramPacket protocolPacket = new DatagramPacket(BUFFER, BUFFER.length); // Create a packet to receive the protocol message
			do {
				try {
					chatSocket.receive(protocolPacket); // Wait for a message from the remote party
					String protocolMessage = new String(
							protocolPacket.getData(),
							protocolPacket.getOffset(),
							protocolPacket.getLength()); // Extract the received protocol message
					if (protocolMessage.startsWith("PROTOCOL:")) { // Check if the message specifies the protocol
						this.selectedProtocol = protocolMessage.split(":")[1].trim(); // Parse the selected protocol from the message
						textArea.append("Protocol received: " + this.selectedProtocol + "\n"); // Log the received protocol
						isProtocolSelected = true; // Mark protocol selection as complete
						break; // Exit the loop once the protocol is received
					}
				} catch (IOException e) {
					e.printStackTrace(); // Log any errors encountered during protocol message reception
				}
			} while (true); // Keep waiting until a valid protocol message is received
		}

		// Determine if the selected protocol is UDP
		boolean useUDP = getSelectedProtocol().equals("UDP"); // Check if the user or remote party selected UDP

		// Start the appropriate listener based on the selected protocol
		if (useUDP) {
			textArea.append("---------------------------      UDP Connection      ---------------------------\n");
			startUDPListener(); // Start the UDP listener for receiving messages
		} else {
			textArea.append("---------------------------      TCP Connection      ---------------------------\n");
			chatSocket.close(); // Close the UDP chat socket as TCP will be used
			startTCPServer(); // Start the TCP server for managing connections and messages
		}
	}

	/**
	 * Handle connection state messages
	 * 
	 * @param receivedMessage The received message to process
	 */
	private void handleConnectionState(String receivedMessage) {

		if ("CALL_REQUEST".equals(receivedMessage)) {

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
	 * Handles disconnection from the remote party. Cleans up resources and prompts
	 * the user to decide whether to wait for a new connection.
	 */
	private void handleDisconnect() {
		textArea.append("Connection lost. The other party has closed the application.\n");

		// Update application state variables
		isConnected = false;
		securityModule.setSecureConnectionStatus(false);
		isInitiator = true;

		if (isCallActive) { // If a call is active, stop it
			stopCall();
		}

		// Prompt the user for action after disconnection
		int choice = JOptionPane.showOptionDialog(
				null,
				"The other party has left. Would you like to close the application?",
				"Connection Lost",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				new String[] { "Close App", "Wait for new connection" },
				null);

		if (choice == JOptionPane.YES_OPTION) {
			App.getInstance().closeApplication(); // Close the application
		} else if (choice == JOptionPane.NO_OPTION) {

			if ("TCP".equals(App.getInstance().getSelectedProtocol())) { // If TCP was used, close the server socket
				try {
					if (serverSocket != null && !serverSocket.isClosed()) {
						serverSocket.close();
						textArea.append("Closed TCP server socket.\n");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			
			isProtocolSelected = false;
			selectedProtocol = null;

			// Initialize new UDP sockets with a delay to ensure proper cleanup
			try {
				Thread.sleep(1000); // Add a small delay to ensure sockets are properly closed

				// Attempt to create new UDP sockets
				try {
					chatSocket = new DatagramSocket(LOCAL_CHAT_PORT);
					voiceSocket = new DatagramSocket(LOCAL_VOICE_PORT);
					textArea.append("Successfully initialized UDP sockets for reconnection.\n");
				} catch (SocketException e) {
					e.printStackTrace();
					textArea.append("Critical error initializing UDP sockets: " + e.getMessage() + "\n");
					JOptionPane.showMessageDialog(null,
							"Failed to initialize network connection. Please restart the application.",
							"Connection Error",
							JOptionPane.ERROR_MESSAGE);
					System.exit(1);
					return;
				}

			} catch (Exception e) {
				e.printStackTrace();
				textArea.append("Critical error initializing UDP sockets: " + e.getMessage() + "\n");
				JOptionPane.showMessageDialog(null,
						"Failed to initialize network connection. Please restart the application.",
						"Connection Error",
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
				return;
			}
		}

			// Prompt the user to keep or clear the chat history
			int historyChoice = JOptionPane.showOptionDialog(
					null,
					"Would you like to clear the chat history before continuing?",
					"Chat History",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					new String[] { "Clear Chat", "Keep Chat" },
					null);

			if (historyChoice == JOptionPane.YES_OPTION) {
				// Clear chat history
				textArea.setText("");
			}

			// Return to "waiting for connection" state
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
			if(isConnected) {
				sendConnectionMessage("DISCONNECT");
			}
			dispose(); // Dispose the window
			System.exit(0); // Exit the application
		}
	}

	// *************************************** Network Communication *************************************** //
	// Methods for sending and receiving messages using various protocols.

	/**
	 * Sends a user-entered message using the selected protocol (UDP or TCP).
	 */
	private void sendMessage() {
		if ("UDP".equals(selectedProtocol)) {
			sendMessageWithUDP();
		} else {
			sendMessageWithTCP();
		}
	}

	/**
	 * Sends a user-entered message using UDP.
	 */
	public void sendMessageWithUDP() {
		if (!securityModule.isSecureConnectionEstablished()) {
			textArea.append("Secure connection not established. Cannot send message." + newline);
			return;
		}

		if (inputTextField.getText().equals("DISCONNECT")) {
			textArea.append("okkkkkk.!!!!!!!!!!!!!!" + newline);
		}

		// Get the message from the text field
		String message = inputTextField.getText();

		// Encrypt the message
		String encryptedMessage = securityModule.encrypt(message);

		if (encryptedMessage == null) {
			textArea.append("Message encryption failed." + newline);
			return;
		}

		// Send the encrypted message via UDP
		sendMessageWithUDP(encryptedMessage);

		// Display the message in the text area
		textArea.append("Send (UDP): " + message + newline);

		// Clear the text field
		inputTextField.setText("");
	}

	/**
	 * Sends a message as a UDP packet.
	 * 
	 * @param message The message to be sent.
	 */

	public void sendMessageWithUDP(String message) {
		try {
			DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), remoteIP,
					REMOTE_CHAT_PORT);
			chatSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
			textArea.append("Failed to send message via UDP." + newline);
		}
	}

	/**
	 * Starts a thread to listen for incoming UDP messages.
	 */
	private void startUDPListener() {
		new Thread(() -> {
			while (true) {
				try {
					Arrays.fill(BUFFER, (byte) 0); // Clear buffer
					DatagramPacket packet = new DatagramPacket(BUFFER, BUFFER.length);
					chatSocket.receive(packet);

					String receivedMessage = new String(packet.getData(), packet.getOffset(), packet.getLength());

					// Check if the message matches a connection state
					boolean isConnectionState = false;
					for (String state : CONNECTION_STATES) {
						if (receivedMessage.startsWith(state)) {
							isConnectionState = true;
							handleConnectionState(receivedMessage);
							break;
						}
					}

					if (!isConnectionState) {
						if (securityModule.isSecureConnectionEstablished()) {
							String decryptedMessage = securityModule.decrypt(receivedMessage);
							textArea.append("Received (UDP): " + decryptedMessage + "\n");
						} else {
							textArea.append("Secure connection not established. Discarding message.\n");
						}
					}

				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
		}).start();
	}

	/**
	 * Sends a user-entered message using TCP.
	 */
	public void sendMessageWithTCP() {
		if (!securityModule.isSecureConnectionEstablished()) {
			textArea.append("Secure connection not established. Cannot send message.\n");
			return;
		}

		String message = inputTextField.getText();
		String encryptedMessage = securityModule.encrypt(message);

		if (encryptedMessage == null) {
			textArea.append("Message encryption failed.\n");
			return;
		}

		sendMessageWithTCP(encryptedMessage);
		textArea.append("Sent (TCP): " + message + "\n");
		inputTextField.setText("");

	}

	/**
	 * Sends a message via TCP.
	 * 
	 * @param message The message to send.
	 */
	public void sendMessageWithTCP(String message) {

		try (Socket tcpSocket = new Socket(remoteIP, REMOTE_CHAT_PORT);
				OutputStream out = tcpSocket.getOutputStream()) {

			out.write(message.getBytes());
			out.flush();

		} catch (Exception ex) {
			ex.printStackTrace();
			textArea.append("Failed to send message via TCP.\n");
		}
	}

	/**
	 * Starts a TCP server to listen for incoming messages.
	 */
	private void startTCPServer() {
		new Thread(() -> {
			try {
				serverSocket = new ServerSocket(LOCAL_CHAT_PORT);
				textArea.append("TCP server started. Listening for connections...\n");

				while (!serverSocket.isClosed()) {
					try (Socket clientSocket = serverSocket.accept();
							InputStream in = clientSocket.getInputStream()) {

						byte[] buffer = new byte[1024];
						int bytesRead = in.read(buffer);
						if (bytesRead > 0) {
							String receivedMessage = new String(buffer, 0, bytesRead);

							// Handle connection states or regular messages
							boolean isConnectionState = false;
							for (String state : CONNECTION_STATES) {
								if (receivedMessage.startsWith(state)) {
									isConnectionState = true;
									handleConnectionState(receivedMessage);
									break;
								}
							}

							// Process decrypted message if connection is secure
							if (!isConnectionState) {
								if (securityModule.isSecureConnectionEstablished()) {
									receivedMessage = securityModule.decrypt(receivedMessage);
									textArea.append("Received (TCP): " + receivedMessage + newline);
								} else {
									textArea.append("Secure connection not established. Discarding message (TCP).\n");
								}
							}
						}

					} catch (IOException e) {
						if (!serverSocket.isClosed()) { // Log errors only if the server is not shutting down
							e.printStackTrace();
							textArea.append("TCP server error while processing client connection.\n");
						}
					}
				}

			} catch (IOException ex) {
				if (!serverSocket.isClosed()) { // Log errors only if the server is not shutting down
					ex.printStackTrace();
					textArea.append("TCP server error.\n");
				}
			}
		}).start();
	}

	// *************************************** Call Management *************************************** //
	// Methods for managing call sessions.

	/**
	 * Initiates a call session.
	 */
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
							DatagramPacket packet = new DatagramPacket(voiceBuffer, count, remoteIP, REMOTE_VOICE_PORT);
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

	/**
	 * Stops an active call session.
	 */
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

	// *************************************** Call Actions (UI Event Handlers) *************************************** //
	// Methods responding to user actions during a call.

	/**
	 * Handles the "Call" button action to initiate or end a call.
	 */
	private void handleCallButton() {
		if (!isConnected) {
			textArea.append("Connection not established. Cannot initiate call." + newline);
		} else if (!isCallActive) {
			callButton.setEnabled(false);
			callButton.setText("Calling...");
			callButton.setBackground(Color.orange);
			sendConnectionMessage("CALL_REQUEST");
			textArea.append("Call request sent." + newline);
		} else {
			stopCall();
			sendConnectionMessage("END_CALL");
			textArea.append("Ended call." + newline);
		}
	}

	/**
	 * Handles the "Accept" button action for incoming calls.
	 */
	private void handleAcceptButton() {
		acceptButton.setVisible(false);
		rejectButton.setVisible(false);
		if (incomingCallClip != null && incomingCallClip.isRunning()) {
			incomingCallClip.stop();
		}
		sendConnectionMessage("CALL_ACCEPT");
		textArea.append("Call accepted." + newline);
		callButton.setVisible(true);
		if (!isCallActive) {
			startCall();
		}
	}

	/**
	 * Handles the "Reject" button action for incoming calls.
	 */
	private void handleRejectButton() {
		acceptButton.setVisible(false);
		rejectButton.setVisible(false);
		if (incomingCallClip != null && incomingCallClip.isRunning()) {
			incomingCallClip.stop();
		}
		sendConnectionMessage("CALL_REJECT");
		textArea.append("Call rejected." + newline);
		callButton.setVisible(true);
	}

	// *************************************** Security *************************************** //
	// Methods related to security features.

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

	// *************************************** Event Handling *************************************** //
	// General event handling, including window events.

	/**
	 * The method that corresponds to the Action Listener. Whenever an action is
	 * performed (i.e., one of the buttons is clicked) this method is executed.
	 * 
	 * @param e The ActionEvent triggered by button clicks.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == sendButton) {
			sendMessage();
		} else if (e.getSource() == callButton) {
			handleCallButton();
		} else if (e.getSource() == acceptButton) {
			handleAcceptButton();
		} else if (e.getSource() == rejectButton) {
			handleRejectButton();
		}
	}

	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the window).
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