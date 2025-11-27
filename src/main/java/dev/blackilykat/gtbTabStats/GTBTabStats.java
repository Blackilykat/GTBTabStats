package dev.blackilykat.gtbTabStats;

import net.fabricmc.api.ClientModInitializer;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

public class GTBTabStats implements ClientModInitializer {
	public static final String API_KEY = "to hypixel reviewers: this variable is temporary! I will not share my actual API key here.";

	@Override
	public void onInitializeClient() {
	}

	public static Stats getStats(UUID uuid) {
		Stats toReturn = new Stats();
		try {
			URL url = URI.create("https://api.hypixel.net/v2/player?uuid=" + uuid.toString()).toURL();

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

			conn.setRequestProperty("API-Key", API_KEY);

			int r = conn.getResponseCode();
			if(r != 200) {
				return null;
			} else {
				JSONParser parser = new JSONParser();
				JSONObject o = (JSONObject) parser.parse(conn.getInputStream());
				JSONObject jsonPlayer = (JSONObject) o.get("player");

				if(jsonPlayer == null) {
					return null;
				}

				toReturn.language = jsonPlayer.getAsString("userLanguage");
				if(toReturn.language == null) toReturn.language = "ENGLISH";

				JSONObject stats = (JSONObject) jsonPlayer.get("stats");
				if(stats != null) {
					JSONObject bb = (JSONObject) stats.get("BuildBattle");
					if(bb != null) {
						Number t = bb.getAsNumber("wins_guess_the_build");
						if(t != null) toReturn.wins = t.intValue();
						t = bb.getAsNumber("score");
						if(t != null) toReturn.score = t.intValue();
					}
				}
			}
		} catch(IOException | ParseException e) {
			return null;
		}
		return toReturn;
	}
}
