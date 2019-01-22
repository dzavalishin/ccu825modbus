package ru.dz.ccu825.push;

import java.io.IOException;
import ru.dz.mqtt_udp.PublishPacket;

public class PushMqttUdp extends AbstractPushOpenHab {

	@Override
	public void sendValue(String name, String value) throws IOException {
		new PublishPacket(name,value).send();
	}

}
