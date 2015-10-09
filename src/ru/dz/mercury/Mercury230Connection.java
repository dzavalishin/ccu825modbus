package ru.dz.mercury;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import ru.dz.ccu825.CCU825Test;
import ru.dz.mercury.data.MercuryActivePower;
import ru.dz.mercury.data.MercuryEnergy;
import ru.dz.mercury.data.MercuryFixed;
import ru.dz.mercury.data.MercuryFreq;
import ru.dz.mercury.data.MercuryIV;
import ru.dz.mercury.data.MercuryInfo;
import ru.dz.mercury.pkt.ChannelOpenPacket;
import ru.dz.mercury.pkt.ChannelTestPacket;
import ru.dz.mercury.pkt.Packet;
import ru.dz.mercury.pkt.ParameterReadRequestPacket;
import ru.dz.mercury.pkt.ParameterWriteRequestPacket;
import ru.dz.openhab.AbstractPushOpenHab;


/**
 * Connect to Mercury 230 AC power meter RS485 port through TCP/IP protocol converter.
 * <p>
 * <li>Read power measure results as displayed on meter indicator.
 * <li>Read instant I/V/Freq
 * <li>Read active/reactive/total power 
 * <p>
 * 
 * @author dz
 *
 */
public class Mercury230Connection 
{
	private boolean dumpPacketData = false;

	private static final int MAX_PKT_LEN = 512; // Why? max payload 256?

	/** Power metering data contain special info in hight bits. */
	private static final byte P_MASK = 0x3F;  

	//private ISimpleBytePipe pipe;

	private String hostName;
	private int port;

	private InputStream is;
	private OutputStream os;
	private Socket clientSocket;
	private byte netAddress = 0; // broadcast

	private boolean isSilent = false;

	private int nError = 0;
	private final static int MAX_ERROR = 20;

	public String getHostName() {		return hostName;	}
	public void setHostName(String hostName) {		this.hostName = hostName;	}
	public int getPort() {		return port;	}
	public void setPort(int port) {		this.port = port;	}

	private boolean getSilent() {
		return isSilent;
	}

	private void setSilent(boolean b) {
		this.isSilent = b;
	}

	private boolean isOk() 
	{
		return nError < MAX_ERROR;
	}

	private void resetErrorCounter() {
		nError = 0;
	}
	
	public void connect() throws UnknownHostException, IOException
	{

		clientSocket = new Socket(hostName, port);

		//clientSocket.setSoTimeout(getNextPktTimeout()); // Wait this long if no data
		clientSocket.setSoTimeout(500); // Wait this long if no data

		os = clientSocket.getOutputStream();
		is = clientSocket.getInputStream();
	}

	private void sendByte(byte c) throws IOException {
		os.write(c);
	}

	private void sendShort(int s) throws IOException {
		os.write((byte) (s & 0xFF));
		os.write((byte) ((s>>8) & 0xFF));
	}

	private int readByte() throws IOException {
		int b = is.read();

		if( b == -1 ) throw new IOException();

		return b;
	}

	private int readShort() throws IOException {

		int lo = readByte();
		int hi = readByte();

		int shortValue = (hi & 0xFF) << 8;
		shortValue |= (lo & 0xFF);

		return shortValue;
	}


	private void drainInput() throws IOException {
		while( is.available() > 0 )
			readByte();		
	}


	public void disconnect() throws IOException
	{
		clientSocket.close();
	}

	// ---------------------------------------------------------------------------
	//
	// ---------------------------------------------------------------------------

	/**
	 * Timeout at the end of packet
	 * @return time, msec
	 */
	private int getSendPktEndTimeout() {
		// For 9600
		return 5; 
	}

	/**
	 * Timeout before sending next packet
	 * @return time, msec
	 */
	private int getNextPktTimeout() {
		// For 9600
		return 150; 
	}


	public int getNetAddress() {
		return ((int)netAddress) & 0xFF;
	}
	public void setNetAddress(byte netAddress) {
		this.netAddress = netAddress;
	}

