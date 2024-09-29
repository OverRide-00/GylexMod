package me.override.gylexmod.Commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.override.gylexmod.Item.ModItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.Item;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.Objects;

public class Permit {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("setPermit")
                    .then(literal("name")
                            .then(argument("name", StringArgumentType.greedyString())
                                    .executes(Permit::setPermitName)))
                    .then(literal("description")
                            .then(argument("description", StringArgumentType.greedyString())
                                    .executes(Permit::setPermitDescription)))
                    .then(literal("items")
                            .then(argument("items", StringArgumentType.greedyString())
                                    .suggests(itemSuggestionProvider())
                                    .executes(Permit::setPermitItems)))
                    .then(literal("reset")
                            .executes(Permit::clearPermit))
                    .then(literal("clearLore")
                            .executes(Permit::clearLore))
                    .then(literal("rank")
                            .then(argument("rank", StringArgumentType.word())
                                    .suggests((context, builder) -> builder.suggest("Diamond").suggest("Gold").suggest("Iron").buildFuture())
                                    .executes(Permit::setPermitRank)))
                    .then(literal("space")
                            .executes(Permit::addPermitSpace))
                    .then(literal("save")
                            .executes(Permit::savePermit))
            );

            dispatcher.register(literal("permitItems")
                    .executes(Permit::listPermitItems)
            );
        });
    }

    private static SuggestionProvider<ServerCommandSource> itemSuggestionProvider() {
        return (context, builder) -> {
            for (Item item : Registries.ITEM) {
                Identifier itemId = Registries.ITEM.getId(item);
                builder.suggest(itemId.toString());
            }
            return builder.buildFuture();
        };
    }

    private static int setPermitName(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        String name = StringArgumentType.getString(context, "name");
        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            Formatting color = getPermitColor(heldItem);
            heldItem.setCustomName(Text.literal(name).formatted(color));
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("Permit name set to: ").formatted(Formatting.GREEN))
                    .append(Text.literal(name).formatted(Formatting.YELLOW)), false);
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static int setPermitDescription(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        String description = StringArgumentType.getString(context, "description");
        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            NbtCompound displayTag = heldItem.getOrCreateSubNbt("display");
            NbtList loreList = new NbtList();

            loreList.add(NbtString.of(Text.Serializer.toJson(Text.literal(""))));
            loreList.add(NbtString.of(Text.Serializer.toJson(Text.literal(description).formatted(Formatting.GRAY))));

            displayTag.put("Lore", loreList);
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("Permit description set to: ").formatted(Formatting.GREEN))
                    .append(Text.literal(description).formatted(Formatting.YELLOW)), false);
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static int addPermitSpace(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            NbtCompound displayTag = heldItem.getOrCreateSubNbt("display");
            NbtList loreList = displayTag.contains("Lore") ? displayTag.getList("Lore", 8) : new NbtList();

            loreList.add(NbtString.of(Text.Serializer.toJson(Text.literal(""))));

            displayTag.put("Lore", loreList);
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("An empty lore line has been added.").formatted(Formatting.GREEN)), false);
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static int setPermitItems(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        String itemsInput = StringArgumentType.getString(context, "items");
        String[] items = itemsInput.split(",");

        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            NbtCompound permitData = heldItem.getOrCreateNbt();
            NbtList itemsList = new NbtList();

            for (String item : items) {
                String trimmedItem = item.trim();
                Identifier itemId = new Identifier(trimmedItem);

                if (Registries.ITEM.containsId(itemId)) {
                    itemsList.add(NbtString.of(trimmedItem));
                } else {
                    player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                            .append(Text.literal("Item " + trimmedItem + " does not exist!").formatted(Formatting.RED)), false);
                    return 0;
                }
            }

            permitData.put("PermitItems", itemsList);
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("Permit items set to: ").formatted(Formatting.GREEN))
                    .append(Text.literal(itemsInput).formatted(Formatting.YELLOW)), false);
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static int listPermitItems(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            NbtCompound permitData = heldItem.getOrCreateNbt();
            if (permitData.contains("PermitItems")) {
                NbtList itemsList = permitData.getList("PermitItems", 8);

                // Use StringBuilder to build the response text with new lines
                StringBuilder items = new StringBuilder();
                for (int i = 0; i < itemsList.size(); i++) {
                    String item = itemsList.getString(i);
                    items.append(item);
                    if (i < itemsList.size() - 1) {
                        items.append("\n"); // Add a new line between items
                    }
                }

                player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                        .append(Text.literal("The Items for the permit are:").formatted(Formatting.GREEN))
                        .append(Text.literal("\n").formatted(Formatting.RESET)) // Add a newline before the item list
                        .append(Text.literal(items.toString()).formatted(Formatting.YELLOW)), false);
            } else {
                player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                        .append(Text.literal("No items found for this permit!").formatted(Formatting.RED)), false);
            }
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static int clearPermit(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            heldItem.removeSubNbt("display");
            heldItem.removeSubNbt("PermitItems");
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("Permit cleared.").formatted(Formatting.GREEN)), false);
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static int clearLore(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            NbtCompound displayTag = heldItem.getOrCreateSubNbt("display");
            displayTag.remove("Lore");
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("Lore cleared.").formatted(Formatting.GREEN)), false);
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static int setPermitRank(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        String rank = StringArgumentType.getString(context, "rank");
        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            NbtCompound permitData = heldItem.getOrCreateNbt();
            permitData.putString("Rank", rank);

            NbtCompound displayTag = heldItem.getOrCreateSubNbt("display");
            NbtList loreList = displayTag.contains("Lore") ? displayTag.getList("Lore", 8) : new NbtList();

            Formatting color = getPermitColor(rank);
            loreList.add(NbtString.of(Text.Serializer.toJson(Text.literal(rank.toUpperCase() + " RANK").formatted(color))));

            displayTag.put("Lore", loreList);

            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("Permit rank set to: ").formatted(Formatting.GREEN))
                    .append(Text.literal(rank).formatted(Formatting.YELLOW)), false);
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static int savePermit(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        assert player != null;
        ItemStack heldItem = player.getMainHandStack();

        if (isPermitItem(heldItem)) {
            // Retrieve the name of the permit (custom name)
            String name = heldItem.hasCustomName() ? heldItem.getName().getString() : "Unnamed Permit";

            // Retrieve the description from the lore (assuming it's the second line of the lore)
            NbtCompound displayTag = heldItem.getSubNbt("display");
            String description = "No Description";
            if (displayTag != null && displayTag.contains("Lore")) {
                NbtList loreList = displayTag.getList("Lore", 8);  // 8 means it's stored as a string
                if (loreList.size() > 1) {
                    description = Objects.requireNonNull(Text.Serializer.fromJson(loreList.getString(1))).getString();
                }
            }

            // Retrieve the list of permitted items
            NbtCompound permitData = heldItem.getOrCreateNbt();
            StringBuilder items = new StringBuilder("No items assigned");
            if (permitData.contains("PermitItems")) {
                NbtList itemsList = permitData.getList("PermitItems", 8);
                items = new StringBuilder();
                for (int i = 0; i < itemsList.size(); i++) {
                    items.append(itemsList.getString(i));
                    if (i < itemsList.size() - 1) {
                        items.append(", ");
                    }
                }
            }

            // Retrieve the rank from the NBT data
            String rank = permitData.contains("Rank") ? permitData.getString("Rank") : "No rank assigned";

            // Log or save the permit details somewhere (for now, we'll send the player a message)
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("Permit saved with details:").formatted(Formatting.GREEN)).append("\n")
                    .append(Text.literal("Name: " + name).formatted(Formatting.YELLOW)).append("\n")
                    .append(Text.literal("Description: " + description).formatted(Formatting.YELLOW)).append("\n")
                    .append(Text.literal("Items: " + items).formatted(Formatting.YELLOW)).append("\n")
                    .append(Text.literal("Rank: " + rank).formatted(Formatting.YELLOW)), false);
        } else {
            player.sendMessage(Text.literal("[GylexMC] : ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("You must be holding a permit item to use this command!").formatted(Formatting.RED)), false);
        }

        return 1;
    }

    private static boolean isPermitItem(ItemStack block) {
        return block.isOf(ModItems.GOLD_PERMIT) || block.isOf(ModItems.IRON_PERMIT) || block.isOf(ModItems.DIAMOND_PERMIT);
    }

    private static Formatting getPermitColor(ItemStack permit) {
        if (permit.isOf(ModItems.GOLD_PERMIT)) {
            return Formatting.GOLD;
        } else if (permit.isOf(ModItems.DIAMOND_PERMIT)) {
            return Formatting.AQUA;
        } else if (permit.isOf(ModItems.IRON_PERMIT)) {
            return Formatting.GRAY;
        }

        return Formatting.WHITE;
    }

    private static Formatting getPermitColor(String rank) {
        return switch (rank) {
            case "Diamond" -> Formatting.AQUA;
            case "Gold" -> Formatting.GOLD;
            case "Iron" -> Formatting.GRAY;
            default -> Formatting.WHITE;
        };
    }
}
