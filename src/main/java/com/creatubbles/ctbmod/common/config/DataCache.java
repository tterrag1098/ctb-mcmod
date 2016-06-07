package com.creatubbles.ctbmod.common.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Value;

import org.apache.commons.io.FileUtils;

import com.creatubbles.api.core.User;
import com.creatubbles.api.response.auth.OAuthAccessTokenResponse;
import com.creatubbles.ctbmod.common.http.CreationRelations;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

public class DataCache {
    
    @Value
    public static class OAuth {
        @SerializedName("access_token")
        private String accessToken;
        @SerializedName("token_type")
        private String tokenType;
    }
 
    @Value
    @EqualsAndHashCode(exclude = "auth")
    public static class UserAndAuth {
        private User user;
        private OAuth auth;
    }
    
    @Value
    public static class CacheVersion {
        private int userVersion;
        private int creationVersion;
    }
    
    private static class UserAndAuthBackwardsCompat implements JsonDeserializer<UserAndAuth> {
        @Override
        public UserAndAuth deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            if (obj.has("user") || obj.has("auth")) {
                return new UserAndAuth(context.<User>deserialize(obj.get("user"), User.class), context.<OAuth>deserialize(obj.get("auth"), OAuth.class));
            } else {
                return new UserAndAuth(context.<User>deserialize(obj, User.class), null);
            }
        }
    }
    
    private static final int USER_VERSION = 1;
    private static final int CREATION_VERSION = 1;
    
    public static final CacheVersion VERSION = new CacheVersion(USER_VERSION, CREATION_VERSION);

    public static final File cacheFolderv2 = new File(".", "creatubblesv2");
    public static final File cacheFolder = new File(".", "creatubbles");
    private static final File version = new File(cacheFolder, ".cacheversion");
    private static final File cache = new File(cacheFolder, "usercache.json");
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(UserAndAuth.class, new UserAndAuthBackwardsCompat()).setPrettyPrinting().create();

    public static final File creations = new File(cacheFolder, "creations");

    private final Set<UserAndAuth> savedUsers = Sets.newHashSet();

    @Getter
    private OAuth OAuth;
    
    @Getter
    private User activeUser;
    
    /**
     * This is not written to file, it is to save creations between openings of the GUI.
     */
    @Getter
    @Setter
    private transient List<CreationRelations> creationCache;

    @Getter
    private transient boolean dirty;

    @SneakyThrows
    public static DataCache loadCache() {
        if (cacheFolderv2.exists()) {
            FileUtils.deleteDirectory(cacheFolderv2);
        }
        
        cacheFolder.mkdir();
        
        CacheVersion ver;
        if (version.exists()) {
            ver = gson.fromJson(new FileReader(version), CacheVersion.class);
        } else {
            ver = new CacheVersion(0, 0);
        }
        
        if (ver.getCreationVersion() < CREATION_VERSION) {
            FileUtils.deleteDirectory(creations);
        }
        if (ver.getUserVersion() < USER_VERSION) {
            cache.delete();
        }
        
        FileWriter vfw = new FileWriter(version);
        vfw.write(gson.toJson(VERSION));
        vfw.flush();
        vfw.close();

        if (cache.exists() && Configs.refreshUserCache) {
            cache.delete();
        }
        cache.createNewFile();
        JsonElement parsed = new JsonParser().parse(new FileReader(cache));
        if (parsed != null && !parsed.isJsonNull()) {
            gson.fromJson(parsed, DataCache.class);
        }
        return new DataCache();
    }

    public void activateUser(UserAndAuth user) {
        if (user != null) {
            savedUsers.remove(user);
            savedUsers.add(user);
            activeUser = user.getUser();
            OAuth = user.getAuth();
        } else {
            activeUser = null;
        }
        save();
    }

    public void setOAuth(OAuthAccessTokenResponse response) {
        // copy data for immutable state
        this.OAuth = response == null ? null : new OAuth(response.getAccessToken(), response.getType());        
    }
    
    public Collection<UserAndAuth> getSavedUsers() {
        return ImmutableSet.copyOf(savedUsers);
    }

    @SneakyThrows
    public void save() {
        FileWriter fw = new FileWriter(cache);
        fw.write(gson.toJson(this));
        fw.flush();
        fw.close();
    }

    public void dirty(boolean dirty) {
        this.dirty = dirty;
    }
}
