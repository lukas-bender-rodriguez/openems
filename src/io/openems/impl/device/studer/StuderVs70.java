package io.openems.impl.device.studer;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.studer.StuderDevice;

public class StuderVs70 extends StuderDevice {
	/*
	 * Config
	 */
	@ConfigInfo(title = "Sets the charger nature", type = StuderVs70Charger.class)
	public final ConfigChannel<StuderVs70Charger> charger = new ConfigChannel<StuderVs70Charger>("charger", this);

	public StuderVs70() throws OpenemsException {
		super();
	}

	@Override
	public String toString() {
		return "StuderVs70 [charger=" + charger + ", getThingId()=" + id() + "]";
	}

	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (charger.valueOptional().isPresent()) {
			natures.add(charger.valueOptional().get());
		}
		return natures;
	}
}