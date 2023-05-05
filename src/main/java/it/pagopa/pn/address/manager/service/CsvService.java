package it.pagopa.pn.address.manager.service;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.AddressModel;
import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.model.CountryModel;
import it.pagopa.pn.address.manager.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.rest.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.address.manager.rest.v1.dto.NormalizeResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_CSVERROR;

@Component
public class CsvService {

    private static final String VERIFY_CSV_ERROR = "Error during verify CSV";
    private static final String WRITING_CSV_ERROR = "Error during writing CSV";

    private final String countryPath;
    private final String capPath;

    public CsvService(@Value("${pn.address.manager.csv.path.country}") String countryPath,
                      @Value("${pn.address.manager.csv.path.cap}") String capPath) {
        this.countryPath = countryPath;
        this.capPath = capPath;
    }

    public void createAddressCsv(List<AddressModel> addressModels){
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        String currentDateTime = dateFormat.format(new Date());
        String fileName = currentDateTime+".csv";
        try (FileWriter writer = new FileWriter(new ClassPathResource("/").getFile().getAbsolutePath()+fileName)) {
            ColumnPositionMappingStrategy<AddressModel> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(AddressModel.class);

            StatefulBeanToCsv<AddressModel> beanToCsv = new StatefulBeanToCsvBuilder<AddressModel>(writer)
                    .withMappingStrategy(strategy)
                    .withQuotechar('"')
                    .withSeparator(',')
                    .build();

            beanToCsv.write(addressModels);
            writer.flush();

        }   catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            throw new PnAddressManagerException(WRITING_CSV_ERROR, "Error writing file: " + "pago.csv", HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
        }
    }

    public Map<String, NormalizeItemsResult> readNormalizeItemsResultFromCsv(){
        String folderPath = "path/to/folder";
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        Map<String, NormalizeItemsResult> cxIdNormalizeItemResult = new HashMap<>();
        for (File file : files) {
            try(FileReader fileReader = new FileReader(ResourceUtils.getFile("classpath:" + file.getName()))) {
                CsvToBeanBuilder<AddressModel> csvToBeanBuilder = new CsvToBeanBuilder<>(fileReader);
                csvToBeanBuilder.withSkipLines(1);
                csvToBeanBuilder.withType(AddressModel.class);

                List<AddressModel> addressModels = csvToBeanBuilder.build().parse();

                Map<String, Map<String, List<AddressModel>>> addressModelsForCxIdAndCorrelationId = addressModels.stream()
                        .collect(Collectors.groupingBy(AddressModel::getCxid,
                                Collectors.groupingBy(AddressModel::getCorrelationId)));

                for (Map.Entry<String, Map<String, List<AddressModel>>> entry : addressModelsForCxIdAndCorrelationId.entrySet()) {
                    String cxId = entry.getKey();

                    NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
                    Map<String, List<AddressModel>> addressModelsForCorrelationId = entry.getValue();

                    for (Map.Entry<String, List<AddressModel>> innerEntry : addressModelsForCorrelationId.entrySet()) {
                        List<AddressModel> analogAddressModels = innerEntry.getValue();
                        List<NormalizeResult> list = analogAddressModels.stream().map(analogAddressModel -> {
                            NormalizeResult normalizeResult = new NormalizeResult();
                            normalizeResult.setId(analogAddressModel.getAddressId());
                            normalizeResult.setNormalizedAddress(createAnalogAddressByModel(analogAddressModel));
                            return normalizeResult;
                        }).toList();
                        normalizeItemsResult.setCorrelationId(innerEntry.getKey());
                        normalizeItemsResult.setResultItems(list);
                    }

                    cxIdNormalizeItemResult.put(cxId,normalizeItemsResult);
                }

            } catch (IOException e) {
                throw new PnAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + file.getName(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
            }
        }
        return cxIdNormalizeItemResult;
    }

    private AnalogAddress createAnalogAddressByModel(AddressModel analogAddressModel){
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow(analogAddressModel.getAddressRow());
        analogAddress.setAddressRow(analogAddressModel.getAddressRow2());
        analogAddress.setCap(analogAddressModel.getCap());
        analogAddress.setCity(analogAddressModel.getCity());
        analogAddress.setCity2(analogAddressModel.getCity2());
        analogAddress.setCountry(analogAddressModel.getCountry());
        analogAddress.setPr(analogAddressModel.getPr());
        return analogAddress;
    }

    public Map<String, String> countryMap() {
        try(FileReader fileReader = new FileReader(ResourceUtils.getFile("classpath:" + countryPath))) {
            CsvToBeanBuilder<CountryModel> csvToBeanBuilder = new CsvToBeanBuilder<>(fileReader);
            csvToBeanBuilder.withSkipLines(1);
            csvToBeanBuilder.withType(CountryModel.class);
            return csvToBeanBuilder.build().parse()
                    .stream().collect(Collectors.toMap(CountryModel::getName, CountryModel::getIsocode, (o, o2) -> o));
        } catch (IOException e) {
            throw new PnAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + countryPath, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
        }
    }

    public Map<String, Object> capMap() {
        try(FileReader fileReader = new FileReader(ResourceUtils.getFile("classpath:" + capPath))) {
            CsvToBeanBuilder<CapModel> csvToBeanBuilder = new CsvToBeanBuilder<>(fileReader);
            csvToBeanBuilder.withSkipLines(1);
            csvToBeanBuilder.withType(CapModel.class);
            return csvToBeanBuilder.build().parse()
                    .stream().collect(Collectors.toMap(CapModel::getCap, o -> o, (o, o2) -> o));
        } catch (IOException e) {
            throw new PnAddressManagerException(VERIFY_CSV_ERROR, "Error reading file: " + capPath, HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_CSVERROR);
        }
    }
}