	public Packet readPacket() throws Mercury230CRCException, IOException, Mercury230ProtocolTimeoutException
	{
		int pos = 0;
		byte[] ans = new byte[MAX_PKT_LEN];

		try{

			// for timeout to work
			ans[pos++] = (byte) readByte();

			while( is.available() > 0 )
			{
				ans[pos++] = (byte) readByte();
			}

		} catch(java.net.SocketTimeoutException t)
		{
			// It's ok, timeout means we have a packet
		}

		if( pos < 2 )
			throw new Mercury230ProtocolTimeoutException();
		//throw new Mercury230UnexpectedPacketException(ans, "zero length packet")

		byte [] reply = new byte[pos];
		System.arraycopy(ans, 0, reply, 0, pos);

		if(dumpPacketData) CCU825Test.dumpBytes("got pklt", reply);

		Packet p = new Packet(reply); 

		return p;
	}


	public Packet readNonRcPacket() throws Mercury230ProtocolException, IOException
	{
		Packet pkt = readPacket();

		if(pkt.isReturnCodePacket())
			throw new Mercury230ProtocolException("Got rc="+pkt.getReturnCode());

		return pkt;
	}

	int readRetCodePacket() throws Mercury230UnexpectedPacketException, Mercury230CRCException, IOException, Mercury230ProtocolTimeoutException
	{
		Packet p = readPacket();
		if(!p.isReturnCodePacket())
			throw new Mercury230UnexpectedPacketException(p,"Expected result code packet");

		return p.getReturnCode();
	}



	public void sendPacked(Packet p) throws IOException
	{
		sleep(getNextPktTimeout());

		drainInput();

		p.setAddress(netAddress);
		byte[] toSend = p.getPacketBytes();

		if(dumpPacketData) CCU825Test.dumpBytes("send pkt", toSend);

		//for( byte b : toSend )			sendByte(b);

		os.write(toSend, 0, toSend.length);


		sleep(getSendPktEndTimeout());
		sleep(getNextPktTimeout());
	}










	public void sendParameterReadRequestPacket(int nParam) throws IOException
	{
		sendPacked(new ParameterReadRequestPacket(nParam));		
	}

	public void sendParameterReadRequestPacket(int nParam, int subParam) throws IOException
	{
		sendPacked(new ParameterReadRequestPacket(nParam, subParam));		
	}

	// ---------------------------------------------------------------------------
	// Read meter general parameters
	// ---------------------------------------------------------------------------

	public int readDeviceAddress() throws IOException, Mercury230ProtocolException
	{
		sendParameterReadRequestPacket(5);
		byte[] payload = readNonRcPacket().getPayload();
		return payload[1];
	}

	public void programDeviceAddress(int address) throws IOException, Mercury230ProtocolException
	{
		byte [] a = new byte[1];
		sendPacked(new ParameterWriteRequestPacket(Packet.PKT_W_PARAM_ADDDRESS, a));
	}


	// ---------------------------------------------------------------------------
	//
	// ---------------------------------------------------------------------------

