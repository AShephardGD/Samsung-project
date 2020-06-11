package server.serv;



import com.google.gson.Gson;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Objects;

public class Room {
    public static ArrayList<Room> rooms = new ArrayList<>();
    private String password;
    private User creator;
    private ArrayList<User> connectedUser = new ArrayList<>();
    private int userPassed = 0;
    private ArrayList<WebSocketSession> users = new ArrayList<>(), checkingfortime = new ArrayList<>();
    private HashMap<WebSocketSession, String> usersService = new HashMap<>();
    private ArrayList<Audio> songs = new ArrayList<>();
    private Gson gson = new Gson();
    private String nowPlaying = null;
    private ArrayList<File> mySongs = new ArrayList<>();
    private HashMap<WebSocketSession, Boolean> canDownload = new HashMap<>();
    private HashMap<WebSocketSession, ArrayList<File>> listToDownload = new HashMap<>();
    private File catalog;
    private ArrayList<String> playlist = new ArrayList<>();

    public Room(User user, String pass) {
        creator = user;
        password = pass;
        connectedUser.add(user);
        catalog = new File("C:\\Users\\AdrianShephard\\Desktop\\RockBeeServer\\" + user.getUUID());
        catalog.mkdir();
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
        playlist = new ArrayList<>();
        userPassed = 0;
        MessageToWebSocket message = new MessageToWebSocket();
        message.setCommand("allmusicended");
        message.setData("");
        for(WebSocketSession session: usersService.keySet()){
            if(!usersService.get(session).equals(creator.getUUID())){
                try{
                    session.sendMessage(new TextMessage(gson.toJson(message)));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e){}
            }
        }
        for(WebSocketSession session: users){
            try{
                session.sendMessage(new TextMessage(gson.toJson(message)));
            } catch (IOException e) {
                e.printStackTrace();
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
                for (WebSocketSession s : usersService.keySet()) {
                        if(usersService.get(s).equals(creator.getUUID())) {
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
    public void deleteSession(WebSocketSession s){
        users.remove(s);
    }
    public void addService(WebSocketSession s, String UUID){
        usersService.put(s, UUID);
        canDownload.put(s, true);
        listToDownload.put(s, new ArrayList<>());
        System.out.println(this + "\nStatus: Service connected\n============================");
    }
    public void deleteService(WebSocketSession s){
        usersService.remove(s);
        canDownload.remove(s);
        listToDownload.remove(s);
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
            StringBuilder input = new StringBuilder();
            for (String s: audio.getAudioData()) input.append(s);
            songs.remove(audio);
            byte[] bytes = input.toString().getBytes(StandardCharsets.ISO_8859_1);
            File song = new File(catalog.getAbsolutePath() + "\\" + audio.getName());
            try {
                FileOutputStream fos = new FileOutputStream(song, false);
                fos.write(bytes);
                fos.close();
            } catch (IOException e) {
                return;
            }
            mySongs.add(song);
            System.out.println(this + "\nStatus: Got audio\n============================");
            AudioData data1 = new AudioData();
            data1.setName(song.getName());
            data1.setLenAudio(String.valueOf(song.length()));
            MessageToWebSocket message = new MessageToWebSocket();
            message.setCommand("check");
            message.setData(gson.toJson(data1));
            for(WebSocketSession s: usersService.keySet()){
                if(!usersService.get(s).equals(data.getBy())){
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
        File song;
        public SendAudio(WebSocketSession session, File f) {
            this.s = session;
            song = f;
        }

        @Override
        public void run() {
            System.out.println("Start sending");
            canDownload.put(s, false);
            ArrayList<File> temp = listToDownload.get(s);
            temp.remove(song);
            listToDownload.put(s, temp);
            MessageToWebSocket message = new MessageToWebSocket();
            message.setCommand("audio");
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(song.toPath());
            } catch (IOException e) {
                return;
            }
            String toSend = new String(bytes, StandardCharsets.ISO_8859_1);
            int len = toSend.length()/256;
            if(toSend.length() % 256 != 0) len++;
            AudioData data = new AudioData();
            data.setName(song.getName());
            data.setLenAudio(Integer.toString(len));
            for (int i = 0; i < len; i++) {
                if (((i + 1) * 256) < toSend.length())
                    data.setAudio(toSend.substring(i * 256, (i + 1) * 256));
                else data.setAudio(toSend.substring(i * 256));
                data.setPart(Integer.toString(i));
                message.setData(new Gson().toJson(data));
                try {
                    s.sendMessage(new TextMessage(gson.toJson(message)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("send audio complete");
            temp = listToDownload.get(s);
            if(!temp.isEmpty()) new SendAudio(s, temp.get(0)).start();
            else canDownload.put(s, true);
        }
    }
    public void sendAudio(WebSocketSession session, String s){
        File song = null;
        for (File f: mySongs){
            if(f.getName().equals(s))song = f;
        }
        if(song == null) return;
        ArrayList<File> temp = listToDownload.get(session);
        temp.add(song);
        listToDownload.put(session, temp);
        if(canDownload.get(session)){
            new SendAudio(session, song).start();
        }
    }


    public HashMap<WebSocketSession, String> getUsersService() {
        return usersService;
    }
    public void startPlaying(String s){
        System.out.println(this + "\nStatus: Now playing: " + s + "\n============================");
        String wasPlaying = nowPlaying;
        playlist.remove(wasPlaying);
        nowPlaying = s;
        userPassed = 0;
        MessageToWebSocket message = new MessageToWebSocket();
        message.setCommand("startedplaying");
        message.setData(s);
        for(WebSocketSession session: users){
            try {
                session.sendMessage(new TextMessage(gson.toJson(message)));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException | ConcurrentModificationException e){
                //usersService.remove(session);
            }
        }
        for(WebSocketSession session: usersService.keySet()){
            if(!usersService.get(session).equals(creator.getUUID())){
                try {
                    session.sendMessage(new TextMessage(gson.toJson(message)));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalStateException | ConcurrentModificationException e){
                    //usersService.remove(session);
                }
            }
        }
        message.setCommand("deletefromtheplaylist");
        message.setData(wasPlaying);
        for(WebSocketSession session: users){
            try {
                session.sendMessage(new TextMessage(gson.toJson(message)));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException | ConcurrentModificationException e){
                //usersService.remove(s);
            }
        }
    }

    public ArrayList<String> getPlaylist() {
        return playlist;
    }
    public void addCheckingTime(WebSocketSession s){
        checkingfortime.add(s);
    }

    public ArrayList<WebSocketSession> getCheckingfortime() {
        return checkingfortime;
    }
    public void deleteCatalog(){
        for(File f: Objects.requireNonNull(catalog.listFiles()))f.delete();
        catalog.delete();
    }
    public void addToThePlaylist(String s){
        System.out.println(this + "\nStatus: Song added to the playlist: " + s + "\n============================");
        MessageToWebSocket message = new MessageToWebSocket();
        message.setCommand("addtotheplaylist");
        message.setData(s);
        playlist.add(s);
        for(WebSocketSession session: users){
            try{
                session.sendMessage(new TextMessage(gson.toJson(message)));
            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<File> getMySongs() {
        return mySongs;
    }
}
