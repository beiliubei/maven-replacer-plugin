package com.google.code.maven_replacer_plugin;

import static org.apache.commons.lang.StringUtils.isBlank;

import com.google.code.maven_replacer_plugin.file.FileUtils;
import com.google.code.maven_replacer_plugin.include.FileSelector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * Goal replaces token with value inside file
 *
 * @goal replace
 *
 * @phase compile
 * 
 * @threadSafe
 * 
 */
public class ReplacerMojo extends AbstractMojo {
	private static final String INVALID_IGNORE_MISSING_FILE_MESSAGE = "<ignoreMissingFile> only useable with <file>";
	private static final String REGEX_PATTERN_WITH_DELIMITERS_MESSAGE = "Error: %s. " +
		"Check that your delimiters do not contain regex characters. (e.g. '$'). " +
		"Either remove the regex characters from your delimiters or set <regex>false</regex>" +
		" in your configuration.";
	
	private final FileUtils fileUtils;
	private final ReplacerFactory replacerFactory;
	private final TokenValueMapFactory tokenValueMapFactory;
	private final FileSelector fileSelector;
	private final PatternFlagsFactory patternFlagsFactory;
	private final OutputFilenameBuilder outputFilenameBuilder;
	private final SummaryBuilder summaryBuilder;
	private final ReplacementProcessor processor;

	/**
	 * File to check and replace tokens.
	 * Path to single file to replace tokens in. 
	 * The file must be text (ascii). 
	 * Based on current execution path.
	 *
	 * @parameter 
	 */
	private String file;

	/**
	 * List of files to include for multiple (or single) replacement. 
	 * In Ant format (*\/directory/**.properties) 
	 * Cannot use with outputFile.
	 *
	 * @parameter
	 */
	private List<String> includes = new ArrayList<String>();

	/**
	 * List of files to exclude for multiple (or single) replacement. 
	 * In Ant format (*\/directory/**.properties) 
	 * Cannot use with outputFile.
	 *
	 * @parameter
	 */
	private List<String> excludes = new ArrayList<String>();

	/**
	 * Comma separated list of includes. 
	 * This is split up and used the same way a array of includes would be.
	 * In Ant format (*\/directory/**.properties). 
	 * Files not found are ignored by default. 
	 *
	 * @parameter 
	 */
	private String filesToInclude;

	/**
	 * List of comma separated files to exclude (must have some includes) for multiple (or single) replacement. 
	 * This is split up and used the same way a array of excludes would be.
	 * In Ant format (**\/directory/do-not-replace.properties). 
	 * The files replaced will be derived from the list of includes and excludes.
	 *
	 * @parameter 
	 */
	private String filesToExclude;

	/**
	 * Token to replace.
	 * The text to replace within the given file. 
	 * This may or may not be a regular expression (see regex notes above).
	 *
	 * @parameter 
	 */
	private String token;

	/**
	 * Token file containing a token to be replaced in the target file/s.
	 * May be multiple words or lines. 
	 * This is useful if you do not wish to expose the token within your pom or the token is long.
	 *
	 * @parameter 
	 */
	private String tokenFile;

	/**
	 * Ignore missing target file. 
	 * Use only with file configuration (not includes etc).
	 * Set to true to not fail build if the file is not found. 
	 * First checks if file exists and exits without attempting to replace anything.
	 *
	 * @parameter 
	 */
	private boolean ignoreMissingFile;

	/**
	 * Value to replace token with.
	 * The text to be written over any found tokens. 
	 * If no value is given, the tokens found are replaced with an empty string (effectively removing any tokens found). 
	 * You can also reference grouped regex matches made in the token here by $1, $2, etc.
	 *
	 * @parameter 
	 */
	private String value;

	/**
	 * A file containing a value to replace the given token with. 
	 * May be multiple words or lines.
	 * This is useful if you do not wish to expose the value within your pom or the value is long.
	 *
	 * @parameter 
	 */
	private String valueFile;

	/**
	 * Indicates if the token should be located with regular expressions. 
	 * This should be set to false if the token contains regex characters which may miss the desired tokens or even replace the wrong tokens.
	 *
	 * @parameter 
	 */
	private boolean regex = true;

