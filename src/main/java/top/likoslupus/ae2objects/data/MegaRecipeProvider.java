package top.likoslupus.ae2objects.data;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import top.likoslupus.ae2objects.cell.model.CellContentType;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.cell.model.CellForm;
import top.likoslupus.ae2objects.cell.model.CellTier;
import top.likoslupus.ae2objects.registry.CellComponentSources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Emits the MEGA-tier recipes as raw, condition-gated JSON.
 *
 * <p>MEGA components ({@code megacells:cell_component_*}) do not exist at development datagen
 * time, so the recipe builders cannot be used. These recipes reference the components by id and
 * only load when the MEGA mod is present.</p>
 *
 * <p>TODO: replace with builder-based recipes once MEGA 26.1 is available on the datagen
 * classpath.</p>
 */
public final class MegaRecipeProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MEGA_MOD_ID = "megacells";

    private static final String QUARTZ_GLASS = "ae2:quartz_glass";
    private static final String REDSTONE = "#c:dusts/redstone";
    private static final String NETHERITE = "#c:ingots/netherite";
    private static final String AMETHYST = "#c:gems/amethyst";
    private static final String ME_CHEST = "ae2:me_chest";
    private static final String ENERGY_CELL = "ae2:energy_cell";

    private final PackOutput.PathProvider recipes;

    public MegaRecipeProvider(PackOutput output) {
        this.recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List.of(CellContentType.ITEM, CellContentType.FLUID).forEach(type -> {
            var housing = "ae2objects:deep_" + type.id() + "_cell_housing";
            CellTier.megaTiers().forEach(tier -> {
                var component = CellComponentSources.forTier(tier)
                        .itemId()
                        .toString();
                var drive = new CellDefinition(type, tier, CellForm.DRIVE);
                save(
                        cache,
                        drive.id(),
                        shapedDrive(component, drive)
                );
                save(
                        cache,
                        drive.id().withSuffix("_with_housing"),
                        shapeless(List.of(housing, component), drive)
                );
                var portable = new CellDefinition(type, tier, CellForm.PORTABLE);
                save(
                        cache,
                        portable.id(),
                        shapeless(
                                List.of(ME_CHEST, component, ENERGY_CELL, housing),
                                portable
                        )
                );
            });
        });

        return CompletableFuture.completedFuture(null);
    }

    private void save(
            CachedOutput cache,
            Identifier id,
            JsonObject json
    ) {
        var path = recipes.json(id);
        try {
            var bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
            cache.writeIfNeeded(
                    path,
                    bytes,
                    Hashing.sha256().hashBytes(bytes)
            );
        } catch (IOException e) {
            throw new RuntimeException("Couldn't write MEGA recipe " + id, e);
        }
    }

    private JsonObject shapedDrive(String component, CellDefinition definition) {
        var recipe = baseRecipe("minecraft:crafting_shaped", definition);

        var pattern = new JsonArray();
        pattern.add("aba");
        pattern.add("bcb");
        pattern.add("ded");
        recipe.add("pattern", pattern);

        var key = new JsonObject();
        key.addProperty("a", QUARTZ_GLASS);
        key.addProperty("b", REDSTONE);
        key.addProperty("c", component);
        key.addProperty("d", NETHERITE);
        key.addProperty("e", AMETHYST);
        recipe.add("key", key);

        return recipe;
    }

    private JsonObject shapeless(
            List<String> ingredients,
            CellDefinition definition
    ) {
        var recipe = baseRecipe("minecraft:crafting_shapeless", definition);

        var array = new JsonArray();
        ingredients.forEach(array::add);
        recipe.add("ingredients", array);

        return recipe;
    }

    private JsonObject baseRecipe(String type, CellDefinition definition) {
        var recipe = new JsonObject();
        recipe.addProperty("type", type);
        recipe.addProperty("category", "misc");

        var condition = new JsonObject();
        condition.addProperty("type", "neoforge:mod_loaded");
        condition.addProperty("modid", MEGA_MOD_ID);
        var conditions = new JsonArray();
        conditions.add(condition);
        recipe.add("neoforge:conditions", conditions);

        var result = new JsonObject();
        result.addProperty("id", "ae2objects:" + definition.itemId());
        recipe.add("result", result);

        return recipe;
    }

    @Override
    public String getName() {
        return "AE2Objects MEGA Recipes";
    }

}
