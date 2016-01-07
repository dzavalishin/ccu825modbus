package ru.dz.chime;

public class ExecChimePlayer extends AbstractChimeRunnable {

	//private String audioFileName;
	private String cmd;

	public ExecChimePlayer(String audioFileName) {
		//this.audioFileName = audioFileName;
		cmd = String.format("sox -q %s -d", audioFileName);		
	}

	@Override
	protected void playChime(ChimeType t, int hr, int min) {
		
		// We know how to play full hour only
		if( t != ChimeType.FullHour )
			return;
		
		for( int i = hr; i > 0; i-- )
			playWav();
		
	}

	private void playWav() {
		Process p;
		try {

			p = Runtime.getRuntime().exec(cmd);
			int rc = p.waitFor();

			if( rc != 0 )
			{
				//System.console().printf("Exec error %d: \"%s\"", rc, cmd );
				System.out.println(String.format("Exec error %d: \"%s\"", rc, cmd ));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
