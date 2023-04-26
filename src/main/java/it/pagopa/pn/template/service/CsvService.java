package it.pagopa.pn.template.service;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import it.pagopa.pn.template.exception.PnAddressManagerException;
import it.pagopa.pn.template.model.Cap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class CsvService {

    private final String countryPath;
    private final String capPath;

    public CsvService(@Value("${pn.address.manager.csv.path.country}") String countryPath,
                      @Value("${pn.address.manager.csv.path.cap}") String capPath) {
        this.countryPath = countryPath;
        this.capPath = capPath;
    }

    public Map<String, String> countryMap() {
        String[] nextLine;
        Map<String, String> countryMap = new HashMap<>();
        InputStream inputStream = getClass().getResourceAsStream(countryPath);
        if (inputStream == null)
            throw new PnAddressManagerException("File not found: " + countryPath, HttpStatus.NOT_FOUND);
        try {
            CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream, "Windows-1252"))
                    .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                    .withSkipLines(1)
                    .build();
            while ((nextLine = csvReader.readNext()) != null) {
                countryMap.put(nextLine[0], nextLine[1]);
            }
            csvReader.close();
            inputStream.close();
            return countryMap;
        } catch (IOException | CsvValidationException e) {
            throw new PnAddressManagerException("Error while reading file: " + countryPath, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public Map<String, Object> capMap() {
        String[] nextLine;
        Map<String, Object> capMap = new HashMap<>();
        InputStream inputStream = getClass().getResourceAsStream(capPath);
        if (inputStream == null)
            throw new PnAddressManagerException("File not found: " + capPath, HttpStatus.NOT_FOUND);
        try {
            CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream, "Windows-1252"))
                    .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                    .withSkipLines(1)
                    .build();
            while ((nextLine = csvReader.readNext()) != null) {
                Cap cap = new Cap();
                cap.setCap(nextLine[0]);
                cap.setRegione(nextLine[1]);
                cap.setProvincia(nextLine[2]);
                capMap.put(nextLine[0], cap);
            }
            csvReader.close();
            inputStream.close();
            return capMap;
        } catch (IOException | CsvValidationException e) {
            throw new PnAddressManagerException("Error while reading file: " + countryPath, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}