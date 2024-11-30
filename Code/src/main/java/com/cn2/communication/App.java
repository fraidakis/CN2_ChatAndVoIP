// cd "c:\Users\Lenovo Ideapad 5 Pro\OneDrive - Αριστοτέλειο Πανεπιστήμιο Θεσσαλονίκης\Επιφάνεια εργασίας\Δίκτυα ΙΙ\Project\Code\CN2_AUTH_ChatAndVoIP\src\main\java" ; if ($?) { javac com\cn2\communication\App.java } ; if ($?) { java com.cn2.communication.App }

/** 
 *! Instructions:
 ** 1. Replace the IP address in the main method with the actual IP address of the target machine.
 * In windows, you can find the IP address by running the command "ipconfig" in the command prompt.
 * It is the value of the "IPv4 Address" field. Usually, it is something like "192.168.1.XXX".
 * 
 ** 2. To run the program, cd ./Code/src/main/java and run the following command:
 ** 	  javac com/cn2/communication/App.java ; java com.cn2.communication.App
 */


package com.cn2.communication;

import java.io.*; // Import the java.io package to use the classes for input/output operations
import java.net.*; // Import the java.net package to use the classes for networking operations

import javax.swing.JFrame; // Import the JFrame class from the javax.swing package to create the app's window
import javax.swing.JTextField; // Import the JTextField class from the javax.swing package to create the text field
import javax.swing.JButton; // Import the JButton class from the javax.swing package to create the button
import javax.swing.JTextArea; // Import the JTextArea class from the javax.swing package to create the text area
import javax.swing.JScrollPane; // Import the JScrollPane class from the javax.swing package to create the scroll pane

// Import the Color class from the java.awt package to set the color of the GUI components
import java.awt.*; // Import the java.awt package to use the classes for the GUI components
import java.awt.event.*; // Import the java.awt.event package to use the classes for the events
import java.lang.Thread; // Import the Thread class from the java.lang package to create a new thread

// Mine declarations
import javax.sound.sampled.*;

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

	// GUI related fields
	static TextField inputTextField; // The text field where the user types the message
	static JTextArea textArea; // The text area where the messages are displayed
	static JFrame frame; // The app's window
	static JButton sendButton; // The button that sends the message
	static JTextField meesageTextField; // ???? Never used. Instead we have inputTextField...)
	public static Color gray; // The color of the GUI components
	final static String newline = "\n";
	static JButton callButton; // The button that initiates the call

	// TODO: Please define and initialize your variables here...

	// Network related fields
	private static DatagramSocket chatSocket; // Socket for chat messages
	private static DatagramSocket voiceSocket; // Socket for voice data
	private static InetAddress targetIP; // Target IP address
	private static int targetChatPort = 8000; // Target chat port
	private static int targetVoicePort = 8001; // Target voice port
	private static int localChatPort = 8000; // Local chat port
	private static int localVoicePort = 8001; // Local voice port
	private static byte[] buffer = new byte[1024]; // Buffer for receiving data

	// Audio related fields
	private static AudioFormat audioFormat; // Audio format for voice data
	private static TargetDataLine microphone; // Microphone for recording voice
	private static SourceDataLine speakers; // Speakers for playing voice
	private static boolean isCallActive = false; // Flag for call status
	private static Thread receiveVoiceThread; // Thread for receiving voice data
	private static Thread sendVoiceThread; // Thread for sending voice data
	private static Thread receiveChatThread; // Thread for receiving chat messages

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
		textArea.setEditable(false); // The user cannot edit the displayed messages
		JScrollPane scrollPane = new JScrollPane(textArea); // Adding a scroll pane to the text area
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Setting the vertical scroll bar to always be visible

		// Setting up the buttons
		sendButton = new JButton("Send");
		callButton = new JButton("Call");

		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);
		add(inputTextField);
		add(sendButton);
		add(callButton);

		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this); // The ActionListener will listen for the "Send" button to be clicked
		sendButton.setBackground(Color.CYAN); // Setting the background color of the "Send" button

		callButton.addActionListener(this); // The ActionListener will listen for the "Call" button to be clicked
		callButton.setBackground(Color.GREEN); // Setting the background color of the "Call" button

		inputTextField.addActionListener(e -> sendMessage()); // The ActionListener will listen for the "Enter" key to be pressed in the text field
	}

	/**
	 * The main method of the App class. It creates an instance of the App class and
	 * initializes the network sockets. It also starts the thread for receiving chat 
	 * messages and continuously listens new messages.
	 */
	public static void main(String[] args) {

		try {
			/*
			 * 1. Create the app's window
			 */
			App app = new App("CN2 - AUTH"); // TODO: You can add the title that will displayed on the Window of the App here
			app.setSize(500, 250);
			app.setVisible(true);

			// Initialize network sockets
			chatSocket = new DatagramSocket(localChatPort); // Create a new DatagramSocket for chat messages (listening on port localChatPort=8000)
			voiceSocket = new DatagramSocket(localVoicePort); // Create a new DatagramSocket for voice data (listening on port localVoicePort=8001)

			// Get remote IP (for testing, using localhost)
			targetIP = InetAddress.getLocalHost(); // Get the IP address of the local machine

			// targetIP = InetAddress.getByName("192.168.1.122"); // Replace with the IP address you want to communicate with
			

			// Initialize audio format (8000Hz, 8bit, mono, signed) - PCM format (Pulse Code Modulation)
			audioFormat = new AudioFormat(8000.0f, 8, 1, true, false);


			// Start chat message receiver thread
			receiveChatThread = new Thread(() -> {
				/*
				 * 2. Listen for new messages
				 */
				do {
					// TODO: Be in a loop and listen for new messages

					try {
						// Prepare packet for receiving
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // Create a new DatagramPacket for receiving data
						chatSocket.receive(packet); // Receive data and store it in the packet

						// Convert received data to string and display
						String message = new String(packet.getData(), packet.getOffset(), packet.getLength()); // Convert the received data to a string

						textArea.append("Receive: " + message + newline); // Display the received message in the text area

					} catch (IOException e) {
						e.printStackTrace(); // Print any exceptions that occur while receiving messages
					}

				} while (true);
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
	 * performed
	 * (i.e., one of the buttons is clicked) this method is executed.
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
			if (!isCallActive) {
				startCall();
			} else {
				stopCall();
			}
		}
	}

	/**
	 * The method that sends the message to the target machine.
	 */
	public void sendMessage() {
		try {
			// Get the message from the text field
			String message = inputTextField.getText();

			// Convert the message to bytes
			byte[] data = message.getBytes();

			// Create a packet with the message and the target IP and port
			DatagramPacket packet = new DatagramPacket(data, data.length, targetIP, targetChatPort);

			// Send the packet
			chatSocket.send(packet);

			// Display the message in the text area
			textArea.append("Send: " + message + newline);

			// Clear the text field
			inputTextField.setText("");

		} catch (IOException e) {
			e.printStackTrace();
		}
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

			// Open and start audio devices
			microphone.open(audioFormat);
			speakers.open(audioFormat);
			microphone.start();
			speakers.start();

			isCallActive = true;
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
							DatagramPacket packet = new DatagramPacket(voiceBuffer, count, targetIP, targetVoicePort);
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
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
	}
}
