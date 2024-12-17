import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;

public class Player extends JFrame {
    private JTextField inputField;   // 입력 패널
    private JTextPane chatPane;      // 채팅 메세지 스페이스
    private JButton sendButton;      // 메세지 전송 버튼
    private JButton startGameButton; // "게임 시작" 버튼
    private String userName;         // 유저 닉네임
    private JTextArea playerInfoArea; // 플레이어 정보 출력 영역

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
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 배경 이미지 설정
        // JAR 내부 리소스로 배경 이미지 로드
        URL bgURL = getClass().getResource("/assets/image/game_background.jpg");
        ImageIcon backgroundImage = new ImageIcon(bgURL);
        JLabel backgroundLabel = new JLabel(backgroundImage);
        backgroundLabel.setLayout(new BorderLayout());
        setContentPane(backgroundLabel);

        // 투명도 있는 패널을 만들기 위한 유틸 메서드
        JPanel transparentPanel = createTransparentPanel();

        // 상단 타이틀 패널
        JPanel headerPanel = createTransparentPanel();
        JLabel titleLabel = new JLabel("✨ 단어 맞추기 게임 ✨");
        titleLabel.setFont(new Font("Cafe24Oneprettynight", Font.BOLD, 48));
        titleLabel.setForeground(new Color(255, 255, 255));
        headerPanel.add(titleLabel);
        backgroundLabel.add(headerPanel, BorderLayout.NORTH);

