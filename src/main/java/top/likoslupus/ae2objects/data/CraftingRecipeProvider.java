package top.likoslupus.ae2objects.data;

import appeng.core.definitions.AEBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.neoforge.common.Tags;
import top.likoslupus.ae2objects.registry.Ae2ObjectsItems;

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
        itemHousingRecipe();
    }

    private void storageCellRecipes() {
        Ae2ObjectsItems.storageCells().forEach(registration -> {
            var cell = registration.item().get();
            var component = registration.coreItem().get();
            var housing = registration.housingItem().get();
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
                            registration.item().getId().withSuffix("_with_housing").toString()
                    );
        });
    }

    private void itemHousingRecipe() {
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
            return "AE2Objects Recipes";
        }

    }

}
