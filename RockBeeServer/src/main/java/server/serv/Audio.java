package server.serv;

import java.util.ArrayList;

public class Audio {
    private String name, by;
    private ArrayList<String> audioData = new ArrayList<>();
    private int len;

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void addData(String s){
        audioData.add(s);
    }

    public ArrayList<String> getAudioData() {
        return audioData;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        Audio data = (Audio) obj;
        return this.name.equals(data.getName()) && this.len == data.getLen();
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }
}
