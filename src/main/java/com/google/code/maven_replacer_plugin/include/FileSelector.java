package com.google.code.maven_replacer_plugin.include;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;

public class FileSelector {

	public List<String> listIncludes(String basedir, List<String> includes, List<String> excludes) {
		if (includes == null || includes.isEmpty()) {
			return Collections.emptyList();
		}

		DirectoryScanner directoryScanner = new DirectoryScanner();
		directoryScanner.addDefaultExcludes();
		directoryScanner.setBasedir(new File(basedir));
		directoryScanner.setIncludes(stringListToArray(includes));
		directoryScanner.setExcludes(stringListToArray(excludes));

		directoryScanner.scan();
		return Arrays.asList(directoryScanner.getIncludedFiles());
	}

	private String[] stringListToArray(List<String> stringList) {
		if (stringList == null) {
			return null;
		}
		return stringList.toArray(new String[] {});
	}
}
