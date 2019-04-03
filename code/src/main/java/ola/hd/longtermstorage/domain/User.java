package ola.hd.longtermstorage.domain;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

@Entry(
        base = "ou=people",
        objectClasses = {"inetOrgPerson", "organizationalPerson", "person", "top"}
)
public class User {

    @Id
    private Name id;

    @Attribute(name = "uid")
    private String username;

    @Attribute(name = "userPassword")
    private String password;

    public Name getId() {
        return id;
    }

    public void setId(Name id) {
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
