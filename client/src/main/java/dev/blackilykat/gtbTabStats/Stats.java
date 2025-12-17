package dev.blackilykat.gtbTabStats;

public class Stats {
	public String language;
	public int wins;
	public int score;

	@Override
	public String toString() {
		return "Stats(" + language + ", " + wins + ", " + score + ")";
	}
}

