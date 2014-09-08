package ru.dz.ccu825;

import ru.dz.ccu825.util.CRC16;

/**
 * Represents CCU825-SM protocol packet. Does assembly/disassembly.
 * 
 * @author dz
 *
 */

public class CCU825Packet {

	private static final int PKT_MAX_PAYLOAD = 1545;
	private static final int PKT_HEADER_LEN = 8;

	public static final int MAXPACKET = PKT_MAX_PAYLOAD + PKT_HEADER_LEN + 1; // + 1 to make it even
	
	public static final byte PKT_FLAG_ENC = 0x01;
	public static final byte PKT_FLAG_SYN = 0x02;
	public static final byte PKT_FLAG_ACK = 0x04;


	// Answers
	public static final byte PKT_TYPE_RETCODE = 0x01; // ANSWER
	
	public static final byte PKT_TYPE_EMPTY = 0x00;

	public static final byte PKT_TYPE_DEVICEINFOREQ = 0x01;
	public static final byte PKT_TYPE_DEVICEINFO = 0x02; // ANSWER

	public static final byte PKT_TYPE_SYSINFO = 03;

	

	private byte[] data;
	private byte[] payload;
	
	/**
	 * Construct packet object from raw protol data received from modbus io transaction.
	 * 
	 * @param data What we've got from modbus fn23 
	 * 
	 * @throws CCU825CheckSumException
	 * @throws CCU825PacketFormatException
	 */
	
	public CCU825Packet( byte [] data ) throws CCU825CheckSumException, CCU825PacketFormatException {
		if( data.length < PKT_HEADER_LEN )
			throw new CCU825PacketFormatException("packet len < " + PKT_HEADER_LEN);

		if( data[0] != 0x01 )
			throw new CCU825PacketFormatException("Wrong header byte");
		
		int plen = checkLen(data);		
		checkCheckSum( data );
		
		payload = new byte[plen];
		System.arraycopy(data, PKT_HEADER_LEN, payload, 0, plen);
		
		this.data = data;
	}



	/**
	 * Get raw packet to send to modbus fn23.
	 * @return packet bytes.
	 */

	public byte[] getPacketBytes() {
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
	 * Check if packet length field is correct. 
	 * 
	 * @param data Packet data bytes
	 * @return Actual payload length
	 * @throws CCU825PacketFormatException Length value is insane
	 */
	
	private int checkLen(byte[] data) throws CCU825PacketFormatException  {
		
		int rll = data[6];
		int rlh = data[7];
		
		rll &= 0xFF;
		rlh &= 0xFF;
		
		int recvLen = (rlh << 8) | rll; 
		
		if( recvLen+8 < data.length )
			throw new CCU825PacketFormatException("got " + recvLen+8 + "len in pkt, actual "+ data.length);
		
		return recvLen;
	}
	
	/**
	 * Check if packet checksum is correct.
	 * NB! Clears checksum bytes in packet!
	 * @param data Packet data.
	 * @throws CCU825CheckSumException Checksum was wrong.
	 */
	
	private void checkCheckSum(byte[] data) throws CCU825CheckSumException {
		int rcl = data[4];
		int rch = data[5];
		
		rcl &= 0xFF;
		rch &= 0xFF;
		
		int recvCheckSum = (rch << 8) | rcl; 
		
		data[4] = 0;
		data[5] = 0;
		
		int calcCheckSum = makeCheckSum(data);
		
		if( calcCheckSum != recvCheckSum )
			throw new CCU825CheckSumException("got " + recvCheckSum + "in pkt, calculated "+ calcCheckSum);
	}

	private int makeCheckSum(byte[] data) {
		return CRC16.crc(data) & 0xFFFF; // Make sure int has just 16 bits
	}
	
	
	
	
	protected CCU825Packet( byte flags, byte [] payload ) {

		assert( payload.length <= PKT_MAX_PAYLOAD );
		
		this.payload = payload;
		
		int outSize = payload.length + PKT_HEADER_LEN;
		
		if( (outSize & 0x01 ) != 0 )
			outSize++;
		
		byte[] out = new byte[outSize];
		
		System.arraycopy(payload, 0, out, PKT_HEADER_LEN, payload.length);
		
		// TODO little endian! 
		
		out[0] = 0x01;
		out[1] = flags;
		
		out[2] = 0; // seq - io code will do  
		out[3] = 0; // ack - io code will do  

		out[4] = 0; // csum  
		out[5] = 0; //   

		out[6] = (byte) ( payload.length & 0xFF );  
		out[7] = (byte) ((payload.length >> 8) & 0xFF );    		
		
		int calcCheckSum = makeCheckSum(out);
		
		out[4] = (byte) ( calcCheckSum & 0xFF );  
		out[5] = (byte) ((calcCheckSum >> 8) & 0xFF );    		
		
		
		data = out;
	}




	public void setSeqNum(int seq) { data[2] = (byte)seq; }
	public void setAckNum(int seq) { data[3] = (byte)seq; }


	public int getSeqNum() { return data[2]; }
	public int getAckNum() { return data[3]; }

	



	
}
