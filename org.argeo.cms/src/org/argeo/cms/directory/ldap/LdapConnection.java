package org.argeo.cms.directory.ldap;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;

import org.argeo.api.acr.ldap.LdapAttr;
import org.argeo.api.cms.transaction.WorkingCopy;

/** A synchronized wrapper for a single {@link InitialLdapContext}. */
// TODO implement multiple contexts and connection pooling.
public class LdapConnection {
	private InitialLdapContext initialLdapContext = null;

	public LdapConnection(String url, Dictionary<String, ?> properties) {
		try {
			Hashtable<String, Object> connEnv = new Hashtable<String, Object>();
			connEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			connEnv.put(Context.PROVIDER_URL, url);
			connEnv.put("java.naming.ldap.attributes.binary", LdapAttr.userPassword.name());
			// use pooling in order to avoid connection timeout
//			connEnv.put("com.sun.jndi.ldap.connect.pool", "true");
//			connEnv.put("com.sun.jndi.ldap.connect.pool.timeout", 300000);

			initialLdapContext = new InitialLdapContext(connEnv, null);
			// StartTlsResponse tls = (StartTlsResponse) ctx
			// .extendedOperation(new StartTlsRequest());
			// tls.negotiate();
			Object securityAuthentication = properties.get(Context.SECURITY_AUTHENTICATION);
			if (securityAuthentication != null)
				initialLdapContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, securityAuthentication);
			else
				initialLdapContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
			Object principal = properties.get(Context.SECURITY_PRINCIPAL);
			if (principal != null) {
				initialLdapContext.addToEnvironment(Context.SECURITY_PRINCIPAL, principal.toString());
				Object creds = properties.get(Context.SECURITY_CREDENTIALS);
				if (creds != null) {
					initialLdapContext.addToEnvironment(Context.SECURITY_CREDENTIALS, creds.toString());
				}
			}
		} catch (NamingException e) {
			throw new IllegalStateException("Cannot connect to LDAP", e);
		}

	}

	public void init() {

	}

	public void destroy() {
		try {
			// tls.close();
			initialLdapContext.close();
			initialLdapContext = null;
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	protected InitialLdapContext getLdapContext() {
		return initialLdapContext;
	}

	protected void reconnect() throws NamingException {
		initialLdapContext.reconnect(initialLdapContext.getConnectControls());
	}

	public synchronized NamingEnumeration<SearchResult> search(LdapName searchBase, String searchFilter,
			SearchControls searchControls) throws NamingException {
		NamingEnumeration<SearchResult> results;
		try {
			results = getLdapContext().search(searchBase, searchFilter, searchControls);
		} catch (CommunicationException e) {
			reconnect();
			results = getLdapContext().search(searchBase, searchFilter, searchControls);
		}
		return results;
	}

	public synchronized Attributes getAttributes(LdapName name) throws NamingException {
		try {
			return getLdapContext().getAttributes(name);
		} catch (CommunicationException e) {
			reconnect();
			return getLdapContext().getAttributes(name);
		}
	}

	public synchronized boolean entryExists(LdapName name) throws NamingException {
		String[] noAttrOID = new String[] { "1.1" };
		try {
			getLdapContext().getAttributes(name, noAttrOID);
			return true;
		} catch (CommunicationException e) {
			reconnect();
			getLdapContext().getAttributes(name, noAttrOID);
			return true;
		} catch (NameNotFoundException e) {
			return false;
		}
	}

	public synchronized void prepareChanges(WorkingCopy<?, ?, LdapName> wc) throws NamingException {
		// make sure connection will work
		reconnect();

		// delete
		for (LdapName dn : wc.getDeletedData().keySet()) {
			if (!entryExists(dn))
				throw new IllegalStateException("User to delete no found " + dn);
		}
		// add
		for (LdapName dn : wc.getNewData().keySet()) {
			if (entryExists(dn))
				throw new IllegalStateException("User to create found " + dn);
		}
		// modify
		for (LdapName dn : wc.getModifiedData().keySet()) {
			if (!wc.getNewData().containsKey(dn) && !entryExists(dn))
				throw new IllegalStateException("User to modify not found " + dn);
		}

	}

//	protected boolean entryExists(LdapName dn) throws NamingException {
//		try {
//			return getAttributes(dn).size() != 0;
//		} catch (NameNotFoundException e) {
//			return false;
//		}
//	}

	public synchronized void commitChanges(LdapEntryWorkingCopy wc) throws NamingException {
		// delete
		for (LdapName dn : wc.getDeletedData().keySet()) {
			getLdapContext().destroySubcontext(dn);
		}
		// add
		for (LdapName dn : wc.getNewData().keySet()) {
			LdapEntry user = wc.getNewData().get(dn);
			getLdapContext().createSubcontext(dn, user.getAttributes());
		}
		// modify
		for (LdapName dn : wc.getModifiedData().keySet()) {
			Attributes modifiedAttrs = wc.getModifiedData().get(dn);
			getLdapContext().modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, modifiedAttrs);
		}
	}
}
