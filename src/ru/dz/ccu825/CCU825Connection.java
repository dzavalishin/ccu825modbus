package ru.dz.ccu825;

import java.util.logging.Logger;

import ru.dz.ccu825.data.CCU825ReturnCode;
import ru.dz.ccu825.payload.CCU825DeviceInfo;
import ru.dz.ccu825.payload.CCU825Events;
import ru.dz.ccu825.payload.CCU825EventsEx;
import ru.dz.ccu825.payload.CCU825OutState;
import ru.dz.ccu825.payload.CCU825SysInfo;
import ru.dz.ccu825.payload.CCU825SysInfoEx;
import ru.dz.ccu825.payload.ICCU825Events;
import ru.dz.ccu825.payload.ICCU825SysInfo;
import ru.dz.ccu825.pkt.CCU825DeviceInfoAckPacket;
import ru.dz.ccu825.pkt.CCU825DeviceInfoReqPacket;
import ru.dz.ccu825.pkt.CCU825EventsReqPacket;
import ru.dz.ccu825.pkt.CCU825OutStateCmdPacket;
import ru.dz.ccu825.pkt.CCU825SysInfoReqPacket;
import ru.dz.ccu825.pkt.CCU825ZeroLenghPacket;
import ru.dz.ccu825.transport.ICCU825KeyRing;
import ru.dz.ccu825.transport.IModBusConnection;
import ru.dz.ccu825.util.CCU825Exception;
import ru.dz.ccu825.util.CCU825PacketFormatException;
import ru.dz.ccu825.util.CCU825ProtocolException;
import ru.dz.ccu825.util.RC4;

/**
 *
 * Connection to an instance of CCU825 device. Implements device-specific protocol over the (implemented elsewhere) modbus.
 * <p>
 * Conforms to CCU825-SM protocol spec. from 5 September 2014, firmware ver. 01.02.
 * 
 * Needs an external modbus io driver to work.
 * 
 * @author dz
 *
 */

public class CCU825Connection {

	private static final int NTRIES = 5;

	private final IModBusConnection mc;
	private final ICCU825KeyRing keyRing;
	/** communications encryption key. Contact radsel to get one. Have your device IMEI handy. */
	private byte[] key = null;

	private int currentSeq = 0;
	private int currentAck = 0;

	private int lastRecvSeq;

	private CCU825DeviceInfo deviceInfo;

	private boolean encryptionEnabled = false;

	private boolean dataDumpEnabled = false;

	private boolean packetDumpEnabled = true;


	/**
	 * Initialize a connection. Does nothing.
	 * 
	 * @param mc ModbBus connection implementation, used just for fn23 io
	 * @param keyRing Source to get a key for a given IMEI from
	 * @throws CCU825Exception Mostly due to communication or protocol errors.
	 */

	public CCU825Connection( IModBusConnection mc, ICCU825KeyRing keyRing  ) {
		this.mc = mc;
		this.keyRing = keyRing;
		
		//dataDumpEnabled = true;
	}

	/**
	 * Connect and do handshake as required by protocol.
	 * @return handshake result.
	 * @throws CCU825Exception in case of communications error or device malfunction
	 */
	
	public CCU825ReturnCode connect() throws CCU825Exception
	{
		setupModBus();
		mc.connect();

		return initProtocol();
	}
	
	/**
	 * 
	 * Set mode. Must be ModBus RTU, 9600, 8N1
	 * 
	 */
	private void setupModBus() {
		
		mc.setSpeed(9600);		
	}

	/** 
	 * Actually just calls underlying ModBus connector disconnect().
	 */
	public void disconnect()
	{
		mc.disconnect();
	}

	/** 
	 * Do a protocol transaction sending and receiving one packet.
	 * 
	 * @param send Packet to send
	 * @return Packet received
	 * @throws CCU825ProtocolException 
	 */
	
