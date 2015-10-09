package ru.dz.ccu825.transport;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException; 

//import com.ghgande.j2mod.modbus.Modbus;
//import com.ghgande.j2mod.modbus.io.ModbusTCPTransport;



import ru.dz.ccu825.util.CCU825Exception;
import ru.dz.ccu825.util.CCU825ProtocolException;

public class CCU825_tcp2com_connector implements IModBusConnection {

	private String dest;
	private int baud;
	private int unit = 1;

	private Socket s;
	private String hostName;
	private int port;

	@Override
	public void setDestination(String dest) {		
		this.dest = dest;

		String parts[] = dest.split(":");
		if (parts == null || parts.length < 2)
			throw new IllegalArgumentException("missing connection information");

		if (parts[0].toLowerCase().equals("tcp")) {

			hostName = parts[1];
			port = 502; //Modbus.DEFAULT_PORT;

			if (parts.length > 2)
				port = Integer.parseInt(parts[2]);

		}
		else
			throw new IllegalArgumentException("unknown conn type");


	}

	@Override
	public String getDestination() {				return dest;		}

	@Override
	public void setSpeed(int baud) {				this.baud = baud; 	}

	@Override
	public void setModbusUnitId(int unit) {			this.unit = unit;	}

	@Override
	public void connect() throws CCU825Exception {
		try {
			s = new Socket(hostName, port);
		} catch (UnknownHostException e) {
			throw new CCU825Exception(e);
		} catch (IOException e) {
			throw new CCU825Exception(e);
		}

	}

	@Override
	public void disconnect() {
		try {
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public byte[] rwMultiple(int nRead, byte[] writeData)
			throws CCU825ProtocolException {
		if( (writeData.length & 1) != 0 )
		{
			byte[] replacement = new byte[writeData.length+1];
			
			replacement[replacement.length-1] = 0;
			System.arraycopy(writeData, 0, replacement, 0, writeData.length);
			
			writeData = replacement;
		}
		
		int m_ReadReference = 0;
		int m_WriteReference = 0;
		
		int m_WriteCount = writeData.length/2;
		int m_ReadCount = nRead;
		
		byte results[] = new byte[11+2 + 2 * m_WriteCount];
		
		results[0] = (byte) unit;
		results[1] = (byte) 0x17; // modbus func 
				
		results[2] = (byte) (m_ReadReference >> 8);
		results[3] = (byte) (m_ReadReference & 0xFF);
		results[4] = (byte) (m_ReadCount >> 8);
		results[5] = (byte) (m_ReadCount & 0xFF);
		results[6] = (byte) (m_WriteReference >> 8);
		results[7] = (byte) (m_WriteReference & 0xFF);
		results[8] = (byte) (m_WriteCount >> 8);
		results[9] = (byte) (m_WriteCount & 0xFF);
		// TO DO is it correct ModBus? possibly odd byte count
		//results[8] = (byte) (sendData.length);
		results[10] = (byte) (m_WriteCount * 2);
		int offset = 11;
		for (int i = 0; i < writeData.length; i++) {
			results[offset++] = writeData[i];
		}

		//int dataLength = 11 + m_WriteCount * 2;

		int crc = ru.dz.crc.CRC16.crc( results, results.length-2 );
		
		results[offset++] = (byte) (crc & 0xFF);
		results[offset++] = (byte) ((crc >> 8) & 0xFF);

		try {
			s.getOutputStream().write(results);
		} catch (IOException e) {
			throw new CCU825ProtocolException(e);
		}
		
		byte[] resp = new byte[4096]; // size ok?;
		int respLen = 0; 
		try {

			// Read from socket until timeout
			s.setSoTimeout(2000); // Wait 100 msec for no more data
			respLen = s.getInputStream().read(resp);

		} catch (IOException e) {
			throw new CCU825ProtocolException(e);
		}
	
		if( resp.length == 0 )
			return null;

		crc = ru.dz.crc.CRC16.crc( resp, resp.length-2 );

		if( 
				( resp[resp.length-2] != (byte) (crc & 0xFF)) || 
				(resp[resp.length-1] != (byte) ((crc >> 8) & 0xFF))
				)
		{
			throw new CCU825ProtocolException("Modbus responce CRC error");
		}

		int respUnit = resp[0] & 0xFF;
		int respFunc = resp[1] & 0xFF;
		int respBytes = resp[2] & 0xFF;
	
		if( respUnit != unit )
			throw new CCU825ProtocolException("Modbus responce unit error");

		if( respFunc != 0x17 )
			throw new CCU825ProtocolException("Modbus responce func error");
		
		if( respBytes != respLen - 5)
			throw new CCU825ProtocolException("Modbus responce data size error, respBytes = "+respBytes+", respLen = "+respLen);
		
		byte [] respData = new byte[respBytes];
		
		System.arraycopy(resp, 3, respData, 0, respBytes);
		
		return respData;
	}


}
