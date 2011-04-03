package org.argeo.eclipse.ui.jcr.wizards;

import java.io.File;
import java.io.FileInputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.dialogs.Error;
import org.argeo.eclipse.ui.specific.ImportToServerWizardPage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

public class ImportFileSystemWizard extends Wizard {
	private final static Log log = LogFactory
			.getLog(ImportFileSystemWizard.class);

	private ImportToServerWizardPage importPage;
	private final Node folder;

	public ImportFileSystemWizard(Node folder) {
		this.folder = folder;
		setWindowTitle("Import from file system");
	}

	@Override
	public void addPages() {
		importPage = new ImportToServerWizardPage();
		addPage(importPage);
		setNeedsProgressMonitor(importPage.getNeedsProgressMonitor());
	}

	/**
	 * Called when the user click on 'Finish' in the wizard. The real upload to
	 * the JCR repository is done here.
	 */
	@Override
	public boolean performFinish() {

		// Initialization
		final String objectType = importPage.getObjectType();
		final String objectPath = importPage.getObjectPath();

		// We do not display a progress bar for one file only
		if (importPage.FILE_ITEM_TYPE.equals(objectType)) {
			// In Rap we must force the "real" upload of the file
			importPage.performFinish();
			try {
				Node fileNode = folder.addNode(importPage.getObjectName(),
						NodeType.NT_FILE);
				Node resNode = fileNode.addNode(Property.JCR_CONTENT,
						NodeType.NT_RESOURCE);
				Binary binary = null;
				try {
					binary = folder.getSession().getValueFactory()
							.createBinary(importPage.getFileInputStream());
					resNode.setProperty(Property.JCR_DATA, binary);
				} finally {
					if (binary != null)
						binary.dispose();
					IOUtils.closeQuietly(importPage.getFileInputStream());
				}
				folder.getSession().save();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		} else if (importPage.FOLDER_ITEM_TYPE.equals(objectType)) {
			if (objectPath == null || !new File(objectPath).exists()) {
				Error.show("Directory " + objectPath + " does not exist");
				return false;
			}

			Boolean failed = false;
			final File dir = new File(objectPath).getAbsoluteFile();
			final Long sizeB = directorySize(dir, 0l);
			final Stats stats = new Stats();
			Long begin = System.currentTimeMillis();
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						try {
							Integer sizeKB = (int) (sizeB / FileUtils.ONE_KB);
							monitor.beginTask("", sizeKB);
							importDirectory(folder, dir, monitor, stats);
							monitor.done();
						} catch (Exception e) {
							if (e instanceof RuntimeException)
								throw (RuntimeException) e;
							else
								throw new ArgeoException("Cannot import "
										+ objectPath, e);
						}
					}
				});
			} catch (Exception e) {
				Error.show("Cannot import " + objectPath, e);
				failed = true;
			}

			Long duration = System.currentTimeMillis() - begin;
			Long durationS = duration / 1000l;
			String durationStr = (durationS / 60) + " min " + (durationS % 60)
					+ " s";
			StringBuffer message = new StringBuffer("Imported\n");
			message.append(stats.fileCount).append(" files\n");
			message.append(stats.dirCount).append(" directories\n");
			message.append(FileUtils.byteCountToDisplaySize(stats.sizeB));
			if (failed)
				message.append(" of planned ").append(
						FileUtils.byteCountToDisplaySize(sizeB));
			message.append("\n");
			message.append("in ").append(durationStr).append("\n");
			if (failed)
				MessageDialog.openError(getShell(), "Import failed",
						message.toString());
			else
				MessageDialog.openInformation(getShell(), "Import successful",
						message.toString());

			return true;
		}
		return false;

	}

	/** Recursively computes the size of the directory in bytes. */
	protected Long directorySize(File dir, Long currentSize) {
		Long size = currentSize;
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				size = directorySize(file, size);
			} else {
				size = size + file.length();
			}
		}
		return size;
	}

	/**
	 * Import recursively a directory and its content to the repository.
	 */
	protected void importDirectory(Node folder, File dir,
			IProgressMonitor monitor, Stats stats) {
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					Node childFolder = folder.addNode(file.getName(),
							NodeType.NT_FOLDER);
					importDirectory(childFolder, file, monitor, stats);
					folder.getSession().save();
					stats.dirCount++;
				} else {
					Long fileSize = file.length();

					// we skip tempory files that are created by apps when a
					// file is being edited.
					// TODO : make this configurable.
					if (file.getName().lastIndexOf('~') != file.getName()
							.length() - 1) {

						monitor.subTask(file.getName() + " ("
								+ FileUtils.byteCountToDisplaySize(fileSize)
								+ ") " + file.getCanonicalPath());
						try {
							Node fileNode = folder.addNode(file.getName(),
									NodeType.NT_FILE);
							Node resNode = fileNode.addNode(
									Property.JCR_CONTENT, NodeType.NT_RESOURCE);
							Binary binary = null;
							try {
								binary = folder
										.getSession()
										.getValueFactory()
										.createBinary(new FileInputStream(file));
								resNode.setProperty(Property.JCR_DATA, binary);
							} finally {
								if (binary != null)
									binary.dispose();
							}
							folder.getSession().save();
							stats.fileCount++;
							stats.sizeB = stats.sizeB + fileSize;
						} catch (Exception e) {
							log.warn("Import of "
									+ file
									+ " ("
									+ FileUtils
											.byteCountToDisplaySize(fileSize)
									+ ") failed: " + e);
							folder.getSession().refresh(false);
						}
						monitor.worked((int) (fileSize / FileUtils.ONE_KB));
					}
				}
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot import " + dir + " to " + folder,
					e);
		}
	}

	static class Stats {
		public Long fileCount = 0l;
		public Long dirCount = 0l;
		public Long sizeB = 0l;
	}
}
