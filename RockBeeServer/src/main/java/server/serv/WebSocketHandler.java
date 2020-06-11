package server.serv;

import com.google.gson.Gson;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ConcurrentModificationException;

class WebSocketHandler extends TextWebSocketHandler {
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Gson gson = new Gson();
        MessageFromWebSocket msg = gson.fromJson(message.getPayload(), MessageFromWebSocket.class);
        Room r = findRoom(msg.getUUID());
        if(r == null) {
            MessageToWebSocket error = new MessageToWebSocket();
            error.setCommand("error");
            error.setData("1");
            try {
                session.sendMessage(new TextMessage(gson.toJson(error)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        switch(msg.getCommand()){
            case "connect":
                ConnectToTheRoomData data = gson.fromJson(msg.getData(), ConnectToTheRoomData.class);
                if(!r.getPassword().equals(data.getPassword())){
                    MessageToWebSocket error = new MessageToWebSocket();
                    error.setCommand("error");
                    error.setData("2");
                    try {
                        session.sendMessage(new TextMessage(gson.toJson(error)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    User user = new User(data.getNameNewUser(), data.getUUIDNewUser());
                    MessageToWebSocket newUser = new MessageToWebSocket();
                    newUser.setCommand("connect");
                    ConnectData data1 = new ConnectData();
                    data1.setNameNewUser(user.getName());
                    data1.setUUIDNewUser(user.getUUID());
                    newUser.setData(gson.toJson(data1));
                    for(WebSocketSession s: r.getUsers()){
                        try{
                            s.sendMessage(new TextMessage(gson.toJson(newUser)));
                        } catch(IllegalStateException | ConcurrentModificationException e){
                            //r.getUsers().remove(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    MessageToWebSocket users = new MessageToWebSocket();
                    users.setCommand("userslist");
                    for(User u: r.getConnectedUser()){
                        users.setData(gson.toJson(u));
                        try {
                            session.sendMessage(new TextMessage(gson.toJson(users)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    users.setCommand("songended");
                    try {
                        users.setData("start");
                        session.sendMessage(new TextMessage(gson.toJson(users)));
                        for(String audio: r.getPlaylist()) {
                            users.setData(audio);
                            session.sendMessage(new TextMessage(gson.toJson(users)));
                        }
                        users.setData("end");
                        session.sendMessage(new TextMessage(gson.toJson(users)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    r.connect(user, session);
                    MessageToWebSocket success = new MessageToWebSocket();
                    success.setCommand("successconnection");
                    success.setData("");
                    try {
                        session.sendMessage(new TextMessage(gson.toJson(success)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "disconnect":
                ConnectData data1 = gson.fromJson(msg.getData(), ConnectData.class);
                User user = new User(data1.getNameNewUser(), data1.getUUIDNewUser());
                r.disconnect(user, session);
                MessageToWebSocket disconnect = new MessageToWebSocket();
                disconnect.setCommand("disconnect");
                disconnect.setData(msg.getData());
                for(WebSocketSession s: r.getUsers()){
                    try{
                        s.sendMessage(new TextMessage(gson.toJson(disconnect)));
                    } catch(IllegalStateException | ConcurrentModificationException e){
                        //r.getUsers().remove(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "password":
                r.setPassword(msg.getData());
                MessageToWebSocket password = new MessageToWebSocket();
                password.setCommand("password");
                password.setData(msg.getData());
                for(WebSocketSession s: r.getUsers()){
                    try{
                        s.sendMessage(new TextMessage(gson.toJson(password)));
                    } catch (IllegalStateException  | ConcurrentModificationException e){
                        //r.getUsers().remove(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "close":
                MessageToWebSocket close = new MessageToWebSocket();
                close.setCommand("close");
                close.setData("");
                Room.rooms.remove(r);
                for(WebSocketSession s: r.getUsers()){
                    try{
                        s.sendMessage(new TextMessage(gson.toJson(close)));
                    } catch (IllegalStateException | ConcurrentModificationException e){
                        //r.getUsers().remove(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(r + "\nStatus: Room closed\n============================");
                r.deleteCatalog();
                System.out.println(Room.rooms.toString());
                r = null;
                break;
            case "pass":
                System.out.println("pass");
                r.addPassed();
                MessageToWebSocket pass = new MessageToWebSocket();
                pass.setCommand("pass");
                pass.setData(Integer.toString(r.getPassed()));
                for(WebSocketSession s: r.getUsers()){
                    try{
                        s.sendMessage(new TextMessage(gson.toJson(pass)));
                    }catch(IllegalStateException | ConcurrentModificationException e){
                        //r.getUsers().remove(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "connectservice":
                r.addService(session, msg.getData());
                break;
            case "disconnectservice":
                r.deleteService(session);
                break;
            case "oncreate":
                MessageToWebSocket users = new MessageToWebSocket();
                users.setCommand("userslist");
                for(User u: r.getConnectedUser()){
                    users.setData(gson.toJson(u));
                    try {
                        session.sendMessage(new TextMessage(gson.toJson(users)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                users.setCommand("songended");
                try {
                    users.setData("start");
                    session.sendMessage(new TextMessage(gson.toJson(users)));
                    for(String audio: r.getPlaylist()) {
                        users.setData(audio);
                        session.sendMessage(new TextMessage(gson.toJson(users)));
                    }
                    users.setData("end");
                    session.sendMessage(new TextMessage(gson.toJson(users)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                users.setCommand("password");
                users.setData(r.getPassword());
                try {
                    session.sendMessage(new TextMessage(gson.toJson(users)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                users.setCommand("pass");
                users.setCommand(Integer.toString(r.getPassed()));
                try {
                    session.sendMessage(new TextMessage(gson.toJson(users)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            case "creatorsession":
                r.addSession(session);
                break;
            case "ondestroy":
                r.deleteSession(session);
                break;
            case "audio":// Закачиваю песню на сервер
                AudioData data2 = gson.fromJson(msg.getData(), AudioData.class);
                if(data2.getPart().equals("0"))System.out.println(r + "\nStatus: Starting download:" + data2.getName() + "\n============================");
                r.addSong(data2);
                break;
            case "checked":
                AudioData data3 = gson.fromJson(msg.getData(), AudioData.class);
                boolean bool = Boolean.parseBoolean(data3.getAudio());
                System.out.println(bool);
                if(!bool){//Отправка песни клиенту
                    r.sendAudio(session, data3.getName());
                }
                break;
            case "startplaying":
                r.startPlaying(msg.getData());
                break;
            case "newplaylist"://Обновление плейлиста
                AudioData data4 = gson.fromJson(msg.getData(), AudioData.class);
                switch(data4.getAudio()){
                    case "add":
                        r.addToThePlaylist(data4.getName());
                        break;
                }
                break;
            case "songended":
                r.songEnded();
                break;
            case "checkfortime":
                r.addCheckingTime(session);
                for(WebSocketSession s: r.getUsersService().keySet()){
                    if(r.getUsersService().get(s).equals(r.getCreator().getUUID())){
                        MessageToWebSocket message2 = new MessageToWebSocket();
                        message2.setCommand("needtime");
                        message2.setData("");
                        try {
                            s.sendMessage(new TextMessage(gson.toJson(message2)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case "catchtime":
                while(!r.getCheckingfortime().isEmpty()){
                    MessageToWebSocket message2 = new MessageToWebSocket();
                    message2.setCommand("catchtime");
                    message2.setData(msg.getData());
                    try {
                        r.getCheckingfortime().get(0).sendMessage(new TextMessage(gson.toJson(message2)));
                        r.getCheckingfortime().remove(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "addfromnotcreator":
                for(WebSocketSession s: r.getUsersService().keySet()){
                    if(r.getUsersService().get(s).equals(r.getCreator().getUUID())){
                        MessageToWebSocket message2 = new MessageToWebSocket();
                        message2.setCommand("addfromnotcreator");
                        message2.setData(msg.getData());
                        try {
                            s.sendMessage(new TextMessage(gson.toJson(message2)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;

        }
    }
    public Room findRoom(String UUID){
        for (Room r: Room.rooms){
            if (r.getCreator().getUUID().equals(UUID)) return r;
        }
        return null;
    }

}
