package org.argeo.security.crypto;

import org.argeo.util.security.Keyring;

/**
 * Advanced keyring based on cryptography that can easily be centralized and
 * coordinated with {@link KeyringLoginModule} (since they ar ein the same
 * package)
 */
public interface CryptoKeyring extends Keyring {

}
