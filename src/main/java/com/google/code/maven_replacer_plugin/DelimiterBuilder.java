package com.google.code.maven_replacer_plugin;

import static org.apache.commons.lang.StringUtils.defaultString;


public class DelimiterBuilder {
	private static final String FORMAT = "%s%s%s";
	
	private final String start;
	private final String end;
	
	public DelimiterBuilder(String delimiter) {
		StringBuilder startBuilder = new StringBuilder();
		StringBuilder endBuilder = new StringBuilder();
		boolean buildingStart = true;
		boolean hasMiddle = false;
		
		for (char c : defaultString(delimiter).toCharArray()) {
			if (c == '*') {
				buildingStart = false;
				hasMiddle = true;
				continue;
			}
			
			if (buildingStart) {
				startBuilder.append(c);
			} else {
				endBuilder.append(c);
			}
		}
		
		this.start = startBuilder.toString();
		if (hasMiddle) { 
			this.end = endBuilder.toString();
		} else {
			this.end = this.start;
		}
	}

	public String apply(String token) {
		if (token == null || token.length() == 0) {
			return token;
		}

		return String.format(FORMAT, start, token, end);
	}
}
