package server.serv;

public class ConnectToTheRoomData {
    public String UUIDNewUser, nameNewUser, password;
    public String getUUIDNewUser() { return UUIDNewUser; }
    public void setUUIDNewUser(String UUIDNewUser) { this.UUIDNewUser = UUIDNewUser; }
    public String getNameNewUser() {
        return nameNewUser;
    }
    public void setNameNewUser(String nameNewUser) {
        this.nameNewUser = nameNewUser;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
