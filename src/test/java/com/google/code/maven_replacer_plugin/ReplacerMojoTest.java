package com.google.code.maven_replacer_plugin;


import static java.util.Arrays.asList;
import static junit.framework.Assert.assertSame;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.code.maven_replacer_plugin.file.FileUtils;
import com.google.code.maven_replacer_plugin.include.FileSelector;

@RunWith(MockitoJUnitRunner.class)
public class ReplacerMojoTest {

	private static final String ENCODING = "encoding";
	private static final String XPATH = "xpath";
	private static final String REGEX_FLAG = "regex flag";
	private static final String FILE = "file";
	private static final boolean REGEX = true;
	private static final String OUTPUT_FILE = "output file";
	private static final int REGEX_PATTERN_FLAGS = 999;
	private static final String BASE_DIR = "base dir";
	private static final String TOKEN_VALUE_MAP = "token value map";
	private static final String TOKEN_FILE = "token file";
	private static final String VALUE_FILE = "value file";
	private static final String TOKEN = "token";
	private static final String VALUE = "value";
	private static final String NO_ENCODING_SET = null;

	@Mock
	private FileUtils fileUtils;
	@Mock
	private ReplacementProcessor processor;
	@Mock
	private ReplacerFactory replacerFactory;
	@Mock
	private TokenValueMapFactory tokenValueMapFactory;
	@Mock
	private FileSelector fileSelector;
	@Mock
	private PatternFlagsFactory patternFlagsFactory;
	@Mock
	private Log log;
	@Mock
	private OutputFilenameBuilder outputFilenameBuilder;
	@Mock
	private SummaryBuilder summaryBuilder;
	
	private List<String> regexFlags;
	private ReplacerMojo mojo;

	@Before
	public void setUp() throws Exception {
		regexFlags = asList(REGEX_FLAG);
		when(patternFlagsFactory.buildFlags(regexFlags)).thenReturn(REGEX_PATTERN_FLAGS);

		mojo = new ReplacerMojo(fileUtils, processor, replacerFactory, tokenValueMapFactory,
				fileSelector, patternFlagsFactory, outputFilenameBuilder, summaryBuilder) {
			@Override
			public Log getLog() {
				return log;
			}
		};
		when(outputFilenameBuilder.buildFrom(FILE, mojo)).thenReturn(OUTPUT_FILE);
	}

	@Test
	public void shouldReplaceContentsInReplacements() throws Exception {
		Replacement replacement = mock(Replacement.class);
		List<Replacement> replacements = asList(replacement);

		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setReplacements(replacements);
		mojo.setFile(FILE);
		mojo.setIgnoreMissingFile(true);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.setEncoding(ENCODING);
		mojo.execute();
		
		assertSame(FILE, mojo.getFile());
		verify(processor).replace(replacements, REGEX, BASE_DIR + File.separator + FILE, 
				OUTPUT_FILE, REGEX_PATTERN_FLAGS, ENCODING);
		verify(summaryBuilder).add(BASE_DIR + File.separator + FILE, OUTPUT_FILE, ENCODING, log);
		verify(summaryBuilder).print(log);
	}
	
	@Test
	public void shouldSkipAndDoNothing() throws Exception {
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setFile(FILE);
		mojo.setSkip(true);
		mojo.execute();
		
		verifyZeroInteractions(processor);
		verifyZeroInteractions(summaryBuilder);
	}
	
	@Test
	public void shouldIgnoreBaseDirWhenFileIsAbsolutePathed() throws Exception {
		Replacement replacement = mock(Replacement.class);
		List<Replacement> replacements = asList(replacement);

		when(fileUtils.isAbsolutePath(FILE)).thenReturn(true);
		mojo.setReplacements(replacements);
		mojo.setFile(FILE);
		mojo.execute();
		verify(processor).replace(replacements, REGEX, FILE, OUTPUT_FILE, 0, null);
		verify(summaryBuilder).add(FILE, OUTPUT_FILE, null, log);
		verify(summaryBuilder).print(log);
	}

    @Test
    public void shouldLimitReplacementsToMaxReplacements() throws Exception {
        Replacement replacement1 = mock(Replacement.class);
        Replacement replacement2 = mock(Replacement.class);
        List<Replacement> replacements = asList(replacement1, replacement2);

        when(fileUtils.isAbsolutePath(FILE)).thenReturn(true);
        mojo.setReplacements(replacements);
        mojo.setMaxReplacements(1);
        mojo.setFile(FILE);
        mojo.execute();
        verify(processor).replace(asList(replacement1), REGEX, FILE, OUTPUT_FILE, 0, null);
        verify(summaryBuilder).add(FILE, OUTPUT_FILE, null, log);
        verify(summaryBuilder).print(log);
    }

