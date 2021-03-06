package ru.dz.ccu825.util;

/**
 * Thrown if packet with a wrong format or unexpected packet type is received
 * @author dz
 *
 */


public class CCU825PacketFormatException extends CCU825ProtocolException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2300204512620994535L;

	public CCU825PacketFormatException() {
	}

	public CCU825PacketFormatException(String message) {
		super(message);
	}

	public CCU825PacketFormatException(Throwable cause) {
		super(cause);
	}

	public CCU825PacketFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	public CCU825PacketFormatException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
