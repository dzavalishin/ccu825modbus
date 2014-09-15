package ru.dz.ccu825.payload;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ru.dz.ccu825.CCU825Packet;
import ru.dz.ccu825.util.CCU825PacketFormatException;

/**
 * SysInfo payload decoder
 * @author dz
 *
 */


public class CCU825SysInfo implements ICCU825SysInfo {

	
	public static final int N_IN = 8;
	
	
	private final int inBits;
	private final int outBits;
	
	private final double[] inValue = new double[N_IN];

	private final boolean powerOk;
	private final boolean balanceValid;
	private final boolean caseOpen;

	private final byte batteryPercentage;
	private final byte deviceTemperature;

	private final double powerVoltage;
	private final double GSMBalance;


	public CCU825SysInfo(byte [] in ) throws CCU825PacketFormatException {
		ByteBuffer bb = ByteBuffer.wrap(in);
		
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		if( in[0] != CCU825Packet.PKT_TYPE_SYSINFO )
			throw new CCU825PacketFormatException("Wrong SysInfo payload header byte");
		
		inBits = ((int)in[1]) & 0xFF;
		outBits = ((int)in[18]) & 0xFF;
		
		for( int i = 0; i < N_IN; i++ )
		{
			inValue [i] = ((double)bb.getShort(i+2)) * 10.0 / 4095; 
		}

		// TODO extract as sub-object
		{
		byte S1 = in[19];
		powerOk = (S1 & 0x08) != 0;		
		balanceValid = (S1 & 0x04) != 0;
		}
		
		{
		byte S2 = in[20];
		caseOpen = (S2 & 0x01) != 0;
		}
		
		powerVoltage = ((double)in[21])/10.0;
		
		batteryPercentage = in[22];
		
		deviceTemperature = in[23];
		
		GSMBalance = Float.intBitsToFloat( bb.getInt(24) ); 
		
	}

	
	
	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#toString()
	 */
	@Override
	public String toString() {
		
		return 
				"In bits "+inBits+" out bits "+outBits+" GSM balance "+GSMBalance+" battery "+batteryPercentage+"% temp "+deviceTemperature+" voltage "+
				powerVoltage
				;
	}
		

	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#getInBits()
	 */
	@Override
	public int getInBits() {		return inBits;	}


	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#getOutBits()
	 */
	@Override
	public int getOutBits() {		return outBits;	}

	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#getInValue()
	 */
	@Override
	public double[] getInValue() {		return inValue;	}


	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#isPowerOk()
	 */
	@Override
	public boolean isPowerOk() {		return powerOk;	}


	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#isBalanceValid()
	 */
	@Override
	public boolean isBalanceValid() {		return balanceValid;	}


	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#isCaseOpen()
	 */
	@Override
	public boolean isCaseOpen() {		return caseOpen;	}


	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#getBatteryPercentage()
	 */
	@Override
	public byte getBatteryPercentage() {		return batteryPercentage;	}


	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#getDeviceTemperature()
	 */
	@Override
	public byte getDeviceTemperature() {		return deviceTemperature;	}


	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#getPowerVoltage()
	 */
	@Override
	public double getPowerVoltage() {		return powerVoltage;	}


	/* (non-Javadoc)
	 * @see ru.dz.ccu825.payload.ICCU825SysInfo#getGSMBalance()
	 */
	@Override
	public double getGSMBalance() {		return GSMBalance; }



	@Override
	public int nInputs() {		return N_IN;	}
	
}
