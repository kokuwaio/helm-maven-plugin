package io.kokuwa.maven.helm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests stripping of {@link StripSensitiveDataLog}.
 *
 * @author Stephan Schnabel
 */
public class HideSensitiveDataLogTest {

	@Test
	void nothingToHide() {
		String content = "abcd";
		String expected = content;
		String actual = StripSensitiveDataLog.strip(content);
		assertEquals(expected, actual);
	}

	@Test
	void hideHelmPassword() {
		String content = "abcd --password=ABCDEF";
		String expected = "abcd --password=*****";
		String actual = StripSensitiveDataLog.strip(content);
		assertEquals(expected, actual);
	}

	@Test
	void hideKubeToken() {
		String content = "abcd --kube-token ABCDEF";
		String expected = "abcd --kube-token *****";
		String actual = StripSensitiveDataLog.strip(content);
		assertEquals(expected, actual);
	}
}
