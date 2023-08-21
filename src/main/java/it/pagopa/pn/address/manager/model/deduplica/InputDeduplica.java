package it.pagopa.pn.address.manager.model.deduplica;

public class InputDeduplica {

	private ConfigIn configIn;
	private MasterIn masterIn;
	private SlaveIn slaveIn;
	public ConfigIn getConfigIn() {
		return configIn;
	}
	public void setConfigIn(ConfigIn configIn) {
		this.configIn = configIn;
	}
	public MasterIn getMasterIn() {
		return masterIn;
	}
	public void setMasterIn(MasterIn masterIn) {
		this.masterIn = masterIn;
	}
	public SlaveIn getSlaveIn() {
		return slaveIn;
	}
	public void setSlaveIn(SlaveIn slaveIn) {
		this.slaveIn = slaveIn;
	}
	@Override
	public String toString() {
		return "InputDeduplica [configIn=" + configIn + ", masterIn=" + masterIn + ", slaveIn=" + slaveIn + "]";
	}
	
	
	
}
