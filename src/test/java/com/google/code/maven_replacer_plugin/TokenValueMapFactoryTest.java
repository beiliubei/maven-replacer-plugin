package com.google.code.maven_replacer_plugin;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.code.maven_replacer_plugin.file.FileUtils;

@RunWith(MockitoJUnitRunner.class)
public class TokenValueMapFactoryTest {
	private static final String FILENAME = "some file";
	private static final boolean COMMENTS_ENABLED = true;
	private static final boolean COMMENTS_DISABLED = false;
	private static final String ENCODING = "encoding";

	@Mock
	private FileUtils fileUtils;

	private TokenValueMapFactory factory;

	@Before
	public void setUp() {
		factory = new TokenValueMapFactory(fileUtils);
	}
	
	@Test
	public void shouldReturnReplacementsFromFile() throws Exception {
		when(fileUtils.readFile(FILENAME, ENCODING)).thenReturn("token=value");
		
		List<Replacement> replacements = factory.replacementsForFile(FILENAME, COMMENTS_DISABLED, false, ENCODING);
		assertThat(replacements, notNullValue());
		assertThat(replacements.size(), is(1));
		assertThat(replacements.get(0).getToken(), equalTo("token"));
		assertThat(replacements.get(0).getValue(), equalTo("value"));
	}

	@Test
	public void shouldReturnReplacementsFromFileAndIgnoreBlankLinesAndComments() throws Exception {
		when(fileUtils.readFile(FILENAME, ENCODING)).thenReturn("\n  \ntoken1=value1\ntoken2 = value2\n#some comment\n");
		
		List<Replacement> replacements = factory.replacementsForFile(FILENAME, COMMENTS_ENABLED, false, ENCODING);
		assertThat(replacements, notNullValue());
		assertThat(replacements.size(), is(2));
		assertThat(replacements.get(0).getToken(), equalTo("token1"));
		assertThat(replacements.get(0).getValue(), equalTo("value1"));
		assertThat(replacements.get(1).getToken(), equalTo("token2"));
		assertThat(replacements.get(1).getValue(), equalTo("value2"));
	}
	
	@Test
	public void shouldReturnReplacementsFromFileAndIgnoreBlankLinesUsingCommentLinesIfCommentsDisabled() throws Exception {
		when(fileUtils.readFile(FILENAME, ENCODING)).thenReturn("\n  \ntoken1=value1\ntoken2=value2\n#some=#comment\n");
		
		List<Replacement> replacements = factory.replacementsForFile(FILENAME, COMMENTS_DISABLED, false, ENCODING);
		assertThat(replacements, notNullValue());
		assertThat(replacements.size(), is(3));
		assertThat(replacements.get(0).getToken(), equalTo("token1"));
		assertThat(replacements.get(0).getValue(), equalTo("value1"));
		assertThat(replacements.get(1).getToken(), equalTo("token2"));
		assertThat(replacements.get(1).getValue(), equalTo("value2"));
		assertThat(replacements.get(2).getToken(), equalTo("#some"));
		assertThat(replacements.get(2).getValue(), equalTo("#comment"));
	}
	
	@Test
	public void shouldIgnoreTokensWithNoSeparatedValue() throws Exception {
		when(fileUtils.readFile(FILENAME, ENCODING)).thenReturn("#comment\ntoken2");
		List<Replacement> replacements = factory.replacementsForFile(FILENAME, COMMENTS_DISABLED, false, ENCODING);
		assertThat(replacements, notNullValue());
		assertTrue(replacements.isEmpty());
	}
	
	@Test
	public void shouldReturnRegexReplacementsFromFile() throws Exception {
		when(fileUtils.readFile(FILENAME, ENCODING)).thenReturn("\\=tok\\=en1=val\\=ue1\nto$ke..n2=value2");
		
		List<Replacement> replacements = factory.replacementsForFile(FILENAME, COMMENTS_ENABLED, false, ENCODING);
		assertThat(replacements, notNullValue());
		assertThat(replacements.size(), is(2));
		assertThat(replacements.get(0).getToken(), equalTo("\\=tok\\=en1"));
		assertThat(replacements.get(0).getValue(), equalTo("val\\=ue1"));
		assertThat(replacements.get(1).getToken(), equalTo("to$ke..n2"));
		assertThat(replacements.get(1).getValue(), equalTo("value2"));
	}
	
	@Test
	public void shouldReturnRegexReplacementsFromFileUnescaping() throws Exception {
		when(fileUtils.readFile(FILENAME, ENCODING)).thenReturn("\\\\=tok\\\\=en1=val\\\\=ue1\nto$ke..n2=value2");
		
		List<Replacement> replacements = factory.replacementsForFile(FILENAME, COMMENTS_ENABLED, true, ENCODING);
		assertThat(replacements, notNullValue());
		assertThat(replacements.size(), is(2));
		assertThat(replacements.get(0).getToken(), equalTo("\\=tok\\=en1"));
		assertThat(replacements.get(0).getValue(), equalTo("val\\=ue1"));
		assertThat(replacements.get(1).getToken(), equalTo("to$ke..n2"));
		assertThat(replacements.get(1).getValue(), equalTo("value2"));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfNoTokenForValue() throws Exception {
		when(fileUtils.readFile(FILENAME, ENCODING)).thenReturn("=value");
		factory.replacementsForFile(FILENAME, COMMENTS_DISABLED, false, ENCODING);
	}
	
	@Test
	public void shouldSupportEmptyFileAndReturnNoReplacements() throws Exception {
		when(fileUtils.readFile(FILENAME, ENCODING)).thenReturn("");
		List<Replacement> replacements = factory.replacementsForFile(FILENAME, COMMENTS_DISABLED, false, ENCODING);
		assertThat(replacements, notNullValue());
		assertTrue(replacements.isEmpty());
	}
	
	@Test
	public void shouldReturnListOfReplacementsFromVariable() {
		List<Replacement> replacements = factory.replacementsForVariable("#comment,token1=value1,token2=value2"
				, true, false, ENCODING);
		assertThat(replacements, notNullValue());
		assertThat(replacements.size(), is(2));
		assertThat(replacements, hasItem(replacementWith("token1", "value1")));
		assertThat(replacements, hasItem(replacementWith("token2", "value2")));
	}
	
	
	@Test
	public void shouldReturnListOfReplacementsFromSingleVariable() {
		List<Replacement> replacements = factory.replacementsForVariable("token1=value1", true, false, ENCODING);
		assertThat(replacements, notNullValue());
		assertThat(replacements.size(), is(1));
		assertThat(replacements, hasItem(replacementWith("token1", "value1")));
	}

	private Matcher<Replacement> replacementWith(final String token, final String value) {
		return new BaseMatcher<Replacement>() {
			public boolean matches(Object o) {
				Replacement replacement = (Replacement)o;
				return token.equals(replacement.getToken()) && value.equals(replacement.getValue());
			}

			public void describeTo(Description desc) {
				desc.appendText("token=" + token + ", value=" + value);
			}
		};
	}
}
