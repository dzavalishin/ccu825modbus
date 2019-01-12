package ru.dz.ccu825;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import ru.dz.ccu825.util.CCU825CheckSumException;
import ru.dz.ccu825.util.CCU825PacketFormatException;
import ru.dz.ccu825.util.CRC16;
import ru.dz.ccu825.util.RC4;

/**
 * Represents CCU825-SM protocol packet. Does assembly/disassembly.
 * 
 * @author dz
 *
 */

public class CCU825Packet {
	private final static Logger log = Logger.getLogger(CCU825Packet.class.getName());

	/** Maximum size of packet payload, bytes. As written in protocol definition. */
	//private static final int PKT_MAX_PAYLOAD = 1545;

	/** Maximum size of packet payload, bytes. As really works. */
	private static final int PKT_MAX_PAYLOAD = 250-10;
	
	/** Packet header length, bytes. */
	private static final int PKT_HEADER_LEN = 8;

	public static final int MAXPACKET = PKT_MAX_PAYLOAD + PKT_HEADER_LEN + 1; // + 1 to make it even
	
	/** Packet header flag, packet is encrypted. */
	public static final byte PKT_FLAG_ENC = 0x01;
	/** Packet header flag, packet is a part of a sync sequence. */
	public static final byte PKT_FLAG_SYN = 0x02;
	/** Packet header flag, packet is a device reply in a sync sequence. */
	public static final byte PKT_FLAG_ACK = 0x04;


	// Answers
	public static final byte PKT_TYPE_RETCODE = 0x01;
	public static final byte PKT_TYPE_DEVICEINFO = 0x02;
	public static final byte PKT_TYPE_SYSINFO = 0x03;
	public static final byte PKT_TYPE_EVENTS = 0x04;
	public static final byte PKT_TYPE_PARTITIONSTATE = 0x05;
	public static final byte PKT_TYPE_OUTSTATE = 0x06;
	public static final byte PKT_TYPE_SYSINFO_EX = 0x0C;
	public static final byte PKT_TYPE_EVENTS_EX = 0x0D;

	// REQUESTS
	public static final byte PKT_TYPE_EMPTY = 0x00;

	public static final byte PKT_TYPE_INFOREQ = 0x01;
	
	// 2nd byte for PKT_TYPE_INFOREQ req
	public static final byte PKT_TYPE_DEVICEINFO_SUBREQ = 0x01;
	public static final byte PKT_TYPE_SYSINFO_SUBREQ = 0x00;
	public static final byte PKT_TYPE_OUTSTATE_SUBREQ = 0x03;
	public static final byte PKT_TYPE_PARTITIONSTATE_SUBREQ = 0x02;




	

	
	/** Complete packet data. On reception checksum bytes are cleared. */
	private byte[] data;
	private byte[] payload;

	private int dataSize;
	private boolean unaligned = false;
	
	/**
	 * Construct packet object from raw protocol data received from ModBus IO transaction.
	 * 
	 * @param iData What we've got from ModBus fn23 
	 * @param key Encryption key. See README.
	 * 
	 * @throws CCU825CheckSumException If checksum does not match.
	 * @throws CCU825PacketFormatException If something is really wrong, like size or prefix.
	 */
	
	public CCU825Packet( byte [] iData, byte[] key ) throws CCU825CheckSumException, CCU825PacketFormatException {
		if( iData.length < PKT_HEADER_LEN )
			throw new CCU825PacketFormatException("packet len < " + PKT_HEADER_LEN);

		if( iData[0] != 0x01 )
			throw new CCU825PacketFormatException("Wrong header byte");
		
		byte[] data = new byte[iData.length];
		System.arraycopy(iData, 0, data, 0, iData.length);
		
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		short pktLen = bb.getShort(6);
		short pktCs = bb.getShort(4);
		
		int plen = checkLen(data, pktLen );		
		checkCheckSum( data, pktLen+8, pktCs );

		// Packet is ok
		
		this.data = data;
		this.dataSize = pktLen+8;
		
		payload = new byte[plen];
		System.arraycopy(data, PKT_HEADER_LEN, payload, 0, plen);
		
		// Encoded? Decode here.
		if( isEnc() )
		{
			RC4 dec = new RC4(key);
			payload = dec.decrypt(payload);
		}
		
	}



	/**
	 * Prepare raw packet to send to ModBus fn23.
	 * @return packet bytes.
	 */

	public byte[] getPacketBytes(byte[] key) {
		
		// Encryption is requested? Do.
		if(isEnc())
		{
			assert(key != null);
			
			byte[] _payload;
			
			RC4 enc = new RC4(key);
			_payload = enc.encrypt(payload);
			
			System.arraycopy(_payload, 0, data, 8, _payload.length);
		}

		int payLen = payload.length;

		// Last byte is paddind for an odd length payload?
		// Set last byte to zero! Encryption might set it to nonzero value.
		if(isUnaligned())
		{
			data[data.length-1] = 0;
			payLen++;
		}
		
		// Payload length, INCLUDING PAD BYTE
		data[6] = (byte) ( payLen & 0xFF );  
		data[7] = (byte) ((payLen >> 8) & 0xFF );    		
		
		// Clear checksum field for calculating checksum.
		data[4] = 0;
		data[5] = 0;
		
		int calcCheckSum = makeCheckSum(data,dataSize);
		
		data[4] = (byte) ( calcCheckSum & 0xFF );  
		data[5] = (byte) ((calcCheckSum >> 8) & 0xFF );    		
		
		return data;
	}


