package dev.blackilykat.gtbTabStats.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.blackilykat.gtbTabStats.GTBTabStats;
import dev.blackilykat.gtbTabStats.Stats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

	@Unique
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

	@Unique
	private static final Map<UUID, Stats> STATS_CACHE = new HashMap<>();

	@ModifyVariable(method = "applyPlayerInfoUpdate", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	public ClientboundPlayerInfoUpdatePacket.Entry GTBTabStats$something(ClientboundPlayerInfoUpdatePacket.Entry entry, @Local(argsOnly = true) PlayerInfo playerInfo, @Local(argsOnly = true) ClientboundPlayerInfoUpdatePacket.Action action) {
		UUID uuid = playerInfo.getProfile().id();

		if(STATS_CACHE.containsKey(uuid)) {
			Stats stats = STATS_CACHE.get(uuid);
			if(stats == null) return entry;

			return getUpdatedEntry(entry, playerInfo, stats);
		}

		if(playerInfo.getGameMode().isCreative() || playerInfo.getProfile().id().equals(Minecraft.getInstance().getGameProfile().id())) {
			STATS_CACHE.put(uuid, null);
			EXECUTOR.submit(() -> {
				Stats stats = GTBTabStats.getStats(uuid);
				if(stats == null) {
					System.out.println("nooo :(");
					return;
				}
				STATS_CACHE.put(uuid, stats);

				playerInfo.setTabListDisplayName(makeDisplayName(playerInfo, stats));
			});
		}

		return entry;
	}

	@Unique
	private ClientboundPlayerInfoUpdatePacket.Entry getUpdatedEntry(ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo, Stats stats) {
		return new ClientboundPlayerInfoUpdatePacket.Entry(
				entry.profileId(),
				entry.profile(),
				entry.listed(),
				entry.latency(),
				entry.gameMode(),
				makeDisplayName(playerInfo, stats),
				entry.showHat(),
				entry.listOrder(),
				entry.chatSession());
	}

	@Unique
	private Component makeDisplayName(PlayerInfo playerInfo, Stats stats) {
		PlayerTeam team = playerInfo.getTeam();
		int color = 0xFFFFFF;
		Component prefix = null;
		Component suffix = null;
		if(team != null) {
			color = team.getColor().getColor() == null ? 0xFFFFFF : team.getColor().getColor();
			prefix = team.getPlayerPrefix();
			suffix = team.getPlayerSuffix();
		}
		if(prefix == null ) prefix = Component.empty();
		if(suffix == null ) suffix = Component.empty();

		//noinspection NoTranslation
		return prefix.copy().append(Component.literal(playerInfo.getProfile().name()).withColor(color).append(suffix)).append(Component.translatable(" (%s, %s wins, %s score)", stats.language.toLowerCase(), stats.wins, stats.score).withColor(0x777777));

	}

}