	@Test
	public void shouldReplaceContentsInLocalFile() throws Exception {
		Replacement replacement = mock(Replacement.class);
		List<Replacement> replacements = asList(replacement);

		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setReplacements(replacements);
		mojo.setFile(FILE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(null);
		mojo.execute();
		
		assertSame(FILE, mojo.getFile());
		verify(processor).replace(replacements, REGEX, FILE, OUTPUT_FILE, REGEX_PATTERN_FLAGS, NO_ENCODING_SET);
		verify(summaryBuilder).add(FILE, OUTPUT_FILE, NO_ENCODING_SET, log);
		verify(summaryBuilder).print(log);
	}
	
	@Test
	public void shouldReplaceContentsInReplacementsButNotPrintSummaryIfQuiet() throws Exception {
		Replacement replacement = mock(Replacement.class);
		List<Replacement> replacements = asList(replacement);

		mojo.setQuiet(true);
		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setReplacements(replacements);
		mojo.setFile(FILE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.execute();

		verify(processor).replace(replacements, REGEX, BASE_DIR + File.separator + FILE, 
				OUTPUT_FILE, REGEX_PATTERN_FLAGS, NO_ENCODING_SET);
		verify(summaryBuilder).add(BASE_DIR + File.separator + FILE, OUTPUT_FILE, NO_ENCODING_SET, log);
		verify(summaryBuilder, never()).print(log);
	}

	@Test
	public void shouldReplaceContentsInIncludeAndExcludes() throws Exception {
		List<String> includes = asList("include");
		List<String> excludes = asList("exclude");
		when(fileSelector.listIncludes(BASE_DIR, includes, excludes)).thenReturn(asList(FILE));

		mojo.setIncludes(includes);
		mojo.setExcludes(excludes);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setBasedir(BASE_DIR);
		mojo.execute();

		assertSame(mojo.getIncludes(), includes);
		assertSame(mojo.getExcludes(), excludes);
		verify(processor).replace(argThat(replacementOf(null, VALUE, false, TOKEN)), eq(REGEX), eq(BASE_DIR  + File.separator + FILE),
			eq(OUTPUT_FILE), anyInt(), eq(NO_ENCODING_SET));
	}

	@Test
	public void shouldReplaceContentsInFilesToIncludeAndExclude() throws Exception {
		String includes = "include1, include2";
		String excludes = "exclude1, exclude2";
		when(fileSelector.listIncludes(BASE_DIR, asList("include1", "include2"), asList("exclude1", "exclude2"))).thenReturn(asList(FILE));

		mojo.setFilesToInclude(includes);
		mojo.setFilesToExclude(excludes);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setBasedir(BASE_DIR);
		mojo.execute();

		assertSame(mojo.getFilesToInclude(), includes);
		assertSame(mojo.getFilesToExclude(), excludes);
		verify(processor).replace(argThat(replacementOf(null, VALUE, false, TOKEN)), eq(REGEX), eq(BASE_DIR + File.separator + FILE),
			eq(OUTPUT_FILE), anyInt(), eq(NO_ENCODING_SET));
	}

	@Test
	public void shouldReplaceContentsWithTokenValuesInMapWithComments() throws Exception {
		Replacement replacement = mock(Replacement.class);
		List<Replacement> replacements = asList(replacement);

		when(tokenValueMapFactory.replacementsForFile(BASE_DIR  + File.separator + TOKEN_VALUE_MAP, 
				true, false, NO_ENCODING_SET)).thenReturn(replacements);

		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setTokenValueMap(TOKEN_VALUE_MAP);
		mojo.setFile(FILE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.execute();

		verify(processor).replace(replacements, 
				REGEX, BASE_DIR  + File.separator + FILE, OUTPUT_FILE, REGEX_PATTERN_FLAGS, NO_ENCODING_SET);
	}

	@Test
	public void shouldReplaceContentsWithTokenValuesInMapWithoutComments() throws Exception {
		Replacement replacement = mock(Replacement.class);
		List<Replacement> replacements = asList(replacement);

		when(tokenValueMapFactory.replacementsForFile(BASE_DIR  + File.separator + TOKEN_VALUE_MAP, 
				false, false, ENCODING)).thenReturn(replacements);

		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setTokenValueMap(TOKEN_VALUE_MAP);
		mojo.setFile(FILE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.setCommentsEnabled(false);
		mojo.setEncoding(ENCODING);
		mojo.execute();

		verify(processor).replace(replacements, 
				REGEX, BASE_DIR  + File.separator + FILE, OUTPUT_FILE, REGEX_PATTERN_FLAGS, ENCODING);
	}

	@Test
	public void shouldReplaceContentsWithTokenAndValueWithDelimiters() throws Exception {
		List<String> delimiters = asList("@", "${*}");
		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setFile(FILE);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.setDelimiters(delimiters);
		mojo.execute();

		assertThat(mojo.getDelimiters(), equalTo(delimiters));
		verify(processor).replace(argThat(replacementOf(null, VALUE, false, "@" + TOKEN + "@", "${" + TOKEN + "}")), 
				eq(REGEX), eq(BASE_DIR  + File.separator + FILE), eq(OUTPUT_FILE), eq(REGEX_PATTERN_FLAGS), eq(NO_ENCODING_SET));
		verify(summaryBuilder).add(BASE_DIR + File.separator + FILE, OUTPUT_FILE, NO_ENCODING_SET, log);
		verify(summaryBuilder).print(log);
	}
	
	@Test
	public void shouldReplaceContentsWithTokenAndValue() throws Exception {
		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setFile(FILE);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.setXpath(XPATH);
		mojo.execute();

		verify(processor).replace(argThat(replacementOf(XPATH, VALUE, false, TOKEN)), eq(REGEX), eq(BASE_DIR  + File.separator + FILE),
			eq(OUTPUT_FILE), eq(REGEX_PATTERN_FLAGS), eq(NO_ENCODING_SET));
		verify(summaryBuilder).add(BASE_DIR + File.separator + FILE, OUTPUT_FILE, NO_ENCODING_SET, log);
		verify(summaryBuilder).print(log);
	}
	
	@Test
	public void shouldReplaceContentsWithTokenAndValueUnescaped() throws Exception {
		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setFile(FILE);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.setUnescape(true);
		mojo.execute();

		assertTrue(mojo.isUnescape());
		verify(processor).replace(argThat(replacementOf(null, VALUE, true, TOKEN)), eq(REGEX), eq(BASE_DIR  + File.separator + FILE),
			eq(OUTPUT_FILE), eq(REGEX_PATTERN_FLAGS), eq(NO_ENCODING_SET));
		verify(summaryBuilder).add(BASE_DIR + File.separator + FILE, OUTPUT_FILE, NO_ENCODING_SET, log);
		verify(summaryBuilder).print(log);
	}

	@Test
	public void shouldReplaceContentsWithTokenValuesInTokenAndValueFiles() throws Exception {
		when(fileUtils.readFile(TOKEN_FILE, ENCODING)).thenReturn(TOKEN);
		when(fileUtils.readFile(VALUE_FILE, ENCODING)).thenReturn(VALUE);

		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setFile(FILE);
		mojo.setTokenFile(TOKEN_FILE);
		mojo.setValueFile(VALUE_FILE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.setEncoding(ENCODING);
		mojo.execute();

		verify(processor).replace(argThat(replacementOf(null, VALUE, false, TOKEN)), eq(REGEX), eq(BASE_DIR  + File.separator + FILE),
				eq(OUTPUT_FILE), eq(REGEX_PATTERN_FLAGS), eq(ENCODING));
		verify(fileUtils).readFile(TOKEN_FILE, ENCODING);
		verify(fileUtils).readFile(VALUE_FILE, ENCODING);
		verify(summaryBuilder).add(BASE_DIR + File.separator + FILE, OUTPUT_FILE, ENCODING, log);
		verify(summaryBuilder).print(log);
	}

	@Test
	public void shouldReplaceContentsInReplacementsInSameFileWhenNoOutputFile() throws Exception {
		Replacement replacement = mock(Replacement.class);
		List<Replacement> replacements = asList(replacement);

		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setReplacements(replacements);
		mojo.setFile(FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.execute();

		verify(processor).replace(replacements, REGEX, BASE_DIR  + File.separator + FILE, OUTPUT_FILE,
			REGEX_PATTERN_FLAGS, NO_ENCODING_SET);
		verify(summaryBuilder).add(BASE_DIR + File.separator + FILE, OUTPUT_FILE, NO_ENCODING_SET, log);
		verify(summaryBuilder).print(log);
	}
	
	@Test
	public void shouldReplaceContentsWithVariableTokenValueMap() throws Exception {
		Replacement replacement = mock(Replacement.class);
		List<Replacement> replacements = asList(replacement);

		when(tokenValueMapFactory.replacementsForVariable(TOKEN_VALUE_MAP, true, false, ENCODING))
			.thenReturn(replacements);
		mojo.setVariableTokenValueMap(TOKEN_VALUE_MAP);
		mojo.setFile(FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.setEncoding(ENCODING);
		mojo.execute();

		assertThat(mojo.getVariableTokenValueMap(), equalTo(TOKEN_VALUE_MAP));
		verify(processor).replace(replacements, true, BASE_DIR  + File.separator + FILE, OUTPUT_FILE, 0, ENCODING);
		verify(summaryBuilder).add(BASE_DIR + File.separator + FILE, OUTPUT_FILE, ENCODING, log);
		verify(summaryBuilder).print(log);
	}

	@Test
	public void shouldNotReplaceIfIgnoringMissingFilesAndFileNotExists() throws Exception {
		when(fileUtils.fileNotExists(BASE_DIR + File.separator + FILE)).thenReturn(true);
		mojo.setFile(FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.setIgnoreMissingFile(true);

		mojo.execute();
		verifyZeroInteractions(replacerFactory);
		verify(log).info(anyString());
		verify(summaryBuilder, never()).add(anyString(), anyString(), anyString(), isA(Log.class));
		verify(summaryBuilder).print(log);
	}
	
	@Test
	public void shouldThrowExceptionWhenUsingIgnoreMissingFilesAndNoFileSpecified() throws Exception {
		mojo.setIgnoreMissingFile(true);
		try {
			mojo.execute();
			fail("Should have thrown exception");
		} catch (MojoExecutionException e) {
			verifyZeroInteractions(replacerFactory);
			verify(log, times(2)).error("<ignoreMissingFile> only useable with <file>");
			verify(summaryBuilder, never()).add(anyString(), anyString(), anyString(), isA(Log.class));
			verify(summaryBuilder).print(log);
		}
	}

	@Test
	public void shouldCreateNewInstancesOfDepenenciesOnConstructor() {
		new ReplacerMojo();
	}

	@Test (expected = MojoExecutionException.class)
	public void shouldRethrowIOExceptionsAsMojoExceptions() throws Exception {
		when(fileUtils.readFile(anyString(), anyString())).thenThrow(new IOException());

		mojo.setRegexFlags(regexFlags);
		mojo.setRegex(REGEX);
		mojo.setFile(FILE);
		mojo.setTokenFile(TOKEN_FILE);
		mojo.setValueFile(VALUE_FILE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.setBasedir(BASE_DIR);
		mojo.execute();
	}
	
	@Test
	public void shouldNotThrowExceptionWhenIgnoringErrors() throws Exception {
		when(fileUtils.readFile(anyString(), anyString())).thenThrow(new IOException());

		mojo.setIgnoreErrors(true);
		mojo.setFile(FILE);
		mojo.setTokenFile(TOKEN_FILE);
		mojo.setValueFile(VALUE_FILE);
		mojo.setOutputFile(OUTPUT_FILE);
		mojo.execute();
	}
	
	private BaseMatcher<List<Replacement>> replacementOf(final String xpath, final String value, 
			final boolean unescape, final String... tokens) {
		return new BaseMatcher<List<Replacement>>() {
			@SuppressWarnings("unchecked")
			public boolean matches(Object arg0) {
				List<Replacement> replacements = (List<Replacement>) arg0;
				for (int i=0; i < tokens.length; i++) {
					Replacement replacement = replacements.get(i);
					EqualsBuilder equalsBuilder = new EqualsBuilder();
					equalsBuilder.append(tokens[i], replacement.getToken());
					equalsBuilder.append(value, replacement.getValue());
					equalsBuilder.append(unescape, replacement.isUnescape());
					equalsBuilder.append(xpath, replacement.getXpath());
					
					boolean equals = equalsBuilder.isEquals();
					if (!equals) {
						return false;
					}
				}
				return true;
			}

			public void describeTo(Description desc) {
				desc.appendText("tokens").appendValue(Arrays.asList(tokens));
				desc.appendText("value").appendValue(value);
				desc.appendText("unescape").appendValue(unescape);
			}
		};
	}
}

