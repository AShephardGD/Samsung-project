package server.serv;

import com.google.gson.Gson;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

@RestController
public class MusicController extends HttpServlet {
    ArrayList<String> UUIDs = new ArrayList<>();
    File load = new File("C:\\Users\\AdrianShephard\\Desktop\\RockBeeServer\\UUIDS.txt");
    public MusicController() {
        if(load.exists() && load.canRead()){
            try(FileReader reader = new FileReader(load))
            {
                int c;
                String word = "";
                while((c=reader.read())!=-1){
                    char a = (char)c;
                    if (a != ' ') word += (char) c;
                    else {
                        UUIDs.add(word);
                        word = "";
                    }
                }
                UUIDs.add(word);
            }
            catch(IOException ex){System.out.println(ex.getMessage()); }
        } else {
            try {
                load.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequestMapping(value = "/generateuuid", method = RequestMethod.POST)
    public String generateUUID(@RequestBody String newDeviceID){
        if(!UUIDs.contains(substring(newDeviceID))) {
            UUIDs.add(substring(newDeviceID));
            System.out.println("New UUID: " + UUIDs.indexOf(substring(newDeviceID)));
            if (load.exists() && load.canWrite()) {
                try (FileWriter writer = new FileWriter(load, false)) {
                    for (String s : UUIDs) {
                        writer.write(s);
                        writer.append(' ');
                    }
                    writer.flush();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        return UUIDs.indexOf(substring(newDeviceID)) + "";
    }
    @RequestMapping(value = "/checkuuid", method = RequestMethod.POST)
    public Boolean checkUUID(@RequestBody ArrayList<Object> params){
        if(params.get(0) == null) return false;
        return UUIDs.get(Integer.parseInt((String)params.get(0))).equals(params.get(1));
    }

    @RequestMapping(value = "/newroom", method = RequestMethod.POST)
    public boolean createRoom(@RequestBody ArrayList<Object> params){//User creator, String pass
        User creator = new User((String) params.get(0), (String)params.get(1));
        String pass = (String) params.get(2);
        System.out.println(new Room(creator, pass) + "\nStatus: is created\n============================");
        Room.rooms.add(new Room(creator, pass));
        return Room.rooms.contains(new Room(creator, pass));
    }

    @RequestMapping(value = "/checkingsong", method = RequestMethod.POST)
    public Boolean checkingSongInTheRoom(@RequestBody ArrayList<Object> params){
        String UUID = (String)params.get(0);
        int len = (Integer) params.get(1);
        String name = (String) params.get(2);
        for (Room r: Room.rooms){
            if (r.getCreator().getUUID().equals(UUID)) {
                for(File f: r.getMySongs()){
                    if(f.length() == len && name.equals(f.getName())) return true;
                }
                return false;
            }
        }
        return null;
    }

    @RequestMapping(value = "/checkingroom", method = RequestMethod.POST)
    public Boolean checkingRoom(@RequestBody ArrayList<Object> params){
        String UUID = (String) params.get(0);
        for(Room r: Room.rooms){
            if(r.getCreator().getUUID().equals(UUID)){
                System.out.println("true " + UUID + "\n"+ r);
                return true;
            }
        }
        System.out.println("false " + UUID);
        return false;
    }

    public String substring(String s){
        if(!s.equals("")) return s.substring(1, s.length() - 1);
        else return "";
    }
}
