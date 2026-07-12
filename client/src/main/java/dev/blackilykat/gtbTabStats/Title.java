package dev.blackilykat.gtbTabStats;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public record Title(int score, String title, boolean bold, String color) {
	/// List of all supported titles sorted by score
	public static List<Title> titles = List.of(new Title(0, "Rookie", false, "FFFFFF"));

	public static void loadTitles() {
		try(InputStream is = Title.class.getResourceAsStream("/assets/gtb-tab-stats/titles.json")) {
			titles = GTBTabStats.GSON.fromJson(new String(is.readAllBytes()), new TypeToken<>() {});
		} catch(IOException e) {
			GTBTabStats.LOGGER.error("Failed to load titles", e);
		}
	}

	public static Title getTitle(int score) {
		assert !titles.isEmpty();

		if(score < 0) {
			GTBTabStats.LOGGER.warn("Tried to find title for negative score ({})", score);
			return titles.getFirst();
		}

		Title title = null;
		for(Title t : titles) {
			if(t.score() > score) break;
			title = t;
		}
		assert title != null;

		return title;
	}

	public int colorAsInt() {
		return Integer.valueOf(color, 16);
	}
}
