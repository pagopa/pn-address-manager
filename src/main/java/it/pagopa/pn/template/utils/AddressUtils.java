package it.pagopa.pn.template.utils;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import it.pagopa.pn.template.exception.PnAddressManagerException;
import it.pagopa.pn.template.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.template.rest.v1.dto.NormalizeRequest;
import it.pagopa.pn.template.rest.v1.dto.NormalizeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class AddressUtils {

    private final boolean flagCsv;
    private static final String PATH_CSV_CAP = "/PagoPA-ListaCAP.csv";
    private static final String PATH_CSV_NAZIONI = "/PagoPA-ListaNazioni.csv";
    private static final int INDEX_COLUMN_CAP = 0;
    private static final int INDEX_COLUMN_NAZIONALITY = 0;

    public AddressUtils(
            @Value("${pn.address.manager.flag.csv}") boolean flagCsv){
        this.flagCsv = flagCsv;
    }

    public boolean compareAddress(AnalogAddress baseAddress, AnalogAddress targetAddress){
        return compare(baseAddress.getAddressRow(), targetAddress.getAddressRow())
                && compare(baseAddress.getAddressRow2(), targetAddress.getAddressRow2())
                && compare(baseAddress.getCap(), targetAddress.getCap())
                && compare(baseAddress.getCity(), targetAddress.getCity())
                && compare(baseAddress.getCity2(), targetAddress.getCity2())
                && compare(baseAddress.getPr(), targetAddress.getPr())
                && compare(baseAddress.getCountry(), targetAddress.getCountry());
    }

    private boolean compare(String base, String target){
        String trimmedBase = Optional.ofNullable(base).orElse("").trim();
        String trimmedTarget = Optional.ofNullable(target).orElse("").trim();
        return trimmedBase.equalsIgnoreCase(trimmedTarget);
    }

    public AnalogAddress normalizeAddress(AnalogAddress analogAddress){
        if(verifyAddress(analogAddress)) {
            return toUpperCase(analogAddress);
        }else{
           throw new PnAddressManagerException("Cannot verify address", HttpStatus.BAD_REQUEST);
        }
    }

    private AnalogAddress toUpperCase(AnalogAddress analogAddress) {
        analogAddress.setAddressRow(analogAddress.getAddressRow().toUpperCase());
        analogAddress.setCity(analogAddress.getCity().toUpperCase());
        analogAddress.setCap(Optional.ofNullable(analogAddress.getCap()).map(String::toUpperCase).orElse(null));
        analogAddress.setPr(Optional.ofNullable(analogAddress.getPr()).map(String::toUpperCase).orElse(null));
        analogAddress.setCountry(Optional.ofNullable(analogAddress.getCountry()).map(String::toUpperCase).orElse(null));
        analogAddress.setAddressRow2(Optional.ofNullable(analogAddress.getAddressRow2()).map(String::toUpperCase).orElse(null));
        analogAddress.setCity2(Optional.ofNullable(analogAddress.getCity2()).map(String::toUpperCase).orElse(null));
        return analogAddress;
    }


    public boolean verifyAddress(AnalogAddress analogAddress) {
        if (flagCsv) {
            return verifyAddressInCsv(analogAddress);
        } else {
            //TODO verify with postel
            return true;
        }
    }

    private boolean verifyAddressInCsv(AnalogAddress analogAddress){
        return ((StringUtils.hasText(analogAddress.getCountry())
                || analogAddress.getCountry().contains("ITAL"))
                && searchStringInColumnInCsv(analogAddress.getCap(), INDEX_COLUMN_CAP, PATH_CSV_CAP))
                || searchStringInColumnInCsv(analogAddress.getCountry(), INDEX_COLUMN_NAZIONALITY, PATH_CSV_NAZIONI);
    }

    private boolean searchStringInColumnInCsv(String string, int columnIndex, String filePath){
        String[] nextLine;
        try {
            InputStream inputStream = AddressUtils.class.getResourceAsStream(filePath);
            assert inputStream != null;
            CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream, "Windows-1252"))
                    .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                    .withSkipLines(1)
                    .build();
            while ((nextLine = csvReader.readNext()) != null) {
                if (nextLine.length > columnIndex && nextLine[columnIndex].equalsIgnoreCase(string)) {
                    csvReader.close();
                    inputStream.close();
                    return true;
                }
            }
            csvReader.close();
            inputStream.close();
            return false; //TODO: MESSAGGIO PARLANTE PER CAP NOT FOUND O COUNTRY NOT FOUND (NON BASTA SOLO IL BOOLEAN)
        } catch (IOException | CsvValidationException e) {
            throw new PnAddressManagerException("Error during verify address with csv file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public NormalizeItemsResult normalizeAddresses(String correlationId, List<NormalizeRequest> requestItems) {
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        normalizeItemsResult.setCorrelationId(correlationId);

        List<NormalizeResult> normalizeResultList = new ArrayList<>();
        for (NormalizeRequest n : requestItems) {
            NormalizeResult normalizeResult = new NormalizeResult();
            normalizeResult.setId(n.getId());
            normalizeResult.setNormalizedAddress(normalizeAddress(n.getAddress()));
            normalizeResultList.add(normalizeResult);
        }

        normalizeItemsResult.setResultItems(normalizeResultList);
        return normalizeItemsResult;
    }
}
