package top.likoslupus.ae2objects.platform;

/** Optional mod integrations that can change content activation or recipes. */
public enum IntegrationId {

    MEGA_CELLS("megacells"),
    APPLIED_MEKANISTICS("appmek");

    private final String modId;

    IntegrationId(String modId) {
        this.modId = modId;
    }

    public String modId() {
        return modId;
    }

}
