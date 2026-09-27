package top.likoslupus.ae2objects.data;

import appeng.core.definitions.AEBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import top.likoslupus.ae2objects.cell.model.CellDefinition;
import top.likoslupus.ae2objects.registry.CellComponentSources;
import top.likoslupus.ae2objects.registry.RegisteredCells;
import top.likoslupus.ae2objects.registry.RegisteredHousings;

import java.util.concurrent.CompletableFuture;

public final class CraftingRecipeProvider extends RecipeProvider {

    public CraftingRecipeProvider(
            HolderLookup.Provider registries,
            RecipeOutput output
    ) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        storageCellRecipes();
        portableCellRecipes();
        housingRecipes();
    }

    private void storageCellRecipes() {
        RegisteredCells.entries().forEach(entry -> {
            var definition = entry.getKey();
            if (!definition.isDrive()) {
                return;
            }

            var cell = entry.getValue().get();
            var component = componentOf(definition);
            var housing = RegisteredHousings.require(definition.type()).get();

            shaped(RecipeCategory.MISC, cell)
                    .pattern("aba")
                    .pattern("bcb")
                    .pattern("ded")
                    .define('a', AEBlocks.QUARTZ_GLASS)
                    .define('b', Tags.Items.DUSTS_REDSTONE)
                    .define('c', component)
                    .define('d', Tags.Items.INGOTS_NETHERITE)
                    .define('e', Tags.Items.GEMS_AMETHYST)
                    .unlockedBy("has_netherite", has(Tags.Items.INGOTS_NETHERITE))
                    .save(output);
            shapeless(RecipeCategory.MISC, cell)
                    .requires(housing)
                    .requires(component)
                    .unlockedBy("has_housing", has(housing))
                    .unlockedBy("has_component", has(component))
                    .save(
                            output,
                            definition.id().withSuffix("_with_housing").toString()
                    );
        });
    }

    private void portableCellRecipes() {
        RegisteredCells.entries().forEach(entry -> {
            var definition = entry.getKey();
            if (!definition.isPortable()) {
                return;
            }

            var cell = entry.getValue().get();
            var component = componentOf(definition);
            var housing = RegisteredHousings.require(definition.type()).get();

            shapeless(RecipeCategory.MISC, cell)
                    .requires(AEBlocks.ME_CHEST)
                    .requires(component)
                    .requires(AEBlocks.ENERGY_CELL)
                    .requires(housing)
                    .unlockedBy("has_housing", has(housing))
                    .unlockedBy("has_component", has(component))
                    .save(output);
        });
    }

    private void housingRecipes() {
        RegisteredHousings.all().forEach(housing ->
                shaped(RecipeCategory.MISC, housing)
                        .pattern("aba")
                        .pattern("b b")
                        .pattern("ded")
                        .define('a', AEBlocks.QUARTZ_GLASS)
                        .define('b', Tags.Items.DUSTS_REDSTONE)
                        .define('d', Tags.Items.INGOTS_NETHERITE)
                        .define('e', Tags.Items.GEMS_AMETHYST)
                        .unlockedBy("has_netherite", has(Tags.Items.INGOTS_NETHERITE))
                        .save(output)
        );
    }

    private static Item componentOf(CellDefinition definition) {
        return BuiltInRegistries.ITEM
                .getOptional(CellComponentSources.forTier(definition.tier()).itemId())
                .orElse(Items.AIR);
    }

    public static final class Runner extends RecipeProvider.Runner {

        public Runner(
                PackOutput output,
                CompletableFuture<HolderLookup.Provider> lookupProvider
        ) {
            super(output, lookupProvider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(
                HolderLookup.Provider registries,
                RecipeOutput output
        ) {
            return new CraftingRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "AE2Objects Recipes";
        }

    }

}
