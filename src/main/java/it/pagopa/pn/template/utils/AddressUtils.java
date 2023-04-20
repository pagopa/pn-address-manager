package it.pagopa.pn.template.utils;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import it.pagopa.pn.template.exception.PnAddressManagerException;
import it.pagopa.pn.template.rest.v1.dto.AnalogAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Component
@Slf4j
public class AddressUtils {

    private final boolean flagCsv;
    private static final String PATH_CSV_CAP = "/PagoPA - Lista CAP.csv";
    private static final String PATH_CSV_NAZIONI = "/PagoPA - Lista Nazioni.csv";
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
        if(base==null && target==null){
            return true;
        }
        else if(base == null || target == null){
            return false;
        }
        else{
            return base.trim().equalsIgnoreCase(target.trim());
        }
    }

    public AnalogAddress normalizeAddress(AnalogAddress analogAddress){
        if(verifyAddress(analogAddress)) {
            return copyAndUpperCase(analogAddress);
        }else{
            return null;
        }
    }

    private AnalogAddress copyAndUpperCase(AnalogAddress analogAddress){
        if(analogAddress.getCountry()!=null){
            analogAddress.setCountry(analogAddress.getCountry().toUpperCase());
        }
        if(analogAddress.getCap()!=null){
            analogAddress.setCap(analogAddress.getCap().toUpperCase());
        }
        if(analogAddress.getPr()!=null){
            analogAddress.setPr(analogAddress.getPr().toUpperCase());
        }
        if(analogAddress.getAddressRow()!=null){
            analogAddress.setAddressRow(analogAddress.getAddressRow().toUpperCase());
        }
        if(analogAddress.getAddressRow2()!=null){
            analogAddress.setAddressRow2(analogAddress.getAddressRow2().toUpperCase());
        }
        if(analogAddress.getCity()!=null){
            analogAddress.setCity(analogAddress.getCity().toUpperCase());
        }
        if(analogAddress.getCity2()!=null){
            analogAddress.setCity2(analogAddress.getCity2().toUpperCase());
        }
        return analogAddress;
    }



    public boolean verifyAddress(AnalogAddress analogAddress){
        if(flagCsv){
            return verifyAddressInCsv(analogAddress);
        }
        else{
            //TO DO verify address client
            return verifyAddressInCsv(analogAddress);
        }
    }

    private boolean verifyAddressInCsv(AnalogAddress analogAddress){
        return ((analogAddress.getCountry()==null || analogAddress.getCountry().trim().toUpperCase().contains("ITAL")) && searchStringInColumnInCsv(analogAddress.getCap().trim().toUpperCase(), INDEX_COLUMN_CAP, PATH_CSV_CAP))
                || (analogAddress.getCountry()!=null && searchStringInColumnInCsv(analogAddress.getCountry().trim().toUpperCase(), INDEX_COLUMN_NAZIONALITY, PATH_CSV_NAZIONI));
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
            return false;
        }
        catch (IOException | CsvValidationException e) {
            throw new PnAddressManagerException("Problema con i csv", HttpStatus.CONFLICT);
        }
    }


}
