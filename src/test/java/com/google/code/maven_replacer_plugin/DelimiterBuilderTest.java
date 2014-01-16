package com.google.code.maven_replacer_plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;


public class DelimiterBuilderTest {
	private static final String VALUE_WITHOUT_MIDDLE = "@";
	private static final String VALUE_WITH_MIDDLE_START = "${";
	private static final String VALUE_WITH_MIDDLE_END = "}";
	private static final String TOKEN = "token";

	@Test
	public void shouldReturnUnchangedTokenWhenNoValueGiven() {
		assertThat(new DelimiterBuilder(null).apply(TOKEN), equalTo(TOKEN));
		assertThat(new DelimiterBuilder("").apply(TOKEN), equalTo(TOKEN));
	}
	
	@Test
	public void shouldReturnTokenWithValueAtStartAndEndWhenNoMiddle() {
		String result = new DelimiterBuilder(VALUE_WITHOUT_MIDDLE).apply(TOKEN);
		assertThat(result, equalTo(VALUE_WITHOUT_MIDDLE + TOKEN + VALUE_WITHOUT_MIDDLE));
	}
	
	@Test
	public void shouldReturnTokenWithSplitValueAtStartAndEndWhenHasMiddleAsterix() {
		String result = new DelimiterBuilder(VALUE_WITH_MIDDLE_START + "*" + VALUE_WITH_MIDDLE_END).apply(TOKEN);
		assertThat(result, equalTo(VALUE_WITH_MIDDLE_START + TOKEN + VALUE_WITH_MIDDLE_END));
	}
	
	@Test
	public void shouldReturnEmptyOrNullIfTokenEmptyOrNull() {
		assertThat(new DelimiterBuilder(null).apply(""), equalTo(""));
		assertThat(new DelimiterBuilder(null).apply(null), equalTo(null));
	}
}
