package org.argeo.eclipse.ui.jcr.wizards;

import java.io.File;
import java.io.FileInputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.dialogs.Error;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

public class ImportFileSystemWizard extends Wizard {
	private final static Log log = LogFactory
			.getLog(ImportFileSystemWizard.class);

	private ImportFileSystemWizardPage page1;
	private final Node folder;

	public ImportFileSystemWizard(Node folder) {
		this.folder = folder;
		setNeedsProgressMonitor(true);
		setWindowTitle("Import from file system");
	}

	@Override
	public void addPages() {
		page1 = new ImportFileSystemWizardPage();
		addPage(page1);
	}

	@Override
	public boolean performFinish() {
		final String directory = page1.getDirectory();
		if (directory == null || !new File(directory).exists()) {
			Error.show("Directory " + directory + " does not exist");
			return false;
		}

		Boolean failed = false;
		final File dir = new File(directory).getAbsoluteFile();
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
									+ directory, e);
					}
				}
			});
		} catch (Exception e) {
			Error.show("Cannot import " + directory, e);
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

	/** Recursively computes the size of the directory in bytes. */
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
					monitor.subTask(file.getName() + " ("
							+ FileUtils.byteCountToDisplaySize(fileSize) + ") "
							+ file.getCanonicalPath());
					try {
						Node fileNode = folder.addNode(file.getName(),
								NodeType.NT_FILE);
						Node resNode = fileNode.addNode(Property.JCR_CONTENT,
								NodeType.NT_RESOURCE);
						Binary binary = null;
						try {
							binary = folder.getSession().getValueFactory()
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
						log.warn("Import of " + file + " ("
								+ FileUtils.byteCountToDisplaySize(fileSize)
								+ ") failed: " + e);
						folder.getSession().refresh(false);
					}
					monitor.worked((int) (fileSize / FileUtils.ONE_KB));
				}
			}
		} catch (Exception e) {
			throw new ArgeoException("Cannot import " + dir + " to " + folder,
					e);
		}
	}

	protected class ImportFileSystemWizardPage extends WizardPage {
		private DirectoryFieldEditor dfe;

		public ImportFileSystemWizardPage() {
			super("Import from file system");
			setDescription("Import files from the local file system into the JCR repository");
		}

		public void createControl(Composite parent) {
			dfe = new DirectoryFieldEditor("directory", "From",
					parent);
			setControl(dfe.getTextControl(parent));
		}

		public String getDirectory() {
			return dfe.getStringValue();
		}

	}

	static class Stats {
		public Long fileCount = 0l;
		public Long dirCount = 0l;
		public Long sizeB = 0l;
	}
}
