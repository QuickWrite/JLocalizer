package net.quickwrite.localizer;

import net.quickwrite.localizer.processor.PluralRuleGen;

/**
 * A list of all of the plural categories
 * defined by unicode.org.
 *
 * <p>
 * English for example only has two plural categories with cardinal numbers:
 *     <ul>
 *         <li>ONE</li>
 *         <li>OTHER</li>
 *     </ul>
 * <p>
 * but Slovenian for example has four different categories with cardinal numbers:
 *     <ul>
 *         <li>ONE</li>
 *         <li>TWO</li>
 *         <li>FEW</li>
 *         <li>OTHER</li>
 *     </ul>
 * </p>
 */
public enum PluralCategory {
    ZERO,
    ONE,
    TWO,
    FEW,
    MANY,
    OTHER
}
