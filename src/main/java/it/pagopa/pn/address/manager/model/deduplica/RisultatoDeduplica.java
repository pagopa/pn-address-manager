package it.pagopa.pn.address.manager.model.deduplica;

public class RisultatoDeduplica {

	
	private MasterOut masterOut;
	private SlaveOut slaveOut;	
	
	private String risultatoDedu;
	private int erroreDedu;
	private int erroreGenerico;
	
	public MasterOut getMasterOut() {
		return masterOut;
	}
	public void setMasterOut(MasterOut masterOut) {
		this.masterOut = masterOut;
	}
	public SlaveOut getSlaveOut() {
		return slaveOut;
	}
	public void setSlaveOut(SlaveOut slaveOut) {
		this.slaveOut = slaveOut;
	}
	public String getRisultatoDedu() {
		return risultatoDedu;
	}
	public void setRisultatoDedu(String risultatoDedu) {
		this.risultatoDedu = risultatoDedu;
	}
	
	
	public int getErroreDedu() {
		return erroreDedu;
	}
	public void setErroreDedu(int erroreDedu) {
		this.erroreDedu = erroreDedu;
	}
	public int getErroreGenerico() {
		return erroreGenerico;
	}
	public void setErroreGenerico(int erroreGenerico) {
		this.erroreGenerico = erroreGenerico;
	}
	@Override
	public String toString() {
		return "RisultatoDeduplica [masterOut=" + masterOut + ", slaveOut=" + slaveOut + ", risultatoDedu="
				+ risultatoDedu + ", erroreGenerico=" + erroreGenerico + "]";
	}
	
	
	
}
