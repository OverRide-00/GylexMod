package me.override.gylexmod.Item.custom.missions;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IronScrollItem extends Item {

    public IronScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        // Return the name with custom color (e.g., Aqua)
        return Text.translatable(this.getTranslationKey(stack)).styled(style -> style.withColor(Formatting.GRAY));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.gylexmod.iron_scroll.tooltip.l1"));
        super.appendTooltip(stack, world, tooltip, context);
    }
}
