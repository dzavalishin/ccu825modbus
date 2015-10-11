package ru.dz.ccu825.payload;

import ru.dz.ccu825.data.BatteryState;

/**
 * sysInfo/sysInfoEx interface.
 * 
 * @author dz
 *
 */
public interface ICCU825SysInfo {

	/**
	 * @return Number of inputs for actual type of reply.
	 */
	public abstract int nInputs(); 
	
	public abstract String toString();

	/**
	 * @return All inputs as boolean (bits).
	 */
	public abstract int getInBits();

	/**
	 * @return All outputs as boolean (bits).
	 */
	public abstract int getOutBits();

	/**
	 * @return Array of input values in volts
	 */
	public abstract double[] getInValue();

	public abstract boolean isPowerOk();

	public abstract boolean isBalanceValid();

	public abstract boolean isCaseOpen();

	public abstract byte getBatteryPercentage();

	public abstract byte getDeviceTemperature();

	public abstract double getPowerVoltage();

	public abstract double getGSMBalance();

	public abstract BatteryState getBatteryState();

	public abstract boolean isDeviceTemperatureValid();

	public abstract int getInputsCount();

}