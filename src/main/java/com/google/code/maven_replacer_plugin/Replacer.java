package com.google.code.maven_replacer_plugin;



public interface Replacer {
	String replace(String content, Replacement replacement, boolean regex, int regexFlags);
}
