# CCU825 (and compatible) controller protocol driver

Here is what is it: <http://www.radsel.ru/products/ccu825.html> (in Russian).

## How to use this driver.


To use this driver you need an encryption key for your devices.

Driver code requests keys from ```ICCU825KeyRing``` class. If you use
just one device, ConstantKeyRing class can be used as a key provider.
For multi-device project use ArrayKeyRing with all the required keys 
added. The keys themselves are to be asked from RadsEl tech support.
Don't forget to provide your devices *IMEI* to get a key.

Next thing driver needs is a low-level ModBus transport implementation.
We provide a connector to a j2mod ModBus library (j2mod-1.03.jar),
which is available as ```CCU825_j2mod_connector``` class. You can use it or
provide your own ModBus protocol driver.

Main entry point is ```CCU825Connection``` class. Create instance, providing,
as said above, keyring and ModBus connector.

Call ```connect()``` to establish communication, and if ```protocolRC.isOk()``` - you
can go on with communications.

Call ```getSysInfo(), getEvents(), setOutState(), getOutState()```.

Call ```disconnect()``` when done. If communications fail a lot - try to resync
with ```connect()``` again.

## MQTT/UDP or OpenHAB

This code supports two backends, one sends data to OpenHAB, other - to MQTT/UDP
network.

Select one you like in CCU825Main.java line 65:

```java
    //push = new PushOpenHAB("http://smart.local"); // OpenHAB host name
    push = new PushMqttUdp();

```

To find out what MQTT/UDP is, please, visit <https://github.com/dzavalishin/mqtt_udp> and
<https://mqtt-udp.readthedocs.io/en/latest/>.


## How to fix j2mod library


j2mod 1.03 has a bug in serial RTU close method, class ModbusRTUTransport,
which prevents serial poprt from being reopened.

Corect code is here:

```java
public void close() throws IOException {
	m_InputStream.close();
	m_OutputStream.close();
	
	m_CommPort.close();
}
```

