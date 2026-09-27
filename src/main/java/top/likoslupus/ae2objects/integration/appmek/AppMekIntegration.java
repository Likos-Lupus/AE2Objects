package top.likoslupus.ae2objects.integration.appmek;

import top.likoslupus.ae2objects.platform.IntegrationSet;

/**
 * Skeleton for the optional Applied Mekanistics (chemical) integration.
 *
 * <p>Applied Mekanistics has no Minecraft 26.1 build yet, so its chemical {@code AEKeyType}
 * ({@code appmek:chemical} / {@code MekanismKeyType.TYPE}) cannot be referenced. This bootstrap
 * reserves the seam: once appmek ships a 26.1 build, register the chemical channel and portable
 * menu here, and {@code CellRegistrationPlan} will then activate the chemical cells
 * automatically.</p>
 *
 * <p>No appmek or Mekanism class is referenced; the integration is gated purely by mod id.</p>
 */
public final class AppMekIntegration {

    private AppMekIntegration() {
    }

    public static void bootstrapRequired(IntegrationSet integrations) {
        if (!integrations.appliedMekanistics()) {
            return;
        }

        // TODO: when an appmek 26.1 build exists, register the chemical channel and portable menu:
        //   StorageChannelRegistry.INSTANCE.register(new ChemicalChannelBinding(
        //           CellContentType.CHEMICAL, <chemical AEKeyType>, <radioactive-aware accepts>));
        //   PortableMenuRegistry.INSTANCE.register(new PortableMenuBinding(
        //           CellContentType.CHEMICAL, appmek:portable_chemical_cell));
        // TODO: reject radioactive chemicals via Mekanism's ChemicalAttributeValidator.DEFAULT.
        // The chemical AEKeyType acquisition and the attribute check are the remaining inputs.
    }

}
