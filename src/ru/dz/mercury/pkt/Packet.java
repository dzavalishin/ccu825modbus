package ru.dz.mercury.pkt;

import java.util.logging.Logger;

import ru.dz.ccu825.util.CRC16;
import ru.dz.mercury.Mercury230CRCException;

public class Packet {
	protected final static Logger log = Logger.getLogger(Packet.class.getName());
	
	
	/** Used in empty packets */
	protected static final byte [] empty = new byte[0];


	public static final int MAX_PASSWD_LEN = 6;

	protected static final int PKT_TYPE_CHANNEL_TEST = 0;
	protected static final int PKT_TYPE_CHANNEL_OPEN = 1;
	protected static final int PKT_TYPE_CHANNEL_CLOSE = 2; 
	protected static final int PKT_TYPE_WRITE_PARAMETER = 3;
	protected static final int PKT_TYPE_READ_RECORD = 4;
	protected static final int PKT_TYPE_READ_ENERGY = 5;
	protected static final int PKT_TYPE_READ_RAM = 6; // inaccessible, factory only
	protected static final int PKT_TYPE_WRITE_RAM = 7; // inaccessible, factory only
	protected static final int PKT_TYPE_READ_PARAMETER = 8;

	protected static final int PKT_RC_OK = 0;
	protected static final int PKT_RC_UNKNOWN_CMD_OR_PARAMETER = 1;
	protected static final int PKT_RC_INTERNAL_ERROR = 2;
	protected static final int PKT_RC_NO_ACCESS_RIGHTS = 3;
	protected static final int PKT_RC_CLOCK_ALREADY_CORRECTED = 4;
	protected static final int PKT_RC_CHANNEL_IS_NOT_OPEN = 5;

	// PKT_TYPE_WRITE_PARAMETER parameter numbers
	public static final int PKT_W_PARAM_ADDDRESS = 5;
	
	
	private int address = 0; // set in packet send code
	private byte[] payload;
	private int requestCode;
	
	private boolean returnCodePacket = false;
	private int returnCode = -1;

	/**
	 * New packet for transmission.
	 * @param address Address to send packet to, 0 is broadcast.
	 * @param payload Packet payload part.
	 */
	protected Packet(int requestCode, byte [] payload) {
		this.requestCode = requestCode;
		setPayload( payload );
	}

	/**
	 * Packet we just received.
	 * @param recvData Data from comm interface. 
	 * @throws Mercury230CRCException CRC is wrong
	 */
	public Packet(byte [] recvData) throws Mercury230CRCException 
	{
		checkCRC(recvData);
		
		setAddress( recvData[0] );
		this.requestCode = -1; // No request code in reply

		int pLen = recvData.length-3;
		payload = new byte[pLen];
		System.arraycopy(recvData, 1, payload, 0, pLen);
		
		if(pLen == 1)
		{
			returnCodePacket = true;
			returnCode = payload[0] & 0x0F;
		}
	}

	public int getAddress() {		return address;	}

	public void setAddress(int address) 
	{		
		address &= 0xFF;
		this.address = address;	
	}

	public byte[] getPayload() {		return payload;	}

	public void setPayload(byte[] payload) 
	{		
		int len = payload.length;
		this.payload = new byte[len];
		System.arraycopy(payload, 0, this.payload, 0, len);
	}
	
	
	
	// ---------------------------------------------------------------------------
	// CRC
	// ---------------------------------------------------------------------------

	public int getRequestCode() {
		return requestCode;
	}

	public void setRequestCode(int requestCode) {
		this.requestCode = requestCode;
	}

	public boolean isReturnCodePacket() {
		return returnCodePacket;
	}

	public int getReturnCode() {
		return returnCode;
	}

	private int calcCRC(byte [] payload, int len)
	{
		return CRC16.crc(payload, len);
	}

	private void checkCRC(byte [] pkt) throws Mercury230CRCException
	{
		int pLen = pkt.length;

		int crc = calcCRC(pkt,pLen-2);

		if( 
				(pkt[pLen-2] != (byte) (crc & 0xFF)) 
				||
				(pkt[pLen-1] != (byte) ((crc>>8) & 0xFF))
				)
			throw new  Mercury230CRCException(pkt);
	}
	
	// ---------------------------------------------------------------------------
	// Send
	// ---------------------------------------------------------------------------
	
	public byte[] getPacketBytes()
	{
		
		int pLen = payload.length;
		byte[] toSend = new byte[pLen+4];

		toSend[0] = (byte) address;
		toSend[1] = (byte) requestCode;

		System.arraycopy(payload, 0, toSend, 2, pLen);

		int crc = calcCRC(toSend,pLen+2);

		toSend[pLen+2] = (byte) (crc & 0xFF);
		toSend[pLen+3] = (byte) ((crc>>8) & 0xFF);
		
		return toSend;
	}
	
	protected static byte[] makeOneBytePayload(int nParam) {
		byte[] payload = new byte[1];
		payload[0] = (byte) nParam;
		return payload;
	}
	
	protected static byte[] makeTwoBytesPayload(int nParam1, int nParam2) {
		byte[] payload = new byte[2];
		payload[0] = (byte) nParam1;
		payload[1] = (byte) nParam2;
		return payload;
	}
	
}
