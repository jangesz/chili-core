package Configuration;

import Configuration.Authserver.AuthServerSettings;
import Configuration.Gameserver.GameServerSettings;
import Configuration.Gameserver.RealmSettings;
import Configuration.Logserver.LogServerSettings;
import Configuration.MetaServer.MetaServerSettings;
import Protocols.Serializer;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.ArrayList;

import static Configuration.Authserver.AuthServerSettings.AUTHSERVER_PATH;
import static Configuration.Gameserver.GameServerSettings.GAMESERVER_PATH;
import static Configuration.Gameserver.GameServerSettings.REALM_PATH;
import static Configuration.Logserver.LogServerSettings.LOGSERVER_PATH;
import static Configuration.MetaServer.MetaServerSettings.METASERVER_PATH;


/**
 * @author Robin Duda
 *         Handles loading and parsing of the configuration files.
 */
public class FileConfiguration implements ConfigurationLoader {
    private static ConfigurationLoader instance;
    private AuthServerSettings authentication;
    private LogServerSettings logserver;
    private GameServerSettings gameserver;
    private MetaServerSettings webserver;

    private FileConfiguration() {
        authentication = (AuthServerSettings) load(AUTHSERVER_PATH, AuthServerSettings.class);
        gameserver = (GameServerSettings) load(GAMESERVER_PATH, GameServerSettings.class);
        logserver = (LogServerSettings) load(LOGSERVER_PATH, LogServerSettings.class);
        webserver = (MetaServerSettings) load(METASERVER_PATH, MetaServerSettings.class);
        loadRealms(gameserver);
    }

    private Object load(String path, Class clazz) {
        try {
            return Serializer.unpack(JsonFileStore.readObject(path), clazz);
        } catch (IOException e) {
            return null;
        }
    }

    private void loadRealms(GameServerSettings gameserver) {
        ArrayList<RealmSettings> realms = new ArrayList<>();
        try {
            ArrayList<JsonObject> configurations = JsonFileStore.readDirectoryObjects(REALM_PATH);

            for (JsonObject configuration : configurations)
                realms.add((RealmSettings) Serializer.unpack(configuration, RealmSettings.class));

            gameserver.setRealms(realms);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized ConfigurationLoader instance() {
        if (instance == null) {
            instance = new FileConfiguration();

            if (((FileConfiguration) instance).containsAllSettings())
                TokenRefresher.refresh();
        }
        return instance;
    }

    private boolean containsAllSettings() {
        return (authentication != null && logserver != null && gameserver != null && webserver != null);
    }

    @Override
    public MetaServerSettings getMetaServerSettings() {
        return webserver;
    }

    @Override
    public GameServerSettings getGameServerSettings() {
        return gameserver;
    }

    @Override
    public LogServerSettings getLogSettings() {
        return logserver;
    }

    @Override
    public AuthServerSettings getAuthSettings() {
        return authentication;
    }

    void save() {
        Configurable[] configurables = {authentication, logserver, gameserver, webserver};

        for (Configurable configurable : configurables) {
            JsonFileStore.writeObject(Serializer.json(configurable), getConfigPath(configurable));
        }

        for (RealmSettings realm : gameserver.getRealms()) {
            JsonObject json = Serializer.json(realm);

            json.remove("afflictions");
            json.remove("classes");

            JsonFileStore.writeObject(json, getRealmPath(realm));
        }
    }

    private String getRealmPath(RealmSettings realm) {
        return REALM_PATH + realm.getName() + ".json";
    }

    private String getConfigPath(Configurable configurable) {
        return "conf/system/" + configurable.getName() + ".json";
    }
}
