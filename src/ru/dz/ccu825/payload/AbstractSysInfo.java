package ru.dz.ccu825.payload;

import java.nio.ByteBuffer;

import ru.dz.ccu825.data.BatteryState;

/**
 * 
 * General sysInfo/sysInfoEx implementation.
 * 
 * @author dz
 *
 */
public abstract class AbstractSysInfo implements ICCU825SysInfo
{

	protected int inBits;
	protected int outBits;
	
	protected double[] inValue;
	
	protected boolean powerOk;
	protected boolean balanceValid;
	protected boolean caseOpen;
	protected boolean deviceTemperatureValid;
	protected BatteryState batteryState;
	
	protected byte batteryPercentage;
	protected byte deviceTemperature;
	protected double powerVoltage;
	protected double GSMBalance;
	protected int armStateBits = 0;

	AbstractSysInfo() {
		super();
	}

	abstract void decodePayload(ByteBuffer bb);



	@Override
	public String toString() {

		return String.format("In 0x%04X Out 0x%02X GSM bal %2.0f Battery %d%% Temp %d Voltage %.1f", inBits, outBits, GSMBalance, batteryPercentage, deviceTemperature, powerVoltage );

		/*
		return 
				"In bits "+inBits+" out bits "+outBits+" GSM balance "+GSMBalance+" battery "+batteryPercentage+"% temp "+deviceTemperature+" voltage "+
				powerVoltage
				;
		*/
	}

	@Override
	public int getInBits() {		return inBits;	}

	@Override
	public int getOutBits() {		return outBits;	}

	/**
	 * @return Array of input values in volts
	 */
	@Override
	public double[] getInValue() {		return inValue;	}

	@Override
	public boolean isPowerOk() {		return powerOk;	}

	@Override
	public boolean isBalanceValid() {		return balanceValid;	}

	@Override
	public boolean isCaseOpen() {		return caseOpen;	}

	@Override
	public byte getBatteryPercentage() {		return batteryPercentage;	}

	@Override
	public byte getDeviceTemperature() {		return deviceTemperature;	}

	@Override
	public double getPowerVoltage() {		return powerVoltage;	}

	@Override
	public double getGSMBalance() {		return GSMBalance;	}

	@Override
	public boolean isDeviceTemperatureValid() {
		return deviceTemperatureValid;
	}

	@Override
	public BatteryState getBatteryState() {
		return batteryState;
	}

}