package ru.dz.chime;

public class Main {

	public static void main(String[] args) {
		
		AbstractChimeRunnable atHour = new ExecChimePlayer("bell.wav");
		AbstractChimeRunnable atHalf = atHour;
		AbstractChimeRunnable atQuorter = atHour;
		ChimeRunner cr = new ChimeRunner(atHour, atHalf, atQuorter);

	}

}
