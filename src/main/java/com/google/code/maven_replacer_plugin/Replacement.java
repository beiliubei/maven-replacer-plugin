package com.google.code.maven_replacer_plugin;

import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.code.maven_replacer_plugin.file.FileUtils;


public class Replacement {
	private final FileUtils fileUtils;
	
	private DelimiterBuilder delimiter;
	private boolean unescape;
	private String token;
	private String value;
	private String encoding;
	private String xpath;
	
	public Replacement() {
		this.fileUtils = new FileUtils();
		this.unescape = false;
	}

	public Replacement(FileUtils fileUtils, String token, String value, boolean unescape,
			String xpath, String encoding) {
		this.fileUtils = fileUtils;
		setUnescape(unescape);
		setToken(token);
		setValue(value);
		setXpath(xpath);
		setEncoding(encoding);
	}

	public void setTokenFile(String tokenFile) throws IOException {
		if (tokenFile != null) {
			setToken(fileUtils.readFile(tokenFile, getEncoding()));
		}
	}

	public void setValueFile(String valueFile) throws IOException {
		if (valueFile != null) {
			setValue(fileUtils.readFile(valueFile, getEncoding()));
		}
	}

	public String getToken() {
		String newToken = unescape ? unescape(token) : token;
		if (delimiter != null) {
			return delimiter.apply(newToken);
		}
		return newToken;
	}

	public String getValue() {
		return unescape ? unescape(value) : value;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setValue(String value) {
		this.value = value;
	}

	private String unescape(String text) {
		return StringEscapeUtils.unescapeJava(text);
	}

	public void setUnescape(boolean unescape) {
		this.unescape = unescape;
	}

	public boolean isUnescape() {
		return unescape;
	}

	public static Replacement from(Replacement replacement) {
		return new Replacement(replacement.fileUtils, replacement.token, replacement.value,
				replacement.unescape, replacement.xpath, replacement.encoding);
	}

	public Replacement withDelimiter(DelimiterBuilder delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public String getXpath() {
		return xpath;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getEncoding() {
		return encoding;
	}
}
