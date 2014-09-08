package ru.dz.ccu825.pkt;

import ru.dz.ccu825.CCU825Packet;
import ru.dz.ccu825.util.CCU825CheckSumException;
import ru.dz.ccu825.util.CCU825PacketFormatException;

public class CCU825SysInfoReqPacket extends CCU825Packet {
	
	private static final byte[] data = { CCU825Packet.PKT_TYPE_INFOREQ, PKT_TYPE_SYSINFO_SUBREQ } ;

	public CCU825SysInfoReqPacket()
			throws CCU825CheckSumException, CCU825PacketFormatException {
		
		super( (byte)0x00, data);

	}


}
