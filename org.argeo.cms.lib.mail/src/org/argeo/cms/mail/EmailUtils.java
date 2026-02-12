package org.argeo.cms.mail;

import java.util.Date;

import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/** Utilities around emails. */
public class EmailUtils {
	public final static String INBOX = "Inbox";
	public final static String INBOX_UPPER_CASE = "INBOX";
	public final static String MESSAGE_ID = "Message-ID";

	public static String getMessageId(Message msg) {
		try {
			return msg instanceof MimeMessage ? ((MimeMessage) msg).getMessageID() : "<N/A>";
		} catch (MessagingException e) {
			throw new IllegalStateException("Cannot extract message id from " + msg, e);
		}
	}

	public static String describe(Message msg) {
		try {
			return "Message " + msg.getMessageNumber() + " " + msg.getSentDate().toInstant() + " " + getMessageId(msg);
		} catch (MessagingException e) {
			throw new IllegalStateException("Cannot describe " + msg, e);
		}
	}

	static void setHeadersFromFlags(MimeMessage msg, Flags flags) {
		try {
			StringBuilder status = new StringBuilder();
			if (flags.contains(Flags.Flag.SEEN))
				status.append('R');
			if (!flags.contains(Flags.Flag.RECENT))
				status.append('O');
			if (status.length() > 0)
				msg.setHeader("Status", status.toString());
			else
				msg.removeHeader("Status");

			boolean sims = false;
			String s = msg.getHeader("X-Status", null);
			// is it a SIMS 2.0 format X-Status header?
			sims = s != null && s.length() == 4 && s.indexOf('$') >= 0;
			//status.setLength(0);
			if (flags.contains(Flags.Flag.DELETED))
				status.append('D');
			else if (sims)
				status.append('$');
			if (flags.contains(Flags.Flag.FLAGGED))
				status.append('F');
			else if (sims)
				status.append('$');
			if (flags.contains(Flags.Flag.ANSWERED))
				status.append('A');
			else if (sims)
				status.append('$');
			if (flags.contains(Flags.Flag.DRAFT))
				status.append('T');
			else if (sims)
				status.append('$');
			if (status.length() > 0)
				msg.setHeader("X-Status", status.toString());
			else
				msg.removeHeader("X-Status");

			String[] userFlags = flags.getUserFlags();
			if (userFlags.length > 0) {
				status.setLength(0);
				for (int i = 0; i < userFlags.length; i++)
					status.append(userFlags[i]).append(' ');
				status.setLength(status.length() - 1); // smash trailing space
				msg.setHeader("X-Keywords", status.toString());
			}
			if (flags.contains(Flags.Flag.DELETED)) {
				s = msg.getHeader("X-Dt-Delete-Time", null);
				if (s == null)
					// XXX - should be time
					msg.setHeader("X-Dt-Delete-Time", "1");
			}
		} catch (MessagingException e) {
			// ignore it
		}
	}

    public static String getUnixFrom(MimeMessage msg) {
	Address[] afrom;
	String from;
	Date ddate;
	String date;
	try {
	    if ((afrom = msg.getFrom()) == null ||
		    !(afrom[0] instanceof InternetAddress) ||
		    (from = ((InternetAddress)afrom[0]).getAddress()) == null)
		from = "UNKNOWN";
	    if ((ddate = msg.getReceivedDate()) == null ||
		    (ddate = msg.getSentDate()) == null)
		ddate = new Date();
	} catch (MessagingException e) {
	    from = "UNKNOWN";
	    ddate = new Date();
	}
	date = ddate.toString();
	// date is of the form "Sat Aug 12 02:30:00 PDT 1995"
	// need to strip out the timezone
	return "From " + from + " " +
		date.substring(0, 20) + date.substring(24);
    }

	/** Singleton. */
	private EmailUtils() {
	}
}
