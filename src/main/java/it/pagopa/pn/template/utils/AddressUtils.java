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
        return compareAddressRow(baseAddress.getAddressRow(), targetAddress.getAddressRow())
                && compareCap(baseAddress.getCap(), targetAddress.getCap())
                && compareCity(baseAddress.getCity(), targetAddress.getCity())
                && comparePr(baseAddress.getPr(), targetAddress.getPr())
                && compareCountry(baseAddress.getCountry(), targetAddress.getCountry());
    }

    private boolean compareAddressRow(String baseAddressRow, String targetAddressRow){ return baseAddressRow.equalsIgnoreCase(targetAddressRow); }

    private boolean compareCap(String baseCap, String targetCap){
        return baseCap.equalsIgnoreCase(targetCap);
    }

    private boolean compareCity(String baseCity, String targetCity){
        return baseCity.equalsIgnoreCase(targetCity);
    }

    private boolean comparePr(String basePr, String targetPr){
        return basePr.equalsIgnoreCase(targetPr);
    }

    private boolean compareCountry(String baseCountry, String targetCountry){ return baseCountry.equalsIgnoreCase(targetCountry); }

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
        return ((analogAddress.getCountry()==null || analogAddress.getCountry().contains("ITAL")) && searchStringInColumnInCsv(analogAddress.getCap(), INDEX_COLUMN_CAP, PATH_CSV_CAP))
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
            return false;
        }
        catch (IOException | CsvValidationException e) {
            throw new PnAddressManagerException("Problema con i csv", HttpStatus.CONFLICT);
        }
    }


}
