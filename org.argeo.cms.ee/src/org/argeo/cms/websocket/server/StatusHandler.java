package org.argeo.cms.websocket.server;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.argeo.cms.http.HttpHeader.CONTENT_TYPE;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.argeo.api.cms.CmsState;
import org.argeo.cms.CmsDeployProperty;
import org.argeo.cms.CurrentUser;
import org.argeo.cms.auth.RemoteAuthUtils;
import org.argeo.cms.http.server.HttpRemoteAuthExchange;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StatusHandler implements WebsocketEndpoints, HttpHandler {
	private CmsState cmsState;

	@Override
	public Set<Class<?>> getEndPoints() {
		Set<Class<?>> res = new HashSet<>();
		res.add(PingEndpoint.class);
		res.add(EventEndpoint.class);
		return res;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		StringBuilder sb = new StringBuilder();

		StringJoiner sj = new StringJoiner("\n");
		CmsDeployProperty[] deployProperties = CmsDeployProperty.values();
		Arrays.sort(deployProperties, (o1, o2) -> o1.name().compareTo(o2.name()));
		for (CmsDeployProperty deployProperty : deployProperties) {
			List<String> values = cmsState.getDeployProperties(deployProperty.getProperty());
			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				if (value != null) {
					String line = deployProperty.getProperty() + (i == 0 ? "" : "." + i) + "=" + value;
					sj.add(line);
				}
			}
		}
		sb.append(sj.toString());

		RemoteAuthUtils.doAs(() -> sb.append("\n\nUser: " + CurrentUser.getUsername()),
				new HttpRemoteAuthExchange(exchange));

		byte[] msg = sb.toString().getBytes(UTF_8);
		// TODO factorize
		exchange.getResponseHeaders().put(CONTENT_TYPE.get(), singletonList("text/plain; charset=utf-8"));
		exchange.sendResponseHeaders(200, msg.length);
		exchange.getResponseBody().write(msg);
	}

	public void setCmsState(CmsState cmsState) {
		this.cmsState = cmsState;
	}

}
