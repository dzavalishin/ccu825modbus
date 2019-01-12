package test.ru.dz.ccu825;

import static org.junit.Assert.*;

import org.junit.Test;

import ru.dz.ccu825.util.RC4;

public class rc4test {

	@Test
	public void test() {
		byte[] key = { 0x01, 0x02, 0x03, 0x04, 0x05 };
		byte[] inp =  
			{
				(byte)0xb2, 0x39, 0x63, 0x05,  (byte)0xf0, 0x3d, (byte)0xc0, 0x27,   (byte)0xcc, (byte)0xc3, 0x52, 0x4a,  0x0a, 0x11, 0x18, (byte)0xa8
			};
		RC4 enc = new RC4(key );

		byte[] out = enc.encrypt(inp);
		checkzero(out);
		//dumpBytes("enc", out);
	}

	private static void checkzero(byte[] out) {
		for( int i = 0; i < out.length; i++ )
			if( out[i] != 0 )
			{
				//throw new RuntimeException("Nonzero array item at " + i);
				fail("Nonzero array item at " + i);

			}
	}

}
