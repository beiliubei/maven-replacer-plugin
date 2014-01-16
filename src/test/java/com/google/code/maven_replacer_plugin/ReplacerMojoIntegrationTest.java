package com.google.code.maven_replacer_plugin;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;

public class ReplacerMojoIntegrationTest {
	private static final String ENCODING = "UTF-8";
	
	private static final String EXPECTED_XPATH = scrub(
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<people>" +
		"<person>" +
		"<firstname>value</firstname>" +
		"<lastname>change me</lastname>" +
		"<occupation>please</occupation>" +
		"</person>" +
		"<person>" +
		"<firstname>token</firstname>" +
		"<lastname>dont change me</lastname>" +
		"<occupation>please</occupation>" +
		"</person>" +
		"</people>");
	private static final String TOKEN = "token";
	private static final String VALUE = "value";
	private static final String OUTPUT_DIR = "target/outputdir/";
	private static final String XPATH_TEST_FILE = "xpath.xml";
	
	private ReplacerMojo mojo;
	private String filenameAndPath;
	private Log log;
	private String xml;

	@Before
	public void setUp() throws Exception {
		filenameAndPath = createTempFile(TOKEN);
		log = mock(Log.class);
		xml = scrub(IOUtils.toString(getClass().getClassLoader().getResourceAsStream(XPATH_TEST_FILE)));
		
		mojo = new ReplacerMojo() {
			@Override
			public Log getLog() {
				return log;
			}
		};
	}
	
