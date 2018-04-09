package org.argeo.eclipse.ui.fs;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

/** Basic label provider with icon for NIO file viewers */
public class FileIconNameLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = 8187902187946523148L;

	private Image folderIcon;
	private Image fileIcon;

	public FileIconNameLabelProvider() {
		// if (!PlatformUI.isWorkbenchRunning()) {
		folderIcon = ImageDescriptor.createFromFile(getClass(), "folder.png").createImage();
		fileIcon = ImageDescriptor.createFromFile(getClass(), "file.png").createImage();
		// }
	}

	@Override
	public void dispose() {
		if (folderIcon != null)
			folderIcon.dispose();
		if (fileIcon != null)
			fileIcon.dispose();
		super.dispose();
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Path) {
			Path curr = ((Path) element);
			Path name = curr.getFileName();
			if (name == null)
				return "[No name]";
			else
				return name.toString();
		} else if (element instanceof ParentDir) {
			return "..";
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof Path) {
			Path curr = ((Path) element);
			if (Files.isDirectory(curr))
				// if (folderIcon != null)
				return folderIcon;
			// else
			// return
			// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
			// else if (fileIcon != null)
			return fileIcon;
			// else
			// return
			// PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		} else if (element instanceof ParentDir) {
			return folderIcon;
		}
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof Path) {
			Path curr = ((Path) element);
			Path name = curr.getFileName();
			if (name == null)
				return "[No name]";
			else
				return name.toAbsolutePath().toString();
		} else if (element instanceof ParentDir) {
			return ((ParentDir) element).getPath().toAbsolutePath().toString();
		}
		return null;
	}

}