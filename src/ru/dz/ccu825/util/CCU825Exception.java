package ru.dz.ccu825.util;

/**
 * Common class for all CCU825 library exceptions.
 * @author dz
 *
 */


public class CCU825Exception extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1448322018088722728L;

	public CCU825Exception() {
	}

	public CCU825Exception(String message) {
		super(message);
	}

	public CCU825Exception(Throwable cause) {
		super(cause);
	}

	public CCU825Exception(String message, Throwable cause) {
		super(message, cause);
	}

	public CCU825Exception(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