	/**
	 * Output to another file.
	 * The input file is read and the final output (after replacing tokens) is written to this file. 
	 * The path and file are created if it does not exist. 
	 * If it does exist, the contents are overwritten. 
	 * You should not use outputFile when using a list of includes.
	 *
	 * @parameter 
	 */
	private String outputFile;

	/**
	 * Output to another dir.
	 * Destination directory relative to the execution directory for all replaced files to be written to. 
	 * Use with outputDir to have files written to a specific base location.
	 *
	 * @parameter 
	 */
	private String outputDir;

	/**
	 * Map of tokens and respective values to replace with.
	 * A file containing tokens and respective values to replace with. 
	 * This file may contain multiple entries to support a single file containing different tokens to have replaced. 
	 * Each token/value pair should be in the format: "token=value" (without quotations). 
	 * If your token contains ='s you must escape the = character to \=. e.g. tok\=en=value
	 *
	 * @parameter 
	 */
	private String tokenValueMap;

	/**
	 * Optional base directory for each file to replace.
	 * Path to base relative files for replacements from. 
	 * This feature is useful for multi-module projects.
	 * Default "." which is the default Maven basedir. 
	 *
	 * @parameter default-value="."
	 */
	private String basedir = ".";

	/**
	 * List of standard Java regular expression Pattern flags (see Java Doc). 
	 * Must contain one or more of:
	 * * CANON_EQ
	 * * CASE_INSENSITIVE
	 * * COMMENTS
	 * * DOTALL
	 * * LITERAL
	 * * MULTILINE
	 * * UNICODE_CASE
	 * * UNIX_LINES
	 * 
	 * @parameter 
	 */
	private List<String> regexFlags;

	/**
	 * List of replacements with token/value pairs.
	 * Each replacement element to contain sub-elements as token/value pairs. 
	 * Each token within the given file will be replaced by it's respective value.
	 *
	 * @parameter 
	 */
	private List<Replacement> replacements;

	/**
	 * Comments enabled in the tokenValueMapFile. 
	 * Comment lines start with '#'.
	 * If your token starts with an '#' then you must supply the commentsEnabled parameter and with a value of false.
	 * Default is true.
	 *
	 * @parameter default-value="true" 
	 */
	private boolean commentsEnabled = true;
	
	/**
	 * Skip running this plugin. 
	 * Default is false.
	 *
	 * @parameter default-value="false" 
	 */
	private boolean skip = false;
	
	/**
	 * Base directory (appended) to use for outputDir.
	 * Having this existing but blank will cause the outputDir
	 * to be based on the execution directory. 
	 *
	 * @parameter 
	 */
	private String outputBasedir;
	
	/**
	 * Parent directory is preserved when replacing files found from includes and 
	 * being written to an outputDir. 
	 * Default is true.
	 *
	 * @parameter default-value="true" 
	 */
	private boolean preserveDir = true;

	/**
	 * Stops printing a summary of files that have had replacements performed upon them when true. 
	 * Default is false.
	 *
	 * @parameter default-value="false" 
	 */
	private boolean quiet = false;

	/**
	 * Unescape tokens and values to Java format.
	 * e.g. token\n is unescaped to token(carriage return).
	 * Default is false.
	 *
	 * @parameter default-value="false" 
	 */
	private boolean unescape;
	
	/**
	 * Add a list of delimiters which are added on either side of tokens to match against. 
	 * You may also use the '' character to place the token in the desired location for matching. 
	 * e.g. @ would match @token@. 
	 * e.g. ${} would match ${token}.
	 *
	 * @parameter 
	 */
	private List<String> delimiters = new ArrayList<String>();
	
	/**
	 * Variable tokenValueMap. Same as the tokenValueMap but can be an include configuration rather than an outside property file.
	 * Similar to tokenValueMap but incline configuration inside the pom. 
	 * This parameter may contain multiple entries to support a single file containing different tokens to have replaced. 
	 * Format is comma separated. e.g. token=value,token2=value2
	 * Comments are not supported.
	 *
	 * @parameter 
	 */
	private String variableTokenValueMap;
	
