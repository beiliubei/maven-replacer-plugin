package com.google.code.maven_replacer_plugin.include;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class FileSelectorTest {
	private static final String BASE_DIR = "src/test/resources/files";
	private static final String TEST_FILE = "maven-replacer-plugin-test-file";
	private static final String BACK_DIR_SYMBOL = "..";
	
	private FileSelector selector;

	@Before
	public void setUp() {
		selector = new FileSelector();
	}
	
	@Test
	public void shouldReturnMultipleFilesToInclude() {
		List<String> files = selector.listIncludes(BASE_DIR, asList("include1", "file*"), asList("file3"));
		assertThat(files.size(), is(3));
		assertThat(files, equalTo(asList("file1", "file2", "include1")));
	}
	
	@Test
	public void shouldSupportNoExcludes() {
		List<String> files = selector.listIncludes(BASE_DIR, asList("include1", "file*"), null);
		assertThat(files, equalTo(asList("file1", "file2", "file3", "include1")));
	}
	
	@Test
	public void shouldReturnEmptyListWhenEmptyIncludes() {
		assertTrue(selector.listIncludes(BASE_DIR, null, asList("file3")).isEmpty());
		assertTrue(selector.listIncludes(BASE_DIR, Collections.<String>emptyList(), asList("file3")).isEmpty());
	}
	
	@Test
	public void shouldSelectFilesInBackDirectories() throws IOException {
		File file = new File(BACK_DIR_SYMBOL + File.separator + TEST_FILE);
		file.deleteOnExit();
		FileUtils.writeStringToFile(file, BASE_DIR);
		
		List<String> files = selector.listIncludes(BACK_DIR_SYMBOL, asList(TEST_FILE), null);
		assertThat(files, equalTo(asList(TEST_FILE)));
	}
}