package it.pagopa.pn.address.manager.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.model.CountryModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;


@Component
@lombok.CustomLog
public class CsvService {

    private final String countryPath;
    private final String capPath;
    private final ResourceLoader resourceLoader;

    public CsvService(@Value("${pn.address.manager.csv.path.country}") String countryPath,
                      @Value("${pn.address.manager.csv.path.cap}") String capPath, ResourceLoader resourceLoader) {
        this.countryPath = countryPath;
        this.capPath = capPath;
        this.resourceLoader = resourceLoader;
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

    public Map<String, String> countryMap() {
        try {
            List<CountryModel> countryModels = readItemsFromCsv(CountryModel.class, resourceLoader.getResource("classpath:" + countryPath).getFile(),1);
            return countryModels.stream()
                    .filter(countryModel -> !StringUtils.isBlank(countryModel.getName()))
                    .collect(Collectors.toMap(model -> normalizeKey(model.getName()), CountryModel::getIsocode, (o, o2) -> o));
        } catch (IOException e) {
            throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_VERIFY_CSV, ERROR_ADDRESS_MANAGER_VERIFY_CSV_DESCRIPTION + countryPath, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_VERIFY_CSV_ERROR_CODE);
        }
    }

    public Map<String, Object> capMap() {
        try {
            List<CapModel> capModels = readItemsFromCsv(CapModel.class, resourceLoader.getResource("classpath:" + capPath).getFile(),1);
            return capModels.stream()
                    .filter(capModel -> !StringUtils.isBlank(capModel.getCap()))
                    .collect(Collectors.toMap(model -> model.getCap().trim(), o -> o, (o, o2) -> o));
        } catch (IOException e) {
            throw new PnAddressManagerException(ERROR_ADDRESS_MANAGER_VERIFY_CSV, ERROR_ADDRESS_MANAGER_VERIFY_CSV_DESCRIPTION + capPath, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_VERIFY_CSV_ERROR_CODE);
        }
    }

    private String normalizeKey(String key) {
        return StringUtils.normalizeSpace(key).toUpperCase();
    }

}