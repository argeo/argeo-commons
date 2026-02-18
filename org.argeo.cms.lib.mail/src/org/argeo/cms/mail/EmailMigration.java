package org.argeo.cms.mail;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static org.argeo.cms.mail.EmailUtils.describe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;

import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.HeaderTerm;

/** Migrates emails from one storage to the another one. */
public class EmailMigration {
	private final static Logger logger = System.getLogger(EmailMigration.class.getName());

//	private String targetBaseDir;

	private String sourceServer;
	private String sourceUsername;
	private String sourcePassword;

	private String targetServer;
	private String targetUsername;
	private String targetPassword;

	private boolean targetSupportDualTypeFolders = true;

	public void process() throws MessagingException, IOException {
//		Path baseDir = Paths.get(targetBaseDir).resolve(sourceUsername).resolve("mbox");

		Store sourceStore = null;
		Store targetStore = null;
		try {

			sourceStore = connectToSourceStore();
			Folder defaultFolder = sourceStore.getDefaultFolder();
//			migrateFolders(baseDir, defaultFolder);

			// Always start with Inbox
//			Folder inboxFolder = sourceStore.getFolder(EmailUtils.INBOX);
//			migrateFolder(baseDir, inboxFolder);

//			Folder targetFolder = targetStore.getFolder(EmailUtils.INBOX);
//			logger.log(DEBUG, "Source message count " + inboxFolder.getMessageCount());
//			logger.log(DEBUG, "Target message count " + targetFolder.getMessageCount());

			targetStore = connectToTargetStore();
			migrateFolders(defaultFolder, targetStore);
		} finally {
			if (sourceStore != null)
				sourceStore.close();
			if (targetStore != null)
				targetStore.close();
		}
	}

	protected Store connectToSourceStore() throws MessagingException {
		Properties sourceProperties = System.getProperties();
		sourceProperties.setProperty("mail.store.protocol", "imaps");

		Session sourceSession = Session.getInstance(sourceProperties, null);
		// session.setDebug(true);
		Store sourceStore = sourceSession.getStore("imaps");
		sourceStore.connect(sourceServer, sourceUsername, sourcePassword);
		return sourceStore;
	}

	protected Store connectToTargetStore() throws MessagingException {
		Properties targetProperties = System.getProperties();
		targetProperties.setProperty("mail.imap.starttls.enable", "true");
		targetProperties.setProperty("mail.imap.auth", "true");

		Session targetSession = Session.getInstance(targetProperties, null);
		// session.setDebug(true);
		Store targetStore = targetSession.getStore("imap");
		targetStore.connect(targetServer, targetUsername, targetPassword);
		return targetStore;
	}

