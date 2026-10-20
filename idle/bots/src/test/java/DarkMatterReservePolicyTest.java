import static org.junit.jupiter.api.Assertions.*;
import idle.core.math.BigNum;
import org.junit.jupiter.api.Test;

class DarkMatterReservePolicyTest {
    @Test void neConfondPasMatiereGagneeEtReserveDisponible() {
        BigNum threshold = BigNum.of(500);
        assertFalse(DarkMatterReservePolicy.allowPurchase(true, BigNum.of(470), BigNum.of(1), threshold));
        assertFalse(DarkMatterReservePolicy.allowPurchase(true, BigNum.of(505), BigNum.of(6), threshold));
        assertTrue(DarkMatterReservePolicy.allowPurchase(true, BigNum.of(505), BigNum.of(5), threshold));
        assertTrue(DarkMatterReservePolicy.allowPurchase(false, BigNum.of(470), BigNum.of(20), threshold));
    }
}
