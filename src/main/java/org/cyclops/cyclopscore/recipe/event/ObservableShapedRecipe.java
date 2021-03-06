package org.cyclops.cyclopscore.recipe.event;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;

/**
 * A shaped recipe that is observable in terms of the recipe output.
 * The observer can update the output if it desires to do so.
 * @author rubensworks
 *
 */
public class ObservableShapedRecipe extends ShapedRecipes {
	
	private IRecipeOutputObserver observer;

	/**
	 * Make a new instance.
	 * @param group The recipe name.
	 * @param recipeWidth The recipe width.
	 * @param recipeHeight The recipe height.
	 * @param recipeItems The recipe items.
	 * @param recipeOutput The recipe output.
	 * @param observer The observer for the output.
	 */
	public ObservableShapedRecipe(String group, int recipeWidth, int recipeHeight,
								  NonNullList<Ingredient> recipeItems, ItemStack recipeOutput, IRecipeOutputObserver observer) {
		super(group, recipeWidth, recipeHeight, recipeItems, recipeOutput);
		this.observer = observer;
	}
	
	@Override
	public ItemStack getCraftingResult(InventoryCrafting craftingGrid) {
		return observer.getRecipeOutput(craftingGrid, super.getCraftingResult(craftingGrid));
    }

}