	/**
	 * Ignore any errors produced by this plugin such as 
	 * files not being found and continue with the build.
	 * 
	 * First checks if file exists and exits without attempting to replace anything. 
	 * Only usable with file parameter.
	 * 
	 * Default is false.
	 *
	 * @parameter default-value="false" 
	 */
	private boolean ignoreErrors;
	
	/**
	 * X-Path expression for locating node's whose content you wish to replace.
	 * This is useful if you have the same token appearing in many nodes but 
	 * wish to only replace the contents of one or more of them.
	 *
	 * @parameter 
	 */
	private String xpath;
	
	/**
	 * File encoding used when reading and writing files. 
	 * Default system encoding used when not specified.
	 * 
	 * @parameter default-value="${project.build.sourceEncoding}"
	 */
	private String encoding;
	
	/**
	 * Regular expression is run on an input file's name to create the output file with.
	 * Must be used in conjunction with outputFilePattern.
	 * 
	 * @parameter 
	 */
	private String inputFilePattern;
	
	/**
	 * Regular expression groups from inputFilePattern are used in this pattern to create an output file per input file.
	 * Must be used in conjunction with inputFilePattern.
	 * 
	 * The parameter outputFile is ignored when outputFilePattern is used.
	 * 
	 * @parameter 
	 */
	private String outputFilePattern;

    /**
     * Set a maximum number of files which can be replaced per execution.
     *
     * @parameter
     */
    private Integer maxReplacements = Integer.MAX_VALUE;

    /**
     * list files
     * 
     * @parameter
     */
    private List<String> files = new ArrayList<String>();
    
    /**
     * list out put file
     * 
     * @parameter
     */
    private List<String> outputFiles = new ArrayList<String>();
    
	public ReplacerMojo() {
		super();
		this.fileUtils = new FileUtils();
		this.replacerFactory = new ReplacerFactory();
		this.tokenValueMapFactory = new TokenValueMapFactory(fileUtils);
		this.fileSelector = new FileSelector();
		this.patternFlagsFactory = new PatternFlagsFactory();
		this.outputFilenameBuilder = new OutputFilenameBuilder();
		this.summaryBuilder = new SummaryBuilder();
		this.processor = new ReplacementProcessor(fileUtils, replacerFactory);
	}

	public ReplacerMojo(FileUtils fileUtils, ReplacementProcessor processor, ReplacerFactory replacerFactory,
			TokenValueMapFactory tokenValueMapFactory, FileSelector fileSelector,
			PatternFlagsFactory patternFlagsFactory, OutputFilenameBuilder outputFilenameBuilder,
			SummaryBuilder summaryBuilder) {
		super();
		this.fileUtils = fileUtils;
		this.processor = processor;
		this.replacerFactory = replacerFactory;
		this.tokenValueMapFactory = tokenValueMapFactory;
		this.fileSelector = fileSelector;
		this.patternFlagsFactory = patternFlagsFactory;
		this.outputFilenameBuilder = outputFilenameBuilder;
		this.summaryBuilder = summaryBuilder;
	}

	public void execute() throws MojoExecutionException {
		try {
			if (skip) {
				getLog().info("Skipping");
				return;
			}

			if (checkFileExists()) {
				getLog().info("Ignoring missing file");
				return;
			}

			List<Replacement> replacements = getDelimiterReplacements(buildReplacements());
			addIncludesFilesAndExcludedFiles();
			if (includes.isEmpty()) {
			    if (files.isEmpty()) {
			        replaceContents(processor, limit(replacements), file);
                }else {
                    replaceContents(processor, limit(replacements), files);
                }
				return;
			}

            for (String file : limit(fileSelector.listIncludes(basedir, includes, excludes))) {
				replaceContents(processor, replacements, file);
			}
		} catch (Exception e) {
			getLog().error(e.getMessage());
			getLog().error(e);
			if (!isIgnoreErrors()) {
				throw new MojoExecutionException(e.getMessage(), e);
			}
		} finally {
			if (!skip && !quiet) {
				summaryBuilder.print(getLog());
			}
		}
	}

