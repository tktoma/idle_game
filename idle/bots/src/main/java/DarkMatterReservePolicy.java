import idle.core.math.BigNum;

/** Décision pure (testable) : pendant la préparation du Big Bang, protéger les 500 DM DISPO. */
public final class DarkMatterReservePolicy {
    private DarkMatterReservePolicy() {}

    public static boolean allowPurchase(boolean savingForBigBang, BigNum held, BigNum price, BigNum required) {
        if (held == null || price == null || required == null) throw new IllegalArgumentException("null");
        if (!savingForBigBang) return true;
        // Un achat ne doit jamais réduire la réserve disponible sous la condition réelle du moteur.
        return held.subtract(price).gte(required);
    }
}
