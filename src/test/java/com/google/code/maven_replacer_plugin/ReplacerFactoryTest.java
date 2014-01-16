package com.google.code.maven_replacer_plugin;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ReplacerFactoryTest {
	@Mock
	private Replacement replacement;

	@Test
	public void shouldReturnTokenReplacerWhenNotUsingXPath() {
		ReplacerFactory factory = new ReplacerFactory();

		Replacer replacer = factory.create(replacement);
		assertTrue(replacer instanceof TokenReplacer);
	}
	
	@Test
	public void shouldReturnXPathReplacerWhenUsingXPath() {
		ReplacerFactory factory = new ReplacerFactory();
		when(replacement.getXpath()).thenReturn("some xpath");

		Replacer replacer = factory.create(replacement);
		assertTrue(replacer instanceof XPathReplacer);
	}
}
