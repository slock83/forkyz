
package app.crossword.yourealwaysbe.util;

import java.util.Comparator;

/**
 * Compare two clue numbers
 *
 * Try to stick to natural ordering of integers, even when clue numbers are a
 * bit more sophisticated. E.g. "16/21", treat as number 16.
 *
 * Fall back to standard string comparison
 */
public class ClueNumberComparator implements Comparator<String> {
    public int compare(String num1, String num2) {
        if (num1 == null && num2 == null)
            return 0;
        if (num1 == null)
            return -1;
        if (num2 == null)
            return 1;

        Integer digits1 = getLeadingDigits(num1);
        if (digits1 == null)
            return num1.compareTo(num2);

        Integer digits2 = getLeadingDigits(num2);
        if (digits2 == null)
            return num1.compareTo(num2);

        return digits1.compareTo(digits2);
    }

    /**
     * Extract first digits from string, null if does not start with digits
     */
    private Integer getLeadingDigits(String num) {
        if (num == null || num.isEmpty())
            return null;

        int digitsEnd = 0;
        while (
            digitsEnd < num.length() && Character.isDigit(num.charAt(digitsEnd))
        ) {
            digitsEnd++;
        }

        if (digitsEnd == 0)
            return null;
        else if (digitsEnd == num.length())
            return Integer.valueOf(num);
        else
            return Integer.valueOf(num.substring(0, digitsEnd));
    }
}
