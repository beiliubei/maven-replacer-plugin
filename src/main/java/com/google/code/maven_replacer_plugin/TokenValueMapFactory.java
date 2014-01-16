package com.google.code.maven_replacer_plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.google.code.maven_replacer_plugin.file.FileUtils;


public class TokenValueMapFactory {

	private static final char SEPARATOR_ESCAPER = '\\';
	private static final char SEPARATOR = '=';
	private static final String COMMENT_PREFIX = "#";

	private final FileUtils fileUtils;

	public TokenValueMapFactory(FileUtils fileUtils) {
		this.fileUtils = fileUtils;
	}
	
	public List<Replacement> replacementsForVariable(String variable, boolean commentsEnabled, boolean unescape, String encoding) {
		StringTokenizer tokenizer = new StringTokenizer(variable, ",");
		String fragment = null;
		List<Replacement> replacements = new ArrayList<Replacement>();
		while (tokenizer.hasMoreTokens()) {
			fragment = tokenizer.nextToken();
			if (ignoreFragment(fragment, commentsEnabled)) {
				continue;
			}

			appendReplacement(replacements, fragment, unescape, encoding);
		}
		return replacements;
	}

	public List<Replacement> replacementsForFile(String tokenValueMapFile, boolean commentsEnabled, 
			boolean unescape, String encoding) 
		throws IOException {
		String contents = fileUtils.readFile(tokenValueMapFile, encoding);
		BufferedReader reader = new BufferedReader(new StringReader(contents));

		String fragment = null;
		List<Replacement> replacements = new ArrayList<Replacement>();
		while ((fragment = reader.readLine()) != null) {
			fragment = fragment.trim();
			if (ignoreFragment(fragment, commentsEnabled)) {
				continue;
			}

			appendReplacement(replacements, fragment, unescape, encoding);
		}
		return replacements;
	}
	
	private void appendReplacement(List<Replacement> replacements, String fragment, boolean unescape, String encoding) {
		StringBuilder token = new StringBuilder();
		String value = "";
		boolean settingToken = true;
		for (int i=0; i < fragment.length(); i++) {
			if (i == 0 && fragment.charAt(0) == SEPARATOR) {
				throw new IllegalArgumentException(getNoValueErrorMsgFor(fragment));
			}

			if (settingToken && !isSeparatorAt(i, fragment)) {
				token.append(fragment.charAt(i));
			} else if (isSeparatorAt(i, fragment)) {
				settingToken = false;
				continue;
			} else {
				value = fragment.substring(i);
				break;
			}
		}

		if (settingToken) {
			return;
		}
		
		String tokenVal = token.toString().trim();
		replacements.add(new Replacement(fileUtils, tokenVal, value.trim(), unescape, null, encoding));
	}

	private boolean isSeparatorAt(int i, String line) {
		return line.charAt(i) == SEPARATOR && line.charAt(i - 1) != SEPARATOR_ESCAPER;
	}

	private String getNoValueErrorMsgFor(String line) {
		return "No value for token: " + line + ". Make sure that tokens have values in pairs in the format: token=value";
	}

	private boolean ignoreFragment(String line, boolean commentsEnabled) {
		return line.length() == 0 || commentsEnabled && line.startsWith(COMMENT_PREFIX);
	}
}
