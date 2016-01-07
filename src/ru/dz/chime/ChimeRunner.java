package ru.dz.chime;

import java.util.Date;

/**
 * 
 * Run chime once a hour/half/quarter.
 * 
 * @author dz
 *
 */
public class ChimeRunner implements Runnable
{
	private static final boolean test = System.getProperty("ChimeRunnerTest").equalsIgnoreCase("true");

	private AbstractChimeRunnable atHour;
	private AbstractChimeRunnable atHalf;
	private AbstractChimeRunnable atQuorter;

	public ChimeRunner( AbstractChimeRunnable atHour, AbstractChimeRunnable atHalf, AbstractChimeRunnable atQuorter ) 
	{
		this.atHour = atHour;
		this.atHalf = atHalf;
		this.atQuorter = atQuorter;
		
		//if( atHalf == null ) atHalf = atHour;
		//if( atQuorter == null ) atQuorter = atHalf;
		
		if( atHalf == null ) atHalf = atQuorter;
		
		Thread thread = new Thread( this );
		thread.setDaemon(false); // Make sure we won't exit as soon as main is dead
		thread.run();
	}
	
	@Override
	public void run() 
	{
		int lastMinute = -1;
		int lastHour = -1;
		
		while(true)
		{
			sleepASecond();
		
			//long mil = System.currentTimeMillis();
			Date d = new Date();
		
			if( test )
			{
				lastMinute = d.getSeconds();
				lastHour = d.getMinutes();
				
				//System.console().printf("hr:min = %02d:%02d", lastHour, lastMinute);
				System.out.println(String.format("hr:min = %02d:%02d", lastHour, lastMinute));
			}
			else
			{
				if( d.getMinutes() == lastMinute )
					continue;
			
				lastMinute = d.getMinutes();
				lastHour = d.getHours();
			}
			
			if( (lastMinute % 15) != 0 )
				continue;

			if( lastMinute == 0 )
				chime( atHour, lastHour, lastMinute );
			else if( (lastMinute % 30) == 0 )
				chime( atHalf, lastHour, lastMinute );
			else
				chime( atQuorter, lastHour, lastMinute );
		}
		
	}

	private void chime(AbstractChimeRunnable chime, int hr, int min ) 
	{
		if( chime == null ) return;

		// In test mode we get mins as hours, limit it
		//if( hr > 23 ) hr = 23;

		// 12hr mode
		hr %= 12;
		if( hr == 0 ) hr = 12; // Do not ring 0 bells :)

		if(test)
			System.out.println(String.format("will chime for %02d:%02d", hr, min));
		
		chime.setTime( hr, min );		
		chime.setVolume(getVolume(hr));
		
		Thread chimeThread = new Thread( chime );
		chimeThread.run();

	}

	private int getVolume(int hr) {
		
		// -10 db at night
		if( (hr > 0) && (hr < 10) ) return -10;
		
		return 0;
	}

	private void sleepASecond() 
	{
		synchronized (this) 
		{		
			try { this.wait(1000); } 
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}		
	}
	
}
