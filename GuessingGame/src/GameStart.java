import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GameStart extends JFrame {
    public GameStart() {
        setTitle("*단어 맞추기 게임*");
        setSize(465, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        // 배경 GIF 설정
        JLabel backgroundLabel = new JLabel(new ImageIcon("assets/image/giphy2.gif"));
        backgroundLabel.setLayout(new BorderLayout()); // 배경 위에 다른 컴포넌트 추가 가능하게 설정
        setContentPane(backgroundLabel);

        // 제목과 하얀 네모를 겹치게 배치할 레이어드 패널
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(400, 150));

        // 하얀 네모
        JPanel whiteRectangle = new JPanel();
        whiteRectangle.setBackground(Color.WHITE);
        whiteRectangle.setBounds(70, 110, 330, 70); // 네모의 위치와 크기 설정

//        // 제목 패널
//        JPanel titlePanel = new JPanel();
//        titlePanel.setOpaque(false); // 배경 투명하게 설정
//        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));


        // 제목
        JLabel startLabel = new JLabel("단어 맞추기 게임", SwingConstants.CENTER);
        startLabel.setFont(new Font("ChangwonDangamAsac", Font.BOLD, 44));
        startLabel.setBounds(70, 100, 330, 60); // 글자 위치 설정
        startLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 레이어드 패널에 추가 (순서 중요: 네모 -> 글자)
        layeredPane.add(whiteRectangle, Integer.valueOf(0)); // 아래 레이어
        layeredPane.add(startLabel, Integer.valueOf(1)); // 위 레이어

        // 제목 패널에 레이어드 패널 추가
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false); // 배경 투명
        titlePanel.setPreferredSize(new Dimension(400, 150));
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(layeredPane, BorderLayout.CENTER);

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


        //버튼 추가
        JButton startButton = new JButton("게임 접속하기");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);


        startButton.addActionListener((ActionEvent e) -> {
            showRoleSelectionDialog(); 
            dispose(); // 현재 창 닫음
        });



        centerPanel.add(startButton);
        centerPanel.add(Box.createVerticalGlue()); // 아래쪽 여백

        add(centerPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private void showRoleSelectionDialog() {
        String[] options = {"관전자", "플레이어"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "역할을 선택하세요:",
            "역할 선택",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == 0) {
        
            new Player(true); 
        } else if (choice == 1) {
       
            new Player(false); 
        }
    }

    public static void main(String[] args) {
        new GameStart();
    }
}
