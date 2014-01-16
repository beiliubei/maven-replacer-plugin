package com.google.code.maven_replacer_plugin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

public class SummaryBuilderTest {
	@Test
	public void shouldAddToSummaryAndPrintToLog() {
		Log log = mock(Log.class);
		
		SummaryBuilder builder = new SummaryBuilder();
		builder.add("INPUT", "OUTPUT", "ENCODING", log);
		builder.add("INPUT", "OUTPUT", "ENCODING", log);
		
		builder.print(log);
		verify(log, times(2)).debug("Replacement run on INPUT and writing to OUTPUT with encoding ENCODING");
		verify(log).info("Replacement run on 2 files.");
	}
}
