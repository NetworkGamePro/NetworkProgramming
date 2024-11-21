import javax.swing.*;
import java.io.Serializable;

public class ChatMsg implements Serializable {
    private String userID;
    private int mode;  // 1: login, 2: logout, 16: text message, 32: file, 64: image
    private String message;
    private ImageIcon image;

    public ChatMsg(String userID, int mode, String message, ImageIcon image) {
        this.userID = userID;
        this.mode = mode;
        this.message = message;
        this.image = image;
    }

    // Getters and setters
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
}