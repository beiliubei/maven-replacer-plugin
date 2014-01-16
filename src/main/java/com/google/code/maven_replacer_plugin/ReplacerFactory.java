package com.google.code.maven_replacer_plugin;

import static org.apache.commons.lang.StringUtils.isNotEmpty;


public class ReplacerFactory {
	public Replacer create(Replacement replacement) {
		TokenReplacer tokenReplacer = new TokenReplacer();
		
		if (isNotEmpty(replacement.getXpath())) {
			return new XPathReplacer(tokenReplacer);
		}
		return tokenReplacer;
	}

}
