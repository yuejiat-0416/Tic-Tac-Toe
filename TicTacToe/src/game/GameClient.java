package game;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.*;

public class GameClient extends JFrame implements Runnable {

	private static int WIDTH = 400;
	private static int HEIGHT = 600;
	JTextArea jta;
	JTextField jtf;
	Socket socket = null;
	JButton openButton;
	JButton closeButton;
	JButton sendButton;
	JMenu jm;
	ActionListener buttonListener;
	Thread t;
	ObjectOutputStream toServer;
	ObjectInputStream inputFromServer;
	JTextField[][] gameBoard = new JTextField[3][3];
	int playerID;
    boolean gameEnd = false;

	
	public GameClient() {
		super("Game Client");
		this.setSize(GameClient.WIDTH, GameClient.HEIGHT);
		//this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//this.setVisible(true);

		JPanel board = new JPanel();
		board.setLayout(new GridLayout(3,3));
		
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				JTextField jtf = new JTextField();
				gameBoard[i][j] = jtf;
				board.add(jtf);
			}
		}
		
		this.add(board, BorderLayout.NORTH);
		
		this.setJMenuBar(createMenu());
		jta = new JTextArea();
		this.add(jta, BorderLayout.CENTER);
		
		JPanel jp = new JPanel();
		jp.setLayout(new GridLayout(1,2));
		jtf = new JTextField();
		jp.add(jtf);		
		sendButton = new JButton("Send my move");
		buttonListener = new ButtonListener();
		sendButton.addActionListener(buttonListener);
		jp.add(sendButton);
		this.add(jp, BorderLayout.SOUTH);
		t = new Thread(this);
	}
	
	class OpenConnectionListener implements ActionListener {

	@Override
	    public void actionPerformed(ActionEvent e) {
		    try {
			    socket = new Socket("localhost", 9898);
				// cannot start the client thread now, need to wait for server's message
				inputFromServer = new ObjectInputStream(socket.getInputStream());
				jta.append("Waiting for open positions...\n");
                Object object = inputFromServer.readObject();
                String message = object.toString();
                String[] message_l = message.split(",");
                if (message_l[0].compareTo("accepted") == 0) {
                	playerID = Integer.parseInt(message_l[1]);
    			    jta.append("connected! \n");
    			    jta.append("You are player " + message_l[1] + "\n");
    			    jta.append("Please send your move in the format of \"row,col\"\n");
    			    jta.append("The row and column are both 0-index\n");
    			    jta.append("--------------------------------------------------------\n");
    				t.start();
                }else if (message_l[0].compareTo("full") == 0) {
                	jta.append("Connection failure: Server reaches its capacity.");
                }
					    	
			} catch (IOException e1) {
				e1.printStackTrace();
				jta.append("Connection failure.");
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}	  
	}

	
	// send player's move to the server
	class ButtonListener implements ActionListener{ 

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (toServer == null) {
			        toServer = new ObjectOutputStream(socket.getOutputStream());
				}
		        // Get text field
		        String msg = jtf.getText().trim();
		        // include player's ID
		        msg = msg + "," + playerID;
		        toServer.writeObject(msg);
		        toServer.flush();
		        jtf.setText("");
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
	}
	
	
	public JMenuBar createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Connection");
		JMenuItem connectItem = new JMenuItem("Connect");
		JMenuItem disconnectItem = new JMenuItem("Disconnect");
		connectItem.addActionListener(new OpenConnectionListener());
		disconnectItem.addActionListener((e) -> { try { socket.close(); jta.append("connection closed");} catch (Exception e1) {System.err.println("error"); }});
		menu.add(connectItem);
		menu.add(disconnectItem);
		menuBar.add(menu);
		return menuBar;
	}
		
	
	public void run() {
        try {
			System.out.println("Starting the game");
            while (!gameEnd) {
            	// Handles in-game messages from the server ------------ To be fulfilled!
            	// Two cases: 1.players move + playerID 2. game result
                Object object = inputFromServer.readObject();
                String message = object.toString();
                System.out.println(message);
                String[] arr = message.split(",");
                int playerID = Integer.parseInt(arr[2]);
                int rowi = Integer.parseInt(arr[0]);
                int coli = Integer.parseInt(arr[1]);
                int winner = Integer.parseInt(arr[3]);
                if (playerID != this.playerID) {
                    String currMove = "Your opponent makes a move at row "+ rowi + " column " + coli + "\n";
                    currMove = currMove + "Now it's your turn.\n";
                    jta.append(currMove);
                }else if (playerID == this.playerID) {
                    String currMove = "Wait for your opponent to take a move.\n";
                    jta.append(currMove);
                }


                // 1. update the game board for each client with "X" or "O"
                if (playerID == 1) {
                	this.gameBoard[rowi][coli].setText("X");
                }else if(playerID == 2) {
                	this.gameBoard[rowi][coli].setText("O");
                }
                
                
                // 2. check if there is a winner or a draw.
                if (winner == 3) {
                	jta.append("Draws!");
                	gameEnd = true;
                }else if (winner == this.playerID) {
                	jta.append("You win!!!");
                	gameEnd = true;
                }else if (winner != 0) {
                	jta.append("You lose.");
                	gameEnd = true;
                }
            }
            this.t.interrupt();
            this.sendButton.removeActionListener(buttonListener);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	
	public static void main(String[] args) {
		GameClient gameClient = new GameClient();
	    gameClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    gameClient.setVisible(true);
	}
}

