package com.metrix.architecture.services;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;

public class RemoteMessagesHandler {
	public RemoteMessagesHandler(MetrixRemoteExecutor executor) {
	}

	public HashMap<String, String> parse(String data) throws JSONException,
			IOException {

		final HashMap<String, String> namevalues = new HashMap<String, String>();

		return namevalues;
	}

	/**
	 * Handler Exception class to handle error when call the remote web service
	 * 
	 */
	public static class HandlerException extends IOException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 5567647273255095983L;

		public HandlerException(String message) {
			super(message);
		}

		public HandlerException(String message, Throwable cause) {
			super(message);
			initCause(cause);
		}

		@Override
		public String toString() {
			if (getCause() != null) {
				return getLocalizedMessage() + ": " + getCause();
			} else {
				return getLocalizedMessage();
			}
		}
	}
}
