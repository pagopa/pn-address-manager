package it.pagopa.pn.address.manager.service;

import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.CountryModel;
import it.pagopa.pn.address.manager.model.CapModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_CSVERROR;

@Component
public class CsvService {

    private static final String VERIFY_CSV_ERROR = "Error during verify CSV";

    private final String countryPath;
    private final String capPath;

    public CsvService(@Value("${pn.address.manager.csv.path.country}") String countryPath,
                      @Value("${pn.address.manager.csv.path.cap}") String capPath) {
        this.countryPath = countryPath;
        this.capPath = capPath;
    }

    public Map<String, String> countryMap() {
        try(FileReader fileReader = new FileReader(ResourceUtils.getFile("classpath:" + countryPath))) {
            CsvToBeanBuilder<CountryModel> csvToBeanBuilder = new CsvToBeanBuilder<>(fileReader);
            csvToBeanBuilder.withSkipLines(1);
            csvToBeanBuilder.withType(CountryModel.class);
            return csvToBeanBuilder.build().parse()
                    .stream()
                    .filter(countryModel -> !StringUtils.isBlank(countryModel.getName()))
                    .collect(Collectors.toMap(model ->
                            StringUtils.normalizeSpace(model.getName()).toUpperCase(), CountryModel::getIsocode, (o, o2) -> o));
        } catch (IOException e) {
            throw new PnAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + countryPath, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
        }
    }

    public List<CapModel> capList() {
        try(FileReader fileReader = new FileReader(ResourceUtils.getFile("classpath:" + capPath))) {
            CsvToBeanBuilder<CapModel> csvToBeanBuilder = new CsvToBeanBuilder<>(fileReader);
            csvToBeanBuilder.withSkipLines(1);
            csvToBeanBuilder.withSeparator(';');
            csvToBeanBuilder.withType(CapModel.class);
            return csvToBeanBuilder.build().parse()
                    .stream()
                    .filter(capModel -> !StringUtils.isBlank(capModel.getCap()))
                    .toList();
        } catch (IOException e) {
            throw new PnAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + capPath, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
        }
    }
}