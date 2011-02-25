/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.argeo.server.jcr.mvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