    private <T> List<T> limit(List<T> all) {
        if (all.size() > maxReplacements) {
            getLog().info("Max replacements has been exceeded. Limiting to the first: " + maxReplacements);
            return all.subList(0, maxReplacements);
        }
        return all;
    }

    private boolean checkFileExists() throws MojoExecutionException {
		if (ignoreMissingFile && file == null) {
			getLog().error(INVALID_IGNORE_MISSING_FILE_MESSAGE);
			throw new MojoExecutionException(INVALID_IGNORE_MISSING_FILE_MESSAGE);
		}
		return ignoreMissingFile && fileUtils.fileNotExists(getBaseDirPrefixedFilename(file));
	}

	private String getBaseDirPrefixedFilename(String file) {
		if (isBlank(basedir) || fileUtils.isAbsolutePath(file)) {
			return file;
		}
		return basedir + File.separator + file;
	}

	private void addIncludesFilesAndExcludedFiles() {
		if (filesToInclude != null) {
			String[] splitFiles = filesToInclude.split(",");
			addToList(Arrays.asList(splitFiles), includes);
		}

		if (filesToExclude != null) {
			String[] splitFiles = filesToExclude.split(",");
			addToList(Arrays.asList(splitFiles), excludes);
		}
	}

	private void addToList(List<String> toAdds, List<String> destination) {
		for (String toAdd : toAdds) {
			destination.add(toAdd.trim());
		}
	}

	private void replaceContents(ReplacementProcessor processor, List<Replacement> replacements, String inputFile) throws IOException {
	    String outputFileName = outputFilenameBuilder.buildFrom(inputFile, this);
		try {
			processor.replace(replacements, regex, getBaseDirPrefixedFilename(inputFile),
					outputFileName, patternFlagsFactory.buildFlags(regexFlags), encoding);
		} catch (PatternSyntaxException e) {
			if (!delimiters.isEmpty()) {
				getLog().error(String.format(REGEX_PATTERN_WITH_DELIMITERS_MESSAGE, e.getMessage()));
				throw e;
			}
		}
		summaryBuilder.add(getBaseDirPrefixedFilename(inputFile), outputFileName, encoding, getLog());
	}
	
	private void replaceContents(ReplacementProcessor processor, List<Replacement> replacements, List<String> inputFiles) throws IOException {
        int index = 0;
	    for (String inputFile : inputFiles) {
	        getLog().info(inputFile);
            String outputFileName = outputFilenameBuilder.buildFrom(inputFile, this, index);
            getLog().info(outputFileName);
            try {
                processor.replace(replacements, regex, getBaseDirPrefixedFilename(inputFile),
                        outputFileName, patternFlagsFactory.buildFlags(regexFlags), encoding);
            } catch (PatternSyntaxException e) {
                if (!delimiters.isEmpty()) {
                    getLog().error(String.format(REGEX_PATTERN_WITH_DELIMITERS_MESSAGE, e.getMessage()));
                    throw e;
                }
            }
            index ++;
            summaryBuilder.add(getBaseDirPrefixedFilename(inputFile), outputFileName, encoding, getLog());
        }
    }

	private List<Replacement> buildReplacements() throws IOException {
		if (replacements != null) {
			return replacements;
		}

		if (variableTokenValueMap != null) {
			return tokenValueMapFactory.replacementsForVariable(variableTokenValueMap, isCommentsEnabled(),
					unescape, encoding);
		}

		if (tokenValueMap == null) {
			Replacement replacement = new Replacement(fileUtils, token, value, unescape, xpath, encoding);
			replacement.setEncoding(encoding);
			replacement.setTokenFile(tokenFile);
			replacement.setValueFile(valueFile);
			return Arrays.asList(replacement);
		}

		String tokenValueMapFile = getBaseDirPrefixedFilename(tokenValueMap);
		if (fileUtils.fileNotExists(tokenValueMapFile)) {
			getLog().info("'" + tokenValueMapFile + "' does not exist and assuming this is an absolute file name.");
			tokenValueMapFile = tokenValueMap;
		}
		return tokenValueMapFactory.replacementsForFile(tokenValueMapFile, isCommentsEnabled(), unescape, encoding);
	}

