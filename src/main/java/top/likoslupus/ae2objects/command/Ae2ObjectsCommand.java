package top.likoslupus.ae2objects.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.likoslupus.ae2objects.Ae2Objects;
import top.likoslupus.ae2objects.cell.channel.StorageChannelRegistry;
import top.likoslupus.ae2objects.cell.item.DeepCellDefinitionProvider;
import top.likoslupus.ae2objects.cell.model.CellTier;
import top.likoslupus.ae2objects.cell.persistence.CellContentsCodec;
import top.likoslupus.ae2objects.cell.persistence.CellRecord;
import top.likoslupus.ae2objects.cell.stack.CellStackData;
import top.likoslupus.ae2objects.platform.ServerCellContext;
import top.likoslupus.ae2objects.registry.Ae2ObjectsDataComponents;
import top.likoslupus.ae2objects.registry.Ae2ObjectsItems;

import java.util.UUID;

public final class Ae2ObjectsCommand {

    private Ae2ObjectsCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal(Ae2Objects.MOD_ID)
                .executes(Ae2ObjectsCommand::help)
                .then(Commands.literal("recover")
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> recover(
                                        context,
                                        context.getArgument("uuid", UUID.class)
                                ))
                        )
                )
                .then(Commands.literal("getuuid")
                        .executes(Ae2ObjectsCommand::getUuid)
                );

        event.getDispatcher().register(root);
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.literal("/ae2objects recover <UUID> - Recover a deep storage cell."),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("/ae2objects getuuid - Copy the held deep cell UUID."),
                false
        );
        return 1;
    }

    private static int recover(
            CommandContext<CommandSourceStack> context,
            UUID uuid
    ) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var serverContext = ServerCellContext.getOrNull();
        if (serverContext == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.ae2objects.recover_fail", uuid)
            );
            return 0;
        }

        var record = serverContext.repository().find(uuid).orElse(null);
        if (record == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.ae2objects.recover_fail", uuid)
            );
            return 0;
        }

        var recoveredItem = resolveRecoveredItem(record);
        var stack = new ItemStack(recoveredItem);
        var itemId = BuiltInRegistries.ITEM.getKey(recoveredItem).toString();
        var associated = record.withCellItemIdIfMissing(itemId);
        if (associated != record) {
            serverContext.repository().put(uuid, associated);
        }

        stack.set(Ae2ObjectsDataComponents.CELL_ID.get(), uuid);
        CellStackData.updateSummary(
                stack,
                associated.storedAmount(),
                associated.storedTypesCount()
        );
        if (recoveredItem instanceof DeepCellDefinitionProvider provider) {
            var decoded = CellContentsCodec.decode(
                    associated,
                    serverContext.registries(),
                    StorageChannelRegistry.INSTANCE.require(provider.definition().type()).keyType()
            );
            CellStackData.updatePreview(
                    stack,
                    CellContentsCodec.preview(decoded.contents())
            );
        }
        player.addItem(stack);

        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.ae2objects.recover_success",
                        player.getDisplayName(),
                        uuid
                ),
                true
        );
        return 1;
    }

    private static int getUuid(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        var stack = context.getSource().getPlayerOrException().getMainHandItem();
        if (!(stack.getItem() instanceof DeepCellDefinitionProvider)) {
            context.getSource().sendFailure(
                    Component.translatable("command.ae2objects.getuuid_fail_notcell")
            );
            return 0;
        }

        var cellId = CellStackData.cellId(stack);
        if (cellId == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.ae2objects.getuuid_fail_nouuid")
            );
            return 0;
        }

        var text = copyToClipboard(cellId.toString());
        context.getSource().sendSuccess(
                () -> Component.translatable("command.ae2objects.getuuid_success", text),
                false
        );
        return 1;
    }

    private static Item resolveRecoveredItem(CellRecord record) {
        return record.cellItemId()
                .map(Identifier::tryParse)
                .map(BuiltInRegistries.ITEM::getValue)
                .filter(item -> item instanceof DeepCellDefinitionProvider)
                .orElseGet(() -> Ae2ObjectsItems.itemStorageCell(CellTier.K256).item().get());
    }

    private static Component copyToClipboard(String value) {
        return Component.literal(value).withStyle(style -> style
                .withClickEvent(new ClickEvent.CopyToClipboard(value))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.copy.click")))
                .withInsertion(value)
                .withColor(ChatFormatting.GREEN));
    }

}
