package ru.dz.chime;

public abstract class AbstractChimeRunnable implements Runnable {

	private int hr;
	private int min;

	public void setTime( int hr, int min )
	{
		this.hr = hr;
		this.min = min;
		
	}
	
	abstract protected void playChime( ChimeType t, int hr, int min );
	
	
	@Override
	public void run() {
		
		ChimeType t = null;
		
		switch( min )
		{
		case 0: t = ChimeType.FullHour; break;
		case 15: t = ChimeType.QuarterHour; break;
		case 30: t = ChimeType.HalfHour; break;
		case 45: t = ChimeType.QuarterHour; break;
		}
		
		if(t == null)
		{
			System.err.println("No chime type");
		}
		
		playChime(t, hr, min);
		
	}
	
	
}
