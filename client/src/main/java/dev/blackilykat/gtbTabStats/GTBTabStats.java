package dev.blackilykat.gtbTabStats;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

public class GTBTabStats implements ClientModInitializer {
	private static final Gson GSON = new Gson();
	@Override
	public void onInitializeClient() {
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