	@Test
	public void shouldReplaceContentsInFile() throws Exception {
		mojo.setFile(filenameAndPath);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceContentsInAbsolutePathedFile() throws Exception {
		mojo.setFile(new File(filenameAndPath).getAbsolutePath());
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceContentsMaintainingSpacesAndNewLines() throws Exception {
		String valueWithSpacing = " new value" + System.getProperty("line.separator") + " replaced ";
		mojo.setFile(filenameAndPath);
		Replacement replacement = new Replacement();
		replacement.setToken(TOKEN);
		replacement.setValue(valueWithSpacing);
		mojo.setReplacements(asList(replacement));
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(valueWithSpacing));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceRegexTokenLocatedByXPath() throws Exception {
		filenameAndPath = createTempFile(xml);

		mojo.setFile(filenameAndPath);
		mojo.setXpath("//person[firstname='" + TOKEN + "' and lastname='change me']");
		mojo.setToken("(t.K.n)");
		mojo.setValue(VALUE);
		mojo.setRegexFlags(asList("CASE_INSENSITIVE"));
		mojo.execute();

		String results = scrub(FileUtils.readFileToString(new File(filenameAndPath)));
		assertThat(results, equalTo(EXPECTED_XPATH));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceNonRegexTokenLocatedByXPath() throws Exception {
		filenameAndPath = createTempFile(xml);

		mojo.setFile(filenameAndPath);
		mojo.setXpath("//person[firstname='" + TOKEN + "' and lastname='change me']");
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setRegex(false);
		mojo.execute();

		String results = scrub(FileUtils.readFileToString(new File(filenameAndPath)));
		assertThat(results, equalTo(EXPECTED_XPATH));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceNonRegexTokenLocatedByXPathWithinReplacements() throws Exception {
		filenameAndPath = createTempFile(xml);

		Replacement replacement = new Replacement();
		replacement.setToken(TOKEN);
		replacement.setValue(VALUE);
		replacement.setXpath("//person[firstname='" + TOKEN + "' and lastname='change me']");

		mojo.setFile(filenameAndPath);
		mojo.setRegex(false);
		mojo.setReplacements(asList(replacement));
		mojo.execute();

		String results = scrub(FileUtils.readFileToString(new File(filenameAndPath)));
		assertThat(results, equalTo(EXPECTED_XPATH));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldIgnoreErrorsInXPath() throws Exception {
		filenameAndPath = createTempFile(xml);
		mojo.setIgnoreErrors(true);
		mojo.setFile(filenameAndPath);
		mojo.setXpath("some bad xpath");
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(xml));
		//XML parser produces localized error messages!
		verify(log).error(argThat(containsString(": 'bad', 'xpath'")));
		verify(log).info("Replacement run on 0 file.");
	}
	
	@Test
	public void shouldIgnoreErrors() throws Exception {
		mojo.setIgnoreErrors(true);
		mojo.setFile("invalid");
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(TOKEN));
		verify(log).info("Replacement run on 0 file.");
	}
	
	@Test
	public void shouldIgnoreErrorsWithMissingTokenValueMapFile() throws Exception {
		String tokenValueMap = "invalid";
		
		mojo.setIgnoreErrors(true);
		mojo.setFile(filenameAndPath);
		mojo.setTokenValueMap(tokenValueMap);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(TOKEN));
		verify(log).info("Replacement run on 0 file.");
	}
	
	@Test
	public void shouldReplaceContentsInFileWithBackreferences() throws Exception {
		String tokenValueMap = createTempFile("test ([^;]*);=group $1 backreferenced");
		
		filenameAndPath = createTempFile("test 123;");
		mojo.setFile(filenameAndPath);
		mojo.setTokenValueMap(tokenValueMap);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo("group 123 backreferenced"));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceContentsInFileWithDelimiteredToken() throws Exception {
		filenameAndPath = createTempFile("@" + TOKEN + "@ and ${" + TOKEN + "}");
		mojo.setFile(filenameAndPath);
		mojo.setRegex(false);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setDelimiters(asList("@", "${*}"));
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE + " and " + VALUE));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test(expected = MojoExecutionException.class)
	public void shouldLogErrorWhenDelimitersHaveRegexAndRegexEnabled() throws Exception {
		filenameAndPath = createTempFile("@" + TOKEN + "@ and ${" + TOKEN + "}");
		mojo.setFile(filenameAndPath);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setDelimiters(asList("@", "${*}"));
		try {
			mojo.execute();
		} catch (PatternSyntaxException e) {
			String results = FileUtils.readFileToString(new File(filenameAndPath));
			assertThat(results, equalTo("@" + TOKEN + "@ and ${" + TOKEN + "}"));
			verify(log).error(argThat(containsString("Error: Illegal repetition near index 0")));
			verify(log).info("Replacement run on 0 file.");
			throw e;
		}
	}
	
	@Test
	public void shouldReplaceContentsInFileButNotReportWhenQuiet() throws Exception {
		mojo.setQuiet(true);
		mojo.setFile(filenameAndPath);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setEncoding(ENCODING);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
		verify(log, never()).info(anyString());
		verify(log).debug("Replacement run on ." + File.separator + filenameAndPath + 
				" and writing to ." + File.separator + filenameAndPath + " with encoding " + ENCODING);
	}
	
	@Test
	public void shouldReplaceContentsInFileWithTokenContainingEscapedChars() throws Exception {
		filenameAndPath = createTempFile("test\n123\t456");
		
		mojo.setFile(filenameAndPath);
		mojo.setToken("test\\n123\\t456");
		mojo.setValue(VALUE + "\\n987");
		mojo.setUnescape(true);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE + "\n987"));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceAllNewLineChars() throws Exception {
		filenameAndPath = createTempFile("test" + System.getProperty("line.separator") + "123");
		
		mojo.setFile(filenameAndPath);
		mojo.setToken(System.getProperty("line.separator"));
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo("test123"));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceRegexCharContentsInFile() throws Exception {
		filenameAndPath = createTempFile("$to*ken+");
		
		mojo.setRegex(false);
		mojo.setFile(filenameAndPath);
		mojo.setToken("$to*ken+");
		mojo.setValue(VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldRegexReplaceContentsInFile() throws Exception {
		mojo.setFile(filenameAndPath);
		mojo.setToken("(.+)");
		mojo.setValue("$1" + VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(TOKEN + VALUE));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldReplaceContentsAndWriteToOutputDirWithBaseDirAndPreservingAsDefault() throws Exception {
		mojo.setBasedir(".");
		mojo.setFile(filenameAndPath);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setOutputDir(OUTPUT_DIR);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File("./" + OUTPUT_DIR + filenameAndPath));
		assertThat(results, equalTo(VALUE));
		verify(log).info("Replacement run on 1 file.");
	}
	
	@Test
	public void shouldNotReplaceIfIgnoringMissingFilesAndFileNotExists() throws Exception {
		assertFalse(new File("bogus").exists());
		mojo.setFile("bogus");
		mojo.setIgnoreMissingFile(true);
		
		mojo.execute();
		
		assertFalse(new File("bogus").exists());
		verify(log).info("Ignoring missing file");
	}
	
	@Test (expected = MojoExecutionException.class)
	public void shouldRethrowIOExceptionsAsMojoExceptions() throws Exception {
		mojo.setFile("bogus");
		mojo.execute();
		verifyZeroInteractions(log);
	}
	
	@Test
	public void shouldReplaceContentsWithTokenValuesInTokenAndValueFiles() throws Exception {
		String tokenFilename = createTempFile(TOKEN);
		String valueFilename = createTempFile(VALUE);
		
		mojo.setFile(filenameAndPath);
		mojo.setTokenFile(tokenFilename);
		mojo.setValueFile(valueFilename);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldReplaceContentsWithTokenValuesInMap() throws Exception {
		String tokenValueMapFilename = createTempFile(asList("#comment", TOKEN + "=" + VALUE));
		
		mojo.setTokenValueMap(tokenValueMapFilename);
		mojo.setFile(filenameAndPath);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldReplaceContentsWithTokenValuesInMapWithAbsolutePath() throws Exception {
		String tokenValueMapFilename = createTempFile(asList(TOKEN + "=" + VALUE));
		String absolutePath = new File(tokenValueMapFilename).getAbsolutePath();
		mojo.setTokenValueMap(absolutePath);
		mojo.setFile(filenameAndPath);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldReplaceContentsWithTokenValuesInMapWithAbsolutePathAndIncludes() throws Exception {
		String tokenValueMapFilename = createTempFile(asList(TOKEN + "=" + VALUE));
		String absolutePath = new File(tokenValueMapFilename).getAbsolutePath();
		mojo.setTokenValueMap(absolutePath);
		mojo.setIncludes(asList(filenameAndPath));
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldWriteIntoTransformedOutputFilesFromInputFilePattern() throws Exception {
		String inputFile = createTempFile("test-filename", TOKEN);
		mojo.setFile(inputFile);
		mojo.setInputFilePattern("(.*)test-(.+)");
		mojo.setOutputFilePattern("$1test-$2.replaced");
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(inputFile + ".replaced"));
		assertThat(results, equalTo(VALUE));
		verify(log).info("Replacement run on 1 file.");	
	}
	
	@Test
	public void shouldWriteIntoTransformedOutputFilesFromInputFilePatternFromIncludes() throws Exception {
		String inputFile = createTempFile("test-filename", TOKEN);
		mojo.setIncludes(asList(inputFile));
		mojo.setInputFilePattern("(.*)test-(.+)");
		mojo.setOutputFilePattern("$1test-$2.replaced");
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(inputFile + ".replaced"));
		assertThat(results, equalTo(VALUE));
		verify(log).info("Replacement run on 1 file.");	
	}
	
	@Test
	public void shouldWriteIntoTransformedOutputFilesFromInputFilePatternFromFilesToInclude() throws Exception {
		String inputFile = createTempFile("test-filename", TOKEN);
		mojo.setFilesToInclude(inputFile);
		mojo.setInputFilePattern("(.*)test-(.+)");
		mojo.setOutputFilePattern("$1test-$2.replaced");
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(inputFile + ".replaced"));
		assertThat(results, equalTo(VALUE));
		verify(log).info("Replacement run on 1 file.");	
	}
	
	@Test
	public void shouldReplaceContentsWithTokenValuesInInlineMap() throws Exception {
		String variableTokenValueMap = TOKEN + "=" + VALUE;
		
		mojo.setVariableTokenValueMap(variableTokenValueMap);
		mojo.setFile(filenameAndPath);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldReplaceContentsWithTokenValuesInDelimiteredMap() throws Exception {
		filenameAndPath = createTempFile("@" + TOKEN + "@");
		String tokenValueMapFilename = createTempFile(asList("#comment", TOKEN + "=" + VALUE));
		
		mojo.setDelimiters(asList("@"));
		mojo.setTokenValueMap(tokenValueMapFilename);
		mojo.setFile(filenameAndPath);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldReplaceContentsInFilesToInclude() throws Exception {
		String include1 = createTempFile(TOKEN);
		String include2 = createTempFile(TOKEN);
		
		mojo.setFilesToInclude(include1 + ", " + include2);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String include1Results = FileUtils.readFileToString(new File(include1));
		assertThat(include1Results, equalTo(VALUE));
		String include2Results = FileUtils.readFileToString(new File(include2));
		assertThat(include2Results, equalTo(VALUE));
	}

    @Test
    public void shouldOnlyReplaceUpToMaxReplacements() throws Exception {
        String randomBase = String.valueOf(RandomUtils.nextInt(10));
        String include1 = createTempFile(randomBase + "/prefix1", TOKEN);
        String include2 = createTempFile(randomBase + "/prefix2", TOKEN);
        List<String> includes = asList("target/" + randomBase + "**/prefix*");

        mojo.setPreserveDir(false);
        mojo.setIncludes(includes);
        mojo.setToken(TOKEN);
        mojo.setMaxReplacements(1);
        mojo.setValue(VALUE);
        mojo.execute();

        String include1Results = FileUtils.readFileToString(new File(include1));
        String include2Results = FileUtils.readFileToString(new File(include2));
        System.out.println(include1Results);
        System.out.println(include2Results);
        assertTrue((TOKEN.equals(include1Results) && VALUE.equals(include2Results))
                || (VALUE.equals(include1Results) && TOKEN.equals(include2Results)));
    }

	@Test
	public void shouldReplaceContentsInIncludeButNotExcludesAndNotPreserveWhenDisabled() throws Exception {
		String include1 = createTempFile("test/prefix1", TOKEN);
		String include2 = createTempFile("test/prefix2", TOKEN);
		String exclude = createTempFile(TOKEN);
		List<String> includes = asList("target/**/prefix*");
		List<String> excludes = asList(exclude);
		
		mojo.setPreserveDir(false);
		mojo.setIncludes(includes);
		mojo.setExcludes(excludes);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String include1Results = FileUtils.readFileToString(new File(include1));
		assertThat(include1Results, equalTo(VALUE));
		String include2Results = FileUtils.readFileToString(new File(include2));
		assertThat(include2Results, equalTo(VALUE));
		String excludeResults = FileUtils.readFileToString(new File(exclude));
		assertThat(excludeResults, equalTo(TOKEN));
	}
	
	@Test
	public void shouldPreserveFilePathWhenUsingIncludesAndOutputDir() throws Exception {
		String include1 = createTempFile("test/prefix1", TOKEN);
		String include2 = createTempFile("test/prefix2", TOKEN);
		String exclude = createTempFile(TOKEN);
		List<String> includes = asList("target/**/prefix*");
		List<String> excludes = asList(exclude);
		
		mojo.setIncludes(includes);
		mojo.setExcludes(excludes);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.execute();
		
		String include1Results = FileUtils.readFileToString(new File(include1));
		assertThat(include1Results, equalTo(VALUE));
		String include2Results = FileUtils.readFileToString(new File(include2));
		assertThat(include2Results, equalTo(VALUE));
		String excludeResults = FileUtils.readFileToString(new File(exclude));
		assertThat(excludeResults, equalTo(TOKEN));
	}
	
	@Test
	public void shouldReplaceContentsAndWriteToOutputFile() throws Exception {
		String outputFilename = createTempFile("");
		
		mojo.setFile(filenameAndPath);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setOutputFile(outputFilename);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(outputFilename));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldReplaceContentsInReplacementsInSameFileWhenNoOutputFile() throws Exception {
		Replacement replacement = new Replacement();
		replacement.setToken(TOKEN);
		replacement.setValue(VALUE);
		List<Replacement> replacements = asList(replacement);
		
		mojo.setReplacements(replacements);
		mojo.setFile(filenameAndPath);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(filenameAndPath));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldWriteToOutputDirBasedOnOutputBaseDir() throws Exception {
		mojo.setOutputBasedir("target/outputBasedir");
		mojo.setFile(filenameAndPath);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setOutputDir(OUTPUT_DIR);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File("target/outputBasedir/" + OUTPUT_DIR + filenameAndPath));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldWriteToFileOutsideBaseDir() throws Exception {
		String tmpFile = System.getProperty("user.home") + "/tmp/test";
		
		mojo.setFile(filenameAndPath);
		mojo.setToken(TOKEN);
		mojo.setValue(VALUE);
		mojo.setOutputFile(tmpFile);
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(tmpFile));
		assertThat(results, equalTo(VALUE));
	}
	
	@Test
	public void shouldReplaceVersionInPomAsWithXPathTextMatch() throws Exception {
		Replacement replacement = new Replacement();
		replacement.setToken("(.+)");
		replacement.setValue("$1-SNAPSHOT");
		replacement.setXpath("/project/version/text()");
		mojo.setReplacements(asList(replacement));
		mojo.setFile("src/test/resources/pom-for-replace.xml");
		mojo.setOutputFile("target/pom-replaced.xml");
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(mojo.getOutputFile()));
		assertThat(results, containsString("<version>1.5.1-SNAPSHOT</version>"));
	}
	
	@Test
	public void shouldReplaceVersionInPomAsWithXPathAttrMatch() throws Exception {
		Replacement replacement = new Replacement();
		replacement.setToken("(.+)");
		replacement.setValue("value");
		replacement.setXpath("//@name");
		mojo.setReplacements(asList(replacement));
		mojo.setFile("src/test/resources/attr-xpath.xml");
		mojo.setOutputFile("target/attr-xpath-replaced.xml");
		mojo.execute();
		
		String results = FileUtils.readFileToString(new File(mojo.getOutputFile()));
		assertThat(results, containsString("<person name=\"value\" other=\"token\"/>"));
		assertThat(results, containsString("<other name=\"value\" other=\"token\"/>"));
		assertThat(results, containsString("<other other=\"token\"/>"));
	}
	
	private String createTempFile(String contents) throws IOException {
		String filename = new Throwable().fillInStackTrace().getStackTrace()[1].getMethodName();
		return createTempFile(filename, contents);
	}
	
	private String createTempFile(String filename, String contents) throws IOException {
		com.google.code.maven_replacer_plugin.file.FileUtils utils = new com.google.code.maven_replacer_plugin.file.FileUtils();
		String fullname = "target/" + filename + new Random().nextInt();
		utils.ensureFolderStructureExists(fullname);
		File file = new File(fullname);
		FileUtils.writeStringToFile(file, contents);
		file.deleteOnExit();
		return fullname;
	}
	
	private String createTempFile(List<String> contents) throws IOException {
		String filename = new Throwable().fillInStackTrace().getStackTrace()[1].getMethodName();
		File file = new File("target/" + filename);
		FileUtils.writeLines(file, contents);
		file.deleteOnExit();
		return "target/" + file.getName();
	}
	
	private static String scrub(String dirty) {
		return dirty.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\t", "");
	}
}
