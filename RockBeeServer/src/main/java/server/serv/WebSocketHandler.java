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
                        for(Audio audio: r.getPlaylist()) {
                            users.setData(audio.getName());
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
                for(Audio a: r.getPlaylist()){
                    MessageToWebSocket message1 = new MessageToWebSocket();
                    message1.setCommand("check");
                    AudioData toSend = new AudioData();
                    toSend.setBy("");
                    toSend.setAudio("");
                    toSend.setPart("");
                    toSend.setLenAudio(String.valueOf(a.getLen()));
                    toSend.setName(a.getName());
                    message1.setData(gson.toJson(toSend));
                    try {
                        session.sendMessage(new TextMessage(gson.toJson(message1)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
                    for(Audio audio: r.getPlaylist()) {
                        users.setData(audio.getName());
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
                break;
            case "creatorsession":
                r.addSession(session);
                break;
            case "ondestroy":
                r.deleteSession(session);
                break;
            case "audio":
                AudioData data2 = gson.fromJson(msg.getData(), AudioData.class);
                String UUIDuser = data2.getBy();
                if(data2.getPart().equals("0")){
                    System.out.println(r + "\nStatus: Started downloading song\n============================");
                    MessageToWebSocket message1 = new MessageToWebSocket();
                    message1.setCommand("createnewfile");
                    message1.setData(data2.getName());
                    for(WebSocketSession s: r.getUsersService().keySet()){
                        if(!r.getUsersService().get(s).equals(UUIDuser)){
                            try{
                                s.sendMessage(new TextMessage(gson.toJson(message1)));
                            } catch (IllegalStateException | ConcurrentModificationException e){
                                //r.getUsersService().remove(s);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                r.addSong(data2);
                break;
            case "checked":
                AudioData data3 = gson.fromJson(msg.getData(), AudioData.class);
                boolean bool = Boolean.parseBoolean(data3.getAudio());
                System.out.println(bool);
                if(!bool){
                    Audio a = new Audio();
                    a.setName(data3.getName());
                    a.setLen(Integer.parseInt(data3.getLenAudio()));
                    r.sendAudio(session, a);
                }
                break;
            case "startplaying":
                AudioData data5 = gson.fromJson(msg.getData(), AudioData.class);
                Audio a = new Audio();
                a.setName(data5.getName());
                a.setLen(Integer.parseInt(data5.getLenAudio()));
                r.startPlaying(a, Long.parseLong(data5.getPart()));
                break;
            case "addtotheplaylist":
                AudioData data4 = gson.fromJson(msg.getData(), AudioData.class);
                Audio a1 = new Audio();
                a1.setName(data4.getName());
                a1.setLen(Integer.parseInt(data4.getLenAudio()));
                r.getPlaylist().add(a1);
                MessageToWebSocket message1 = new MessageToWebSocket();
                message1.setCommand("songended");
                for(WebSocketSession s: r.getUsers()){
                    try {
                        message1.setData("start");
                        s.sendMessage(new TextMessage(gson.toJson(message1)));
                        for(Audio audio: r.getPlaylist()) {
                            message1.setData(audio.getName());
                            s.sendMessage(new TextMessage(gson.toJson(message1)));
                        }
                        message1.setData("end");
                        s.sendMessage(new TextMessage(gson.toJson(message1)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException  | ConcurrentModificationException e){
                        //r.getUsers().remove(s);
                    }
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
                System.out.println("gottime");
                while(!r.getCheckingfortime().isEmpty()){
                    MessageToWebSocket message2 = new MessageToWebSocket();
                    message2.setCommand("catchtime");
                    message2.setData(msg.getData());
                    try {
                        System.out.println("sendtime");
                        r.getCheckingfortime().get(0).sendMessage(new TextMessage(gson.toJson(message2)));
                        r.getCheckingfortime().remove(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case "addfromuser":
                for(WebSocketSession s: r.getUsersService().keySet()){
                    if(r.getUsersService().get(s).equals(r.getCreator().getUUID())){
                        MessageToWebSocket message2 = new MessageToWebSocket();
                        message2.setCommand("addfromuser");
                        message2.setData(msg.getData());
                        try {
                            s.sendMessage(new TextMessage(gson.toJson(message2)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        }
    }
    public Room findRoom(String UUID){
        for (Room r: Room.rooms){
            if (r.getCreator().getUUID().equals(UUID)) return r;
        }
        return null;
    }

}
