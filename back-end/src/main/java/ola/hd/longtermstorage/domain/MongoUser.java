package ola.hd.longtermstorage.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user")
public class MongoUser {

    @Id
    private String id;

    private String username;

    private String password;

    protected MongoUser() {
        // no-args constructor required by JPA spec
        // this one is protected since it shouldn't be used directly
    }

    public MongoUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
