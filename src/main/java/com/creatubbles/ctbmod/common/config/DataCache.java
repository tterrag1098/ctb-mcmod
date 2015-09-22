package com.creatubbles.ctbmod.common.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import com.creatubbles.ctbmod.common.http.Creation;
import com.creatubbles.ctbmod.common.http.Creator;
import com.creatubbles.ctbmod.common.http.User;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class DataCache {

	public static final File cacheFolder = new File(".", "creatubbles");
	private static final File cache = new File(cacheFolder, "usercache.json");
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private Set<User> savedUsers = Sets.newHashSet();

	@Getter
	private User activeUser;

	@Getter
	private Creator[] creators;

	@Getter
	private Creator activeCreator;

	/**
	 * This is not written to file, it is to save creations between openings of the GUI.
	 */
	@Getter
	@Setter
	private Creation[] creationCache;

	@SneakyThrows
	public static DataCache loadCache() {
		cacheFolder.mkdir();

		if (cache.exists() && Configs.refreshAccessToken) {
			cache.delete();
		}
		cache.createNewFile();
		JsonElement parsed = new JsonParser().parse(new FileReader(cache));
		if (parsed != null && !parsed.isJsonNull()) {
			return gson.fromJson(parsed, DataCache.class);
		}
		return new DataCache();
	}

	public void activateUser(User user) {
		if (user != null) {
			savedUsers.add(user);
		}
		activeUser = user;
		save();
	}

	public void setCreators(Creator[] creators) {
		if (creators == null) {
			this.creators = null;
			this.activeCreator = null;
			return;
		}
		this.creators = Arrays.copyOf(creators, creators.length);
		if (getActiveUser() != null) {
			int userId = getActiveUser().getId();
			for (Creator c : creators) {
				if (c.getUserId() == userId) {
					activeCreator = c;
				}
			}
		}
		save();
	}

	public Iterable<User> getSavedUsers() {
		return ImmutableSet.copyOf(savedUsers);
	}

	@SneakyThrows
	public void save() {
		String json = gson.toJson(this);
		FileWriter fw = new FileWriter(cache);
		fw.write(json);
		fw.flush();
		fw.close();
	}
}