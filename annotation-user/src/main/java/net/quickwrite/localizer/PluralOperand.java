package net.quickwrite.localizer;

/**
 * Plural operands defined by
 * <a href="https://unicode.org/reports/tr35/tr35-numbers.html#Plural_Operand_Meanings">
 * https://unicode.org/reports/tr35/tr35-numbers.html#Plural_Operand_Meanings
 * </a>.
 *
 * <p>
 *     When {@code N} is the number that is stored the following values of the number are defined in this
 *     record like this:
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
    // TODO: Add methods that convert {@link Number} to {@link PluralOperand}.

    // TODO: Skip this value later
    public int e() {
        return 0;
    }
}
