package org.argeo.eclipse.ui.fs;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

public class FileIconNameLabelProvider extends ColumnLabelProvider {
	private static final long serialVersionUID = 8187902187946523148L;

	private Image folderIcon;
	private Image fileIcon;

	public FileIconNameLabelProvider() {
		// if (!PlatformUI.isWorkbenchRunning()) {
		folderIcon = ImageDescriptor.createFromFile(getClass(), "fldr_obj.gif").createImage();
		fileIcon = ImageDescriptor.createFromFile(getClass(), "file_obj.gif").createImage();
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
		}
		return null;
	}
}