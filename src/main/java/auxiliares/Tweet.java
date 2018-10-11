/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxiliares;

/**
 *
 * @author Eduardo
 */
public class Tweet {

    private String id, lang, text, created;
    private int retweeted;
    private User user;

    public Tweet(String id, String lang, String text, String created, int retweeted, User user) {
        this.id = id;
        this.lang = lang;
        this.text = text;
        this.created = created;
        this.retweeted = retweeted;
        this.user = user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public int getRetweeted() {
        return retweeted;
    }

    public void setRetweeted(int retweeted) {
        this.retweeted = retweeted;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Tweet{" + "id=" + id + ", lang=" + lang + ", text=" + text + ", created=" + created + ", retweeted=" + retweeted + ", user=" + user + '}';
    }
}
