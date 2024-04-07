package game;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import javax.swing.*;


public class GameServer extends JFrame implements Runnable {

	private static int WIDTH = 400;
	private static int HEIGHT = 300;
	private int clientNo = 0;
	private JTextArea jta;
	Set<ObjectOutputStream> objectOutputSet = new HashSet<ObjectOutputStream>();
	Thread t;
	String[][] gameBoard = new String[3][3];
	boolean gameEnd = false;
	int winner = 0;


	//Set<Thread> activeThread = new HashSet<Thread>();
	//Queue<Thread> waitingThread = new ArrayDeque<Thread>();

	
	public GameServer() {
		super("Game Server");
		this.setSize(GameServer.WIDTH, GameServer.HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// initialize an empty game board
		// Player 1's move -> "X"
		// Player 2's move -> "O"
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				gameBoard[i][j] = "";
			}
		}
		createMenu();
		jta = createTextArea();
		this.add(jta);
		t = new Thread(this);
		t.start();
		this.setVisible(true);	
	}
	
	
	private void createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener((e) -> System.exit(0));
		menu.add(exitItem);
		menuBar.add(menu);
		this.setJMenuBar(menuBar);
	}
	
	
	public JTextArea createTextArea() {
		JTextArea jta = new JTextArea();
		return jta;
	}
	

	public static void main(String[] args) {
		GameServer gameServer = new GameServer();
	}

	
	@Override
	public void run() {
		
		try {
			ServerSocket ss = new ServerSocket(9898);
			jta.append("GameServer started at " + new Date() + "\n");
			
			while (true) {
				Socket s = ss.accept();
				clientNo ++;
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				
				if (this.clientNo <= 2) { // still accepting new client
					String connMessage = "accepted," + clientNo;
					oos.writeObject(connMessage);
					oos.flush();
					objectOutputSet.add(oos);
					Thread new_t = new Thread(new HandleGameClient(s, clientNo));
					new_t.start();
					jta.append("Starting thread for game client " + clientNo + " at " + new Date() + "\n");
				}else { // prevent excessive clients
					String fullMessage = "full," + clientNo;
					oos.writeObject(fullMessage);
					oos.flush();
				}

				//this.activeThread.add(new_t);
			}
		} catch (IOException ioe) {
			System.err.println(ioe);
		}	
		

	}
	
	/***
	// check and delete closed thread in the set
	public void updateActiveThread() {
		System.out.println("did visit");
		if (this.activeThread.size() != 0) {
			for (Thread t: this.activeThread) {
				if (!t.isAlive()) {
					this.activeThread.remove(t); // remove closed socket
				}
			}
		}
	}
	***/
	
	class HandleGameClient implements Runnable{
		private Socket socket;
		
		public HandleGameClient(Socket socket, int clientNo) {
			this.socket = socket;

		}
		
		public void run() {
	        try {
				ObjectInputStream inputFromClient = new ObjectInputStream(socket.getInputStream());
	            while (!gameEnd) {
	            	// Process player's move, determine if there is a winner, and send to each player
	            	// the sent-out message have four components: row,col,playerID,winner
	            	// we use:
	            	// winner = 0 -> no winner yet, game continues
	            	// winner = 1 -> player 1 wins, game ends
	            	// winner = 2 -> player 2 wins, game ends
	            	// winner = 3 -> gameBoard is full, draws, and game ends
	            	
	            	if (checkFull() == 3) {
	            		gameEnd = true;
	            		break;
	            	}else {
	            		Object object = inputFromClient.readObject();
		                String[] arr = object.toString().split(",");
		                int playerID = Integer.parseInt(arr[2]);
		                int rowi = Integer.parseInt(arr[0]);
		                int coli = Integer.parseInt(arr[1]);
		                String currMove = "Player " + playerID + " makes a move at row "+ rowi + " column " + coli + "\n";
		                jta.append(currMove);
		                
		                // update the gameBoard
		                if (playerID == 1) {
		                	gameBoard[rowi][coli] = "X";
		                }else if (playerID == 2) {
		                	gameBoard[rowi][coli] = "O";
		                }
		            	
		                // check winner
		                winner = checkWinner();
		                
		                // if no winner, check if the gameBoard is full
		                if (winner == 0) {
		                	winner = checkFull();
		                }
		                
		                for (int i = 0; i < 3; i++) {
		                	for (int j =0; j < 3; j++) {
		                		System.out.print(gameBoard[i][j] + ",");
		                	}
		                }
		                
		                // send message to clients
		                String updateMessage = rowi + "," + coli + "," + playerID + "," + winner;
		            	for (ObjectOutputStream oos: objectOutputSet) {
			            	oos.writeObject(updateMessage);
			            	oos.flush();
		            	}
		            	
		            	// if the game ends, terminates the server
		            	if (winner != 0) {
		            		gameEnd = true;
		            	}
	            	}     
	            }
	            t.interrupt();
	            
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		// check and update winner if necessary
		public int checkWinner() {	
			// Check Horizontally
			for (int i = 0; i < 3; i++) {
				// player 1 wins
				if ( (gameBoard[i][0].equals("X") )
					&& (gameBoard[i][1].equals("X"))
					&& (gameBoard[i][2].equals("X"))) {
					return 1;
				// player 2 wins
				}else if ( (gameBoard[i][0].equals("O"))
					&& (gameBoard[i][1].equals("O"))
					&& (gameBoard[i][2].equals("O"))) {
					return 2;
				}
			}
			
			// Check Vertically
			for (int j = 0; j < 3; j++) {
				// player 1 wins
				if ( (gameBoard[0][j].equals("X"))
					&& (gameBoard[1][j].equals("X"))
					&& (gameBoard[2][j].equals("X"))) {
					return 1;
				// player 2 wins
				}else if ( (gameBoard[0][j].equals("O"))
					&& (gameBoard[1][j].equals("O"))
					&& (gameBoard[2][j].equals("O"))) {
					return 2;
				}
			}
			
			// Check diagonal
			// player 1 wins
			if ( (gameBoard[0][0].equals("X"))
				&& (gameBoard[1][1].equals("X"))
				&& (gameBoard[2][2].equals("X"))) {
				return 1;
			// player 2 wins
			}else if ( (gameBoard[0][0].equals("O"))
				&& (gameBoard[1][1].equals("O"))
				&& (gameBoard[2][2].equals("O"))) {
				return 2;
			}
			
			// Check reverse diagonal
			// player 1 wins
			if ( (gameBoard[0][2].equals("X"))
				&& (gameBoard[1][1].equals("X"))
				&& (gameBoard[2][0].equals("X"))) {
				return 1;
			// player 2 wins
			}else if ( (gameBoard[0][2].equals("O"))
				&& (gameBoard[1][1].equals("O"))
				&& (gameBoard[2][0].equals("O"))) {
				return 2;
			}
			
			return 0;
		}
		
		// check if the gameBoard is full.
		// return 0 if not full
		// return 3 if full
		public int checkFull() {
			System.out.println("checkfull");
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					if(gameBoard[i][j].equals("")) {
						return 0; // not full, return 0 right away
					}
				}
			}
			System.out.println("is Full");
			jta.append("Draws.\n");
			return 3;
		}	
	}
}


