package org.eclipse.rap.rwt.client;

import java.io.Serializable;

import org.eclipse.rap.rwt.client.service.ClientService;

public interface Client extends Serializable {

  /**
   * Returns this client's implementation of a given service, if available.
   *
   * @param type the type of the requested service, must be a subtype of ClientService
   * @return the requested service if provided by this client, otherwise <code>null</code>
   * @see ClientService
   */
  <T extends ClientService> T getService( Class<T> type );

}