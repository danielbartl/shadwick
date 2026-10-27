package dev.jbaby.shadwick.examples;

import java.io.Serializable;

/** What the form and the dialog edit. */
public class Profile implements Serializable {

    private String name;
    private String email;
    private String bio;

    public Profile() {
    }

    public Profile(String name, String email, String bio) {
        this.name = name;
        this.email = email;
        this.bio = bio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    @Override
    public String toString() {
        return name + " <" + email + ">";
    }
}
