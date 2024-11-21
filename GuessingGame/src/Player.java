//2071344 고윤영
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Player extends JFrame {
    private JTextField inputField;
    private JTextPane chatPane; 
    private JButton sendButton, connectButton, disconnectButton, exitButton;
    private JTextField nameField, serverField, portField;  
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Player() {
        setTitle("2071344 Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.SOUTH);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        chatPane = new JTextPane();  
        chatPane.setEditable(false);
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        setupActions();

        setVisible(true);
    }

    private void setupActions() {
        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnect());
        sendButton.addActionListener(e -> sendMessage());
        exitButton.addActionListener(e -> System.exit(0));
    }

    private void connectToServer() {
        try {
            String userName = nameField.getText();
            String serverAddress = serverField.getText();
            int port = Integer.parseInt(portField.getText());

            socket = new Socket(serverAddress, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            sendButton.setEnabled(true);

            new Thread(() -> receiveMessages(userName)).start();

        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "서버 접속 에러" + e.getMessage(), "연결 에러", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            sendButton.setEnabled(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        try {
            if (out != null) {
                String messageText = inputField.getText().trim(); 
                if (!messageText.isEmpty()) {
                    ChatMsg chatMsg = new ChatMsg(nameField.getText(), 16, messageText, null);  
                    out.writeObject(chatMsg);  
                }
                inputField.setText("");  
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            ImageIcon imageIcon = new ImageIcon(selectedFile.getPath());

            try {
                if (out != null) {
                    String fileName = selectedFile.getName(); 
                    ChatMsg chatMsg = new ChatMsg(nameField.getText(), 64, fileName, imageIcon);  
                    out.writeObject(chatMsg);  
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveMessages(String userName) {  
        try {  
            while (true) {  
                ChatMsg chatMsg = (ChatMsg) in.readObject();  
                
                if (chatMsg.getMode() == 16) {  
                    appendToChat(chatMsg.getUserID() + ": " + chatMsg.getMessage());  
                } else if (chatMsg.getMode() == 64) {  
                    appendToChat(chatMsg.getUserID() + ": "+ chatMsg.getMessage()); 
                    appendImageToChat(chatMsg.getImage());  
                }
            }
            
        } catch (IOException | ClassNotFoundException e) {  
            e.printStackTrace();  
        }  
    }
     private void appendToChat(String message) {
         try {
             StyledDocument doc = chatPane.getStyledDocument();
             doc.insertString(doc.getLength(), message + "\n", null);
         } catch (BadLocationException e) {
             e.printStackTrace();
         }
     }

     private void appendImageToChat(ImageIcon imageIcon) {
         try {
             StyledDocument doc = chatPane.getStyledDocument();
             chatPane.setCaretPosition(doc.getLength());  

             chatPane.insertIcon(imageIcon);  

             doc.insertString(doc.getLength(), "\n", null);

         } catch (BadLocationException e) {
             e.printStackTrace();
         }
     }

     private JPanel createInputPanel() {  
         JPanel panel = new JPanel(new BorderLayout());  
         
         inputField = new JTextField();  
         sendButton = new JButton("보내기");  
         JButton imageButton = new JButton("이미지 보내기");  

         sendButton.addActionListener(e -> sendMessage());  
         imageButton.addActionListener(e -> sendImage());  

         panel.add(inputField, BorderLayout.CENTER);  
         panel.add(sendButton, BorderLayout.EAST);  
         panel.add(imageButton, BorderLayout.WEST);  

         return panel;  
     }

     private JPanel createControlPanel() {  
         JPanel panel = new JPanel(new GridLayout(2, 4));  

         JLabel nameLabel = new JLabel("아이디:");
         nameField = new JTextField();

         JLabel serverLabel = new JLabel("서버주소:");
         serverField = new JTextField();

         JLabel portLabel = new JLabel("포트번호:");
         portField = new JTextField();

         connectButton = new JButton("접속하기");  
         disconnectButton = new JButton("연결끊기");  
         exitButton = new JButton("끝내기");  

         disconnectButton.setEnabled(false);  

         panel.add(nameLabel);
         panel.add(nameField);
         panel.add(serverLabel);
         panel.add(serverField);
         panel.add(portLabel);
         panel.add(portField);
         
         panel.add(connectButton);  
         panel.add(disconnectButton);  

         return panel;  
     }

     public static void main(String[] args) {  
         new Player();  
     } 
}