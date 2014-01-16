package com.google.code.maven_replacer_plugin;

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.regex.Pattern;

public class TokenReplacer implements Replacer {
	public String replace(String content, Replacement replacement, boolean regex, int regexFlags) {
		if (regex) {
			return replaceRegex(content, replacement.getToken(), replacement.getValue(), regexFlags);
		}
		return replaceNonRegex(content, replacement.getToken(), replacement.getValue());
	}
	
	private String replaceRegex(String content, String token, String value, int flags) {
		final Pattern compiledPattern;
		if (flags == PatternFlagsFactory.NO_FLAGS) {
			compiledPattern = Pattern.compile(token);
		} else {
			compiledPattern = Pattern.compile(token, flags);
		}
		
		return compiledPattern.matcher(content).replaceAll(defaultString(value));
	}

	private String replaceNonRegex(String content, String token, String value) {
		if (isEmpty(content)) {
			return content;
		}

		return content.replace(token, defaultString(value));
	}
}
