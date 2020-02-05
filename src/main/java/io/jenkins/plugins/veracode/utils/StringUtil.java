/*******************************************************************************
 * Copyright (c) 2014 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/

package io.jenkins.plugins.veracode.utils;

/**
 * A utility class for working with Strings.
 * <p>
 * Includes some methods with signatures similar to those exposed by .NET's String class.
 * </p>
 *
 *
 */
public final class StringUtil {
	/**
	 * <p>
	 * Returns the empty String "".
	 * </p>
	 */
	public static final String EMPTY = "";

	/**
	 * <p>
	 * Returns the new line String "\r\n".
	 * </p>
	 */
	public static final String NEWLINE = "\r\n";

	/**
	 * <p>
	 * Determines whether a string is null or empty.
	 * </p>
	 *
	 * @param input String
	 * @return true if the String is null or empty; false otherwise.
	 */
	public static boolean isNullOrEmpty(String input) {
		return input == null || EMPTY.equals(input);
	}

	/**
	 * <p>
	 * Returns null if the input String is empty. Otherwise the String.
	 * </p>
	 *
	 * @param input String
	 * @return String
	 */
	public static String getNullIfEmpty(String input) {
		return !EMPTY.equals(input) ? input : null;
	}

	/**
	 * <p>
	 * Returns the empty string if the input String is null. Otherwise the String.
	 * </p>
	 *
	 * @param input String
	 * @return String
	 */
	public static String getEmptyIfNull(String input) {
		return input != null ? input : EMPTY;
	}

	/**
	 * <p>
	 * Returns a string that contains the specified character count times.
	 * </p>
	 *
	 * @param character char
	 * @param count int
	 * @return String
	 */
	public static String repeatChar(char character, int count) {
		if (count < 0) {
			throw new IllegalArgumentException("Count must be non-negative");
		}
		char[] c = new char[count];
		for (int x = 0; x < count; x++) {
			c[x] = character;
		}
		return new String(c);
	}

	/**
	 * <p>
	 * Appends spaces to a String.
	 * </p>
	 *
	 * @param input String
	 * @param totalWidth int
	 * @return String
	 */
	public static String padRight(String input, int totalWidth) {
		if (totalWidth < 0) {
			throw new IllegalArgumentException("Total Width must be non-negative.");
		} else if (totalWidth == 0) {
			return input;
		} else {
			String format = String.format("%%%ds", -totalWidth);
			return String.format(format, input);
		}
	}

	/**
	 * <p>
	 * Prepends spaces to a String.
	 * </p>
	 *
	 * @param input String
	 * @param totalWidth int
	 * @return String
	 */
	public static String padLeft(String input, int totalWidth) {
		if (totalWidth < 0) {
			throw new IllegalArgumentException("Total Width must be non-negative.");
		} else if (totalWidth == 0) {
			return input;
		} else {
			String format = String.format("%%%ds", totalWidth);
			return String.format(format, input);
		}
	}

	/**
	 * <p>
	 * Compares two Strings.
	 * </p>
	 *
	 * <pre>
	 * StringUtil.compare(null, null) = 0
	 * StringUtil.compare(null, "...") = -1
	 * StringUtil.compare("...", null) = 1
	 * </pre>
	 *
	 * @param str1 String
	 * @param str2 String
	 * @param ignoreCase boolean
	 * @return 1
	 */
	public static int compare(String str1, String str2, boolean ignoreCase) {
		int result = 0;
		if (str1 != null) {
			if (str2 != null) {
				if (ignoreCase) {
					result = str1.compareToIgnoreCase(str2);
				} else {
					result = str1.compareTo(str2);
				}
			} else {
				result = 1;
			}
		} else {
			if (str2 != null) {
				result = -1;
			}
		}
		return result;
	}

	/**
	 * <p>
	 * Joins the elements of a {@code String[]} using the specified separator.
	 * </p>
	 *
	 * @param separator String
	 * @param value String[]
	 * @return the joined string
	 */
	public static String join(String separator, String[] value) {
		if (value != null) {
			if (separator == null) {
				separator = EMPTY;
			}
			boolean addSeparator = false;
			StringBuilder builder = new StringBuilder();
			for (String s : value) {
				if (addSeparator) {
					builder.append(separator);
				}
				if (s != null) {
					builder.append(s);
				}
				addSeparator = true;
			}
			return builder.toString();
		} else {
			throw new IllegalArgumentException("Attempted to join the elements of a null String array.");
		}
	}

	private StringUtil() {
	}
}