package com.google.code.maven_replacer_plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

public class TokenReplacerTest {
	private static final int NO_FLAGS = -1;
	
	private Replacement replacement;
	private TokenReplacer replacer;

	@Before
	public void setUp() {
		replacement = mock(Replacement.class);
		when(replacement.getToken()).thenReturn("t.k.n");
		when(replacement.getValue()).thenReturn("value");
		replacer = new TokenReplacer();
	}

	@Test
	public void shouldReplaceNonRegexTokenWithValue() throws Exception {
		when(replacement.getToken()).thenReturn("$token$");
		String results = replacer.replace("some $token$", replacement, false, NO_FLAGS);
		assertThat(results, equalTo("some value"));
	}

	@Test
	public void shouldReplaceRegexTokenWithValue() throws Exception {
		String results = replacer.replace("some token", replacement, true, NO_FLAGS);
		assertThat(results, equalTo("some value"));
	}

	@Test
	public void shouldReplaceTokenWithEmptyValue() throws Exception {
		when(replacement.getValue()).thenReturn(null);
		String results = replacer.replace("some token", replacement, true, NO_FLAGS);
		assertThat(results, equalTo("some "));
	}

	@Test
	public void shouldReplaceTokenInMulipleLines() throws Exception {
		when(replacement.getValue()).thenReturn(null);
		String results = replacer.replace("some\ntoken", replacement, true, NO_FLAGS);
		assertThat(results, equalTo("some\n"));
	}
	
	@Test
	public void shouldReplaceTokenOnCompleteLine() throws Exception {
		when(replacement.getToken()).thenReturn("^replace=.*$");
		when(replacement.getValue()).thenReturn("replace=value");
		String results = replacer.replace("some\nreplace=token\nnext line", replacement, true, Pattern.MULTILINE);
		assertThat(results, equalTo("some\nreplace=value\nnext line"));
	}
	
	@Test
	public void shouldReplaceTokenWithCaseInsensitivity() throws Exception {
		when(replacement.getToken()).thenReturn("TEST");
		String results = replacer.replace("test", replacement, true, Pattern.CASE_INSENSITIVE);
		assertThat(results, equalTo("value"));
	}

	@Test
	public void shouldHandleEmptyContentsGracefully() {
		String results = replacer.replace("", replacement, true, NO_FLAGS);
		assertThat(results, equalTo(""));

		results = replacer.replace("", replacement, false, NO_FLAGS);
		assertThat(results, equalTo(""));
	}
	
	@Test
	public void shouldHandleEmptyValueForNonRegex() throws Exception {
		when(replacement.getToken()).thenReturn("token");
		when(replacement.getValue()).thenReturn(null);
		String results = replacer.replace("some token", replacement, false, NO_FLAGS);
		assertThat(results, equalTo("some "));
	}
	
	@Test
	public void shouldReplaceWithGroups() throws Exception {
		when(replacement.getToken()).thenReturn("test (.*) number");
		when(replacement.getValue()).thenReturn("group $1 replaced");
		String results = replacer.replace("test 123 number", replacement, true, NO_FLAGS);
		assertThat(results, equalTo("group 123 replaced"));
	}
}
