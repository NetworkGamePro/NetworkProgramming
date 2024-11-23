import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class Player extends JFrame {
    private JTextField inputField;   // 입력 패널
    private JTextPane chatPane;      // 채팅 메세지 스페이스
    private JButton sendButton;      // 메세지 전송 버튼
    private JButton startGameButton; // "게임 시작" 버튼
    private String userName;         // 유저 닉네임
    private Socket socket;           
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 54321;

    private boolean isHost = false; // 방장 여부

    public Player() {
        setTitle("단어 맞추기 게임");
        setSize(400, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.SOUTH);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        promptForNicknameAndConnect();

        setVisible(true);
    }

    // 닉네임 설정
    private void promptForNicknameAndConnect() {
        userName = JOptionPane.showInputDialog(this, "닉네임을 입력하세요:", "닉네임 입력", JOptionPane.PLAIN_MESSAGE);

        if (userName == null || userName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "닉네임을 입력해야 합니다.", "오류", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else {
            connectToServer();
        }
    }

    // 서버 연결 자동으로 시킴
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            appendToChat("닉네임: " + userName);

            // 닉네임 전송
            ChatMsg initialMsg = new ChatMsg(userName, 16, "", null);
            out.writeObject(initialMsg);

            // 수신 메시지 처리 스레드 시작
            new Thread(() -> receiveMessages()).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 접속 에러: " + e.getMessage(), "연결 에러", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // 메세지 전송
    private void sendMessage() {
        try {
            if (out != null) {
                String messageText = inputField.getText().trim();
                if (!messageText.isEmpty()) {
                    ChatMsg chatMsg = new ChatMsg(userName, 16, messageText, null);
                    out.writeObject(chatMsg);

                    inputField.setText("");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // 이미지 서버로 전송
    private void sendImage() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("이미지 파일", "jpg", "png", "gif"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                ImageIcon imageIcon = new ImageIcon(selectedFile.getAbsolutePath());
                ChatMsg imageMsg = new ChatMsg(userName, 22, null, imageIcon); // 모드 22: 이미지 메시지
                out.writeObject(imageMsg);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "이미지 전송 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 파일 서버로 전송
    private void sendFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();

                // 파일 크기 제한
                if (selectedFile.length() > 10 * 1024 * 1024) {
                    JOptionPane.showMessageDialog(this, "파일 크기가 너무 큽니다. 10MB 이하의 파일만 전송 가능합니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 파일 데이터를 읽어서 바이트 배열로 변환
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());

                ChatMsg fileMsg = new ChatMsg(userName, 23, selectedFile.getName(), null);
                fileMsg.setFileData(fileData);
                out.writeObject(fileMsg);
                out.flush();

                appendToChat("SERVER: '" + selectedFile.getName() + "' 파일이 전송되었습니다.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "파일 전송 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 게임 시작 요청
    private void sendStartGameRequest() {
        try {
            if (out != null && isHost) { // 방장만 요청 가능
                ChatMsg startGameRequest = new ChatMsg(userName, 18, "", null); // 모드 18은 게임 시작 요청
                out.writeObject(startGameRequest);

                appendToChat("SERVER: 게임이 곧 시작됩니다!");

                // 3초 후 채팅창 클리어
                clearChatPaneAfterDelay(3000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                ChatMsg chatMsg = (ChatMsg) in.readObject();

                if (chatMsg.getMode() == 16) { // 일반 채팅
                    appendToChat(chatMsg.toString()); // 채팅창에 메시지 추가

                    // 새로운 단어 배정 알림
                    if (chatMsg.getMessage().contains("새로운 단어가 할당되었습니다!")) {
                        JOptionPane.showMessageDialog(this, "새로운 제시어를 확인하세요!", "알림", JOptionPane.INFORMATION_MESSAGE);
                    } 
                    // 승리 횟수 알림
                    else if (chatMsg.getMessage().contains("현재 승리 횟수")) {
                        JOptionPane.showMessageDialog(this, chatMsg.getMessage(), "승리 알림", JOptionPane.INFORMATION_MESSAGE);
                    }

                } else if (chatMsg.getMode() == 22) { // 이미지 메시지
                    appendToChat(chatMsg.getUserID() + "님이 이미지를 보냈습니다.");
                    appendImageToChat(chatMsg.getImage()); // 채팅창에 이미지 추가

                } 
                else if (chatMsg.getMode() == 23) { // 파일 메시지
                    saveReceivedFile(chatMsg);
                    appendToChat(chatMsg.getUserID() + "님이 '" + chatMsg.getMessage() + "' 파일을 보냈습니다.");
                }
                else if (chatMsg.getMode() == 17) { // 방장 확인 메세지
                    isHost = true;
                    enableStartGameButton();
                    appendToChat("SERVER: 당신은 방장입니다. 게임 시작 버튼이 활성화되었습니다.");

                } else if (chatMsg.getMode() == 18) { // 게임 시작 알림
                    appendToChat(chatMsg.getMessage());
                    
                    clearChatPaneAfterDelay(3000); // 3초 후 채팅창 초기화

                } else if (chatMsg.getMode() == 19) { // 유저 목록 업데이트
                    appendToChat(chatMsg.getMessage());

                } else if (chatMsg.getMode() == 20) { // 게임 종료 메시지
                    appendToChat(chatMsg.getMessage());
                    JOptionPane.showMessageDialog(this, "게임이 종료되었습니다!", "알림", JOptionPane.INFORMATION_MESSAGE);
                    break; 
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    // 받은 파일 저장
    private void saveReceivedFile(ChatMsg chatMsg) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(chatMsg.getMessage())); // 기본 저장 이름 설정

            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();

                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    fos.write(chatMsg.getFileData()); // 파일 데이터 저장
                }

                JOptionPane.showMessageDialog(this, "파일이 성공적으로 저장되었습니다: " + saveFile.getAbsolutePath(), "알림", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "파일 저장 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 이미지 파일 출력할때 크기 조정
    private ImageIcon resizeImage(ImageIcon imageIcon, int width, int height) {
        Image image = imageIcon.getImage();
        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }
    
    // 텍스트 메세지 채팅창에 출력
    private void appendToChat(String message) {
        try {
            StyledDocument doc = chatPane.getStyledDocument();
            doc.insertString(doc.getLength(), message + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    // 이미지 채팅창에 출력
    private void appendImageToChat(ImageIcon image) {
        try {
            StyledDocument doc = chatPane.getStyledDocument();
            if (image != null) {
                ImageIcon resizedImage = resizeImage(image, 200, 200);

                chatPane.setCaretPosition(doc.getLength()); // 커서를 이미지 끝으로 이동
                chatPane.insertIcon(resizedImage); 
                doc.insertString(doc.getLength(), "\n", null); 
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    // 입력 창 
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        inputField = new JTextField();
        sendButton = new JButton("보내기");                   // 전송 버튼
        startGameButton = new JButton("게임 시작");            // 게임 시작 버튼  
        JButton sendImageButton = new JButton("이미지 보내기"); // 이미지 보내기 버튼
        JButton sendFileButton = new JButton("파일 보내기");   // 파일 보내기 버튼

        sendButton.addActionListener(e -> sendMessage());
        startGameButton.addActionListener(e -> sendStartGameRequest());
        sendImageButton.addActionListener(e -> sendImage()); 
        sendFileButton.addActionListener(e -> sendFile()); 

        startGameButton.setEnabled(false); // 기본적으로는 비활성화

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        panel.add(startGameButton, BorderLayout.WEST);
        panel.add(sendImageButton, BorderLayout.NORTH); 
        panel.add(sendFileButton, BorderLayout.SOUTH);  

        return panel;
    }

    private void enableStartGameButton() {
        SwingUtilities.invokeLater(() -> startGameButton.setEnabled(true)); // 버튼 활성화
    }

    // 채팅창 내용 초기화
    private void clearChatPaneAfterDelay(int delayMillis) {
        SwingUtilities.invokeLater(() -> {
            Timer timer = new Timer(delayMillis, e -> chatPane.setText(""));
            timer.setRepeats(false); 
            timer.start();
        });
    }

    public static void main(String[] args) {
        new Player();
    }
}