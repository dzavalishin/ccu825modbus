package ru.dz.ccu825.transport;

import java.io.IOException;

import ru.dz.ccu825.CCU825Test;
import ru.dz.ccu825.util.CCU825Exception;
import ru.dz.ccu825.util.CCU825ProtocolException;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusRTUTransport;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransport;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransport;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransport;
import com.ghgande.j2mod.modbus.msg.ExceptionResponse;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadWriteMultipleResponse;
import com.ghgande.j2mod.modbus.net.ModbusMasterFactory;


public class CCU825_j2mod_connector implements IModBusConnection {
	private ModbusTransport transport = null;
	//private String dest = "tcp:localhost:502";
	private String dest = "device:com2";
	private int baud = 9600;
	private int unit = 1;

	@Override
	public void setSpeed(int baud) {
		this.baud = baud; 
	}

	/**
	 * Set target address, such as serial
	 * RS485 device port, TCP/IP address or so. 
	 * <p>
	 * Examples:
	 * <li>"device:com2" - serial port on Windows
	 * <li>"tcp:localhost:502" - modbus/tcp server on localhost port 502 
	 * <li>"udp:localhost:502" - modbus/udp server on localhost port 502 
	 * 
	 * @param dest Address to connect to.
	 */
	public void setDestination(String dest) {
		this.dest = dest;
	}

	/*
	 * (non-Javadoc)
	 * @see ru.dz.ccu825.transport.IModBusConnection#getDestination()
	 */
	@Override
	public String getDestination() {		return dest;	}
	
	/*
	 * (non-Javadoc)
	 * @see ru.dz.ccu825.transport.IModBusConnection#setModbusUnitId(int)
	 */
	@Override
	public void setModbusUnitId(int unit) {
		this.unit = unit;
	}

	/*
	 * (non-Javadoc)
	 * @see ru.dz.ccu825.transport.IModBusConnection#connect()
	 */
	@Override
	public void connect() throws CCU825Exception {


		try {
			// 2. Open the connection.
			transport = ModbusMasterFactory.createModbusMaster(dest);
			if (transport == null)
				throw new CCU825Exception("Cannot open "+dest);


			if (transport instanceof ModbusSerialTransport) {
				((ModbusSerialTransport) transport).setReceiveTimeout(500);
				((ModbusSerialTransport) transport).setBaudRate(baud);
			}

			if (transport instanceof ModbusTCPTransport) {
				System.out.println("TCP - call setRtuTcp()");
				((ModbusTCPTransport) transport).setRtuTcp();

				//unit = 0;
			} else if (transport instanceof ModbusRTUTransport) {
				//?
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new CCU825Exception(ex);
		}


	}

	
	@Override
	public void disconnect() {
		try {

			if (transport != null)
				transport.close();
		} catch (IOException e) {
			e.printStackTrace();
			// Do nothing.
		}
	}

	@Override
	public byte[] rwMultiple(int nRead, byte[] writeData) throws CCU825ProtocolException 
	{
		if( (writeData.length & 1) != 0 )
		{
			byte[] replacement = new byte[writeData.length+1];
			
			replacement[replacement.length-1] = 0;
			System.arraycopy(writeData, 0, replacement, 0, writeData.length);
			
			writeData = replacement;
		}
		
		
		CCU825_ReadWriteMultipleRequest req = new CCU825_ReadWriteMultipleRequest( nRead, writeData.length/2 );
		
		req.setUnitID(unit);	
		req.setSendData(writeData);
		
		// 4. Prepare the transaction
		ModbusTransaction trans = transport.createTransaction();
		
		trans.setRequest(req);
		
		//req.setHeadless(trans instanceof ModbusSerialTransaction);
		req.setHeadless(true); // Modbus over IP is just tunneling
		
		if (Modbus.debug)
			System.out.println("Request: " + req.getHexMessage());

		// 5. Execute the transaction repeat times

		try {
			trans.execute();
		} catch (ModbusException x) {			
			throw new CCU825ProtocolException(x);
		}


		ModbusResponse res = trans.getResponse();		
		
		if (Modbus.debug) 
		{
			if (res != null)
				System.out.println("Response: " + res.getHexMessage());
			else
				System.err.println("No response to ReadWriteMultiple request.");
		}
		
		if (res == null) throw new CCU825ProtocolException("Empty or no reply");
		
		if (res instanceof ExceptionResponse) {
			ExceptionResponse exception = (ExceptionResponse) res;
			throw new CCU825ProtocolException(exception.toString());
		}
		
		if (! (res instanceof ReadWriteMultipleResponse))			
			throw new CCU825ProtocolException("Unexpected reply type "+res.getClass());;
		
		
		ReadWriteMultipleResponse data = (ReadWriteMultipleResponse) res;
		
		byte[] recvData = data.getMessage();
		//CCU825Test.dumpBytes("recv", recvData);
		
		assert( recvData.length == nRead*2+1 );
		
		byte[] justRegs = new byte[nRead*2]; 

		// remove 1st byte - contains len of array
		System.arraycopy(recvData, 1, justRegs, 0, nRead*2);
		
		return justRegs;
		/*
		InputRegister[] values = data.getRegisters();
		System.out.println("Data: " + Arrays.toString(values));
		*/

	}



}
