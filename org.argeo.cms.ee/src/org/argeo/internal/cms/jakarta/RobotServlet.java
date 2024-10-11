package org.argeo.internal.cms.jakarta;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RobotServlet extends HttpServlet {
	private static final long serialVersionUID = 7935661175336419089L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		PrintWriter writer = response.getWriter();
		writer.append("User-agent: *\n");
		writer.append("Disallow:\n");
		response.setHeader("Content-Type", "text/plain");
		writer.flush();
	}

}
