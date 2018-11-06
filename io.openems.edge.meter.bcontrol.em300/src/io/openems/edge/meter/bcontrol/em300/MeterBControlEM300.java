package io.openems.edge.meter.bcontrol.em300;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.BControl.EM300", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE//
) //
public class MeterBControlEM300 extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, EventHandler {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterBControlEM300() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();

		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());

		// Initialize Min/MaxActivePower channels
		this._initializeMinMaxActivePower(this.cm, config.service_pid(), config.minActivePower(),
				config.maxActivePower());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		ACTIVE_POWER_POS(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_NEG(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_L1_POS(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_L1_NEG(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_L2_POS(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_L2_NEG(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_L3_POS(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_L3_NEG(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
		REACTIVE_POWER_POS(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_NEG(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L1_POS(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L1_NEG(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L2_POS(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L2_NEG(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L3_POS(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)),
		REACTIVE_POWER_L3_NEG(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	//
	// Waring: Modbus Registers are not checked and are uncorrect !!!
	//
	protected ModbusProtocol defineModbusProtocol() {
		// Update values here?

		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0, Priority.HIGH, //
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_POS, new UnsignedDoublewordElement(0),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_NEG, new UnsignedDoublewordElement(2),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_POS, new UnsignedDoublewordElement(4),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_NEG, new UnsignedDoublewordElement(6),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						new DummyRegisterElement(8, 15), //
						new DummyRegisterElement(16, 19), // Apparent Power
						new DummyRegisterElement(20, 23), //
						new DummyRegisterElement(24, 25), // Power Factor
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(26)),
						new DummyRegisterElement(28, 39), //

						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L1_POS, new UnsignedDoublewordElement(40),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L1_NEG, new UnsignedDoublewordElement(42),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L1_POS, new UnsignedDoublewordElement(44),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L1_NEG, new UnsignedDoublewordElement(46),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						new DummyRegisterElement(48, 55), //
						new DummyRegisterElement(56, 59), // Apparent Power L1

						cm(new UnsignedDoublewordElement(60)) //
								.m(AsymmetricMeter.ChannelId.CURRENT_L1, ElementToChannelConverter.DIRECT_1_TO_1) //
								.m(SymmetricMeter.ChannelId.CURRENT, ElementToChannelConverter.DIRECT_1_TO_1) //
								.build(), //

						cm(new UnsignedDoublewordElement(62)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.DIRECT_1_TO_1) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1) //
								.build(), //

						new DummyRegisterElement(64, 65), // Power Factor L1
						new DummyRegisterElement(66, 79), //

						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L2_POS, new UnsignedDoublewordElement(80),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L2_NEG, new UnsignedDoublewordElement(82),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L2_POS, new UnsignedDoublewordElement(84),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L2_NEG, new UnsignedDoublewordElement(86),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						new DummyRegisterElement(88, 95), //
						new DummyRegisterElement(96, 99), // Apparent Power L2

						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(100)),

						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(102)),

						new DummyRegisterElement(104, 105), // Power Factor L2
						new DummyRegisterElement(106, 119), //

						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L3_POS, new UnsignedDoublewordElement(120),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.ACTIVE_POWER_L3_NEG, new UnsignedDoublewordElement(122),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L3_POS, new UnsignedDoublewordElement(124),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(MeterBControlEM300.ChannelId.REACTIVE_POWER_L3_NEG, new UnsignedDoublewordElement(126),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						new DummyRegisterElement(128, 135), //
						new DummyRegisterElement(136, 139), // Apparent Power L3

						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(140)),

						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(142)),

						new DummyRegisterElement(144, 145)), // Power Factor L3

				new FC3ReadRegistersTask(512, Priority.LOW, //
						m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedQuadruplewordElement(512),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedQuadruplewordElement(516),
								ElementToChannelConverter.SCALE_FACTOR_1)));
	}

	///////////////////////////////////////////////////////////////////////////////////////
	// Channel get methods
	///////////////////////////////////////////////////////////

	Channel<Integer> getActivePowerPos() {
		return this.channel(ChannelId.ACTIVE_POWER_POS);
	}

	Channel<Integer> getActivePowerNeg() {
		return this.channel(ChannelId.ACTIVE_POWER_NEG);
	}

	Channel<Integer> getActivePowerL1Pos() {
		return this.channel(ChannelId.ACTIVE_POWER_L1_POS);
	}

	Channel<Integer> getActivePowerL1Neg() {
		return this.channel(ChannelId.ACTIVE_POWER_L1_NEG);
	}

	Channel<Integer> getActivePowerL2Pos() {
		return this.channel(ChannelId.ACTIVE_POWER_L2_POS);
	}

	Channel<Integer> getActivePowerL2Neg() {
		return this.channel(ChannelId.ACTIVE_POWER_L2_NEG);
	}

	Channel<Integer> getActivePowerL3Pos() {
		return this.channel(ChannelId.ACTIVE_POWER_L3_POS);
	}

	Channel<Integer> getActivePowerL3Neg() {
		return this.channel(ChannelId.ACTIVE_POWER_L3_NEG);
	}

	Channel<Integer> getReactivePowerPos() {
		return this.channel(ChannelId.REACTIVE_POWER_POS);
	}

	Channel<Integer> getReactivePowerNeg() {
		return this.channel(ChannelId.REACTIVE_POWER_NEG);
	}

	Channel<Integer> getReactivePowerL1Pos() {
		return this.channel(ChannelId.REACTIVE_POWER_L1_POS);
	}

	Channel<Integer> getReactivePowerL1Neg() {
		return this.channel(ChannelId.REACTIVE_POWER_L1_NEG);
	}

	Channel<Integer> getReactivePowerL2Pos() {
		return this.channel(ChannelId.REACTIVE_POWER_L2_POS);
	}

	Channel<Integer> getReactivePowerL2Neg() {
		return this.channel(ChannelId.REACTIVE_POWER_L2_NEG);
	}

	Channel<Integer> getReactivePowerL3Pos() {
		return this.channel(ChannelId.REACTIVE_POWER_L3_POS);
	}

	Channel<Integer> getReactivePowerL3Neg() {
		return this.channel(ChannelId.REACTIVE_POWER_L3_NEG);
	}

	@Override
	public String debugLog() {
		return "EM300: Active Power: " + this.getActivePowerL3().value().asString() + " | Positive:| "
				+ this.getActivePowerL3Pos().value().asString() + " |Negative| "
				+ this.getActivePowerL3Neg().value().asString();

	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:

			// write the result of the arithmetic operation into the corresponding channel

			// Active Power
			this.getActivePower().setNextValue(this.getActivePowerPos().getNextValue().orElse(0)
					- this.getActivePowerNeg().getNextValue().orElse(0));

			// Active Power L1
			this.getActivePowerL1().setNextValue(this.getActivePowerL1Pos().getNextValue().orElse(0)
					- this.getActivePowerL1Neg().getNextValue().orElse(0));

			// Active Power L2
			this.getActivePowerL2().setNextValue(this.getActivePowerL2Pos().getNextValue().orElse(0)
					- this.getActivePowerL2Neg().getNextValue().orElse(0));
			// Active Power L3
			this.getActivePowerL3().setNextValue(this.getActivePowerL3Pos().getNextValue().orElse(0)
					- this.getActivePowerL3Neg().getNextValue().orElse(0));

			// Reactive Power
			this.getReactivePower().setNextValue(this.getReactivePowerPos().getNextValue().orElse(0)
					- this.getReactivePowerNeg().getNextValue().orElse(0));

			// Reactive Power L1
			this.getReactivePowerL1().setNextValue(this.getReactivePowerL1Pos().getNextValue().orElse(0)
					- this.getReactivePowerL1Neg().getNextValue().orElse(0));

			// Reactive Power L2
			this.getReactivePowerL2().setNextValue(this.getReactivePowerL2Pos().getNextValue().orElse(0)
					- this.getReactivePowerL2Neg().getNextValue().orElse(0));
			// Reactive Power L3
			this.getReactivePowerL3().setNextValue(this.getReactivePowerL3Pos().getNextValue().orElse(0)
					- this.getReactivePowerL3Neg().getNextValue().orElse(0));

			break;
		}

	}

}