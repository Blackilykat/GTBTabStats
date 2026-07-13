package dev.blackilykat.gtbTabStats.mixin;

import dev.blackilykat.gtbTabStats.Stats;
import dev.blackilykat.gtbTabStats.Title;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.blackilykat.gtbTabStats.GTBTabStats.STATS_CACHE;

// Hypixel chat is a mess and so is the code for parsing it. Sorry.

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

	/// Title used in hover text for players with unknown stats (either unloaded or nicks)
	@Unique
	private static final Title UNKNOWN_TITLE = new Title(0, "Unknown", true, "FFFFFF");

	@Unique
	private static final Pattern LAST_SECTION_SIGN = Pattern.compile(".+?(§.)");

	@ModifyVariable(
			method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
			at = @At("HEAD"),
			argsOnly = true
	)
	public Component GTBTabStats$injectStatsHover(Component component) {
		return withStatsHover(component);
	}

	// non mutable components are a lie made up by the government
	@Unique
	public Component withStatsHover(Component component) {

		ComponentContents contents = component.getContents();
		if(contents instanceof PlainTextContents textContents) {
			String text = textContents.text();

			// what the hell hypixel section signs in 2026?
			String section = "";
			if(text.startsWith("§")) {
				section = text.substring(0, 2);
				text = text.substring(2);
			}

			// Section sign before colon, which doesn't do anything because it's in another component
			text = text.replaceAll("§.$", "");

			// Using the first section sign doesn't work because guesser chat uses a single literal with section signs.
			// Use the second to last one instead (excluding the one removed in the line above)
			Matcher matcher = LAST_SECTION_SIGN.matcher(text);
			if(matcher.matches()) {
				section = matcher.group(1);
			}

			if(!text.isBlank()) {
				String key = text.trim();

				if(STATS_CACHE.containsKey(key)) {
					Stats stats = STATS_CACHE.get(key);
					component = component.copy().withStyle(component.getStyle().withHoverEvent(buildStatsHover(key, stats)));
				} else {
					// works both when "[RNK] Username" and "§b[RNK§c++§b] §bUsername§7: §fmsg"
					// hypixel does much bs with components
					var split = text.split("( |§.)");
					key = split[split.length - 1];

					if(STATS_CACHE.containsKey(key)) {
						Stats stats = STATS_CACHE.get(key);

						List<Component> siblings = component.getSiblings();
						component = Component.literal(section + text.substring(0, text.length() - key.length()))
								.withStyle(component.getStyle())
								.append(Component.literal(section + key).withStyle(component.getStyle().withHoverEvent(buildStatsHover(key, stats))));

						for(Component sibling : siblings) {
							((MutableComponent)component).append(sibling);
						}
					}
				}
			}
		} else if(contents instanceof TranslatableContents translatable) {
			// Abusing Minecraft code since it probably should be cloning this array instead of passing it.
			// But it just passes it and it allows me to waste less memory :D
			Object[] args = translatable.getArgs();
			for(int i = 0; i < args.length; i++) {
				if(args[i] instanceof String str) {
					String key = str.trim();
					if(STATS_CACHE.containsKey(key)) {
						Stats stats = STATS_CACHE.get(key);
						args[i] = Component.literal(str).withStyle(Style.EMPTY.withHoverEvent(buildStatsHover(key, stats)));
					}
				} else if(args[i] instanceof Component comp) {
					args[i] = withStatsHover(comp);
				}
			}
		}

		// do not set this to an object not obtained through component.copy().
		// There is no guarantee that the siblings list is modifiable (which I need it to be)
		MutableComponent mutable = null;

		List<Component> siblings = component.getSiblings();
		for(int i = 0; i < siblings.size(); i++) {
			Component oldSibling = siblings.get(i);
			Component newSibling = withStatsHover(oldSibling);

			if(oldSibling != newSibling) {
				if(mutable == null) {
					mutable = component.copy();
					mutable.setStyle(mutable.getStyle().withHoverEvent(null));
				}

				mutable.getSiblings().set(i, newSibling);
			}
		}

		return mutable == null ? component : mutable;
	}

	@Unique
	@SuppressWarnings("NoTranslation")
	public HoverEvent buildStatsHover(String username, Stats stats) {
		Title title;
		if(stats == null) {
			stats = new Stats();
			stats.language = "unknown";
			stats.score = 0;
			stats.wins = 0;
			title = UNKNOWN_TITLE;
		} else {
			title = Title.getTitle(stats.score);
		}

		return new HoverEvent.ShowText(
				Component.translatable(
						"%s %s\n\nLanguage: %s\nWins: %s\nScore: %s",
						Component.literal(title.title())
								.withColor(title.colorAsInt())
								.withStyle(title.bold() ? new ChatFormatting[]{ChatFormatting.BOLD} : new ChatFormatting[0]),
						Component.literal(username).withColor(0xFFFFFF),
						Component.literal(stats.language).withColor(0xFFFFFF),
						Component.literal(String.valueOf(stats.wins)).withColor(0xFFFFFF),
						Component.literal(String.valueOf(stats.score)).withColor(0xFFFFFF)
				).withColor(0xAAAAAA)
		);
	}
}
