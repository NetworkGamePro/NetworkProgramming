import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

public class GameStart extends JFrame {

    private Clip backgroundMusic;
    private Clip buttonClickSound;


    public GameStart() {
        setTitle("*단어 맞추기 게임*");
        setSize(465, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // JAR 내부 리소스 로드
        // 리소스는 JAR 내부에 /assets/image/cute_background.gif 로 존재한다고 가정
        URL backgroundURL = getClass().getResource("/assets/image/cute_background.gif");
        JLabel backgroundLabel = new JLabel(new ImageIcon(backgroundURL));
        backgroundLabel.setLayout(new BorderLayout());
        setContentPane(backgroundLabel);

        // 제목과 하얀 네모를 겹치는 패널 생성
        JPanel layeredTitlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRoundRect(50, 40, 350, 80, 30, 30);
            }
        };
        layeredTitlePanel.setOpaque(false);
        layeredTitlePanel.setLayout(new BorderLayout());

        JLabel startLabel = new JLabel("단어 맞추기 게임", SwingConstants.CENTER);
        startLabel.setFont(new Font("ChangwonDangamAsac", Font.BOLD, 44));
        startLabel.setForeground(Color.BLACK);
        layeredTitlePanel.add(startLabel, BorderLayout.CENTER);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setPreferredSize(new Dimension(400, 150));
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(layeredTitlePanel);
        add(titlePanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(Box.createRigidArea(new Dimension(0, 200)));

        JButton startButton = createGameStyledButton("게임 접속하기");
        startButton.addActionListener((ActionEvent e) -> {
            playButtonClickSound();
            showRoleSelectionDialog();
            dispose();
        });

        centerPanel.add(startButton);
        centerPanel.add(Box.createVerticalGlue());
        add(centerPanel, BorderLayout.CENTER);

        playBackgroundMusic();
        setVisible(true);
    }

    private JButton createGameStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setBackground(new Color(45, 45, 45));
        button.setForeground(Color.black);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 5));

        // 마우스 오버 효과
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(60, 60, 60));
                button.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255), 5));
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(45, 45, 45));
                button.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 2));
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }

    // 배경음악 실행
    private void playBackgroundMusic() {
        try {
            // 리소스 스트림 로드 (JAR 내부)
            BufferedInputStream bis = new BufferedInputStream(getClass().getResourceAsStream("/assets/sound/background_music.wav"));
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioInputStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    // 배경음악 중지 메서드
    private void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();  // 음악 중지
            backgroundMusic.close(); // 자원 해제
        }
    }

    // 버튼 클릭 사운드
    private void playButtonClickSound() {
        try {
            BufferedInputStream bis = new BufferedInputStream(getClass().getResourceAsStream("/assets/sound/button_click.wav"));
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);
            buttonClickSound = AudioSystem.getClip();
            buttonClickSound.open(audioInputStream);
            buttonClickSound.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void showRoleSelectionDialog() {
        JDialog dialog = new JDialog(this, "역할 선택", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(255, 240, 245));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JLabel titleLabel = new JLabel("게임 역할을 선택하세요", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Cafe24Oneprettynight", Font.BOLD, 24));
        titleLabel.setForeground(new Color(70, 40, 100));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        dialog.add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        buttonPanel.setOpaque(false);

        JButton spectatorButton = createRoleButton("관전자", "/assets/icons/spectator_icon.png");
        spectatorButton.addActionListener(e -> {
            playButtonClickSound();
            dialog.dispose();
            new Player(true);
            stopBackgroundMusic();
        });

        JButton playerButton = createRoleButton("플레이어", "/assets/icons/player_icon.png");
        playerButton.addActionListener(e -> {
            playButtonClickSound();
            dialog.dispose();
            new Player(false);
            stopBackgroundMusic();
        });

        buttonPanel.add(spectatorButton);
        buttonPanel.add(playerButton);
        dialog.add(buttonPanel, BorderLayout.CENTER);

        JLabel descriptionLabel = new JLabel("<html><center>관전자: 게임을 구경만 합니다.<br>플레이어: 게임에 직접 참여합니다.</center></html>", SwingConstants.CENTER);
        descriptionLabel.setFont(new Font("Cafe24Oneprettynight", Font.PLAIN, 16));
        descriptionLabel.setForeground(new Color(100, 100, 100));
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        dialog.add(descriptionLabel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JButton createRoleButton(String text, String iconPath) {
        JButton button = new JButton(text);
        button.setFont(new Font("Cafe24Oneprettynight", Font.BOLD, 18));
        button.setBackground(new Color(255, 182, 193));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);

        // 아이콘 로드
        URL iconURL = getClass().getResource(iconPath);
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(img));
        }

        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 105, 180), 3),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(255, 105, 180));
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(255, 182, 193));
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return button;
    }

    public static void main(String[] args) {
        new GameStart();
    }
}
