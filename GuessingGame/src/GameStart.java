import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class GameStart extends JFrame {

    private Clip backgroundMusic;
    private Clip buttonClickSound;


    public GameStart() {
        setTitle("*단어 맞추기 게임*");
        setSize(465, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        // 배경 GIF 설정
        JLabel backgroundLabel = new JLabel(new ImageIcon("assets/image/cute_background.gif"));
        backgroundLabel.setLayout(new BorderLayout()); // 배경 위에 다른 컴포넌트 추가 가능하게 설정
        setContentPane(backgroundLabel);



//        // 제목 패널
//        JPanel titlePanel = new JPanel();
//        titlePanel.setOpaque(false); // 배경 투명하게 설정
//        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        // 제목과 하얀 네모를 겹치는 패널 생성
        JPanel layeredTitlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 200)); // 반투명 흰색
                g2d.fillRoundRect(50, 40, 350, 80, 30, 30); // 둥근 모서리 네모
            }
        };
        layeredTitlePanel.setOpaque(false);
        layeredTitlePanel.setLayout(new BorderLayout());


        // 제목 추가
        JLabel startLabel = new JLabel("단어 맞추기 게임", SwingConstants.CENTER);
        startLabel.setFont(new Font("ChangwonDangamAsac", Font.BOLD, 44));
        startLabel.setForeground(Color.BLACK); // 글씨를 검은색으로 설정
        layeredTitlePanel.add(startLabel, BorderLayout.CENTER);


        // 제목 패널에 레이어드 패널 추가
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false); // 배경 투명
        titlePanel.setPreferredSize(new Dimension(400, 150));
        titlePanel.setLayout(new BorderLayout());

        titlePanel.add(layeredTitlePanel);

        add(titlePanel, BorderLayout.NORTH);


        // 제목 위치 조정
//        titlePanel.add(Box.createRigidArea(new Dimension(0, 100)));
//        titlePanel.add(startLabel);
//        add(titlePanel, BorderLayout.NORTH);



        // 중앙 공간 추가
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false); // 배경 투명하게 설정
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(Box.createRigidArea(new Dimension(0, 200)));


        // 커스터마이즈된 버튼 생성
        JButton startButton = createGameStyledButton("게임 접속하기");

        startButton.addActionListener((ActionEvent e) -> {
            playButtonClickSound();
            showRoleSelectionDialog();
            dispose(); // 현재 창 닫음
        });

        centerPanel.add(startButton);
        centerPanel.add(Box.createVerticalGlue()); // 아래쪽 여백

        add(centerPanel, BorderLayout.CENTER);
        playBackgroundMusic();

        setVisible(true);
    }

    private JButton createGameStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 20)); // 굵고 큰 글씨체
        button.setBackground(new Color(45, 45, 45)); // 어두운 배경
        button.setForeground(Color.black); // 텍스트 색상
        button.setFocusPainted(false); // 버튼 포커스 효과 제거
        button.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 5)); // 금색 테두리

        // 마우스 오버 효과 추가
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(60, 60, 60)); // 밝아지는 효과
                button.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 5)); // 테두리 색 변경
                button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // 손가락 커서
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(45, 45, 45)); // 원래 색상으로 복구
                button.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 2)); // 원래 테두리로 복구
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        button.setAlignmentX(Component.CENTER_ALIGNMENT); // 가운데 정렬
        return button;
    }

    //배경음악 실행
    private void playBackgroundMusic() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/sound/background_music.wav"));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioInputStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    //버튼 클릭 사운드
    private void playButtonClickSound() {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("assets/sound/button_click.wav"));
            buttonClickSound = AudioSystem.getClip();
            buttonClickSound.open(audioInputStream);
            buttonClickSound.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void showRoleSelectionDialog() {
        // 커스텀 대화 상자 생성
        JDialog dialog = new JDialog(this, "역할 선택", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(255, 240, 245)); // 연한 핑크 배경
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 타이틀 라벨
        JLabel titleLabel = new JLabel("게임 역할을 선택하세요", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Cafe24Oneprettynight", Font.BOLD, 24));
        titleLabel.setForeground(new Color(70, 40, 100)); // 부드러운 보라색
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        dialog.add(titleLabel, BorderLayout.NORTH);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        buttonPanel.setOpaque(false);

        // 관전자 버튼
        JButton spectatorButton = createRoleButton("관전자", "assets/icons/spectator_icon.png");
        spectatorButton.addActionListener(e -> {
            playButtonClickSound();
            dialog.dispose();
            new Player(true);
        });

        // 플레이어 버튼
        JButton playerButton = createRoleButton("플레이어", "assets/icons/player_icon.png");
        playerButton.addActionListener(e -> {
            playButtonClickSound();
            dialog.dispose();
            new Player(false);
        });

        buttonPanel.add(spectatorButton);
        buttonPanel.add(playerButton);

        dialog.add(buttonPanel, BorderLayout.CENTER);

        // 설명 라벨
        JLabel descriptionLabel = new JLabel("<html><center>관전자: 게임을 구경만 합니다.<br>플레이어: 게임에 직접 참여합니다.</center></html>", SwingConstants.CENTER);
        descriptionLabel.setFont(new Font("Cafe24Oneprettynight", Font.PLAIN, 16));
        descriptionLabel.setForeground(new Color(100, 100, 100));
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        dialog.add(descriptionLabel, BorderLayout.SOUTH);

        // 다이얼로그 중앙 배치
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JButton createRoleButton(String text, String iconPath) {
        JButton button = new JButton(text);
        button.setFont(new Font("Cafe24Oneprettynight", Font.BOLD, 18));
        button.setBackground(new Color(255, 182, 193)); // 파스텔 핑크
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);

        // 아이콘 추가 (존재할 경우)
        try {
            ImageIcon icon = new ImageIcon(iconPath);
            Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            // 아이콘 로드 실패 시 무시
        }

        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 105, 180), 3),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // 호버 효과
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(255, 105, 180)); // 더 선명한 핑크
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(255, 182, 193)); // 원래 색상
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return button;
    }


    public static void main(String[] args) {
        new GameStart();
    }
}
