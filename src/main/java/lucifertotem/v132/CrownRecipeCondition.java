package lucifertotem.v132;

import java.util.function.BooleanSupplier;

import com.google.gson.JsonObject;
import lucifertotem.LTConfig;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

/** recipe condition "lucifertotem:v132_crown_recipe": the Crown of Thorns recipe exists only when crownOfThorns and crownRecipe are on */
public class CrownRecipeCondition implements IConditionFactory {
    @Override public BooleanSupplier parse(JsonContext ctx, JsonObject json) { return () -> LTConfig.v132.crownOfThorns && LTConfig.v132.crownRecipe; }
}
