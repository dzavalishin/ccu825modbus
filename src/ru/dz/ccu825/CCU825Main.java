package ru.dz.ccu825;

import java.io.IOException;
import java.util.logging.Logger;

import ru.dz.ccu825.data.CCU825ReturnCode;
import ru.dz.ccu825.payload.ICCU825Events;
import ru.dz.ccu825.payload.ICCU825SysInfo;
import ru.dz.ccu825.push.PushOpenHAB;
import ru.dz.ccu825.transport.ArrayKeyRing;
import ru.dz.ccu825.transport.CCU825_j2mod_connector;
import ru.dz.ccu825.transport.CCU825_tcp2com_connector;
import ru.dz.ccu825.transport.ICCU825KeyRing;
import ru.dz.ccu825.transport.IModBusConnection;
import ru.dz.ccu825.util.CCU825Exception;

/**
 * 
 * CCU825 driver main class for debugging and test purposes.
 * 
 * @author dz
 *
 */
public class CCU825Main 
{
	private final static Logger log = Logger.getLogger(CCU825Main.class.getName());

	//static IModBusConnection mc = new CCU825_j2mod_connector();
	//static IModBusConnection mc = new CCU825_tcp2com_connector();
	static IModBusConnection mc;
	static boolean doPoll = true;
	static PushOpenHAB oh;

	/**
	 * @param args
	 */
	public static void main(String[] args) {				

		//System.setProperty("com.ghgande.j2mod.modbus.debug", "true");

		if( true )
		{
			mc = new CCU825_tcp2com_connector();
			//mc.setDestination("tcp:192.168.88.128:503"); 
			mc.setDestination("tcp:192.168.88.130:503"); // rack stand module 
			//mc.setDestination("tcp:etherwan.:603"); // etherwan port 3 
		}
		else
		{
		// 9600 8N1
		mc = new CCU825_j2mod_connector();

		//Thread.currentThread().setDaemon(false);
		//mc.setDestination("device:/dev/com10");

		mc.setDestination("device://./com10");
		//mc.setDestination("tcp:192.168.1.142:603");  // Doesnt work yet
		}

		oh = new PushOpenHAB("smart.");
		oh.setDefaultItemNames();

		//oh.setInputItemName(8, "CCU825_SunLight"); // Actually on input 5, why 8?
		//oh.setInputItemName(8, "CCU825_CO2"); // Actually on input 7, why 8?

		oh.setInputItemName(4, "CCU825_SunLight"); // Actually on input 5
		oh.setInputItemName(5, "CCU825_Sound_1"); // In6		
		oh.setInputItemName(6, "CCU825_CO2"); // Actually on input 7
		oh.setInputItemName(7, "CCU825_CO2_2"); // Actually on input 8
		
		ICCU825KeyRing kr = new ArrayKeyRing();

		CCU825Connection c = new CCU825Connection(mc, kr);

		AbstractRequestLoop loop = new AbstractRequestLoop(c) 
		{
			@Override
			protected void pollDevice(CCU825Connection c) throws CCU825Exception 
			{
				CCU825Main.pollDevice(c);				
			}

			@Override
			protected void Say(String string) 
			{
				System.out.println(string);				
			}
		};

		while(true)
		{
			try 
			{ 
				loop.startBlocking();
			} catch( Throwable e )
			{
				System.err.println(e);
			}

			c.disconnect();
		}



		//System.exit(0);

	}






	static int iOut = 0;
	private static void pollDevice(CCU825Connection c) throws CCU825Exception 
	{
		// TODO poll openhab for values? 

		//c.setOutState(iOut++, 0x7F);

		ICCU825SysInfo si;

		ICCU825Events events = c.getEvents();
		if(events != null)
		{
			//System.out.println(events);
			si = events.getSysInfo();
		}
		else
		{
			si = c.getSysInfo();
		}
		try {
			oh.sendSysInfo(si);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(si.getInValue()[5]);


	}





}
