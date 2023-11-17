package it.pagopa.pn.address.manager.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.model.CountryModel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.opencsv.ICSVWriter.*;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_END_WRITING_CSV;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_START_WRITING_CSV;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;


@Component
@lombok.CustomLog
@RequiredArgsConstructor
public class CsvService {

    private static final String VERIFY_CSV_ERROR = "Error during verify CSV";
    private final PnAddressManagerConfig pnAddressManagerConfig;

    public <T> void writeItemsOnCsv(List<T> items, String nameFile, String directoryPath) {
        try (FileWriter writer = new FileWriter(new File(directoryPath, nameFile))) {
            CSVWriter cw = new CSVWriter(writer, ';' , DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(cw)
                    .build();
            beanToCsv.write(items);
            writer.flush();
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new PnInternalAddressManagerException(ERROR_ADDRESS_MANAGER_WRITING_CSV, ERROR_ADDRESS_MANAGER_WRITING_CSV_DESCRIPTION + nameFile, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_WRITING_CSV_ERROR_CODE);
        }
    }

    public <T> String writeItemsOnCsvToString(List<T> items) {
        log.logStartingProcess(PROCESS_START_WRITING_CSV);
        try (StringWriter writer = new StringWriter()) {
            CSVWriter cw = new CSVWriter(writer, ';' , DEFAULT_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
            StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(cw)
                    .build();
            beanToCsv.write(items);
            log.logEndingProcess(PROCESS_END_WRITING_CSV);
            return writer.toString();
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new PnInternalAddressManagerException(ERROR_ADDRESS_MANAGER_WRITING_CSV, ERROR_ADDRESS_MANAGER_WRITING_CSV_DESCRIPTION, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_WRITING_CSV_ERROR_CODE);
        }
    }

    public <T> List<T> readItemsFromCsv(Class<T> csvClass, byte[] file, int skipLines) {
        try {
            StringReader stringReader = new StringReader(new String(file, StandardCharsets.UTF_8));
            CsvToBeanBuilder<T> csvToBeanBuilder = new CsvToBeanBuilder<>(stringReader);
            csvToBeanBuilder.withSeparator(';');
            csvToBeanBuilder.withQuoteChar(DEFAULT_QUOTE_CHARACTER);
            csvToBeanBuilder.withSkipLines(skipLines);
            csvToBeanBuilder.withType(csvClass);

            List<T> parsedItems = csvToBeanBuilder.build().parse();
            return new ArrayList<>(parsedItems);
        }catch (Exception e){
            throw new PnInternalAddressManagerException(ERROR_ADDRESS_MANAGER_READING_CSV, ERROR_ADDRESS_MANAGER_READING_CSV_DESCRIPTION, HttpStatus.BAD_REQUEST.value(), ERROR_ADDRESS_MANAGER_READING_CSV_ERROR_CODE);
        }
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
            throw new PnInternalAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + pnAddressManagerConfig.getCsv().getPathCountry(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
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
            throw new PnInternalAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + pnAddressManagerConfig.getCsv().getPathCap(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
        }
    }

}