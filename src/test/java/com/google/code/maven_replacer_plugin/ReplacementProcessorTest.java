package com.google.code.maven_replacer_plugin;


import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.code.maven_replacer_plugin.file.FileUtils;

@RunWith(MockitoJUnitRunner.class)
public class ReplacementProcessorTest {

	private static final String FILE = "file";
	private static final String OUTPUT_FILE = "outputFile";
	private static final String NEW_CONTENT = "new content";
	private static final int REGEX_FLAGS = 0;
	private static final boolean USE_REGEX = true;
	private static final boolean NO_REGEX = false;
	private static final String TOKEN = "token";
	private static final String CONTENT = "content";
	private static final String VALUE = "value";
	private static final String ENCODING = "encoding";
	
	@Mock
	private FileUtils fileUtils;
	@Mock
	private Replacer replacer;
	@Mock
	private Replacement replacement;
	@Mock
	private ReplacerFactory replacerFactory;
	
	private ReplacementProcessor processor;

	@Before
	public void setUp() throws Exception {
		when(fileUtils.readFile(FILE, ENCODING)).thenReturn(CONTENT);
		when(replacement.getToken()).thenReturn(TOKEN);
		when(replacement.getValue()).thenReturn(VALUE);
		when(replacerFactory.create(replacement)).thenReturn(replacer);
		
		processor = new ReplacementProcessor(fileUtils, replacerFactory);
	}
	
	@Test
	public void shouldWriteReplacedRegexTextToFile() throws Exception {
		when(replacer.replace(CONTENT, replacement, true, REGEX_FLAGS)).thenReturn(NEW_CONTENT);
		
		processor.replace(asList(replacement), USE_REGEX, FILE, OUTPUT_FILE, REGEX_FLAGS, ENCODING);
		verify(fileUtils).writeToFile(OUTPUT_FILE, NEW_CONTENT, ENCODING);
	}
	
	@Test
	public void shouldWriteReplacedNonRegexTextToFile() throws Exception {
		when(replacer.replace(CONTENT, replacement, false, REGEX_FLAGS)).thenReturn(NEW_CONTENT);
		
		processor.replace(asList(replacement), NO_REGEX, FILE, OUTPUT_FILE, REGEX_FLAGS, ENCODING);
		verify(fileUtils).writeToFile(OUTPUT_FILE, NEW_CONTENT, ENCODING);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfNoToken() throws Exception {
		when(replacement.getToken()).thenReturn(null);
		
		processor.replace(asList(replacement), USE_REGEX, FILE, OUTPUT_FILE, REGEX_FLAGS, ENCODING);
		verifyZeroInteractions(fileUtils);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void shouldThrowExceptionIfEmptyToken() throws Exception {
		when(replacement.getToken()).thenReturn("");
		
		processor.replace(asList(replacement), USE_REGEX, FILE, OUTPUT_FILE, REGEX_FLAGS, ENCODING);
		verifyZeroInteractions(fileUtils);
	}
}
