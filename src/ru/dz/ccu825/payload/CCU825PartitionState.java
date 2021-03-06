package ru.dz.ccu825.payload;

import ru.dz.ccu825.CCU825Packet;
import ru.dz.ccu825.data.GuardState;
import ru.dz.ccu825.util.CCU825PacketFormatException;

/**
 * PartitionState payload decoder
 * @author dz
 *
 */

public class CCU825PartitionState {
	public static final int N_PARTITION = 4;
	
	private byte state;
	private GuardState pState[] = new GuardState[N_PARTITION];
	
	
	/**
	 * get and decode PartitionState packet payload.
	 * @param in Packet payload data
	 * @throws CCU825PacketFormatException if payload type byte is wrong
	 */
	public CCU825PartitionState(byte [] in ) throws CCU825PacketFormatException {
		//ByteBuffer bb = ByteBuffer.wrap(in);		
		//bb.order(ByteOrder.LITTLE_ENDIAN);
		
		if( in[0] != CCU825Packet.PKT_TYPE_PARTITIONSTATE )
			throw new CCU825PacketFormatException("Wrong payload header byte");
		
		state = in[1];
	
		pState[0] = GuardState.fromStateBits(state >> 0); 
		pState[1] = GuardState.fromStateBits(state >> 2); 
		pState[2] = GuardState.fromStateBits(state >> 4); 
		pState[3] = GuardState.fromStateBits(state >> 6); 

	}

	
	
	@Override
	public String toString() {	
		return "State p1="+pState[0]+" p2="+pState[1]+" p3="+pState[2]+" p4="+pState[3];
	}
		

	/*
	public byte getStateMask() {
		return state;
	}
	*/
	/**
	 * Get partition arm/protect state.
	 * @param nPartition Number of partition to query, 0 to N_PARTITION-1
	 * @return state (see GuardState for details)
	 */
	public GuardState getPartitionState( int nPartition )
	{
		return pState[nPartition];
	}

	
}