        // 중앙 영역: 채팅창 + 플레이어 정보
        JPanel centerPanel = createTransparentPanel();
        centerPanel.setLayout(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 채팅 패널(반투명)
        JPanel chatPanel = createRoundedPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(Color.pink, 2), "채팅", 0, 0,
                new Font("Cafe24Oneprettynight", Font.PLAIN, 18), Color.BLACK));
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(255,255,255,180));
        JScrollPane chatScroll = new JScrollPane(chatPane);
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        // 플레이어 정보 패널(반투명)
        JPanel infoPanel = createRoundedPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(Color.PINK, 2), "플레이어 상태", 0, 0,
                new Font("Cafe24Oneprettynight", Font.PLAIN, 18), Color.BLACK));
        playerInfoArea = new JTextArea();
        playerInfoArea.setEditable(false);
        playerInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        playerInfoArea.setForeground(Color.BLACK);
        playerInfoArea.setBackground(new Color(255,255,255,180));
        JScrollPane infoScroll = new JScrollPane(playerInfoArea);
        infoScroll.setOpaque(false);
        infoScroll.getViewport().setOpaque(false);
        infoPanel.add(infoScroll, BorderLayout.CENTER);
        infoPanel.setPreferredSize(new Dimension(310, 0));
        infoPanel.revalidate();
        infoPanel.repaint();

        centerPanel.add(chatPanel, BorderLayout.CENTER);
        centerPanel.add(infoPanel, BorderLayout.EAST);

        backgroundLabel.add(centerPanel, BorderLayout.CENTER);

        // 하단 입력 패널
        JPanel inputPanel = createInputPanel();
        backgroundLabel.add(inputPanel, BorderLayout.SOUTH);

        if (isSpectator) {
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            startGameButton.setEnabled(false);
        }

        // 닉네임 입력 및 서버 연결
        promptForNicknameAndConnect();
        setVisible(true);
    }

    private JPanel createTransparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    // 둥근 모서리 반투명 패널 생성
    private JPanel createRoundedPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 약간 투명한 흰색
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2d.dispose();
            }
        };
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

    // 서버 연결
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

                // 파일 크기 제한 (10MB)
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
                    appendToChat(chatMsg.toString());

                    if (chatMsg.getMessage().contains("새로운 단어가 할당되었습니다!")) {
                        JOptionPane.showMessageDialog(this, "새로운 제시어를 확인하세요!", "알림", JOptionPane.INFORMATION_MESSAGE);
                    } else if (chatMsg.getMessage().contains("승리 횟수")) {
                        SwingUtilities.invokeLater(() -> showVictoryCountDialog(chatMsg.getMessage()));
                    }
                } else if (chatMsg.getMode() == 22) { // 이미지 메시지
                    appendToChat(chatMsg.getUserID() + "님이 이미지를 보냈습니다.");
                    appendImageToChat(chatMsg.getImage());

                } else if (chatMsg.getMode() == 23) { // 파일 메시지
                    saveReceivedFile(chatMsg);
                    appendToChat(chatMsg.getUserID() + "님이 '" + chatMsg.getMessage() + "' 파일을 보냈습니다.");

                } else if (chatMsg.getMode() == 17) {
                    isHost = true;
                    enableStartGameButton();
                    appendToChat("SERVER: 당신은 방장입니다. 게임 시작 버튼이 활성화되었습니다.");

                } else if (chatMsg.getMode() == 18) {
                    appendToChat(chatMsg.getMessage());
                    clearChatPaneAfterDelay(3000);
                    showOverlayGIF();

                } else if (chatMsg.getMode() == 19) {
                    appendToChat(chatMsg.getMessage());

                } else if (chatMsg.getMode() == 20) {
                    appendToChat(chatMsg.getMessage());
// 승리 유저 이름 추출 및 축하 메시지 표시
                    String msg = chatMsg.getMessage();
                    String winnerName = "우승자"; // 기본값
                    if (msg.contains("님이 최종 우승!")) {
                        winnerName = msg.split("님")[0];
                    }
                    showWinnerDialog(winnerName);

                    break;

                } else if (chatMsg.getMode() == 24) {
                    updatePlayerInfo(chatMsg.getMessage());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void showVictoryCountDialog(String message) {
        // JDialog 생성
        JDialog dialog = new JDialog(this, "승리 알림", true);
        dialog.setSize(500, 300);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(255, 223, 186)); // 밝은 오렌지색 배경
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 상단 제목 라벨
        JLabel titleLabel = new JLabel("🏆 축하해요! 🏆", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Cafe24Oneprettynight", Font.BOLD, 24));
        titleLabel.setForeground(new Color(255, 69, 0)); // 진한 오렌지색
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        dialog.add(titleLabel, BorderLayout.NORTH);

        // 중앙 패널: 메시지와 아이콘
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        centerPanel.setOpaque(false);

        // 축하 아이콘
        JLabel iconLabel = new JLabel();
        URL iconURL = getClass().getResource("/assets/icons/spectator_icon.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            // 아이콘 크기 조정
            Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(img));
        }

        // 메시지 라벨
        JLabel messageLabel = new JLabel("<html><center>" + message + "</center></html>");
        messageLabel.setFont(new Font("Cafe24Oneprettynight", Font.PLAIN, 18));
        messageLabel.setForeground(new Color(255, 140, 0)); // 주황색
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        centerPanel.add(iconLabel);
        centerPanel.add(messageLabel);

        dialog.add(centerPanel, BorderLayout.CENTER);

        // 하단 "확인" 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        buttonPanel.setOpaque(false);

        JButton okButton = createStyledButton("확인");
        okButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }


    // 현황패널 업데이트
    private void updatePlayerInfo(String data) {
        if (!data.startsWith("USER_DATA")) return;
        String[] parts = data.split(";");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-10s %-10s %s\n", "닉네임", "할당단어", "승리횟수"));
        sb.append("-------------------------------------\n");
        for (int i = 1; i < parts.length; i++) {
            String[] playerData = parts[i].split("\\|");
            if (playerData.length == 3) {
                String name = playerData[0];
                String word = playerData[1];
                String wins = playerData[2];

                // 본인의 단어는 "가려짐"
                if (name.equals(userName)) {
                    word = "가려짐";
                }
                sb.append(String.format("%-10s %-10s %s\n", name, word, wins));
            }
        }
        playerInfoArea.setText(sb.toString());
    }

    // 파일 저장
    private void saveReceivedFile(ChatMsg chatMsg) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(chatMsg.getMessage()));

            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();
                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    fos.write(chatMsg.getFileData());
                }
                JOptionPane.showMessageDialog(this, "파일이 성공적으로 저장되었습니다: " + saveFile.getAbsolutePath(), "알림", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "파일 저장 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ImageIcon resizeImage(ImageIcon imageIcon, int width, int height) {
        Image image = imageIcon.getImage();
        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }

    private void appendToChat(String message) {
        try {
            StyledDocument doc = chatPane.getStyledDocument();
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setFontSize(style, 16);
            StyleConstants.setForeground(style, Color.BLACK);
            doc.insertString(doc.getLength(), message + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendImageToChat(ImageIcon image) {
        try {
            StyledDocument doc = chatPane.getStyledDocument();
            if (image != null) {
                ImageIcon resizedImage = resizeImage(image, 200, 200);
                chatPane.setCaretPosition(doc.getLength());
                chatPane.insertIcon(resizedImage);
                doc.insertString(doc.getLength(), "\n", null);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // 입력창 패널
    private JPanel createInputPanel() {
        JPanel panel = createTransparentPanel();
        panel.setLayout(new BorderLayout(5, 5));

        inputField = new JTextField();
        inputField.setFont(new Font("Cafe24Oneprettynight", Font.PLAIN, 16));
        inputField.setOpaque(true);
        inputField.setBackground(new Color(255, 255, 255, 230));
        inputField.setBorder(BorderFactory.createLineBorder(Color.PINK, 2));
        panel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = createTransparentPanel();
        buttonPanel.setLayout(new GridLayout(1, 4, 5, 5));

        sendButton = createStyledButton("💬 보내기");
        sendButton.addActionListener(e -> {
            sendMessage();
            playSound("/assets/sound/button_click.wav");
        });

        startGameButton = createStyledButton("🚀 게임 시작");
        startGameButton.setEnabled(false);
        startGameButton.addActionListener(e -> {playSound("/assets/sound/button_click.wav");
        sendStartGameRequest();});

        JButton sendImageButton = createStyledButton("🎨 이미지");
        sendImageButton.addActionListener(e -> {
            playSound("/assets/sound/button_click.wav");
            sendImage();
        });

        JButton sendFileButton = createStyledButton("📁 파일");
        sendFileButton.addActionListener(e -> sendFile());

        panel.add(sendButton, BorderLayout.EAST);
        buttonPanel.add(startGameButton);
        buttonPanel.add(sendImageButton);
        buttonPanel.add(sendFileButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Cafe24Oneprettynight", Font.BOLD, 18));
        button.setBackground(new Color(255, 228, 225));
        button.setForeground(Color.DARK_GRAY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.PINK, 2));
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Color.PINK);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(255, 228, 225));
            }
        });
        return button;
    }

    private void playSound(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedInputStream bis = new BufferedInputStream(is);
             AudioInputStream audioStream = AudioSystem.getAudioInputStream(bis)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 5. 시작하기버튼 누르면 화면 전체를 GIF로 덮는 기능
    private void enableStartGameButton() {
        SwingUtilities.invokeLater(() -> {
            startGameButton.setEnabled(true);
            // 시작하기 버튼 액션 리스너 변경
            for (ActionListener al : startGameButton.getActionListeners()) {
                startGameButton.removeActionListener(al);
            }
            startGameButton.addActionListener(e -> {
                sendStartGameRequest();
            });
        });
    }

    // GIF 오버레이 메서드
    private void showOverlayGIF() {
        JWindow overlay = new JWindow();
        overlay.setSize(getSize());
        overlay.setLocationRelativeTo(this);

        // GIF 이미지 로드
        URL gifURL = getClass().getResource("/assets/image/start_animation.gif");
        ImageIcon gifIcon = new ImageIcon(gifURL);
        JLabel gifLabel = new JLabel(gifIcon);
        overlay.getContentPane().add(gifLabel);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 최상위에 표시 (반투명 창 가능)
        overlay.setVisible(true);

        // 5초 후 오버레이 제거
        new Timer(4500, e -> overlay.dispose()).start();
    }

    private void showWinnerDialog(String winnerName) {
        // JDialog 생성
        JDialog dialog = new JDialog(this, "축하합니다!", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true); // 타이틀 바 제거

        // JLayeredPane을 사용하여 배경과 컴포넌트를 겹치게 함
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(500, 400));

        // 배경 GIF 로드
        URL gifURL = getClass().getResource("/assets/image/space1.gif");

        ImageIcon backgroundGif = new ImageIcon(gifURL);
        JLabel backgroundLabel = new JLabel(backgroundGif);
        backgroundLabel.setBounds(0, 0, 500, 400);
        layeredPane.add(backgroundLabel, new Integer(0));

        // 축하 메시지 라벨
        JLabel messageLabel = new JLabel("와! " + winnerName + " 님이" +System.lineSeparator()+ "이겼어요!");
        messageLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 24));
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setBounds(50, 150, 400, 50);
        layeredPane.add(messageLabel, new Integer(1));

        // "게임 나가기" 버튼
        JButton exitButton = new JButton("게임 나가기");
        exitButton.setFont(new Font("Comic Sans MS", Font.PLAIN, 14));
        exitButton.setBounds(200, 250, 100, 40);
        exitButton.addActionListener(e -> {
            dialog.dispose();
            System.exit(0); // 애플리케이션 종료
        });
        layeredPane.add(exitButton, new Integer(1));

        // 레이어드 페인 추가
        dialog.setContentPane(layeredPane);
        dialog.setVisible(true);
    }


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
