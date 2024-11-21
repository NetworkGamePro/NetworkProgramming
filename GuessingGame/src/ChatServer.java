//2071344 고윤영
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer extends JFrame {
    private JTextArea displayArea;
    private JButton exitButton;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();  
    
    public ChatServer() {
        setTitle("2071344 Server");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createDisplayPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);

        setVisible(true);
        
        initializeServer();
    }

    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(54321); 
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                appendToDisplay("사용자가 접속되었습니다.");
                
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);  
                clientHandler.start();  
            }
            
        } catch (IOException e) {
            appendToDisplay("서버 시작 에러 " + e.getMessage());
        }
    }

     private class ClientHandler extends Thread {
         private Socket clientSocket;
         private ObjectInputStream in;
         private ObjectOutputStream out;

         public ClientHandler(Socket socket) {
             this.clientSocket = socket;
         }

         @Override
         public void run() {
             try {
                 in = new ObjectInputStream(clientSocket.getInputStream());
                 out = new ObjectOutputStream(clientSocket.getOutputStream());

                 receiveMessages();

             } catch (IOException e) {  
                 appendToDisplay("사용자 접속 에러 " + e.getMessage());
                 
             } finally {  
                 try { 
                     if (clientSocket != null) clientSocket.close(); 
                 } catch (IOException e) { 
                     appendToDisplay("소켓 에러 " + e.getMessage()); 
                 }
             }
         }

         private void receiveMessages() {  
        	    try {  
        	        while (true) {  
        	            ChatMsg chatMsg = (ChatMsg) in.readObject();  

        	            if (chatMsg.getMode() == 16) { 
        	                appendToDisplay(chatMsg.getUserID() + ": " + chatMsg.getMessage()); 
        	                broadcastMessage(chatMsg);  
        	            } else if (chatMsg.getMode() == 64) { 
        	                appendToDisplay(chatMsg.getUserID() + ": " + chatMsg.getMessage()); 

        	                broadcastMessage(chatMsg); 
        	            }
        	        }
        	        
        	    } catch (EOFException e) { 
        	        appendToDisplay("사용자가 연결을 종료했습니다.");
        	        
        	    } catch (IOException | ClassNotFoundException e) { 
        	        appendToDisplay("메세지 에러 " + e.getMessage());
        	        
        	    }
        	}
         
         private void broadcastMessage(ChatMsg chatMsg) {
             for (ClientHandler client : clients) {
                 try {
                     client.out.writeObject(chatMsg); 
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }
      }

     private void appendToDisplay(String message) {  
         SwingUtilities.invokeLater(() -> displayArea.append(message + "\n"));  
     }

     private JPanel createDisplayPanel() {  
         JPanel panel = new JPanel(new BorderLayout());  
         displayArea = new JTextArea();  
         displayArea.setEditable(false);  
         JScrollPane scrollPane = new JScrollPane(displayArea);  
         panel.add(scrollPane, BorderLayout.CENTER);  
         return panel;  
     }

     private JPanel createControlPanel() {  
         JPanel panel = new JPanel(new BorderLayout());  
         exitButton = new JButton("끝내기");  
         exitButton.addActionListener(e -> System.exit(0));  
         panel.add(exitButton, BorderLayout.CENTER);  
         return panel;  
     }

     public static void main(String[] args) { 
          new ChatServer(); 
      } 
}