	private void ping()
	{
		try {
			System.out.print("Ping... ");

			sendPacked(new ChannelTestPacket());
			int retCode = readRetCodePacket();

			System.out.println("rc = "+retCode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Mercury230UnexpectedPacketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Mercury230CRCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Mercury230ProtocolTimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	int openChannel(int level, String passwd) throws Mercury230ProtocolException
	{
		try {
			sendPacked(new ChannelOpenPacket(level, passwd));
			return readRetCodePacket();
		} catch (IOException e) {
			throw new Mercury230ProtocolException(e);		
		}
	}

	void sleep(int mSec)
	{
		synchronized (this) {


			try {
				this.wait(mSec);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	void getInstantPowerValues(AbstractPushOpenHab pusher) throws Mercury230CRCException, Mercury230ProtocolTimeoutException 
	{

		try {

			MercuryIV iv = new MercuryIV(this);			
			//MercuryPower power = new MercuryPower(this);
			MercuryActivePower power = new MercuryActivePower(this);
			MercuryEnergy energy = new MercuryEnergy(this);

			if( !isSilent )
			{
				iv.dump();
				power.dump();			
				energy.dump();
			}

			//pusher.sendValue("activePower", power.getTotalP() );
			//pusher.sendValue("currentVoltage", iv.getV()[0] );

			pusher.sendValue("activePower", Integer.toString( (int)power.getTotalP() ) );
			pusher.sendValue("currentVoltage", Integer.toString( (int)iv.getV()[0] ) );

		} catch (IOException e) {
			nError++;
			e.printStackTrace();
		} // Current
		catch (Mercury230ProtocolException e) {
			nError++;
			e.printStackTrace();
		}


	}

	/**
	 * Read and decode typical I/V/etc reply with 3 fixed numbers 3 bytes each.
	 * <p>
	 * 
	 * 
	 * @throws IOException
	 * @throws Mercury230ProtocolException 
	 */
	public double[] read3dPacket() throws IOException, Mercury230ProtocolException {
		double[] v = new double[3];
		byte[] packet = readNonRcPacket().getPayload();
		MercuryFixed.decode3x3(packet, v);
		return v;
	}

	/**
	 * Read and decode typical power reply with 4 fixed numbers 3 bytes each.
	 * <p>
	 * NB! We do clear power specific high bits in 1st bytes of 4 numbers 
	 * 
	 * 
	 * @throws IOException
	 * @throws Mercury230ProtocolException 
	 */
	public double[] read4dPacket() throws IOException, Mercury230ProtocolException {
		double[] v = new double[4];
		byte[] packet = readNonRcPacket().getPayload();


		packet[0] &= P_MASK;
		packet[3] &= P_MASK;
		packet[6] &= P_MASK;
		packet[9] &= P_MASK;

		v[0] = MercuryFixed.decode3b(packet,0);
		v[1] = MercuryFixed.decode3b(packet,3);
		v[2] = MercuryFixed.decode3b(packet,6);
		v[3] = MercuryFixed.decode3b(packet,9);

		return v;
	}




	@SuppressWarnings("unused")
	public static void main(String[] args) throws UnknownHostException, IOException, Mercury230ProtocolException {

		AbstractPushOpenHab pusher = new AbstractPushOpenHab("smart."); 		
		Mercury230Connection c = new Mercury230Connection();


		for( String arg : args )
		{
			if( arg.equalsIgnoreCase("-silent") )
				c.setSilent(true);
		}

		if(true)
		{
			c.setHostName("etherwan.");
			c.setPort(604);
		} else {
			c.setHostName("moxa.");
			c.setPort(4002);
		}

		while(true)
		{

			c.connect();

			for(int i = 0; i < 1; i++)
			{
				c.ping();
				c.sleep(200);
			}

			c.openChannel(1, "\1\1\1\1\1\1");

			if( !c.getSilent() )
			{
				int addr = c.readDeviceAddress();
				System.out.println("Device address = "+addr);

				(new MercuryFreq(c)).dump();
				(new MercuryInfo(c)).dump();
			}

			while(c.isOk())
			{
				c.getInstantPowerValues(pusher);			
			}

			c.disconnect();
			c.resetErrorCounter();
		}

	}



	/* зафиксированная энергия. ХЗ, что это.

	sendparameterReadRequestPacket(0x14, 0xF0);
	packet = readPacket().getPayload();
	CCU825Test.dumpBytes("Energy", packet);

	sendparameterReadRequestPacket(0x14, 0xF1);
	packet = readPacket().getPayload();
	CCU825Test.dumpBytes("Energy T1", packet);

	sendparameterReadRequestPacket(0x14, 0xF2);
	packet = readPacket().getPayload();
	CCU825Test.dumpBytes("Energy T2", packet);

	sendparameterReadRequestPacket(0x14, 0xF3);
	packet = readPacket().getPayload();
	CCU825Test.dumpBytes("Energy T3", packet);

	sendparameterReadRequestPacket(0x14, 0xF4);
	packet = readPacket().getPayload();
	CCU825Test.dumpBytes("Energy T4", packet);
	 */			


}
