package top.likoslupus.ae2objects.data;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.neoforge.common.Tags;
import top.likoslupus.ae2objects.registry.Ae2ObjectsItems;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CraftingRecipeProvider extends RecipeProvider {

    public CraftingRecipeProvider(
            HolderLookup.Provider registries,
            RecipeOutput output
    ) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        cellRecipes();
        housingRecipe();
    }

    private void cellRecipes() {
        var componentsToCell = Map.of(
                AEItems.CELL_COMPONENT_1K, Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_1K,
                AEItems.CELL_COMPONENT_4K, Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_4K,
                AEItems.CELL_COMPONENT_16K, Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_16K,
                AEItems.CELL_COMPONENT_64K, Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_64K,
                AEItems.CELL_COMPONENT_256K, Ae2ObjectsItems.DEEP_ITEM_STORAGE_CELL_256K
        );

        componentsToCell.forEach((component, cell) -> {
            shaped(RecipeCategory.MISC, cell.get())
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

            shapeless(RecipeCategory.MISC, cell.get())
                    .requires(Ae2ObjectsItems.DEEP_ITEM_CELL_HOUSING)
                    .requires(component)
                    .unlockedBy("has_housing", has(Ae2ObjectsItems.DEEP_ITEM_CELL_HOUSING))
                    .unlockedBy("has_component", has(component))
                    .save(output, cell.getId().withSuffix("_with_housing").toString());
        });
    }

    private void housingRecipe() {
        shaped(RecipeCategory.MISC, Ae2ObjectsItems.DEEP_ITEM_CELL_HOUSING)
                .pattern("aba")
                .pattern("b b")
                .pattern("ded")
                .define('a', AEBlocks.QUARTZ_GLASS)
                .define('b', Tags.Items.DUSTS_REDSTONE)
                .define('d', Tags.Items.INGOTS_NETHERITE)
                .define('e', Tags.Items.GEMS_AMETHYST)
                .unlockedBy("has_netherite", has(Tags.Items.INGOTS_NETHERITE))
                .save(output);
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
            return "Ae2Objects Recipes";
        }

    }

}
