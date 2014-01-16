package com.google.code.maven_replacer_plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.google.code.maven_replacer_plugin.file.FileUtils;

public class OutputFilenameBuilderTest {
	private static final String INPUT_FILE = "parent" + File.separator + "input";
	private static final String BASE_DIR = ".";
	private static final String OUTPUT_DIR = "target" + File.separator + "out";
	private static final String OUTPUT_FILE = "outputFile";
	private static final String OUTPUT_FILE_WITH_PARENT = "parent" + File.separator + OUTPUT_FILE;
	private static final String OUTPUT_BASE_DIR = "outputBaseDir";
	
	private OutputFilenameBuilder builder;
	private ReplacerMojo mojo;
	private FileUtils fileUtils;

	@Before
	public void setUp() {
		mojo = mock(ReplacerMojo.class);
		when(mojo.getBasedir()).thenReturn(BASE_DIR);
		
		fileUtils = new FileUtils();
		builder = new OutputFilenameBuilder();
	}
	
	@Test
	public void shouldReturnFullPathWithAllOutputFileParams() {
		when(mojo.getOutputDir()).thenReturn(OUTPUT_DIR);
		when(mojo.getOutputBasedir()).thenReturn(OUTPUT_BASE_DIR);
		when(mojo.getOutputFile()).thenReturn(OUTPUT_FILE_WITH_PARENT);
		when(mojo.isPreserveDir()).thenReturn(true);
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(OUTPUT_BASE_DIR, OUTPUT_DIR, OUTPUT_FILE_WITH_PARENT)));
		
		when(mojo.isPreserveDir()).thenReturn(false);
		output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(OUTPUT_BASE_DIR, OUTPUT_DIR, OUTPUT_FILE)));
	}
	
	@Test
	public void shouldPrefixBasedirWhenNotPreservingPath() {
		when(mojo.isPreserveDir()).thenReturn(false);
		when(mojo.getOutputDir()).thenReturn(OUTPUT_DIR);
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, OUTPUT_DIR, "input")));
	}
	
	@Test
	public void shouldPreservePathWhenPreserveIsEnabled() {
		when(mojo.isPreserveDir()).thenReturn(true);
		when(mojo.getOutputDir()).thenReturn(OUTPUT_DIR);
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, OUTPUT_DIR, INPUT_FILE)));
	}
	
	@Test
	public void shouldPrefixOutputDirWhenUsingOutputDirAndOutputFile() {
		when(mojo.getOutputDir()).thenReturn(OUTPUT_DIR);
		when(mojo.getOutputFile()).thenReturn(OUTPUT_FILE);
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, OUTPUT_DIR, OUTPUT_FILE)));
	}
	
	@Test
	public void shouldReturnReplacedOutputFilenameFromPatterns() {
		when(mojo.getInputFilePattern()).thenReturn("(.+)");
		when(mojo.getOutputFilePattern()).thenReturn("$1");
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, INPUT_FILE)));
		
		when(mojo.getInputFilePattern()).thenReturn("(.+)");
		when(mojo.getOutputFilePattern()).thenReturn("$1-new");
		
		output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, INPUT_FILE + "-new")));
	}
	
	@Test
	public void shouldNotReturnReplacedOutputFilenameWhenMissingEitherInputOrOutputPattern() {
		when(mojo.getInputFilePattern()).thenReturn(null);
		when(mojo.getOutputFilePattern()).thenReturn("$1-new");
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, not(endsWith("-new")));
		
		when(mojo.getInputFilePattern()).thenReturn("(.+)");
		when(mojo.getOutputFilePattern()).thenReturn(null);
		
		output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, INPUT_FILE)));
	}
	
	@Test
	public void shouldPrefixBasedirWhenNotUsingOutputBasedir() {
		when(mojo.getOutputDir()).thenReturn(OUTPUT_DIR);
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, OUTPUT_DIR, "input")));
	}
	
	@Test
	public void shouldPrefixWithOutputBasedirWhenUsingOutputBasedir() {
		when(mojo.getOutputBasedir()).thenReturn(OUTPUT_BASE_DIR);
		when(mojo.getOutputDir()).thenReturn(OUTPUT_DIR);
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(OUTPUT_BASE_DIR, OUTPUT_DIR, "input")));
	}
	
	@Test
	public void shouldReturnInputFileWithBaseDirWhenNoOutputDirOrNoOutputFile() {
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, INPUT_FILE)));
	}
	
	@Test
	public void shouldWriteToOutputFileWhenNotUsingOutputDirAndIsSet() {
		when(mojo.getOutputFile()).thenReturn(OUTPUT_FILE);
		
		String output = builder.buildFrom(INPUT_FILE, mojo);
		assertThat(output, equalTo(fileUtils.createFullPath(BASE_DIR, OUTPUT_FILE)));
	}
	
	@Test
	public void shouldReturnIgnoreBaseDirForOutputFileWhenStartsWithAbsolutePath() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("windows") < 0) {
			when(mojo.getOutputFile()).thenReturn(File.separator + "output");
			String output = builder.buildFrom(INPUT_FILE, mojo);
			assertThat(output, equalTo(File.separator + "output"));
		} else {
			when(mojo.getOutputFile()).thenReturn("C:" + File.separator + "output");
			String output = builder.buildFrom(INPUT_FILE, mojo);
			assertThat(output, equalTo("C:" + File.separator + "output"));
		}
	}
}