	private CCU825Packet exchange( CCU825Packet send ) throws CCU825ProtocolException
	{
		if( packetDumpEnabled  ) CCU825Test.dumpBytes( "send packet pl", send.getPacketPayload() );

		send.setSeqNum( currentSeq++ );
		send.setAckNum( currentAck );
		//send.setAckNum( lastRecvSeq );

		send.setEnc(encryptionEnabled);

		byte [] packetBytes = send.getPacketBytes();

		int writeBytes = packetBytes.length;

		assert( (writeBytes & 1) == 0 );

		if(encryptionEnabled)
		{
			assert(key != null);
			
			byte[] payload = new byte[writeBytes-8];
			System.arraycopy(packetBytes,8,payload,0,writeBytes-8);
			
			RC4 enc = new RC4(key);
			payload = enc.encrypt(payload);
			
			//spd = new byte[writeBytes];
			System.arraycopy(payload, 0, packetBytes, 8, writeBytes-8);
		}

		int recvShortsCount = 125; // (CCU825Packet.MAXPACKET+1)/2

		if( dataDumpEnabled ) CCU825Test.dumpBytes( "modbus send", packetBytes );
		byte[] rcv = mc.rwMultiple( recvShortsCount , packetBytes );
		if( dataDumpEnabled) CCU825Test.dumpBytes( "modbus recv", rcv );

		
		CCU825Packet rp = new CCU825Packet(rcv,key);

		int recvAck = rp.getAckNum();
		lastRecvSeq = rp.getSeqNum();

		// It seems to be a "can't happen" thing in modbus-based proto, as modbus io is syncronous, but...

		if( recvAck != currentSeq )
		{
			String msg = "our seq = " + currentSeq + " recv ack = " + recvAck; 
			logErr(msg);
			throw new CCU825PacketFormatException(msg);
		}

		if( currentAck != lastRecvSeq )
			logErr("our ack = " + (currentAck) + " recv seq = " + lastRecvSeq);

		currentAck++;

		if( packetDumpEnabled  ) CCU825Test.dumpBytes( "recv packet pl", rp.getPacketPayload() );
		if( packetDumpEnabled  ) System.out.println(rp);
		
		return rp;
	}

	/**
	 * Do an initial protocol transactions (handshake) as defined 
	 * in CCU825-SM protocol spec. from 5 September 2014, firmware ver. 01.02
	 * 
	 * @return device return code
	 * @throws CCU825Exception
	 */

	private CCU825ReturnCode initProtocol() throws CCU825Exception {
		int tries;

		// Start with no encryption
		setEncryptionEnabled(false);

		// 1. send/get syn
		//dataDumpEnabled = true;
		
		for( tries = NTRIES; tries > 0; tries-- )
		{

			try {
				CCU825ZeroLenghPacket sp = new CCU825ZeroLenghPacket();

				sp.setSyn( true );

				CCU825Packet rp = exchange( sp );

				byte[] rdata = rp.getPacketBytes();

				/*
				if( rdata[0] != CCU825Packet.PKT_TYPE_EMPTY )
					logErr("wrong packet type" + rdata[0] );
				*/
				if( !rp.isSyn() )
					throw new CCU825PacketFormatException("no syn in reply" + rdata[1]);
				//logErr("no syn in reply" + rdata[1] );

				if( !rp.isAck() )
					throw new CCU825PacketFormatException("no ack in reply" + rdata[1]);

				break;

			} catch( CCU825ProtocolException ex )
			{
				logProtoErr( ex );
			}

		}

		if( tries == 0 ) throw new CCU825Exception("Can't syn");

		// 2. req/get deviceInfo

		for( tries = NTRIES; tries > 0; tries-- )
		{

			try {
				CCU825Packet rp = exchange(new CCU825DeviceInfoReqPacket() );		
				deviceInfo = new CCU825DeviceInfo(rp.getPacketPayload());
				break;
			} catch( CCU825ProtocolException ex )
			{
				logProtoErr( ex );
			}

		}

		if( tries == 0 ) throw new CCU825Exception("Can't get device info");

		System.out.println(deviceInfo);
		// Now find out an encryption key and switch to encrypted mode
		key = keyRing.getKeyForIMEI(deviceInfo.getIMEI());
		if( key == null ) throw new CCU825ProtocolException("No key for IMEI="+deviceInfo.getIMEI());
		setEncryptionEnabled(true);
		
		// 3. Ack devinfo and get return code

		int protocolRC = 0;

		for( tries = NTRIES; tries > 0; tries-- )
		{

			try {
				CCU825Packet rp = exchange(new CCU825DeviceInfoAckPacket() );

				byte[] rdata = rp.getPacketPayload();

				if( rdata[0] != CCU825Packet.PKT_TYPE_RETCODE )
					logErr("wrong packet type" + rdata[0] );

				protocolRC = rdata[1];
				break;
				
			} catch( CCU825ProtocolException ex )
			{
				logProtoErr( ex );
			}

		}

		if( tries == 0 ) throw new CCU825Exception("Can't ack device info");

		return new CCU825ReturnCode( protocolRC );		
	}


