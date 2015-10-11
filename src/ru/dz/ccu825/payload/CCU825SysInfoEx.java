package ru.dz.ccu825.payload;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ru.dz.ccu825.CCU825Packet;
import ru.dz.ccu825.data.BatteryState;
import ru.dz.ccu825.util.CCU825PacketFormatException;

/**
 * SysInfoEx payload decoder
 * @author dz
 *
 */


public class CCU825SysInfoEx extends AbstractSysInfo  {

	
	public static final int N_IN = 16;
	
	
	public CCU825SysInfoEx(byte [] in ) throws CCU825PacketFormatException {
		if( in[0] != CCU825Packet.PKT_TYPE_SYSINFO_EX )
			throw new CCU825PacketFormatException("Wrong SysInfoEx payload header byte");
		
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
	public CCU825SysInfoEx(ByteBuffer bb) 
	{		
		decodePayload(bb); 		
	}

	void decodePayload(ByteBuffer bb) {
		inValue = new double[N_IN];

		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		inBits = bb.getShort(1);		
		outBits = bb.get(35);
		
		// TODO actually order is wrong - in5 (counting from 1) appears in inValue[8]
		for( int i = 0; i < N_IN; i++ )
		{
			inValue [i] = ((double)bb.getShort(i*2+3)) * 10.0 / 4095; 
		}

		decodeS1(bb.get(36));
		decodeS2(bb.get(37));
		
		powerVoltage = (((int)bb.get(38)) & 0xFF) / 10.0;	
		batteryPercentage = bb.get(39);		
		deviceTemperature = bb.get(40);		
		GSMBalance = Float.intBitsToFloat( bb.getInt(41) );
	}

	private void decodeS1(byte S1)
	{
		armStateBits = 0;
		if( (S1 & 0x01) != 0 )
			armStateBits = 1;
		if( (S1 & 0x10) != 0 )
			armStateBits = 2;
		
		// S1 & 0x02 - zone 2 state?
	}

	private void decodeS2(byte S2)
	{
		caseOpen = (S2 & 0x02) != 0;
		balanceValid = (S2 & 0x04) != 0;
		powerOk = (S2 & 0x08) != 0;		
		batteryState = BatteryState.fromStateBits(S2>>4);
		deviceTemperatureValid = (S2 & 0x80) != 0;
	}
	
	
	@Override
	public int nInputs() {
		return N_IN;
	}
	
}
