package ru.dz.ccu825;

public class CCU825DeviceInfoAckPacket extends CCU825Packet {

	private static final byte[] data = { CCU825Packet.PKT_TYPE_DEVICEINFOREQ, 0x0B, 0x00 } ;

	public CCU825DeviceInfoAckPacket()
			throws CCU825CheckSumException, CCU825PacketFormatException {
		
		super( (byte)0x00, data);

	}

}
