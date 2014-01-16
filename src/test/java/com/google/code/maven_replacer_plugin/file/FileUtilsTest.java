package com.google.code.maven_replacer_plugin.file;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.StringStartsWith.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilsTest {
	private static final String NON_ASCII_CONTENT = "한국어/조선말";
	private static final String CONTENT = "content";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private FileUtils fileUtils;

	@Before
	public void setUp() {
		fileUtils = new FileUtils();
	}

	@Test
	public void shouldDetermineIfFileExists() throws Exception {
		File file = folder.newFile("tempfile");
		assertTrue(fileUtils.fileNotExists("non existant"));
		assertTrue(fileUtils.fileNotExists(null));
		assertTrue(fileUtils.fileNotExists(""));
		assertFalse(fileUtils.fileNotExists(file.getAbsolutePath()));
	}

	@Test
	public void shouldEnsureFileFolderExists() throws Exception {
		String tempFile = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + "/tempfile";
		fileUtils.ensureFolderStructureExists(tempFile);
		new File(tempFile).createNewFile();
		assertTrue(new File(tempFile).exists());
	}
	
	@Test
	public void shouldNotDoAnythingIfRootDirectory() {
		fileUtils.ensureFolderStructureExists("/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionIfFileIsDirectory() throws Exception {
		String tempFile = System.getProperty("java.io.tmpdir");
		fileUtils.ensureFolderStructureExists(tempFile);
	}
	
	@Test
	public void shouldWriteToFileEnsuringFolderStructureExists() throws Exception {
		String tempFile = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + "/tempfile";
		fileUtils.writeToFile(tempFile, CONTENT, "UTF-8");
		assertThat(org.apache.commons.io.FileUtils.readFileToString(new File(tempFile)), equalTo(CONTENT));
	}
	
	@Test
	public void shouldWriteFileWithoutSpecifiedEncoding() throws Exception {
		String tempFile = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + "/tempfile";
		fileUtils.writeToFile(tempFile, NON_ASCII_CONTENT, "UTF-8");
		assertThat(fileUtils.readFile(tempFile, "UTF-8"), equalTo(NON_ASCII_CONTENT));
	}
	
	@Test
	public void shouldWriteFileWithSpecifiedEncoding() throws Exception {
		String tempFile = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID() + "/tempfile";
		fileUtils.writeToFile(tempFile, NON_ASCII_CONTENT, "UTF-8");
		assertThat(fileUtils.readFile(tempFile, "UTF-8"), equalTo(NON_ASCII_CONTENT));
		
		assertThat(fileUtils.readFile(tempFile, "US-ASCII"), not(equalTo(NON_ASCII_CONTENT)));
	}
	
	@Test
	public void shouldReturnFileText() throws Exception {
		File file = folder.newFile("tempfile");
		FileWriter writer = new FileWriter(file);
		writer.write("test\n123\\t456");
		writer.close();

		String data = fileUtils.readFile(file.getAbsolutePath(), "UTF-8");
		assertThat(data, equalTo("test\n123\\t456"));
	}
	
	@Test
	public void shouldReturnFilenameWhenJustFilenameParam() {
		String result = fileUtils.createFullPath("tempFile");
		assertThat(result, equalTo("tempFile"));
	}
	
	@Test
	public void shouldBuildFullPathFromDirsAndFilename() {
		String result = fileUtils.createFullPath("1", "2", "3", "tempFile");
		assertThat(result, equalTo(join(asList("1", "2", "3", "tempFile"), File.separator)));
	}
	
	@Test
	public void shouldSkipNullsGracefullyWhenBuildingPath() {
		String result = fileUtils.createFullPath(null, "1", null, "2", null, "3", null);
		assertThat(result, equalTo(join(asList("1", "2", "3", ""), File.separator)));
	}
	
	@Test
	public void shouldThrowExceptionWhenCannotCreateDir() {
		try {
			fileUtils.ensureFolderStructureExists("/f*\"%e$d/a%*bc$:\\te\"st");
			fail("Should have thrown Error");
		} catch (IllegalStateException e) {
			assertThat(e.getMessage(), startsWith("Error creating directory"));
		}
	}
	
	@Test
	public void shouldReturnTrueWhenAbsolutePathFilename() {
		assertFalse(fileUtils.isAbsolutePath("target/somedir/somepath"));
		assertTrue(fileUtils.isAbsolutePath(new File("target/somefile").getAbsolutePath()));
	}
}
