package org.argeo.server.jcr.mvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

@Controller
public class JcrBrowserController implements JcrMvcConstants {

	@RequestMapping("/getJcrItem.*")
	public Item getJcrItem(WebRequest webRequest,
			@RequestParam("path") String path) throws RepositoryException {
		return ((Session) webRequest.getAttribute(REQUEST_ATTR_SESSION,
				RequestAttributes.SCOPE_REQUEST)).getItem(path);
	}

	@RequestMapping("/queryJcrNodes.*")
	public List<String> queryJcrNodes(WebRequest webRequest,
			@RequestParam("statement") String statement,
			@RequestParam("language") String language)
			throws RepositoryException {
		Session session = ((Session) webRequest.getAttribute(
				REQUEST_ATTR_SESSION, RequestAttributes.SCOPE_REQUEST));
		Query query = session.getWorkspace().getQueryManager().createQuery(
				statement, language);
		NodeIterator nit = query.execute().getNodes();
		List<String> paths = new ArrayList<String>();
		while (nit.hasNext()) {
			paths.add(nit.nextNode().getPath());
		}
		return paths;
	}

	@RequestMapping("/queryJcrTable.*")
	public List<List<String>> queryJcrTable(WebRequest webRequest,
			@RequestParam("statement") String statement,
			@RequestParam("language") String language)
			throws RepositoryException {
		Session session = ((Session) webRequest.getAttribute(
				REQUEST_ATTR_SESSION, RequestAttributes.SCOPE_REQUEST));
		Query query = session.getWorkspace().getQueryManager().createQuery(
				statement, language);
		QueryResult queryResult = query.execute();
		List<List<String>> results = new ArrayList<List<String>>();
		results.add(Arrays.asList(queryResult.getColumnNames()));
		RowIterator rit = queryResult.getRows();

		while (rit.hasNext()) {
			Row row = rit.nextRow();
			List<String> lst = new ArrayList<String>();
			for (Value value : row.getValues()) {
				lst.add(value.getString());
			}
		}
		return results;
	}
}
