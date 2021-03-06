package org.argeo.cms.internal.auth;

import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/** Callback handler to be used with a command line UI. */
public class ConsoleCallbackHandler implements CallbackHandler {

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		Console console = System.console();
		if (console == null)
			throw new IllegalStateException("No console available");

		PrintWriter writer = console.writer();
		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof TextOutputCallback) {
				TextOutputCallback callback = (TextOutputCallback) callbacks[i];
				writer.write(callback.getMessage());
			} else if (callbacks[i] instanceof NameCallback) {
				NameCallback callback = (NameCallback) callbacks[i];
				writer.write(callback.getPrompt());
				if (callback.getDefaultName() != null)
					writer.write(" (" + callback.getDefaultName() + ")");
				writer.write(" : ");
				String answer = console.readLine();
				if (callback.getDefaultName() != null && answer.trim().equals(""))
					callback.setName(callback.getDefaultName());
				else
					callback.setName(answer);
			} else if (callbacks[i] instanceof PasswordCallback) {
				PasswordCallback callback = (PasswordCallback) callbacks[i];
				writer.write(callback.getPrompt());
				char[] answer = console.readPassword();
				callback.setPassword(answer);
				Arrays.fill(answer, ' ');
			}
//			else if (callbacks[i] instanceof LocaleChoice) {
//				LocaleChoice callback = (LocaleChoice) callbacks[i];
//				writer.write("Language");
//				writer.write("\n");
//				for (int j = 0; j < callback.getLocales().size(); j++) {
//					Locale locale = callback.getLocales().get(j);
//					writer.print(j + " : " + locale.getDisplayName() + "\n");
//				}
//				writer.write("(" + callback.getDefaultIndex() + ") : ");
//				String answer = console.readLine();
//				if (answer.trim().equals(""))
//					callback.setSelectedIndex(callback.getDefaultIndex());
//				else
//					callback.setSelectedIndex(new Integer(answer.trim()));
//			}
		}
	}

}
