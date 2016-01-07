package ru.dz.ccu825.push;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ru.dz.ccu825.CCU825Test;
import ru.dz.ccu825.payload.CCU825SysInfo;
import ru.dz.ccu825.payload.CCU825SysInfoEx;
import ru.dz.ccu825.payload.ICCU825SysInfo;
import ru.dz.openhab.AbstractPushOpenHab;

/**
 * Push data to OpenHAB instance with http requests.
 * 
 * TODO REST https://github.com/openhab/openhab/wiki/REST-API
 * 
 * @author dz
 *
 */

public class PushOpenHAB extends AbstractPushOpenHab {

	private String chargeItemName;

	
	
	
	public PushOpenHAB( String openHABHostName ) {
		super(openHABHostName);
	}


	private static final Map<Integer,String> items = new HashMap<Integer,String>();

	/**
	 * Map CCU825 input to named OpenHAB item. 
	 * @param input CCU825 input number, 0-15
	 * @param itemName OpehNAB item to translate data to
	 */
	public void setInputItemName( int input, String itemName )
	{
		items.put(input, itemName);
	}


	public void sendSysInfo( ICCU825SysInfo si ) throws IOException
	{
		int cnt = si.getInputsCount();
		for( int i = 0; i < cnt; i++ )
		{
			String name = items.get(i);
			if( name == null ) continue;

			double v = si.getInValue()[i];
			
			sendValue( name, Double.toString( v ) );
			//System.out.print( name + "=" + Double.toString( v ) + " " );
			//if( v > 0.01 )				System.out.print( String.format("%s = %.2f ", name, v ) );
		}
		
		//System.out.println();
		
		if(chargeItemName != null) sendValue( chargeItemName, Byte.toString( si.getBatteryPercentage() ) );
		
		sendValue( "CCU825_Device_Temperature", Byte.toString( si.getDeviceTemperature() ) );
		sendValue( "CCU825_Power_Voltage", Double.toString( si.getPowerVoltage() ) );

		if( si.isBalanceValid() ) 
			sendValue( "CCU825_GSM_Balance", Double.toString( si.getGSMBalance() ) );
	}




	public void setDefaultItemNames() 
	{
		for( int i = 0; i < CCU825SysInfoEx.N_IN; i++ )
		{
			setInputItemName(i, String.format( "CCU825_In%d", i) );
		}
		
		chargeItemName = "CCU825_Battery_Charge";
	}

	
	
	public String getChargeItemName() {
		return chargeItemName;
	}


	public void setChargeItemName(String chargeItemName) {
		this.chargeItemName = chargeItemName;
	}



}
