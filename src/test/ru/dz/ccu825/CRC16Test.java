package test.ru.dz.ccu825;

import static org.junit.Assert.*;

import org.junit.Test;

import ru.dz.ccu825.util.CRC16;

public class CRC16Test {

	
	@Test
	public void testCrc1() {
		byte[] testIn = { 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39 };
		int crc = CRC16.crc( testIn, testIn.length );
		assertEquals(0x4B37, crc);
	}
	
	@Test
	public void testCrc2() {
		
		byte [] testIn = new byte[8];
		
		fillArray( testIn, (byte)0 );
		
		int crc = CRC16.crc(testIn, testIn.length);		
		
		assertEquals(0x0B40, crc);
	}

	private void fillArray(byte[] testIn, byte c) {
		for( int i = 0; i < testIn.length; i++ )
			testIn[i] = c;		
	}

}
