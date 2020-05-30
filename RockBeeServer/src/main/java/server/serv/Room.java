package server.serv;



import com.google.gson.Gson;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

public class Room {
    public static ArrayList<Room> rooms = new ArrayList<>();
    private String password;
    private User creator;
    private ArrayList<User> connectedUser = new ArrayList<>();
    private long startedTime;
    private int userPassed = 0;
    private ArrayList<WebSocketSession> users = new ArrayList<>(), checkingfortime = new ArrayList<>();
    private HashMap<WebSocketSession, String> usersService = new HashMap<>();
    private ArrayList<Audio> songs = new ArrayList<>();
    private Gson gson = new Gson();
    private Audio nowPlaying;
    private ArrayList<Audio> playlist = new ArrayList<>();

    public Room(User user, String pass) {
        creator = user;
        password = pass;
        rooms.add(this);
        connectedUser.add(user);
    }
    public void disconnect(User user, WebSocketSession session){
        connectedUser.remove(user);
        users.remove(session);
        System.out.println(this + "\nStatus: User disconnected\n============================");
    }
    public void connect(User user, WebSocketSession session){
        connectedUser.add(user);
        users.add(session);
        System.out.println(this + "\nStatus: User connected\n============================");
    }
    public void songEnded(){
        System.out.println(this + "\nStatus: Song ended\n============================");
        nowPlaying = null;
        userPassed = 0;
        playlist.remove(0);
        MessageToWebSocket message1 = new MessageToWebSocket();
        message1.setCommand("songended");
        for(WebSocketSession s: users){
            try {
                message1.setData("start");
                s.sendMessage(new TextMessage(gson.toJson(message1)));
                for(Audio audio: playlist) {
                    message1.setData(audio.getName());
                    s.sendMessage(new TextMessage(gson.toJson(message1)));
                }
                message1.setData("end");
                s.sendMessage(new TextMessage(gson.toJson(message1)));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException  | ConcurrentModificationException e){
                users.remove(s);
            }
        }
    }
    @Override
    public String toString() {return "Room: \nCreator:\n" + creator + "\nPassword: \"" + password + "\"";}
    public ArrayList<User> getConnectedUser() {return connectedUser;}
    public String getPassword() {return password;}
    public void setPassword(String p){
        password = p;
        System.out.println(this + "\nStatus: Password changed\n============================");
    }
    public void addPassed(){
        if(nowPlaying != null) {
            userPassed++;
            if (((double) userPassed / connectedUser.size()) >= 0.5) {
                songEnded();
                for (WebSocketSession s : usersService.keySet()) {
                        try {
                            MessageToWebSocket message = new MessageToWebSocket();
                            message.setCommand("songendedfromusers");
                            message.setData("");
                            s.sendMessage(new TextMessage(gson.toJson(message)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (IllegalStateException | ConcurrentModificationException e) {
                            //users.remove(creatorSession);
                        }
                        break;

                }

            }
        }
    }
    public User getCreator(){return creator;}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Room r = (Room) obj;
        return r.getCreator().getUUID().equals(creator.getUUID());
    }

    public ArrayList<WebSocketSession> getUsers() {
        return users;
    }
    public void addSession(WebSocketSession s){users.add(s);}
    public int getPassed(){return userPassed;}
    public void deleteSession(WebSocketSession s){users.remove(s);}
    public void addService(WebSocketSession s, String UUID){
        usersService.put(s, UUID);
        System.out.println(this + "\nStatus: Service connected\n============================");
    }
    public void deleteService(WebSocketSession s){
        usersService.remove(s);
        System.out.println(this + "\nStatus: Service disconnected\n============================");
    }
    public void addSong(AudioData data){
        String name = data.getName();
        int len = Integer.parseInt(data.getLenAudio()), part = Integer.parseInt(data.getPart());
        Audio audio = new Audio();
        audio.setName(name);
        audio.setLen(len);
        audio.setBy(data.getBy());
        if(part != 0){
            audio = songs.get(songs.indexOf(audio));
            audio.addData(data.getAudio());
            songs.add(songs.indexOf(audio), audio);
        } else {
            audio.addData(data.getAudio());
            songs.add(audio);
        }
        if (part + 1 == len){
            System.out.println(this + "\nStatus: Got audio\n============================");
            for(WebSocketSession s: usersService.keySet()){
                if(!usersService.get(s).equals(data.getBy()))
                {
                    MessageToWebSocket message = new MessageToWebSocket();
                    message.setCommand("check");
                    AudioData toSend = new AudioData();
                    toSend.setBy("");
                    toSend.setAudio("");
                    toSend.setPart("");
                    toSend.setLenAudio(data.getLenAudio());
                    toSend.setName(data.getName());
                    message.setData(gson.toJson(toSend));
                    try {
                        s.sendMessage(new TextMessage(gson.toJson(message)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    class SendAudio extends Thread{
        WebSocketSession s;
        Audio audio;
        public SendAudio(WebSocketSession s, Audio a) {
            this.s = s;
            audio = a;
        }

        @Override
        public void run() {
            MessageToWebSocket message = new MessageToWebSocket();
            message.setCommand("audio");
            for(Audio a: songs){
                if(a.getName().equals(audio.getName()) && a.getLen() == audio.getLen()){
                    audio = a;
                    break;
                }
            }
            AudioData audioData = new AudioData();
            audioData.setLenAudio(Integer.toString(audio.getLen()));
            audioData.setName(audio.getName());
            audioData.setBy(audio.getBy());
            for(String data: audio.getAudioData()){
                audioData.setPart(Integer.toString(audio.getAudioData().indexOf(data)));
                audioData.setAudio(data);
                message.setData(gson.toJson(audioData));
                try {
                    s.sendMessage(new TextMessage(gson.toJson(message)));
                    System.out.println("send audio: " + audio.getAudioData().indexOf(data) + "/" + audio.getLen());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void sendAudio(WebSocketSession s, Audio a){
        new SendAudio(s, a).start();
    }

    public ArrayList<Audio> getSongs() {
        return songs;
    }

    public HashMap<WebSocketSession, String> getUsersService() {
        return usersService;
    }
    public void startPlaying(Audio a, Long l){
        System.out.println(this + "\nStatus: Now playing: " + a.getName() + "\n============================");
        nowPlaying = a;
        startedTime = l;
        userPassed = 0;
        MessageToWebSocket message = new MessageToWebSocket();
        message.setCommand("startedplaying");
        AudioData data = new AudioData();
        data.setName(a.getName());
        data.setLenAudio(Integer.toString(a.getLen()));
        data.setPart(Long.toString(l));
        message.setData(gson.toJson(data));
        for(WebSocketSession s: usersService.keySet()){
            if(!usersService.get(s).equals(creator.getUUID())){
                try {
                    s.sendMessage(new TextMessage(gson.toJson(message)));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalStateException | ConcurrentModificationException e){
                    usersService.remove(s);
                }
            }
        }
        message.setData(a.getName());
        for(WebSocketSession s: users){
            try {
                s.sendMessage(new TextMessage(gson.toJson(message)));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException | ConcurrentModificationException e){
                usersService.remove(s);
            }
        }
    }

    public ArrayList<Audio> getPlaylist() {
        return playlist;
    }
    public void addCheckingTime(WebSocketSession s){
        checkingfortime.add(s);
    }

    public ArrayList<WebSocketSession> getCheckingfortime() {
        return checkingfortime;
    }
}
