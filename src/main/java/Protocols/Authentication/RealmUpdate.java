package Protocols.Authentication;

import Protocols.Header;

/**
 * @author Robin Duda
 */
public class RealmUpdate {
    public static final String ACTION = "realm.update";
    private Header header;
    private Integer players;

    public RealmUpdate() {
        this.header = new Header(ACTION);
    }

    public RealmUpdate(int players) {
        this();
        this.players = players;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Integer getPlayers() {
        return players;
    }

    public void setPlayers(Integer players) {
        this.players = players;
    }
}