	private List<Replacement> getDelimiterReplacements(List<Replacement> replacements) {
		if (delimiters.isEmpty()) {
			return replacements;
		}

		List<Replacement> newReplacements = new ArrayList<Replacement>();
		for (Replacement replacement : replacements) {
			for (DelimiterBuilder delimiter : buildDelimiters()) {
				Replacement withDelimiter = Replacement.from(replacement).withDelimiter(delimiter);
				newReplacements.add(withDelimiter);
			}
		}
		return newReplacements;
	}

	private List<DelimiterBuilder> buildDelimiters() {
		List<DelimiterBuilder> built = new ArrayList<DelimiterBuilder>();
		for (String delimiter : delimiters) {
			built.add(new DelimiterBuilder(delimiter));
		}
		return built;
	}

	public void setRegex(boolean regex) {
		this.regex = regex;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getFile() {
		return file;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setTokenFile(String tokenFile) {
		this.tokenFile = tokenFile;
	}

	public void setValueFile(String valueFile) {
		this.valueFile = valueFile;
	}

	public void setIgnoreMissingFile(boolean ignoreMissingFile) {
		this.ignoreMissingFile = ignoreMissingFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public void setTokenValueMap(String tokenValueMap) {
		this.tokenValueMap = tokenValueMap;
	}

	public void setFilesToInclude(String filesToInclude) {
		this.filesToInclude = filesToInclude;
	}

	public void setFilesToExclude(String filesToExclude) {
		this.filesToExclude = filesToExclude;
	}

	public void setBasedir(String baseDir) {
		this.basedir = baseDir;
	}

	public void setReplacements(List<Replacement> replacements) {
		this.replacements = replacements;
	}

	public void setRegexFlags(List<String> regexFlags) {
		this.regexFlags = regexFlags;
	}

	public void setIncludes(List<String> includes) {
		this.includes = includes;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}

	public List<String> getExcludes() {
		return excludes;
	}

	public String getFilesToInclude() {
		return filesToInclude;
	}

	public String getFilesToExclude() {
		return filesToExclude;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public boolean isCommentsEnabled() {
		return commentsEnabled;
	}

	public void setCommentsEnabled(boolean commentsEnabled) {
		this.commentsEnabled = commentsEnabled;
	}

	public void setOutputBasedir(String outputBasedir) {
		this.outputBasedir = outputBasedir;
	}

	public boolean isPreserveDir() {
		return preserveDir;
	}

	public void setPreserveDir(boolean preserveDir) {
		this.preserveDir = preserveDir;
	}

	public String getBasedir() {
		return basedir;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public String getOutputBasedir() {
		return outputBasedir;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	public void setDelimiters(List<String> delimiters) {
		this.delimiters = delimiters;
	}

	public List<String> getDelimiters() {
		return delimiters;
	}

	public void setUnescape(boolean unescape) {
		this.unescape = unescape;
	}

	public boolean isUnescape() {
		return unescape;
	}

	public void setVariableTokenValueMap(String variableTokenValueMap) {
		this.variableTokenValueMap = variableTokenValueMap;
	}

	public String getVariableTokenValueMap() {
		return variableTokenValueMap;
	}

	public void setIgnoreErrors(boolean ignoreErrors) {
		this.ignoreErrors = ignoreErrors;
	}

	public boolean isIgnoreErrors() {
		return ignoreErrors;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setInputFilePattern(String inputFilePattern) {
		this.inputFilePattern = inputFilePattern;
	}

	public void setOutputFilePattern(String outputFilePattern) {
		this.outputFilePattern = outputFilePattern;
	}

	public String getInputFilePattern() {
		return inputFilePattern;
	}

	public String getOutputFilePattern() {
		return outputFilePattern;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	public boolean isSkip() {
		return skip;
	}

    public void setMaxReplacements(Integer maxReplacements) {
        this.maxReplacements = maxReplacements;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public List<String> getOutputFiles() {
        return outputFiles;
    }

    public void setOutputFiles(List<String> outputFiles) {
        this.outputFiles = outputFiles;
    }
}
