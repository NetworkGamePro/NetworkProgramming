import java.util.HashMap;

public class GameManager {
    private HashMap<String, Integer> winCounts = new HashMap<>(); // 각 플레이어의 승리 횟수 저장

    // 유저가 단어를 맞췃을때 승 수 올라감
    public void incrementWin(String userID) {
        winCounts.put(userID, winCounts.getOrDefault(userID, 0) + 1);
    }

    public int getWins(String userID) {
        return winCounts.getOrDefault(userID, 0);
    }

    // 모든 유저의 승 수
    public String getWinStatus() {
        StringBuilder sb = new StringBuilder("현재 승리 현황:\n");
        for (String user : winCounts.keySet()) {
            sb.append(user).append(": ").append(winCounts.get(user)).append("번 승리\n");
        }
        return sb.toString();
    }
}

