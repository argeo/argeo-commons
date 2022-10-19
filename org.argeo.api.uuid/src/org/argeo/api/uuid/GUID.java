package org.argeo.api.uuid;

import java.util.UUID;

/**
 * A variant 6 {@link UUID}.
 * 
 * @see "https://datatracker.ietf.org/doc/html/rfc4122#section-4.1.1"
 */
public class GUID extends TypedUuid {
	private static final long serialVersionUID = APM.SERIAL;

	/** Constructor based on a {@link UUID}. */
	public GUID(UUID uuid) {
		super(uuid);
		if (uuid.variant() != 6)
			throw new IllegalArgumentException("The provided UUID is not a GUID.");
	}

	/**
	 * Formats N, D, B, P are supported:
	 * <ul>
	 * <li>D: 1db31359-bdd8-5a0f-b672-30c247d582c5</li>
	 * <li>N: 1db31359bdd85a0fb67230c247d582c5</li>
	 * <li>B: {1db31359-bdd8-5a0f-b672-30c247d582c5}</li>
	 * <li>P: (1db31359-bdd8-5a0f-b672-30c247d582c5)</li>
	 * </ul>
	 * 
	 * @see "https://docs.microsoft.com/en-us/dotnet/api/system.guid.tostring"
	 */
	public static String toString(UUID uuid, char format, boolean upperCase) {
		String str;
		switch (format) {
		case 'D':
			str = uuid.toString();
			break;
		case 'N':
			str = UuidBinaryUtils.toCompact(uuid);
			break;
		case 'B':
			str = "{" + uuid.toString() + "}";
			break;
		case 'P':
			str = "(" + uuid.toString() + ")";
			break;
		default:
			throw new IllegalArgumentException("Unsupported format : " + format);
		}
		return upperCase ? str.toUpperCase() : str;
	}

}