	/**
	 * Get packet payload data (offset 8).
	 * @return Payload bytes.
	 */
	
	public byte[] getPacketPayload() {
		return payload;
	}
	
	
	/**
	 * Set packet's SYN header flag.
	 * @param b set or reset
	 */
	
	public void setSyn(boolean b) {
		if( b )
			data[1] |= PKT_FLAG_SYN;
		else
			data[1] &= ~PKT_FLAG_SYN;
	}
	
	
	/**
	 * Set packet's ENC (encrypted) header flag.
	 * @param encryptionEnabled set or reset
	 */

	public void setEnc(boolean encryptionEnabled) {
		if( encryptionEnabled )
			data[1] |= PKT_FLAG_ENC;
		else
			data[1] &= ~PKT_FLAG_ENC;
	}

		
	/**
	 * @return True if packet header has SYN flag.
	 */
	
	public boolean isSyn() {
		return (data[1] & PKT_FLAG_SYN) != 0;
	}

	/**
	 * @return True if packet header has ACK flag.
	 */
	
	public boolean isAck() {
		return (data[1] & PKT_FLAG_ACK) != 0;
	}
	
	
	/**
	 * @return True if packet header has ENC flag. (Packet payload is encrypted)	
	 */
	
	public boolean isEnc() {
		return (data[1] & PKT_FLAG_ENC) != 0;
	}
	
	
	
	/**
	 * Check if packet length field is correct. 
	 * 
	 * @param data Packet data bytes
	 * @param recvLen length field from packet 
	 * @return Actual <b>payload</b> length
	 * @throws CCU825PacketFormatException Length value is insane
	 */
	
	private int checkLen(byte[] data, short recvLen) throws CCU825PacketFormatException  
	{
		if( recvLen+8 > data.length )
			throw new CCU825PacketFormatException("got len=" + recvLen+8 + " in pkt, actual "+ data.length);
		
		return recvLen;
	}
	
	/**
	 * Check if packet checksum is correct.
	 * <b>NB! Clears checksum bytes in packet!</b>
	 * 
	 * @param data Packet data.
	 * @param recvCheckSum checksum field of the packet. 
	 * @throws CCU825CheckSumException Checksum was wrong.
	 */
	
	private void checkCheckSum(byte[] data, int len, int recvCheckSum) throws CCU825CheckSumException 
	{
		data[4] = 0;
		data[5] = 0;
		
		int calcCheckSum = makeCheckSum(data,len);
		
		if( calcCheckSum != (recvCheckSum & 0xFFFF) )
		{
			String msg = String.format( "got checksum=%04X in pkt, calculated=%04X", recvCheckSum, calcCheckSum );
			log.warning( msg );
			throw new CCU825CheckSumException( msg );
		}
	}

	
	/** 
	 * Calculate a checksum for a packet.
	 * 
	 * @param data packet
	 * @param len length of packet, bytes.
	 * @return 16 bits of a checksum
	 */
	
	private int makeCheckSum(byte[] data, int len) {
		return CRC16.crc(data,len) & 0xFFFF; // Make sure int has just 16 bits
	}
	
	
	
	/**
	 * Construct packet for transmission from flags byte and payload. 
	 * @param flags - usually zero, except for handshake.
	 * @param payload - data bytes to transmit.
	 */
	
	
	protected CCU825Packet( byte flags, byte [] payload ) {

		assert( payload.length <= PKT_MAX_PAYLOAD );
		
		this.payload = new byte[payload.length];
		System.arraycopy(payload, 0, this.payload, 0, payload.length);
		
		int outSize = payload.length + PKT_HEADER_LEN;
		
		// Odd pkt size? Pad.
		unaligned = (outSize & 0x01 ) != 0;
		if( unaligned )			outSize++;
		
		byte[] out = new byte[outSize];
		
		System.arraycopy(payload, 0, out, PKT_HEADER_LEN, payload.length);
		
		out[0] = 0x01;
		out[1] = flags;
		
		out[2] = 0; // seq - io code will do  
		out[3] = 0; // ack - io code will do  

		out[4] = 0; // csum  
		out[5] = 0; //   

		data = out;
		dataSize = outSize;
	}



	/** Set packet sequential number. <b>For protocol code internal use only.</b> */
	public void setSeqNum(int seq) { data[2] = (byte)seq; }
	/** Set packet acknowledge number. <b>For protocol code internal use only.</b> */
	public void setAckNum(int seq) { data[3] = (byte)seq; }


	/** Get packet sequential number. <b>For protocol code internal use only.</b> */
	public int getSeqNum() { return ((int)data[2]) & 0xFF; }
	/** Get packet acknowledge number. <b>For protocol code internal use only.</b> */
	public int getAckNum() { return ((int)data[3]) & 0xFF; }

	/** 
	 * Dump packet header state. 
	 * 
	 * @return String describing this packet header.
	 */
	@Override
	public String toString()
	{		
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);

		short pktLen = bb.getShort(6);
		//short pktCs = bb.getShort(4);
	
		return String.format("pkt len %d, seq %d ack %d %s%s%s", pktLen, getSeqNum(), getAckNum(),
				isEnc() ? "Enc " : "",
				isSyn() ? "Syn " : "",
				isAck() ? "Ack " : ""
				);
	}


	/** 
	 * Original payload length was odd. Used by protocol code.
	 * 
	 * @return True if original payload size was odd and we added one pad byte to the end.
	 */
	public boolean isUnaligned() {
		return unaligned;
	}
	
	
}
