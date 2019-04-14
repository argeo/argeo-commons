package org.argeo.cms.desktop.mini;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class MiniTerminal implements KeyListener, PaintListener {

	private Canvas area;
	private Caret caret;

	private StringBuffer buf = new StringBuffer("");
	private StringBuffer userInput = new StringBuffer("");
	private List<String> history = new ArrayList<>();

	private Point charExtent = null;
	private int charsPerLine = 0;
	private String[] lines = new String[0];
	private List<String> logicalLines = new ArrayList<>();

	private Font mono;
	private Charset charset;

	private Path currentDir;
	private Path homeDir;
	private String host = "localhost";
	private String username;

	private boolean running = false;
	private OutputStream stdIn = null;

	public MiniTerminal(Composite parent, int style) {
		charset = StandardCharsets.UTF_8;

		Display display = parent.getDisplay();
		// Linux-specific
		mono = new Font(display, "Monospace", 10, SWT.NONE);

		parent.setLayout(new GridLayout());
		area = new Canvas(parent, SWT.NONE);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		caret = new Caret(area, SWT.NONE);
		area.setCaret(caret);

		area.addKeyListener(this);
		area.addPaintListener(this);

		username = System.getProperty("user.name");
		try {
			host = InetAddress.getLocalHost().getHostName();
			if (host.indexOf('.') > 0)
				host = host.substring(0, host.indexOf('.'));
		} catch (UnknownHostException e) {
			host = "localhost";
		}
		homeDir = Paths.get(System.getProperty("user.home"));
		currentDir = homeDir;

		buf = new StringBuffer(prompt());
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.keyLocation != 0)
			return;// weird characters
		// System.out.println(e.character);
		if (e.keyCode == 0xd) {// return
			markLogicalLine();
			if (!running)
				processUserInput();
			// buf.append(prompt());
		} else if (e.keyCode == 0x8) {// delete
			if (userInput.length() == 0)
				return;
			userInput.setLength(userInput.length() - 1);
			if (!running && buf.length() > 0)
				buf.setLength(buf.length() - 1);
		} else {
			// if (!running)
			buf.append(e.character);
			userInput.append(e.character);
		}

		if (area.isDisposed())
			return;
		area.redraw();
		// System.out.println("Append " + e);

		if (running) {
			if (stdIn != null) {
				try {
					stdIn.write(Character.toString(e.character).getBytes(charset));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

	protected String prompt() {
		String fileName = currentDir.equals(homeDir) ? "~" : currentDir.getFileName().toString();
		String end = username.equals("root") ? "]# " : "]$ ";
		return "[" + username + "@" + host + " " + fileName + end;
	}

	private void displayPrompt() {
		buf.append(prompt() + userInput);
	}

	protected void markLogicalLine() {
		String str = buf.toString().trim();
		logicalLines.add(str);
		buf = new StringBuffer("");
	}

	private void processUserInput() {
		String cmd = userInput.toString();
		userInput = new StringBuffer("");
		processUserInput(cmd);
		history.add(cmd);
	}

	protected void processUserInput(String input) {
		try {
			StringTokenizer st = new StringTokenizer(input);
			List<String> args = new ArrayList<>();
			while (st.hasMoreTokens())
				args.add(st.nextToken());
			if (args.size() == 0) {
				displayPrompt();
				return;
			}

			// change directory
			if (args.get(0).equals("cd")) {
				if (args.size() == 1) {
					setPath(homeDir);
				} else {
					Path newPath = currentDir.resolve(args.get(1));
					if (!Files.exists(newPath) || !Files.isDirectory(newPath)) {
						println(newPath + ": No such file or directory");
						return;
					}
					setPath(newPath);
				}
				displayPrompt();
				return;
			}
			// show current directory
			else if (args.get(0).equals("pwd")) {
				println(currentDir);
				displayPrompt();
				return;
			}
			// exit
			else if (args.get(0).equals("exit")) {
				println("logout");
				area.getShell().dispose();
				return;
			}

			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream(true);
			pb.directory(currentDir.toFile());
//			Process process = Runtime.getRuntime().exec(input, null, currentPath.toFile());
			Process process = pb.start();

			stdIn = process.getOutputStream();
			Thread readOut = new Thread("Read out") {
				@Override
				public void run() {
					running = true;
					try (BufferedReader in = new BufferedReader(
							new InputStreamReader(process.getInputStream(), charset))) {
						String line = null;
						while ((line = in.readLine()) != null) {
							println(line);
						}
					} catch (IOException e) {
						println(e.getMessage());
					}
					stdIn = null;
					displayPrompt();
					running = false;
				}
			};
			readOut.start();
		} catch (IOException e) {
			println(e.getMessage());
			displayPrompt();
		}
	}

	protected int linesForLogicalLine(char[] line) {
		return line.length / charsPerLine + 1;
	}

	protected void println(Object line) {
		buf.append(line);
		markLogicalLine();
	}

	protected void refreshLines(int charPerLine, int nbrOfLines) {
		if (lines.length != nbrOfLines) {
			lines = new String[nbrOfLines];
			Arrays.fill(lines, null);
		}
		if (this.charsPerLine != charPerLine)
			this.charsPerLine = charPerLine;

		int currentLine = nbrOfLines - 1;
		// current line
		if (buf.length() > 0) {
			lines[currentLine] = buf.toString();
		} else {
			lines[currentLine] = "";
		}
		currentLine--;

		logicalLines: for (int i = logicalLines.size() - 1; i >= 0; i--) {
			char[] logicalLine = logicalLines.get(i).toCharArray();
			int linesNeeded = linesForLogicalLine(logicalLine);
			for (int j = linesNeeded - 1; j >= 0; j--) {
				int from = j * charPerLine;
				int to = j == linesNeeded - 1 ? from + charPerLine : Math.min(from + charPerLine, logicalLine.length);
				lines[currentLine] = new String(Arrays.copyOfRange(logicalLine, from, to));
//				System.out.println("Set line " + currentLine + " to : " + lines[currentLine]);
				currentLine--;
				if (currentLine < 0)
					break logicalLines;
			}
		}
	}

	@Override
	public void paintControl(PaintEvent e) {
		GC gc = e.gc;
		gc.setFont(mono);
		if (charExtent == null)
			charExtent = gc.textExtent("a");

		Point areaSize = area.getSize();
		int charPerLine = areaSize.x / charExtent.x;
		int nbrOfLines = areaSize.y / charExtent.y;
		refreshLines(charPerLine, nbrOfLines);

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line != null)
				gc.drawString(line, 0, i * charExtent.y);
		}
//		String toDraw = buf.toString();
//		gc.drawString(toDraw, 0, 0);
//		area.setCaret(caret);
	}

	public void setPath(String path) {
		this.currentDir = Paths.get(path);
	}

	public void setPath(Path path) {
		this.currentDir = path;
	}

	public static void main(String[] args) {
		Display display = Display.getCurrent() == null ? new Display() : Display.getCurrent();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);

		MiniTerminal miniBrowser = new MiniTerminal(shell, SWT.NONE);
		String url = args.length > 0 ? args[0] : System.getProperty("user.home");
		miniBrowser.setPath(url);

		shell.open();
		shell.setSize(new Point(800, 480));
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

}
