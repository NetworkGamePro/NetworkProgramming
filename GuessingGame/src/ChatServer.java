import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer extends JFrame {
	private GameManager gameManager = new GameManager();
    private JTextArea displayArea; // 서버 로그 출력 스페이스
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private List<String> usernames = new CopyOnWriteArrayList<>(); // 접속 중인 유저 목록
    private static final int MAX_CLIENTS = 6; // 최대 유저 수
    private WordCategory selectedCategory; // 랜덤으로 선택된 카테고리
    private ClientHandler hostClient; // 방장 저장
    

    public ChatServer() {
        setTitle("Game Server");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createDisplayPanel(), BorderLayout.CENTER);
        setVisible(true);
        new Thread(this::initializeServer).start();
    }

    private void initializeServer() {
        try {
            // 카테고리 초기화 및 랜덤 선택
            List<WordCategory> categories = WordCategory.initializeCategories();
            selectedCategory = getRandomCategory(categories);
            appendToDisplay("선택된 카테고리: " + selectedCategory.getCategoryName());

            serverSocket = new ServerSocket(54321); // 포트 54321에서 대기
            appendToDisplay("서버가 시작되었습니다.");

            while (true) {
                if (clients.size() < MAX_CLIENTS) { // 최대 유저 수 제한 확인
                    Socket clientSocket = serverSocket.accept();
                    appendToDisplay("유저가 접속하였습니다.");

                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler); // 유저 목록에 추가
                    clientHandler.start(); //
                } else {
                    appendToDisplay("인원이 가득 찼습니다. 새로운 연결을 거부합니다.");
                }
            }

        } catch (IOException e) {
            appendToDisplay("서버 시작 에러: " + e.getMessage());
        }
    }

    // 랜덤으로 카테고리를 선택
    private WordCategory getRandomCategory(List<WordCategory> categories) {
        Random random = new Random();
        int randomIndex = random.nextInt(categories.size());
        return categories.get(randomIndex);
    }

    // 접속 유저 목록을 모든 유저에게 전송
    private void broadcastUserList() {
        // Count only non-spectator clients
        int currentPlayerCount = (int) clients.stream().filter(client -> !client.isSpectator).count();
        String fullMessage = "현재 서버 접속 인원: " + currentPlayerCount + "/6\n접속 중인 유저: " + 
                             String.join(", ", usernames.stream().filter(username -> 
                             clients.stream().anyMatch(client -> client.userName.equals(username) && !client.isSpectator)).toList());
        ChatMsg userListUpdate = new ChatMsg("SERVER", 19, fullMessage, null);
        for (ClientHandler client : clients) {
            try {
                client.out.writeObject(userListUpdate);
                client.out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String userName; // 사용자 닉네임 저장
        private String assignedWord; // 사용자에게 할당된 단어
        private boolean isSpectator;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());

                // 첫 메시지로 닉네임 수신
                ChatMsg initialMsg = (ChatMsg) in.readObject();

                if (initialMsg.getMode() == 16) { // 텍스트 메시지 모드
                    userName = initialMsg.getUserID(); // 닉네임 저장	
                    isSpectator = initialMsg.getMessage().contains("[관전자]");

                    if (isSpectator) {
                        userName += " (관전자)";
                    } else {
                        usernames.add(userName);
                        assignedWord = selectedCategory.getRandomWord();
                        if (assignedWord != null) {
                            appendToDisplay(userName + "에게 단어 '" + assignedWord + "'가 할당되었습니다.");
                        } else {
                            out.writeObject(new ChatMsg("SERVER", 16, "모든 단어가 소진되었습니다.", null));
                            out.flush();
                        }
                    }
                    
                    appendToDisplay(userName + "님이 접속했습니다.");
                    broadcastUserList(); 

                    ChatMsg categoryMsg = new ChatMsg("SERVER", 16, "선택된 카테고리: " + selectedCategory.getCategoryName(), null);
                    out.writeObject(categoryMsg);
                    out.flush();
                    
                    // 유저 체크
                    if (!isSpectator) {
                        // 유저 체크
                        String joinMessage = userName + "이 들어왔습니다. 현재 인원수: " + clients.size() + "/6";
                        ChatMsg joinNotification = new ChatMsg("SERVER", 16, joinMessage, null);
                        
                        for (ClientHandler client : clients) {
                            if (!client.isSpectator) { // 관전자가 아닌 경우에만 전송
                                client.out.writeObject(joinNotification);
                                client.out.flush();
                            }
                        }
                    }

                 // 현재 접속 인원 수 전송
                    int currentPlayerCount = getCurrentPlayerCount();
                    String playerCountMessage = "현재 서버 접속 인원: " + currentPlayerCount + "명";
                    out.writeObject(new ChatMsg("SERVER", 16, playerCountMessage, null));
                    out.flush();
                    
                    // 방장 지정 (첫 번째 유저)
                    if (hostClient == null && !isSpectator) {
                        hostClient = this; // 현재 클라이언트를 방장으로 설정
                        out.writeObject(new ChatMsg("SERVER", 17, "당신은 방장입니다. 게임 시작 버튼이 활성화됩니다.", null));
                        out.flush();
                    }
                    
               
                }

                while (!clientSocket.isClosed()) {
                    ChatMsg chatMsg = (ChatMsg) in.readObject();

                    if (chatMsg.getMode() == 16) { // 일반 채팅 메시지
                        if (chatMsg.getMessage().equalsIgnoreCase(assignedWord)) {
                            // 플레이어가 단어를 맞췄을 경우
                            gameManager.incrementWin(userName); // 해당 플레이어의 승리 횟수 증가

                            // 현재 승리 횟수 가져오기
                            int currentWins = gameManager.getWins(userName);
                            
                            String resultMessage = userName + "님이 단어 '" + assignedWord + "'를 맞추고 승리하였습니다! 현재 승리 횟수: " + currentWins;

                            // 플레이어 정보 업데이트 브로드캐스트
                            broadcastPlayerInfo();
                            // 모든 유저에게 결과 알림
                            broadcastMessage(new ChatMsg("SERVER", 16, resultMessage, null), null);

                            // 최종 우승 여부 확인
                            if (currentWins == 3) { // 3승 조건
                                broadcastMessage(new ChatMsg("SERVER", 16, userName + "님이 최종 우승하였습니다!", null), null);
                                endGameForAll(); // 게임 종료 처리
                            
                            }
                            assignNewWordsToAllPlayers();
                            // 새 단어 할당 후에도 다시 정보 브로드캐스트
                            broadcastPlayerInfo();

                        } else {
                            broadcastMessage(chatMsg, this); // 일반 메시지
                        }
                    } else if (chatMsg.getMode() == 18) { // 게임 시작 요청
                        startGame();
                    } else if (chatMsg.getMode() == 22) { // 이미지 메시지 처리
                        broadcastMessage(chatMsg, this); // 이미지를 모든 유저에게 출력
                        appendToDisplay(userName + "님이 이미지를 보냈습니다.");
                    
                    } else if (chatMsg.getMode() == 23) { // 파일 전송 모드
                        appendToDisplay(userName + "님이 파일을 전송했습니다: " + chatMsg.getMessage());

                        // 수신한 파일 데이터를 저장
                        saveReceivedFile(chatMsg);

                        // 모든 클라이언트에게 파일 전송 알림 브로드캐스트
                        broadcastMessage(new ChatMsg("SERVER", 16, userName + "님이 파일 '" + chatMsg.getMessage() + "'을(를) 보냈습니다.", null), this);
                        
                        appendToDisplay("파일 저장 완료: " + chatMsg.getMessage());
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                appendToDisplay(userName + "님 연결 종료: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }
        
        private int getCurrentPlayerCount() {
            return (int) clients.stream().filter(client -> !client.isSpectator).count();
        }

       
        private void saveReceivedFile(ChatMsg chatMsg) {
            try {
                // 기본 저장 경로 설정
                File directory = new File("received_files");
                if (!directory.exists()) {
                    directory.mkdir(); // 디렉토리가 없으면 생성
                }

                // 파일 저장 경로 설정
                File file = new File(directory, chatMsg.getMessage());

                // 파일 데이터 저장
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(chatMsg.getFileData());
                    System.out.println("파일이 성공적으로 저장되었습니다: " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("파일 저장 중 오류 발생: " + e.getMessage());
            }
        }

        // 랜덤 단어 가져오기
        private void assignNewWordsToAllPlayers() {
            for (ClientHandler client : clients) {
                if (!client.isSpectator) {
                    String newWord = selectedCategory.getRandomWord();
                    if (newWord != null) {
                        client.assignedWord = newWord;
                        // Removed broadcasting logic
                        appendToDisplay(client.userName + "에게 단어 '" + newWord + "'가 배정되었습니다.");
                    } else {
                        appendToDisplay("모든 단어가 소진되었습니다. 더 이상 할당할 단어가 없습니다.");
                    }
                }
            }
        }


        
        // 게임 종료 메서드 추가
        private void endGameForAll() {
            ChatMsg endGameMessage = new ChatMsg("SERVER", 20, "게임이 종료되었습니다!", null); // 게임 종료 알림
            for (ClientHandler client : clients) {
                try {
                    client.out.writeObject(endGameMessage);
                    client.out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            appendToDisplay("게임이 종료되었습니다.");
        }
        
        private void broadcastMessage(ChatMsg chatMsg, ClientHandler sender) {
            for (ClientHandler client : clients) {
                try {
                    if (chatMsg.getMode() == 22) { 
                        client.out.writeObject(chatMsg);
                    } else if (sender == null) {
                        client.out.writeObject(new ChatMsg("SERVER", 16, chatMsg.getMessage(), null));
                    } else if (client == sender) {
                        if (sender.isSpectator) {
                            client.out.writeObject(new ChatMsg(sender.userName, 16, chatMsg.getMessage(), null));
                        } else {
                            client.out.writeObject(new ChatMsg(sender.userName, 16, chatMsg.getMessage(), null, "단어 숨김"));
                        }
                    } else {
                      
                        client.out.writeObject(new ChatMsg(sender.userName, 16, chatMsg.getMessage(), null, sender.assignedWord));
                    }
                    client.out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        

        private void startGame() {
            if (this == hostClient) { // 방장만 게임 시작 가능
                // 모든 유저에게 "게임 시작" 메시지 전송
                ChatMsg gameStartMsg = new ChatMsg("SERVER", 18, "게임이 시작되었습니다!", null);
                for (ClientHandler client : clients) {
                    try {
                        client.out.writeObject(gameStartMsg); // 모든 유저에게 메시지 전송
                        client.out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                appendToDisplay("방장이 게임을 시작했습니다."); // 서버 로그에 기록
                broadcastPlayerInfo();
            } else {
                appendToDisplay("게임 시작 권한이 없습니다.");
            }
        }

        // 모든 플레이어 정보(닉네임, 단어, 승리횟수)를 클라이언트에게 전송하는 메서드 추가
        private void broadcastPlayerInfo() {

            System.out.println("클라이언트에게 전보한다 시발");
            StringBuilder sb = new StringBuilder("USER_DATA");
            // USER_DATA 뒤에 각 플레이어 정보를 "닉네임|할당단어|승수" 형태로 이어붙임
            for (ClientHandler client : clients) {
                if (!client.isSpectator) {
                    int wins = gameManager.getWins(client.userName);
                    String word = client.assignedWord != null ? client.assignedWord : "";
                    sb.append(";").append(client.userName).append("|").append(word).append("|").append(wins);

                }
            }

            ChatMsg playerInfoMsg = new ChatMsg("SERVER", 24, sb.toString(), null);
            for (ClientHandler c : clients) {
                try {
                    c.out.writeObject(playerInfoMsg);
                    c.out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        
        private void closeConnection() {
            try {
                if (clientSocket != null) clientSocket.close();
                if (in != null) in.close();
                if (out != null) out.close();
                
                clients.remove(this);
                usernames.remove(userName);
            
                String leaveMessage = userName + "님이 나갔습니다. 현재 인원수: " + clients.size() + "/6";
                ChatMsg leaveNotification = new ChatMsg("SERVER", 16, leaveMessage, null);
                for (ClientHandler client : clients) {
                    try {
                        client.out.writeObject(leaveNotification);
                        client.out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                
                broadcastUserList();
                broadcastPlayerInfo();
                appendToDisplay(userName + "님이 연결을 종료했습니다.");
            } catch (IOException e) {
                appendToDisplay("소켓 종료 에러: " + e.getMessage());
            }
        }
    }

    // 서버 로그 스페이스에 메세지
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatServer::new); 
    }
}