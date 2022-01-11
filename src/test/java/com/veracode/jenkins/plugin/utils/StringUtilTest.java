package com.veracode.jenkins.plugin.utils;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilTest {

	@Test
	public void testIsNullOrEmpty() {
		boolean nullString = StringUtil.isNullOrEmpty(null);
		Assert.assertTrue("Provided value is not null", nullString);

		boolean emptyString = StringUtil.isNullOrEmpty("");
		Assert.assertTrue("Provided value is not empty", emptyString);

		boolean validString = StringUtil.isNullOrEmpty("abcd");
		Assert.assertFalse("Provided value is not empty", validString);
	}

	@Test
	public void testGetNullIfEmpty() {
		String emptyString = StringUtil.getNullIfEmpty("");
		Assert.assertNull("Expected output is not null", emptyString);

		String nullString = StringUtil.getNullIfEmpty(null);
		Assert.assertNull("Expected output is not null", nullString);
	}

	@Test
	public void testGetEmptyIfNull() {
		String emptyString = StringUtil.getEmptyIfNull("");
		Assert.assertEquals("Expected output is not empty", "", emptyString);

		String nullString = StringUtil.getEmptyIfNull(null);
		Assert.assertEquals("Expected output is not empty", "", nullString);
	}

	@Test
	public void testRepeatChar() {
		String string = StringUtil.repeatChar('a', 5);
		Assert.assertEquals("Expected output is not aaaaa", "aaaaa", string);

		String spaceString = StringUtil.repeatChar(' ', 5);
		Assert.assertEquals("Expected output is not space", "     ", spaceString);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRepeatCharWithException() {
		StringUtil.repeatChar('a', -1);
	}

	@Test
	public void testPadRight() {
		String padString = StringUtil.padRight("padRight", 10);
		Assert.assertEquals("Issue with the padRight functionality", "padRight  ", padString);

		String string = StringUtil.padRight("test", 0);
		Assert.assertEquals("Issue with the padRight functionality", "test", string);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPadRightWithException() throws Exception {
		StringUtil.padRight("test", -1);
	}

	@Test
	public void testPadLeft() {
		String padString = StringUtil.padLeft("test", 6);
		Assert.assertEquals("Issue with the padLeft functionality", "  test", padString);

		String string = StringUtil.padLeft("test", 0);
		Assert.assertEquals("Issue with the padLeft functionality", "test", string);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPadLeftWithException() {
		StringUtil.padLeft("test", -1);
	}

	@Test
	public void testCompare() {
		int nullValues = StringUtil.compare(null, null, true);
		Assert.assertEquals("Issue with the compare functionality", 0, nullValues);

		int only2ndValuesNull = StringUtil.compare("test", null, true);
		Assert.assertEquals("Issue with the compare functionality", 1, only2ndValuesNull);

		int equalValuesWithIgnoreCase = StringUtil.compare("test", "test", true);
		Assert.assertEquals("Issue with the compare functionality", 0, equalValuesWithIgnoreCase);

		int onlyFirstValueIsNull = StringUtil.compare(null, "test", true);
		Assert.assertEquals("Issue with the compare functionality", -1, onlyFirstValueIsNull);

		int equalValuesWithOutIgnoreCase = StringUtil.compare("test", "test", false);
		Assert.assertEquals("Issue with the compare functionality", 0, equalValuesWithOutIgnoreCase);

		int notEqualValues = StringUtil.compare("test", "run", false);
		Assert.assertEquals("Issue with the compare functionality", 2, notEqualValues);
	}

	@Test
	public void testJoin() {
		String joinString = StringUtil.join("/", new String[] { "test1", "test2", "test3" });
		Assert.assertEquals("Issue with the join functionality", "test1/test2/test3", joinString);

		String nullSeparator = StringUtil.join(null, new String[] { "test1", "test2", "test3" });
		Assert.assertEquals("Issue with the join functionality", "test1test2test3", nullSeparator);

		String nullStringAndNullSeparator = StringUtil.join(null, new String[] { null, null });
		Assert.assertEquals("Issue with the join functionality", "", nullStringAndNullSeparator);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJoinWithException() {
		StringUtil.join("/", null);
	}
}