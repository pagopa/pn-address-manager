package it.pagopa.pn.template.service;

import it.pagopa.pn.template.model.Cap;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class CsvService {

    private static final String COUNTRY_PATH = "/PagoPA-Lista-Nazioni.csv";
    private static final String CAP_PATH = "/PagoPA-ListaCAP.csv";

    //method to read csv file and return countrymap
    public Map<String, String> countryMap() {
        Map<String, String> countryMap = new HashMap<>();
        InputStream inputStream = getClass().getResourceAsStream(COUNTRY_PATH);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                countryMap.put(values[0], values[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return countryMap;
    }

    //method to read csv file and return capmap
    public Map<String, Object> capMap() {
        Map<String, Object> capMap = new HashMap<>();
        InputStream inputStream = getClass().getResourceAsStream(CAP_PATH);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String CAP = values[0];
                Cap cap = new Cap(); // Oggetto contenente i 3 valori della riga
                cap.setCap(values[0]);
                cap.setRegione(values[1]);
                cap.setProvincia(values[2]);
                capMap.put(CAP, cap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return capMap;
    }

}