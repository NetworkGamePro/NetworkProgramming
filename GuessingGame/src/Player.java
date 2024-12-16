import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private boolean isSpectator;

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 54321;

    private boolean isHost = false; // 방장 여부

    public Player(boolean isSpectator) {
        this.isSpectator = isSpectator;
        setTitle("단어 맞추기 게임");


        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        // **헤더 패널**
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("✨ 단어 맞추기 게임 ✨");
        titleLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 36));
        titleLabel.setForeground(Color.BLUE);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);




        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.SOUTH);


        chatPane = new JTextPane();
        chatPane.setEditable(false);
        add(new JScrollPane(chatPane), BorderLayout.CENTER);


        if (isSpectator) {
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            startGameButton.setEnabled(false);

        }
        // 닉네임 입력 및 서버 연결
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
        appendToChat("👋 환영합니다, " + userName + "님!");
    }

    // 서버 연결 자동으로 시킴
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            String initialMessage = isSpectator ? "[관전자] 관전자로 접속했습니다" : "";

            ChatMsg initialMsg = new ChatMsg(userName, 16, initialMessage, null);
            out.writeObject(initialMsg);

            // 메시지 수신 스레드 시작
            new Thread(this::receiveMessages).start();
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
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setFontSize(style, 16);
            StyleConstants.setForeground(style, Color.DARK_GRAY);
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
        JPanel panel = new JPanel(new BorderLayout(5, 5)); // BorderLayout 사용

//        panel.setOpaque(false);

        // 첫 번째 줄: inputField가 전체를 차지하도록 설정
        inputField = new JTextField();
        inputField.setFont(new Font("Comic Sans MS", Font.PLAIN, 16)); // 가독성을 위한 폰트 설정
        panel.add(inputField, BorderLayout.CENTER); // inputField는 북쪽 영역에 배치

        // 두 번째 줄: 버튼들
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 5)); // 버튼을 한 줄로 배치

        // **버튼들**
        sendButton = createStyledButton("💬 보내기");                  // 전송 버튼
        sendButton.addActionListener(e -> {
            sendMessage();
            playSound("assets/sound/button_click.wav");
        });

        startGameButton = createStyledButton("🚀 게임 시작");           // 게임 시작 버튼
        JButton sendImageButton = createStyledButton("🖼️ 이미지"); // 이미지 보내기 버튼
        sendImageButton.addActionListener(e -> {
            playSound("assets/sound/button_click.wav");
        });
        JButton sendFileButton = createStyledButton("📁 파일");   // 파일 보내기 버튼


        sendButton.addActionListener(e -> sendMessage());
        startGameButton.addActionListener(e -> sendStartGameRequest());
        sendImageButton.addActionListener(e -> sendImage());
        sendFileButton.addActionListener(e -> sendFile());

        startGameButton.setEnabled(false); // 기본적으로는 비활성화

//        panel.add(inputField, BorderLayout.CENTER);
//        panel.add(sendButton, BorderLayout.EAST);
//        panel.add(startGameButton, BorderLayout.WEST);
//        panel.add(sendImageButton, BorderLayout.NORTH);
//        panel.add(sendFileButton, BorderLayout.SOUTH);

        // 버튼들을 버튼 패널에 추가
        panel.add(sendButton, BorderLayout.EAST);
        buttonPanel.add(startGameButton);
        buttonPanel.add(sendImageButton);
        buttonPanel.add(sendFileButton);

        // 버튼 패널을 SOUTH에 추가
        panel.add(buttonPanel, BorderLayout.SOUTH);


        return panel;
    }
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);

        // 기본 스타일 설정
        button.setFont(new Font("Comic Sans MS", Font.BOLD, 18));
        button.setBackground(new Color(255, 228, 225)); // 파스텔톤 배경
        button.setForeground(Color.DARK_GRAY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.PINK, 2));
        button.setOpaque(true); // Opaque를 true로 설정
        button.setContentAreaFilled(true); // 버튼 배경 영역 활성화

        // 마우스 이벤트로 색 변경
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Color.PINK); // 마우스 오버 색상
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(255, 228, 225)); // 기본 배경색으로 복원
            }
        });

        return button;
    }
    private void playSound(String filePath) {
        try {
            File soundFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        boolean isSpectatorMode = false;
        new Player(isSpectatorMode);
    }
}