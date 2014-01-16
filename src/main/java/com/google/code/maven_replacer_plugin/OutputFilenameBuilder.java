package com.google.code.maven_replacer_plugin;

import java.io.File;

import com.google.code.maven_replacer_plugin.file.FileUtils;

public class OutputFilenameBuilder {
	
	private final FileUtils fileUtils;

	public OutputFilenameBuilder() {
		fileUtils = new FileUtils();
	}
	
	public String buildFrom(String inputFilename, ReplacerMojo mojo) {
		String based = buildOutputFile(inputFilename, mojo);
		if (mojo.getInputFilePattern() != null && mojo.getOutputFilePattern() != null) {
			based = based.replaceAll(mojo.getInputFilePattern(), mojo.getOutputFilePattern());
		}
		
		return based;
	}
	
	private String buildOutputFile(String inputFilename, ReplacerMojo mojo) {
		String basedir = fileUtils.isAbsolutePath(inputFilename) ? "" : mojo.getBasedir();
		
		if (mojo.getOutputDir() != null && mojo.getOutputFile() != null) {
			String cleanResult = mojo.isPreserveDir() ? mojo.getOutputFile() : stripPath(mojo.getOutputFile());
			if (mojo.getOutputBasedir() != null) {
				return fileUtils.createFullPath(mojo.getOutputBasedir(), mojo.getOutputDir(), cleanResult);
			}
			return fileUtils.createFullPath(basedir, mojo.getOutputDir(), cleanResult);
		}
		
		if (mojo.getOutputDir() != null) {
			String cleanResult = mojo.isPreserveDir() ? inputFilename : stripPath(inputFilename);
			if (mojo.getOutputBasedir() != null) {
				return fileUtils.createFullPath(mojo.getOutputBasedir(), mojo.getOutputDir(), cleanResult);
			}
			return fileUtils.createFullPath(basedir, mojo.getOutputDir(), cleanResult);
		}
		
		if (mojo.getOutputFile() != null) {
			File outFile = new File(mojo.getOutputFile());
			if (outFile.isAbsolute()) {
				return fileUtils.createFullPath(mojo.getOutputFile());
			}
			return fileUtils.createFullPath(basedir, mojo.getOutputFile());
		}
		return fileUtils.createFullPath(basedir, inputFilename);
	}

	private String stripPath(String inputFilename) {
		return new File(inputFilename).getName();
	}
	
	public String buildFrom(String inputFilename, ReplacerMojo mojo, int index) {
        String based = buildOutputFile(inputFilename, mojo, index);
        return based;
    }
    
    private String buildOutputFile(String inputFilename, ReplacerMojo mojo, int index) {
        String basedir = "";
        
        if (!mojo.getOutputFiles().isEmpty()) {
            File outFile = new File(mojo.getOutputFiles().get(index));
            if (outFile.isAbsolute()) {
                return fileUtils.createFullPath(mojo.getOutputFiles().get(index));
            }
            return fileUtils.createFullPath(basedir, mojo.getOutputFiles().get(index));
        }
        return fileUtils.createFullPath(basedir, inputFilename);
    }


}
