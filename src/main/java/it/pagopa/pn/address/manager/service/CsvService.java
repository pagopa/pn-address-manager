package it.pagopa.pn.address.manager.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.model.CountryModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;


@Component
@lombok.CustomLog
public class CsvService {

    private static final String VERIFY_CSV_ERROR = "Error during verify CSV";
    private final PnAddressManagerConfig pnAddressManagerConfig;

    public CsvService(PnAddressManagerConfig pnAddressManagerConfig) {
        this.pnAddressManagerConfig = pnAddressManagerConfig;
    }

    public <T> void writeItemsOnCsv(List<T> items, String nameFile, String directoryPath) {
        try (FileWriter writer = new FileWriter(new File(directoryPath, nameFile))) {
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withQuotechar('"')
                    .withSeparator(';')
                    .build();
            beanToCsv.write(items);
            writer.flush();
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_WRITING_CSV, ERROR_ADDRESS_MANAGER_WRITING_CSV_DESCRIPTION + nameFile, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_WRITING_CSV_ERROR_CODE);
        }
    }

    public <T> String writeItemsOnCsvToString(List<T> items) {
        try (StringWriter writer = new StringWriter()) {
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
                    .withQuotechar('"')
                    .withSeparator(';')
                    .build();
            beanToCsv.write(items);
            return writer.toString();
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_WRITING_CSV, ERROR_ADDRESS_MANAGER_WRITING_CSV_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_WRITING_CSV_ERROR_CODE);
        }
    }

    /*
    private <T> List<T> readItemsFromCsv(Class<T> csvClass, File filePath, int skipLines) {
        List<T> items = new ArrayList<>();
        try (FileReader fileReader = new FileReader(filePath)) {
            CsvToBeanBuilder<T> csvToBeanBuilder = new CsvToBeanBuilder<>(fileReader);
            csvToBeanBuilder.withSkipLines(skipLines);
            csvToBeanBuilder.withType(csvClass);

            List<T> parsedItems = csvToBeanBuilder.build().parse();
            items.addAll(parsedItems);
        } catch (IOException e) {
            throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_READING_CSV, ERROR_ADDRESS_MANAGER_READING_CSV_DESCRIPTION + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_READING_CSV_ERROR_CODE);
        }
        return items;
    }
     */

    public <T> List<T> readItemsFromCsv(Class<T> csvClass, byte[] file, int skipLines) {
        StringReader stringReader = new StringReader(Arrays.toString(file));
        CsvToBeanBuilder<T> csvToBeanBuilder = new CsvToBeanBuilder<>(stringReader);
        csvToBeanBuilder.withSkipLines(skipLines);
        csvToBeanBuilder.withType(csvClass);

        List<T> parsedItems = csvToBeanBuilder.build().parse();
        return new ArrayList<>(parsedItems);
    }

    public Map<String, String> countryMap() {
        try(FileReader fileReader = new FileReader(ResourceUtils.getFile("classpath:" + pnAddressManagerConfig.getCsv().getPathCountry()))) {
            CsvToBeanBuilder<CountryModel> csvToBeanBuilder = new CsvToBeanBuilder<>(fileReader);
            csvToBeanBuilder.withSkipLines(1);
            csvToBeanBuilder.withType(CountryModel.class);
            return csvToBeanBuilder.build().parse()
                    .stream()
                    .filter(countryModel -> !StringUtils.isBlank(countryModel.getName()))
                    .collect(Collectors.toMap(model ->
                            StringUtils.normalizeSpace(model.getName()).toUpperCase(), CountryModel::getIsocode, (o, o2) -> o));
        } catch (IOException e) {
            throw new PnAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + pnAddressManagerConfig.getCsv().getPathCountry(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
        }
    }

    public List<CapModel> capList() {
        try(FileReader fileReader = new FileReader(ResourceUtils.getFile("classpath:" + pnAddressManagerConfig.getCsv().getPathCap()))) {
            CsvToBeanBuilder<CapModel> csvToBeanBuilder = new CsvToBeanBuilder<>(fileReader);
            csvToBeanBuilder.withSkipLines(1);
            csvToBeanBuilder.withSeparator(';');
            csvToBeanBuilder.withType(CapModel.class);
            return csvToBeanBuilder.build().parse()
                    .stream()
                    .filter(capModel -> !StringUtils.isBlank(capModel.getCap()))
                    .toList();
        } catch (IOException e) {
            throw new PnAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + pnAddressManagerConfig.getCsv().getPathCap(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
        }
    }

    /*
    private String normalizeKey(String key)
        return StringUtils.normalizeSpace(key).toUpperCase();
    }
    */

}