package com.kiwigrid.helm.maven.plugin.exception;

/**
 * Indicates that the chart upload was invalid.
 *
 * @author Axel Koehler
 * @since 2.1
 */
public class BadUploadException extends Exception {

	public BadUploadException() {
	}

	public BadUploadException(String message) {
		super(message);
	}
}
