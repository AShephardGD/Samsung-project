package server.serv;

public class User {
    private String name, UUID;

    public User(String name, String UUID) {
        this.name = name;
        this.UUID = UUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    @Override
    public String toString() {
        return "Name: " + name + '\n' +
                "UUID: " + UUID;
    }
    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        User u = (User) obj;
        return u.getName().equals(this.name) && u.getUUID().equals(this.UUID);
    }
}
