package io.kokuwa.maven.helm;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;

/**
 * Delegate for maven {@link Log} to strip sensitive data before logging.
 *
 * @author Stephan Schnabel
 */
public class StripSensitiveDataLog implements Log {

	private static final Pattern HELM_PASSWORD = Pattern.compile("^.*--password=(?<secret>[^-\\s]+).*$");
	private static final Pattern KUBE_TOKEN = Pattern.compile("^.*--kube-token (?<secret>[^-\\s]+).*$");
	private static final List<Pattern> PATTERNS = Arrays.asList(HELM_PASSWORD, KUBE_TOKEN);
	private static final String REPLACEMENT = "*****";

	static String strip(CharSequence content) {
		String stripped = content.toString();
		for (Pattern pattern : PATTERNS) {
			Matcher matcher = pattern.matcher(content);
			if (matcher.matches()) {
				stripped = stripped.replace(matcher.group("secret"), REPLACEMENT);
			}
		}
		return stripped;
	}

	private final Log delegate;

	public StripSensitiveDataLog(Log delegate) {
		this.delegate = delegate;
	}

	@Override
	public void debug(CharSequence content) {
		delegate.debug(strip(content));
	}

	@Override
	public void debug(CharSequence content, Throwable error) {
		delegate.debug(content, error);
	}

	@Override
	public void debug(Throwable error) {
		delegate.debug(error);
	}

	@Override
	public void info(CharSequence content) {
		delegate.info(strip(content));
	}

	@Override
	public void info(CharSequence content, Throwable error) {
		delegate.info(content, error);
	}

	@Override
	public void info(Throwable error) {
		delegate.info(error);
	}

	@Override
	public void warn(CharSequence content) {
		delegate.warn(strip(content));
	}

	@Override
	public void warn(CharSequence content, Throwable error) {
		delegate.warn(content, error);
	}

	@Override
	public void warn(Throwable error) {
		delegate.warn(error);
	}

	@Override
	public void error(CharSequence content) {
		delegate.error(strip(content));
	}

	@Override
	public void error(CharSequence content, Throwable error) {
		delegate.error(content, error);
	}

	@Override
	public void error(Throwable error) {
		delegate.error(error);
	}

	@Override
	public boolean isDebugEnabled() {
		return delegate.isDebugEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return delegate.isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return delegate.isWarnEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return delegate.isErrorEnabled();
	}
}
