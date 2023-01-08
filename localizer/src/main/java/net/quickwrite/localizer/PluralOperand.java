package net.quickwrite.localizer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Plural operands defined by
 * <a href="https://unicode.org/reports/tr35/tr35-numbers.html#Plural_Operand_Meanings">
 * https://unicode.org/reports/tr35/tr35-numbers.html#Plural_Operand_Meanings
 * </a>.
 *
 * <p>
 * When {@code N} is the number that is stored the following values of the number are defined in this
 * record like this:
 * <table>
 *     <thead>
 *         <tr>
 *             <th>Symbol</th>
 *             <th>Value</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td>n</td>
 *             <td>the absolute value of N.</td>
 *         </tr>
 *         <tr>
 *             <td>i</td>
 *             <td>the integer digits of N.</td>
 *         </tr>
 *         <tr>
 *             <td>v</td>
 *             <td>the number of visible fraction digits in N, <em>with</em> trailing zeros.</td>
 *         </tr>
 *         <tr>
 *             <td>w</td>
 *             <td>the number of visible fraction digits in N, <em>without</em> trailing zeros.</td>
 *         </tr>
 *         <tr>
 *             <td>f</td>
 *             <td>the visible fraction digits in N, <em>with</em> trailing zeros, expressed as an integer.</td>
 *         </tr>
 *         <tr>
 *             <td>t</td>
 *             <td>the visible fraction digits in N, <em>without</em> trailing zeros, expressed as an integer.</td>
 *         </tr>
 *     </tbody>
 * </table>
 * </p>
 * <br />
 * <hr />
 * <br />
 * <p>
 *     <strong>Examples:</strong>
 *     <table>
 *     <thead>
 *         <tr>
 *             <th align="right">source</th>
 *             <th align="right">n</th>
 *             <th align="right">i</th>
 *             <th align="right">v</th>
 *             <th align="right">w</th>
 *             <th align="right">f</th>
 *             <th align="right">t</th>
 *         </tr>
 *     </thead>
 *     <tbody>
 *         <tr>
 *             <td align="right">1</td>
 *             <td align="right">1</td>
 *             <td align="right">1</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1.0</td>
 *             <td align="right">1</td>
 *             <td align="right">1</td>
 *             <td align="right">1</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1.00</td>
 *             <td align="right">1</td>
 *             <td align="right">1</td>
 *             <td align="right">2</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1.3</td>
 *             <td align="right">1.3</td>
 *             <td align="right">1</td>
 *             <td align="right">1</td>
 *             <td align="right">1</td>
 *             <td align="right">3</td>
 *             <td align="right">3</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1.30</td>
 *             <td align="right">1.3</td>
 *             <td align="right">1</td>
 *             <td align="right">2</td>
 *             <td align="right">1</td>
 *             <td align="right">30</td>
 *             <td align="right">3</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1.03</td>
 *             <td align="right">1.03</td>
 *             <td align="right">1</td>
 *             <td align="right">2</td>
 *             <td align="right">2</td>
 *             <td align="right">3</td>
 *             <td align="right">3</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1.230</td>
 *             <td align="right">1.23</td>
 *             <td align="right">1</td>
 *             <td align="right">3</td>
 *             <td align="right">2</td>
 *             <td align="right">230</td>
 *             <td align="right">23</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1200000</td>
 *             <td align="right">1200000</td>
 *             <td align="right">1200000</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1.2c6</td>
 *             <td align="right">1200000</td>
 *             <td align="right">1200000</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *         </tr>
 *         <tr>
 *             <td align="right">123c6</td>
 *             <td align="right">123000000</td>
 *             <td align="right">123000000</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *         </tr>
 *         <tr>
 *             <td align="right">123c5</td>
 *             <td align="right">12300000</td>
 *             <td align="right">12300000</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *             <td align="right">0</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1200.50</td>
 *             <td align="right">1200.5</td>
 *             <td align="right">1200</td>
 *             <td align="right">2</td>
 *             <td align="right">1</td>
 *             <td align="right">50</td>
 *             <td align="right">5</td>
 *         </tr>
 *         <tr>
 *             <td align="right">1.20050c3</td>
 *             <td align="right">1200.5</td>
 *             <td align="right">1200</td>
 *             <td align="right">2</td>
 *             <td align="right">1</td>
 *             <td align="right">50</td>
 *             <td align="right">5</td>
 *         </tr>
 *     </tbody>
 * </table>
 * </p>
 */
public record PluralOperand(double n, long i, int v, int w, long f, long t) {
    public static PluralOperand from(final int value) {
        return new PluralOperand(value, value, 0, 0, 0, 0);
    }

    public static PluralOperand from(final long value) {
        return new PluralOperand(value, value, 0, 0, 0, 0);
    }

    public static PluralOperand from(final double value) {
        return from(Double.toString(value));
    }

    public static PluralOperand from(final BigDecimal value) {
        final BigDecimal noTrailingZeros = value.stripTrailingZeros();
        final long nonTrailingZeroFraction = noTrailingZeros
                .remainder(BigDecimal.ONE)
                .abs()
                .movePointRight(noTrailingZeros.scale())
                .longValue();

        return new PluralOperand(
                value.doubleValue(),
                value.longValue(),
                value.scale(),
                noTrailingZeros.scale(),
                nonTrailingZeroFraction * powerN(10, value.scale() - noTrailingZeros.scale()),
                nonTrailingZeroFraction
        );
    }

    public static PluralOperand from(final BigInteger value) {
        return new PluralOperand(value.doubleValue(), value.longValue(), 0, 0, 0, 0);
    }

    public static PluralOperand from(final String value) {
        return from(new BigDecimal(value));
    }

    // TODO: Skip this value later
    public int e() {
        return 0;
    }

    private static long powerN(final long number, int power){
        long res = 1;
        long sq = number;
        while(power > 0){
            if(power % 2 == 1){
                res *= sq;
            }
            sq = sq * sq;
            power /= 2;
        }
        return res;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluralOperand operand = (PluralOperand) o;
        return Double.compare(operand.n, n) == 0 && i == operand.i && v == operand.v && w == operand.w && f == operand.f && t == operand.t;
    }

    @Override
    public int hashCode() {
        return Objects.hash(n, i, v, w, f, t);
    }

    @Override
    public String toString() {
        return "PluralOperand{" +
                "n=" + n +
                ", i=" + i +
                ", v=" + v +
                ", w=" + w +
                ", f=" + f +
                ", t=" + t +
                '}';
    }
}