	protected void migrateFolders(Folder sourceParentFolder, Store targetStore) throws MessagingException, IOException {
		folders: for (Folder sourceFolder : sourceParentFolder.list()) {
			String sourceFolderName = sourceFolder.getName();

			String sourceFolderFullName = sourceFolder.getFullName();
			char sourceFolderSeparator = sourceParentFolder.getSeparator();
			char targetFolderSeparator = targetStore.getDefaultFolder().getSeparator();
			String targetFolderFullName = sourceFolderFullName.replace(sourceFolderSeparator, targetFolderSeparator);

			// GMail specific
			if (sourceFolderFullName.equals("[Gmail]")) {
				migrateFolders(sourceFolder, targetStore);
				continue folders;
			}
			if (sourceFolderFullName.startsWith("[Gmail]")) {
				String subFolderName = null;
				// Make it configurable
				switch (sourceFolderName) {
				case "All Mail":
				case "Important":
				case "Spam":
					continue folders;
				case "Sent Mail":
					subFolderName = "Sent";
				default:
					// does nothing
				}
				targetFolderFullName = subFolderName == null ? sourceFolder.getName() : subFolderName;
			}

			// nature of the source folder
			int messageCount = (sourceFolder.getType() & Folder.HOLDS_MESSAGES) != 0 ? sourceFolder.getMessageCount()
					: 0;
			boolean hasSubFolders = (sourceFolder.getType() & Folder.HOLDS_FOLDERS) != 0
					? sourceFolder.list().length != 0
					: false;

			Folder targetFolder;
			if (targetSupportDualTypeFolders) {
				targetFolder = targetStore.getFolder(targetFolderFullName);
				if (!targetFolder.exists()) {
					targetFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
					logger.log(DEBUG, "Created HOLDS_FOLDERS | HOLDS_MESSAGES folder " + targetFolder.getFullName());
				}

			} else {
				if (hasSubFolders) {// has sub-folders
					if (messageCount == 0) {
						targetFolder = targetStore.getFolder(targetFolderFullName);
						if (!targetFolder.exists()) {
							targetFolder.create(Folder.HOLDS_FOLDERS);
							logger.log(DEBUG, "Created HOLDS_FOLDERS folder " + targetFolder.getFullName());
						}
					} else {// also has messages
						Folder parentFolder = targetStore.getFolder(targetFolderFullName);
						if (!parentFolder.exists()) {
							parentFolder.create(Folder.HOLDS_FOLDERS);
							logger.log(DEBUG, "Created HOLDS_FOLDERS folder " + parentFolder.getFullName());
						}
						String miscFullName = targetFolderFullName + targetFolderSeparator + "_Misc";
						targetFolder = targetStore.getFolder(miscFullName);
						if (!targetFolder.exists()) {
							targetFolder.create(Folder.HOLDS_MESSAGES);
							logger.log(DEBUG, "Created HOLDS_MESSAGES folder " + targetFolder.getFullName());
						}
					}
				} else {// no sub-folders
					if (messageCount == 0) { // empty
						logger.log(DEBUG, "Skip empty folder " + targetFolderFullName);
						continue folders;
					}
					targetFolder = targetStore.getFolder(targetFolderFullName);
					if (!targetFolder.exists()) {
						targetFolder.create(Folder.HOLDS_MESSAGES);
						logger.log(DEBUG, "Created HOLDS_MESSAGES folder " + targetFolder.getFullName());
					}
				}
			}

			if (messageCount != 0) {

				targetFolder.open(Folder.READ_WRITE);
				try {
					long begin = System.currentTimeMillis();
					sourceFolder.open(Folder.READ_ONLY);
					migrateFolder(sourceFolder, targetFolder);
					long duration = System.currentTimeMillis() - begin;
					logger.log(DEBUG, targetFolderFullName + " - Migration of " + messageCount + " messages took "
							+ (duration / 1000) + " s (" + (duration / messageCount) + " ms per message)");
				} finally {
					sourceFolder.close();
					targetFolder.close();
				}
			}

			// recursive
			if (hasSubFolders) {
				migrateFolders(sourceFolder, targetStore);
			}
		}
	}

