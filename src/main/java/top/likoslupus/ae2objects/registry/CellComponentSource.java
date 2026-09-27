package top.likoslupus.ae2objects.registry;

import net.minecraft.resources.Identifier;
import top.likoslupus.ae2objects.platform.IntegrationId;

import java.util.Optional;

/**
 * Where the storage component for a tier comes from.
 *
 * <p>Tier stays a pure numeric model; the component source is a separate concern that may depend
 * on
 * an optional mod.</p>
 */
public record CellComponentSource(
        Identifier itemId,
        Optional<IntegrationId> requiredMod
) {

}
