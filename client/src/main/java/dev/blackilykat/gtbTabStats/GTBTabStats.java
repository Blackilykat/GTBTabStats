package dev.blackilykat.gtbTabStats;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GTBTabStats implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("gtb-tab-stats");
	public static final Gson GSON = new Gson();

	/// Never-expiring clientside cache of stats.
	///
	/// Uses usernames instead of UUIDs as keys for 2 reasons:
	/// - to cache 404 results for nicked players, which get assigned UUIDs randomly every time they are loaded
	/// - to avoid making a bagillion requests for NPCs in victory dances
	@Unique
	public static final Map<String, Stats> STATS_CACHE = new HashMap<>();

	@Override
	public void onInitializeClient() {
		Title.loadTitles();
	}

	public static Stats getStats(UUID uuid) {
		Stats toReturn = new Stats();
		try {
			URL url = URI.create("https://gtb-tab-stats.blackilykat.dev/api/profile/" + uuid.toString()).toURL();

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

			int r = conn.getResponseCode();
			if(r != 200) {
				return null;
			} else {
				JsonObject o = GSON.fromJson(new String(conn.getInputStream().readAllBytes()), JsonObject.class);

				toReturn.language = o.get("language").getAsString();
				if(toReturn.language == null || toReturn.language.isEmpty()) toReturn.language = "ENGLISH";
				toReturn.wins = o.get("wins").getAsInt();
				toReturn.score= o.get("score").getAsInt();
			}
		} catch(IOException e) {
			return null;
		}
		return toReturn;
	}
}
