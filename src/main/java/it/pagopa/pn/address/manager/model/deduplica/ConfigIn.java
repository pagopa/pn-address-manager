package it.pagopa.pn.address.manager.model.deduplica;

public class ConfigIn {

	String authKey;
	String configurazioneDeduplica;
	String configurazioneNorm;
	public String getAuthKey() {
		return authKey;
	}
	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}
	public String getConfigurazioneDeduplica() {
		return configurazioneDeduplica;
	}
	public void setConfigurazioneDeduplica(String configurazioneDeduplica) {
		this.configurazioneDeduplica = configurazioneDeduplica;
	}
	public String getConfigurazioneNorm() {
		return configurazioneNorm;
	}
	public void setConfigurazioneNorm(String configurazioneNorm) {
		this.configurazioneNorm = configurazioneNorm;
	}
	@Override
	public String toString() {
		return "ConfigIn [authKey=" + authKey + ", configurazioneDeduplica=" + configurazioneDeduplica
				+ ", configurazioneNorm=" + configurazioneNorm + "]";
	}
	
	
}
