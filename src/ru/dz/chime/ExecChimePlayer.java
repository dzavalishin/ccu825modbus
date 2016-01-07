package ru.dz.chime;

public class ExecChimePlayer extends AbstractChimeRunnable {

	//private String audioFileName;
	private final String normalCmd;
	private final String fastCmd;

	public ExecChimePlayer(String audioFileName, String hiBellFileName) {
		//this.audioFileName = audioFileName;
		normalCmd = String.format("sox -q %s -d", audioFileName);
		fastCmd = String.format("sox -q %s -d", hiBellFileName);
	}

	@Override
	protected void playChime(ChimeType t, int hr, int min) {
		
		// We know how to play full hour only
		if( t != ChimeType.FullHour )
			return;
		
		// Make first 1-3 chimes to sound faster for easier time determination
		
		int first = hr % 3;
		
		if( first == 0 ) first = 3;
		
		hr -= first;

		for( int i = first; i > 0; i-- )
			playWavfast();
		
		for( int i = hr; i > 0; i-- )
			playWav();
		
	}

	private void playWav() {
		runCmd(normalCmd);
	}

	private void playWavfast() {
		runCmd(fastCmd);
	}

	private void runCmd(String rCmd) {
		Process p;
		try {

			p = Runtime.getRuntime().exec(rCmd);
			int rc = p.waitFor();

			if( rc != 0 )
			{
				//System.console().printf("Exec error %d: \"%s\"", rc, rCmd );
				System.out.println(String.format("Exec error %d: \"%s\"", rc, rCmd ));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
