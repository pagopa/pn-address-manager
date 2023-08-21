package it.pagopa.pn.address.manager.model.deduplica;

public class MasterIn {

	private String id;
	private String provincia;
	private String cap;
	private String localita;
	private String localitaAggiuntiva;
	private String indirizzo;
	private String stato;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getProvincia() {
		return provincia;
	}
	public void setProvincia(String provincia) {
		this.provincia = provincia;
	}
	public String getCap() {
		return cap;
	}
	public void setCap(String cap) {
		this.cap = cap;
	}
	public String getLocalita() {
		return localita;
	}
	public void setLocalita(String localita) {
		this.localita = localita;
	}
	public String getLocalitaAggiuntiva() {
		return localitaAggiuntiva;
	}
	public void setLocalitaAggiuntiva(String localitaAggiuntiva) {
		this.localitaAggiuntiva = localitaAggiuntiva;
	}
	public String getIndirizzo() {
		return indirizzo;
	}
	public void setIndirizzo(String indirizzo) {
		this.indirizzo = indirizzo;
	}
	public String getStato() {
		return stato;
	}
	public void setStato(String stato) {
		this.stato = stato;
	}
	@Override
	public String toString() {
		return "MasterIn [id=" + id + ", provincia=" + provincia + ", cap=" + cap + ", localita=" + localita
				+ ", localitaAggiuntiva=" + localitaAggiuntiva + ", indirizzo=" + indirizzo + ", stato=" + stato + "]";
	}
	
	
	
}
