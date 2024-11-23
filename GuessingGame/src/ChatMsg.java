import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChatMsg implements Serializable {
    private String userID;       // 유저 닉네임
    private int mode;            // 메세지 모드
    // 모드 종류(16: 일반 채팅, 17: 방장 확인, 18: 게임 시작 알림, 19: 유저 목록 업데이트, 20: 게임 종료, 22: 이미지 메세지, 23: 파일 메세지)
    private String message;      // 메세지 내용
    private ImageIcon image;     // 이미지
    private String attachedWord; // 배정되는 단어
    private byte[] fileData;     // 파일 데이터

    // 파일 데이터 x
    public ChatMsg(String userID, int mode, String message, ImageIcon image) {
        this(userID, mode, message, image, null, null);
    }

    // 배정된 단어는 있고 파일 데이터 x
    public ChatMsg(String userID, int mode, String message, ImageIcon image, String attachedWord) {
        this(userID, mode, message, image, attachedWord, null);
    }

    // 파일 데이터 O
    public ChatMsg(String userID, int mode, String message, ImageIcon image, String attachedWord, byte[] fileData) {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
        this.image = image;
        this.attachedWord = attachedWord;
        this.fileData = fileData;
    }

    // Getters, setters 
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ImageIcon getImage() {
        return image;
    }

    public void setImage(ImageIcon image) {
        this.image = image;
    }

    public String getAttachedWord() {
        return attachedWord;
    }

    public void setAttachedWord(String attachedWord) {
        this.attachedWord = attachedWord;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    // 메세지 문자열로 반환
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // 서버 메세지 앞에 서버 표시
        sb.append(userID != null ? userID : "SERVER");
        
        // 배정된 단어 추가
        if (attachedWord != null && !attachedWord.isEmpty()) {
            sb.append(" (").append(attachedWord).append(")");
        }
        // 메세지 내용
        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(message);
        }

        return sb.toString();
    }

    // 파일 경로로 파일 내용 바이트 배열로 가져옴	
    public static byte[] readFileToByteArray(String filePath) throws IOException {
        return Files.readAllBytes(Path.of(filePath));
    }
    // 바이트 배열 데이터를 지정된 경로에 파일로 저장
    public static void writeByteArrayToFile(String outputPath, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(data);
        }
    }
}