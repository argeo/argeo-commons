package org.argeo.eclipse.ui.jcr;

import org.eclipse.swt.graphics.Image;

/** Shared icons. */
public class JcrImages {
	public final static Image NODE = JcrUiPlugin.getImageDescriptor(
			"icons/node.gif").createImage();
	public final static Image FOLDER = JcrUiPlugin.getImageDescriptor(
			"icons/folder.gif").createImage();
	public final static Image FILE = JcrUiPlugin.getImageDescriptor(
			"icons/file.gif").createImage();
	public final static Image BINARY = JcrUiPlugin.getImageDescriptor(
			"icons/binary.png").createImage();
	public final static Image HOME = JcrUiPlugin.getImageDescriptor(
			"icons/home.gif").createImage();
	public final static Image SORT = JcrUiPlugin.getImageDescriptor(
			"icons/sort.gif").createImage();

	public final static Image REPOSITORIES = JcrUiPlugin.getImageDescriptor(
			"icons/repositories.gif").createImage();
	public final static Image REPOSITORY_DISCONNECTED = JcrUiPlugin
			.getImageDescriptor("icons/repository_disconnected.gif")
			.createImage();
	public final static Image REPOSITORY_CONNECTED = JcrUiPlugin
			.getImageDescriptor("icons/repository_connected.gif").createImage();
	public final static Image REMOTE_DISCONNECTED = JcrUiPlugin
			.getImageDescriptor("icons/remote_disconnected.gif").createImage();
	public final static Image REMOTE_CONNECTED = JcrUiPlugin
			.getImageDescriptor("icons/remote_connected.gif").createImage();
	public final static Image WORKSPACE_DISCONNECTED = JcrUiPlugin
			.getImageDescriptor("icons/workspace_disconnected.png")
			.createImage();
	public final static Image WORKSPACE_CONNECTED = JcrUiPlugin
			.getImageDescriptor("icons/workspace_connected.png").createImage();

}
