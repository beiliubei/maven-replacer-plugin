package com.google.code.maven_replacer_plugin;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PatternFlagsFactoryTest {
	private List<String> inputFlags;
	private int expectedFlags;

	private PatternFlagsFactory factory;

	public PatternFlagsFactoryTest(List<String> inputFlags, int expectedFlags) {
		this.inputFlags = inputFlags;
		this.expectedFlags = expectedFlags;
	}

	@Parameters
	public static Collection<Object[]> params() {
		return asList(new Object[][] {
				{ null, -1 },
				{ asList(), -1 },
				{ asList("CANON_EQ"), Pattern.CANON_EQ },
				{ asList("CASE_INSENSITIVE"), Pattern.CASE_INSENSITIVE },
				{ asList("COMMENTS"), Pattern.COMMENTS },
				{ asList("DOTALL"), Pattern.DOTALL },
				{ asList("LITERAL"), Pattern.LITERAL },
				{ asList("MULTILINE"), Pattern.MULTILINE },
				{ asList("UNICODE_CASE"), Pattern.UNICODE_CASE },
				{ asList("UNIX_LINES"), Pattern.UNIX_LINES },
				{ asList("CANON_EQ", "CASE_INSENSITIVE"),
						Pattern.CANON_EQ | Pattern.CASE_INSENSITIVE } });
	}

	@Before
	public void setUp() {
		factory = new PatternFlagsFactory();
	}

	@Test
	public void shouldReturnBitValueForFlags() throws Exception {
		assertThat(factory.buildFlags(inputFlags), is(expectedFlags));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIllegalArgumentExceptionWhenFlagIsInvalid() throws Exception {
		factory.buildFlags(asList("invalid"));
	}
}
