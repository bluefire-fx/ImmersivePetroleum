package flaxbeard.immersivepetroleum.common.data.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;
import flaxbeard.immersivepetroleum.common.data.IPBlockLoot;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IPLootGenerator extends LootTableProvider{
	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	
	public IPLootGenerator(DataGenerator pGenerator){
		super(pGenerator);
	}

	private static Path getPath(Path path, ResourceLocation rl){
		return path.resolve("data/" + rl.getNamespace() + "/loot_tables/" + rl.getPath() + ".json");
	}

	@Override
	protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables(){
		return List.of(Pair.of(
				() -> new IPBlockLoot(), LootContextParamSets.BLOCK
		));
	}
}