	private static final Logger log = Logger.getLogger(CCU825Connection.class.getName()); 

	private void logErr(String string) {
		//System.err.println("Error: "+ string );
		log.severe(string);
	}

	private void logProtoErr(CCU825ProtocolException ex) {
		//System.err.println("Protocol err: "+ex.getMessage());
		log.severe("Protocol err: "+ex.getMessage());
	}




	/**
	 * Does a request to get general controller state - inputs, outputs, etc.
	 * @return System information (i/o state etc) at the current moment
	 * @throws CCU825ProtocolException
	 */
	public ICCU825SysInfo getSysInfo() throws CCU825ProtocolException 
	{
		CCU825Packet rp = exchange(new CCU825SysInfoReqPacket() );
		switch( rp.getPacketPayload()[0] )
		{
		case CCU825Packet.PKT_TYPE_SYSINFO:
			return new CCU825SysInfo(rp.getPacketPayload());
		case CCU825Packet.PKT_TYPE_SYSINFO_EX:
			return new CCU825SysInfoEx(rp.getPacketPayload());
		}
		
		throw new CCU825PacketFormatException(String.format("sysInfo payload type is %X", rp.getPacketPayload()[0]));
	}

	/**
	 * Does a request to get Events/EventsEx.
	 * 
	 * @return Events list + system information (i/o state etc) at the current moment
	 * @throws CCU825ProtocolException
	 */
	public ICCU825Events getEvents() throws CCU825ProtocolException 
	{
		CCU825Packet rp = exchange(new CCU825EventsReqPacket() );
		switch( rp.getPacketPayload()[0] )
		{
		case CCU825Packet.PKT_TYPE_EVENTS:
			return new CCU825Events(rp.getPacketPayload());
		case CCU825Packet.PKT_TYPE_EVENTS_EX:
			return new CCU825EventsEx(rp.getPacketPayload());
		}
		
		throw new CCU825PacketFormatException(String.format("Events payload type is %X", rp.getPacketPayload()[0]));
	}
	
	
	/**
	 * Access informaion about the device such as IMEI, HW version, etc.
	 * 
	 * @return device info as we got in protocol init transaction
	 */
	public CCU825DeviceInfo getDeviceInfo() {
		return deviceInfo;
	}


	/**
	 * Does a request to change outputs. Can change a group of outputs at once. 
	 * 
	 * @param state new state of ouputs, bit per output
	 * @param mask only outputs with 1's in corresponding mask bits are changed
	 * 
	 * @return actual outputs state from device
	 * @throws CCU825ProtocolException
	 */
	public int setOutState( int state, int mask ) throws CCU825ProtocolException
	{
		CCU825Packet rp = exchange(new CCU825OutStateCmdPacket( state, mask ) );
		return new CCU825OutState(rp.getPacketPayload()).getOutBits();
	}
	
	/**
	 * Request actual outputs state. 
	 * @return actual outputs state from device
	 * @throws CCU825ProtocolException
	 */
	public int getOutState(  ) throws CCU825ProtocolException
	{
		return setOutState( 0, 0 ); // Mask of zeros = modify none
	}

	/**
	 * Set or reset one output.
	 * 
	 * @param nOutBit Output to change, 0-6
	 * @param state new state
	 * @throws CCU825ProtocolException
	 */
	public void setOutState(int nOutBit, boolean state) throws CCU825ProtocolException 
	{
		int mask = (1 << nOutBit);
		int bits = 0;
		
		if( state )
			bits = mask;
	
		setOutState(bits, mask);
	}

	/**
	 * If encryption is currently enabled.
	 * @return True if we do encryption.
	 */
	public boolean isEncryptionEnabled() {
		return encryptionEnabled;
	}

	/**
	 * Enable or disable encryption. You better don't. Protocol driver does it by itself.
	 * Can be possibly needed to restart handshake. 
	 * 
	 * @param encryptionEnabled True to enable.
	 */
	private void setEncryptionEnabled(boolean encryptionEnabled) {
		this.encryptionEnabled = encryptionEnabled;
	}

	/* who needs
	public boolean isDataDumpEnabled() {
		return dataDumpEnabled;
	}*/

	/**
	 * Dump or not all I/O.
	 * 
	 * @param dataDumpEnabled True to enable packet data logging.
	 */
	public void setDataDumpEnabled(boolean dataDumpEnabled) {
		this.dataDumpEnabled = dataDumpEnabled;
	}

}
