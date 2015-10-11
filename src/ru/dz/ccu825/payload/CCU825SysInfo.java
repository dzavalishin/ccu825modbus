package ru.dz.ccu825.payload;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ru.dz.ccu825.CCU825Packet;
import ru.dz.ccu825.data.BatteryState;
import ru.dz.ccu825.util.CCU825PacketFormatException;

/**
 * SysInfo payload decoder
 * @author dz
 *
 */


public class CCU825SysInfo extends AbstractSysInfo  {

	public static final int N_IN = 8;
	
	
	public CCU825SysInfo(byte [] in ) throws CCU825PacketFormatException {
		
		if( in[0] != CCU825Packet.PKT_TYPE_SYSINFO )
			throw new CCU825PacketFormatException("Wrong SysInfo payload header byte");
		
		ByteBuffer bb = ByteBuffer.wrap(in);
		
		decodePayload(bb); 		
	}

	@Override
	public int getInputsCount() {
		return N_IN;
	}

	/**
	 * Package private constructor, used to decode similar payload in other packets.
	 * @param bb data to decode, no type byte check!
	 */
	public CCU825SysInfo(ByteBuffer bb) 
	{		
		decodePayload(bb); 		
	}


	void decodePayload(ByteBuffer bb)
	{
		inValue = new double[N_IN];
		
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		//inBits = ((int)in[1]) & 0xFF;
		//outBits = ((int)in[18]) & 0xFF;
		inBits = ((int)bb.get(1)) & 0xFF;
		outBits = ((int)bb.get(18)) & 0xFF;
		
		for( int i = 0; i < N_IN; i++ )
		{
			inValue [i] = ((double)bb.getShort(i*2+2)) * 10.0 / 4095; 
		}

		decodeS1(bb.get(19));
		decodeS2(bb.get(20));
		
		powerVoltage = ((double)bb.get(21))/10.0;		
		batteryPercentage = bb.get(22);		
		deviceTemperature = bb.get(23);		
		GSMBalance = Float.intBitsToFloat( bb.getInt(24) );
	}

	private void decodeS1(byte S1)
	{
		powerOk = (S1 & 0x08) != 0;		
		balanceValid = (S1 & 0x04) != 0;
		deviceTemperatureValid = (S1 & 0x80) != 0;
		batteryState = BatteryState.fromStateBits(S1>>4);
		armStateBits = S1 & 0x03;
	}

	private void decodeS2(byte S2)
	{
		caseOpen = (S2 & 0x01) != 0;
	}
	
	
	@Override
	public int nInputs() {		return N_IN;	}

	
}
