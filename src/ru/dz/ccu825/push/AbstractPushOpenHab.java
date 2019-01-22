package ru.dz.ccu825.push;

import java.io.IOException;
import java.util.logging.Logger;

import ru.dz.ccu825.payload.CCU825SysInfoEx;
import ru.dz.ccu825.payload.ICCU825SysInfo;
import ru.dz.openhab.AbstractPushData;

public abstract class AbstractPushOpenHab extends AbstractPushData 
{
	protected final static Logger log = Logger.getLogger(AbstractPushOpenHab.class.getName());



	public void setDefaultItemNames() 
	{
		for( int i = 0; i < CCU825SysInfoEx.N_IN; i++ )
		{
			setInputItemName(i, String.format( "CCU825_In%d", i) );
		}
		
		chargeItemName = "CCU825_Battery_Charge";
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




	
	
}
