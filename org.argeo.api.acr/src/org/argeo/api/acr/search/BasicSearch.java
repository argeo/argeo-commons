package org.argeo.api.acr.search;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.argeo.api.acr.QNamed;

/**
 * A basic search mechanism modelled on WebDav basicsearch.
 * 
 * @see http://www.webdav.org/specs/rfc5323.html
 */
public class BasicSearch {

	private List<QName> select = new ArrayList<>();
	private List<Scope> from = new ArrayList<>();

	private ContentFilter<? extends Composition> where;

	public BasicSearch select(QNamed... attr) {
		for (QNamed q : attr)
			select.add(q.qName());
		return this;
	}

	public BasicSearch select(QName... attr) {
		select.addAll(Arrays.asList(attr));
		return this;
	}

	/**
	 * Convenience method, to search below this absolute path, with depth
	 * {@link Depth#INFINITTY}.
	 */
	public BasicSearch from(String path) {
		return from(URI.create(path), Depth.INFINITTY);
	}

	/** Search below this URI, with depth {@link Depth#INFINITTY}. */
	public BasicSearch from(URI uri) {
		return from(uri, Depth.INFINITTY);
	}

	/** Search below this URI, with this {@link Depth}. */
	public BasicSearch from(URI uri, Depth depth) {
		Objects.requireNonNull(uri);
		Objects.requireNonNull(depth);
		Scope scope = new Scope(uri, depth);
		from.add(scope);
		return this;
	}

	public BasicSearch where(Consumer<AndFilter> and) {
		if (where != null)
			throw new IllegalStateException("A where clause is already set");
		AndFilter subFilter = new AndFilter();
		and.accept(subFilter);
		where = subFilter;
		return this;
	}

	public List<QName> getSelect() {
		return select;
	}

	public List<Scope> getFrom() {
		return from;
	}

	public ContentFilter<? extends Composition> getWhere() {
		return where;
	}

	public static enum Depth {
		ZERO, ONE, INFINITTY;
	}

	public static class Scope {

		URI uri;
		Depth depth;

		public Scope(URI uri, Depth depth) {
			this.uri = uri;
			this.depth = depth;
		}

		public URI getUri() {
			return uri;
		}

		public Depth getDepth() {
			return depth;
		}

	}

//	static void main(String[] args) {
//		BasicSearch search = new BasicSearch();
//		search.select(DName.creationdate.qName()) //
//				.from(URI.create("/test")) //
//				.where((f) -> f.eq(DName.creationdate.qName(), ""));
//	}
}
