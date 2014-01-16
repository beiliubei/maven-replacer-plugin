package com.google.code.maven_replacer_plugin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class XPathReplacerTest {
	private static final int NO_FLAGS = -1;
	
	private Replacement replacement;
	private TokenReplacer tokenReplacer;
	private XPathReplacer replacer;
	
	@Before
	public void setUp() {
		replacement = mock(Replacement.class);
		tokenReplacer = mock(TokenReplacer.class);
		replacer = new XPathReplacer(tokenReplacer);
	}
	
	@Test
	public void shouldReplaceAttributeValueLocatedByXpath() throws Exception {
		when(replacement.getXpath()).thenReturn("/root/@id");
		when(replacement.getToken()).thenReturn("token");
		when(replacement.getValue()).thenReturn("value");
		when(tokenReplacer.replace("token", replacement, false, NO_FLAGS)).thenReturn("value");

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<root id=\"token\" class=\"test\"><element id=\"ID\">foo</element></root>";
		String result = replacer.replace(xml, replacement, false, NO_FLAGS);
		// verify that the id attribute in the <root> tag got replaced
		assertThat(result, containsString("<root class=\"test\" id=\"value\">"));
		//verify that the id attribute in the <element> tag remained untouched
		assertThat(result, containsString("<element id=\"ID\">"));
	}
	
	@Test
	public void shouldReplaceAttributeValueLocatedByXpathInChild() throws Exception {
		when(replacement.getXpath()).thenReturn("foo/bar/@baz");
		when(replacement.getToken()).thenReturn("token");
		when(replacement.getValue()).thenReturn("value");
		when(tokenReplacer.replace("token", replacement, false, NO_FLAGS)).thenReturn("value");

		String xml = "<foo><bar baz=\"token\"/></foo>";
		String result = replacer.replace(xml, replacement, false, NO_FLAGS);
		assertThat(result, containsString("<foo><bar baz=\"value\"/></foo>"));
	}

	@Test
	public void shouldReplaceNodeStringLocatedByXpath() throws Exception {
		when(replacement.getXpath()).thenReturn("//test");
		when(replacement.getToken()).thenReturn("token");
		when(replacement.getValue()).thenReturn("value");
		
		when(tokenReplacer.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>token</test>", 
				replacement, false, NO_FLAGS)).thenReturn("<test>value</test>");
		
		String result = replacer.replace("<parent><test>token</test></parent>", replacement, false, NO_FLAGS);
		assertThat(result, containsString("<parent><test>value</test></parent>"));
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowExceptionWhenAttemptingToConstructWithoutTokenReplacer() throws Exception {
		new XPathReplacer(null);
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowExceptionWhenTryingToReplaceParentNodeValue() throws Exception {
		when(replacement.getXpath()).thenReturn("//test");
		
		try {
			replacer.replace("<test>token</test>", replacement, false, NO_FLAGS);
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("Cannot replace a node's content"));
			throw e;
		}
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowExceptionWhenTryingToReplaceWithInvalidXPath() throws Exception {
		when(replacement.getXpath()).thenReturn("invalid xpath");
		
		try {
			replacer.replace("<test>token</test>", replacement, false, NO_FLAGS);
		} catch (Exception e) {
			//XML parser produces localized error messages!
			assertThat(e.getMessage(), containsString(": 'xpath'"));
			throw e;
		}
	}
}