	protected void migrateFoldersToFs(Path baseDir, Folder sourceFolder) throws MessagingException, IOException {
		folders: for (Folder folder : sourceFolder.list()) {
			String folderName = folder.getName();

			if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
				// Make it configurable
				switch (folderName) {
				case "All Mail":
				case "Important":
					continue folders;
				default:
					// does nothing
				}

				folder.open(Folder.READ_ONLY);

				try {
					int messageCount = folder.getMessageCount();
					logger.log(DEBUG, folderName + " - Message count : " + messageCount);
					if (messageCount == 0)
						return;
//					logger.log(DEBUG, folderName + " - Unread Messages : " + sourceFolder.getUnreadMessageCount());

					long begin = System.currentTimeMillis();
					migrateFolderToFs(baseDir, folder);
					long duration = System.currentTimeMillis() - begin;
					logger.log(DEBUG, folderName + " - Migration of " + messageCount + " messages took "
							+ (duration / 1000) + " s (" + (duration / messageCount) + " ms per message)");
				} finally {
					sourceFolder.close();
				}
			}
			if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
				migrateFoldersToFs(baseDir.resolve(folder.getName()), folder);
			}
		}
	}

	protected void migrateFolderToFs(Path baseDir, Folder sourceFolder) throws MessagingException, IOException {
		// Save as files
		Message messages[] = sourceFolder.getMessages();

		for (int i = 0; i < messages.length; ++i) {
//					logger.log(DEBUG, "MESSAGE #" + (i + 1) + ":");
			Message msg = messages[i];
//					String from = "unknown";
//					if (msg.getReplyTo().length >= 1) {
//						from = msg.getReplyTo()[0].toString();
//					} else if (msg.getFrom().length >= 1) {
//						from = msg.getFrom()[0].toString();
//					}
			String subject = msg.getSubject();
			Instant sentDate = msg.getSentDate().toInstant();
//					logger.log(DEBUG, "Saving ... " + subject + " from " + from + " (" + sentDate + ")");
			String fileName = sentDate + "  " + subject;
			Path file = baseDir.resolve(fileName);
			savePartsAsFiles(msg.getContent(), file);
		}
	}

	protected Folder migrateFolder(Folder sourceFolder, Folder targetFolder) throws MessagingException, IOException {
		String folderName = targetFolder.getName();

		int lastSourceNumber;
		int currentTargetMessageCount = targetFolder.getMessageCount();
		if (currentTargetMessageCount != 0) {
			MimeMessage lastTargetMessage = (MimeMessage) targetFolder.getMessage(currentTargetMessageCount);
			logger.log(DEBUG, folderName + " - Last target message " + describe(lastTargetMessage));
			Date lastTargetSent = lastTargetMessage.getReceivedDate();
			Message[] lastSourceMessage = sourceFolder
					.search(new HeaderTerm(EmailUtils.MESSAGE_ID, lastTargetMessage.getMessageID()));
			if (lastSourceMessage.length == 0)
				throw new IllegalStateException("No message found with message ID " + lastTargetMessage.getMessageID());
			if (lastSourceMessage.length != 1) {
				for (Message msg : lastSourceMessage) {
					logger.log(ERROR, "Message " + describe(msg));

				}
				throw new IllegalStateException(
						lastSourceMessage.length + " messages found with received date " + lastTargetSent.toInstant());
			}
			lastSourceNumber = lastSourceMessage[0].getMessageNumber();
		} else {
			lastSourceNumber = 0;
		}
		logger.log(DEBUG, folderName + " - Last source message number " + lastSourceNumber);

		int countToRetrieve = sourceFolder.getMessageCount() - lastSourceNumber;

		FetchProfile fetchProfile = new FetchProfile();
		fetchProfile.add(FetchProfile.Item.FLAGS);
		fetchProfile.add(FetchProfile.Item.ENVELOPE);
		fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
		fetchProfile.add(FetchProfile.Item.SIZE);
		addFetchProfileItems(sourceFolder, fetchProfile);

		int batchSize = 100;
		int batchCount = countToRetrieve / batchSize;
		if (countToRetrieve % batchSize != 0)
			batchCount = batchCount + 1;
		// int batchCount = 2; // for testing
		for (int i = 0; i < batchCount; i++) {
			long begin = System.currentTimeMillis();

			int start = lastSourceNumber + i * batchSize + 1;
			int end = lastSourceNumber + (i + 1) * batchSize;
			if (end >= (lastSourceNumber + countToRetrieve + 1))
				end = lastSourceNumber + countToRetrieve;
			Message[] sourceMessages = sourceFolder.getMessages(start, end);
			sourceFolder.fetch(sourceMessages, fetchProfile);
			// targetFolder.appendMessages(sourceMessages);
			// sourceFolder.copyMessages(sourceMessages,targetFolder);

			copyMessages(sourceMessages, targetFolder);
//			copyMessagesToMbox(sourceMessages, targetFolder);

			String describeLast = describe(sourceMessages[sourceMessages.length - 1]);

//		if (i % 10 == 9) {
			// free memory from fetched messages
			sourceFolder.close();
			targetFolder.close();

			sourceFolder.open(Folder.READ_ONLY);
			targetFolder.open(Folder.READ_WRITE);
//			logger.log(DEBUG, "Open/close folder in order to free memory");
//		}

			long duration = System.currentTimeMillis() - begin;
			logger.log(DEBUG, folderName + " - batch " + i + " took " + (duration / 1000) + " s, "
					+ (duration / (end - start + 1)) + " ms per message. Last message " + describeLast);
		}

		return targetFolder;
	}

	protected void addFetchProfileItems(Folder sourceFolder, FetchProfile fetchProfile) {
		// TODO make it portable with com.sun.mail.* implementation
//		if (sourceFolder instanceof IMAPFolder) {
//			// IMAPFolder sourceImapFolder = (IMAPFolder) sourceFolder;
//			fetchProfile.add(IMAPFolder.FetchProfileItem.HEADERS);
//			fetchProfile.add(IMAPFolder.FetchProfileItem.MESSAGE);
//		}
	}

	protected void copyMessages(Message[] sourceMessages, Folder targetFolder) throws MessagingException, IOException {
		targetFolder.appendMessages(sourceMessages);
	}

	/** Save body parts and attachments as plain files. */
	protected void savePartsAsFiles(Object content, Path fileBase) throws IOException, MessagingException {
		OutputStream out = null;
		InputStream in = null;
		try {
			if (content instanceof Multipart) {
				Multipart multi = ((Multipart) content);
				int parts = multi.getCount();
				for (int j = 0; j < parts; ++j) {
					MimeBodyPart part = (MimeBodyPart) multi.getBodyPart(j);
					if (part.getContent() instanceof Multipart) {
						// part-within-a-part, do some recursion...
						savePartsAsFiles(part.getContent(), fileBase);
					} else {
						String extension = "";
						if (part.isMimeType("text/html")) {
							extension = "html";
						} else {
							if (part.isMimeType("text/plain")) {
								extension = "txt";
							} else {
								// Try to get the name of the attachment
								extension = part.getDataHandler().getName();
							}
						}
						String filename = fileBase + "." + extension;
						System.out.println("... " + filename);
						out = new FileOutputStream(new File(filename));
						in = part.getInputStream();
						int k;
						while ((k = in.read()) != -1) {
							out.write(k);
						}
					}
				}
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.flush();
				out.close();
			}
		}
	}

	public void setSourceServer(String sourceServer) {
		this.sourceServer = sourceServer;
	}

	public void setSourceUsername(String sourceUsername) {
		this.sourceUsername = sourceUsername;
	}

	public void setSourcePassword(String sourcePassword) {
		this.sourcePassword = sourcePassword;
	}

	public void setTargetServer(String targetServer) {
		this.targetServer = targetServer;
	}

	public void setTargetUsername(String targetUsername) {
		this.targetUsername = targetUsername;
	}

	public void setTargetPassword(String targetPassword) {
		this.targetPassword = targetPassword;
	}

	public static void main(String args[]) throws Exception {
		if (args.length < 6)
			throw new IllegalArgumentException(
					"usage: <source IMAP server> <source username> <source password> <target IMAP server> <target username> <target password>");
		String sourceServer = args[0];
		String sourceUsername = args[1];
		String sourcePassword = args[2];
		String targetServer = args[3];
		String targetUsername = args[4];
		String targetPassword = args[5];

		EmailMigration emailMigration = new EmailMigration();
		emailMigration.setSourceServer(sourceServer);
		emailMigration.setSourceUsername(sourceUsername);
		emailMigration.setSourcePassword(sourcePassword);
		emailMigration.setTargetServer(targetServer);
		emailMigration.setTargetUsername(targetUsername);
		emailMigration.setTargetPassword(targetPassword);

		emailMigration.process();
	}
}
