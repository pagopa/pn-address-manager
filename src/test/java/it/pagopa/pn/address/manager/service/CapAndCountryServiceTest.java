package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.entity.CountryModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.repository.CapRepository;
import it.pagopa.pn.address.manager.repository.CountryRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {CapAndCountryService.class})
@ExtendWith(SpringExtension.class)
class CapAndCountryServiceTest {
    @Autowired
    private CapAndCountryService capAndCountryService;

    @MockBean
    private CapRepository capRepository;

    @MockBean
    private AddressUtils addressUtils;

    @MockBean
    private CountryRepository countryRepository;

    @MockBean
    private PnAddressManagerConfig pnAddressManagerConfig;

    @Test
    void verifyCapAndCountry(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        when(pnAddressManagerConfig.getEnableWhitelisting()).thenReturn(true);
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("ITALIA");
        item.setNormalizedAddress(analogAddress);
        CapModel capModel = new CapModel();
        capModel.setStartValidity(LocalDateTime.now());
        capModel.setEndValidity(LocalDateTime.now().minusDays(1));
        capModel.setCap("12345");
        when(capRepository.findValidCap(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }
    @Test
    void verifyCapAndCountryCountryError(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        when(pnAddressManagerConfig.getEnableWhitelisting()).thenReturn(true);
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("FRANCE");
        item.setNormalizedAddress(analogAddress);
        CountryModel countryModel = new CountryModel();
        countryModel.setCountry("COUNTRY");
        countryModel.setStartValidity(LocalDateTime.now());
        countryModel.setEndValidity(LocalDateTime.now().minusDays(1));
        when(countryRepository.findByName(anyString())).thenReturn(Mono.just(countryModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }
    @Test
    void verifyCapAndCountryValidityError1(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        when(pnAddressManagerConfig.getEnableWhitelisting()).thenReturn(true);
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("FRANCE");
        item.setNormalizedAddress(analogAddress);
        CountryModel countryModel = new CountryModel();
        countryModel.setCountry("COUNTRY");
        countryModel.setStartValidity(null);
        when(countryRepository.findByName(anyString())).thenReturn(Mono.just(countryModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }
    @Test
    void verifyCapAndCountryValidityError2(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        when(pnAddressManagerConfig.getEnableWhitelisting()).thenReturn(true);
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("ITALIA");
        item.setNormalizedAddress(analogAddress);
        CapModel capModel = new CapModel();
        capModel.setStartValidity(null);
        capModel.setCap("12345");
        when(capRepository.findValidCap(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }

    @Test
    void verifyCapAndCountry1(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("ITALIA");
        item.setNormalizedAddress(analogAddress);
        CapModel capModel = new CapModel();
        capModel.setStartValidity(LocalDateTime.now());
        capModel.setEndValidity(LocalDateTime.now().plusDays(1));
        capModel.setCap("12345");
        when(capRepository.findValidCap(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }

    @Test
    void verifyCapAndCountry2(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("ITALIA");
        item.setNormalizedAddress(analogAddress);
        CapModel capModel = new CapModel();
        capModel.setStartValidity(LocalDateTime.now().plusDays(1));
        capModel.setEndValidity(LocalDateTime.now().plusDays(1));
        capModel.setCap("12345");
        when(capRepository.findValidCap(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }

    @Test
    void verifyCapAndCountry3(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("ITALIA");
        item.setNormalizedAddress(analogAddress);
        CapModel capModel = new CapModel();
        capModel.setStartValidity(LocalDateTime.now());
        capModel.setEndValidity(LocalDateTime.now().minusDays(1));
        capModel.setCap("12345");
        when(capRepository.findValidCap(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }


    @Test
    void verifyCapAndCountry4(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("USA");
        item.setNormalizedAddress(analogAddress);
        CountryModel capModel = new CountryModel();
        capModel.setCountry("COUNTRY");
        capModel.setStartValidity(LocalDateTime.now());
        capModel.setEndValidity(LocalDateTime.now().plusDays(1));
        when(countryRepository.findByName(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }

    @Test
    void verifyCapAndCountry5(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("USA");
        item.setNormalizedAddress(analogAddress);
        CountryModel capModel = new CountryModel();
        capModel.setCountry("COUNTRY");
        capModel.setStartValidity(LocalDateTime.now().plusDays(1));
        capModel.setEndValidity(LocalDateTime.now().minusDays(1));
        when(countryRepository.findByName(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }

    @Test
    void verifyCapAndCountry6(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("USA");
        item.setNormalizedAddress(analogAddress);
        CountryModel capModel = new CountryModel();
        capModel.setCountry("COUNTRY");
        capModel.setStartValidity(LocalDateTime.now());
        capModel.setEndValidity(LocalDateTime.now().minusDays(1));
        when(countryRepository.findByName(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }

    @Test
    void verifyCapAndCountry7(){
        DeduplicatesResponse item = new DeduplicatesResponse();
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }

    @Test
    void verifyCapAndCountryList(){
        NormalizeResult item = new NormalizeResult();
        when(pnAddressManagerConfig.getEnableWhitelisting()).thenReturn(true);
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("ITALIA");
        item.setNormalizedAddress(analogAddress);
        CapModel capModel = new CapModel();
        capModel.setStartValidity(LocalDateTime.now());
        capModel.setEndValidity(LocalDateTime.now().minusDays(1));
        capModel.setCap("12345");
        when(capRepository.findValidCap(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }
    @Test
    void verifyCapAndCountryListError(){
        NormalizeResult item = new NormalizeResult();
        when(pnAddressManagerConfig.getEnableWhitelisting()).thenReturn(true);
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("FRANCE");
        item.setNormalizedAddress(analogAddress);
        CountryModel countryModel = new CountryModel();
        countryModel.setCountry("COUNTRY");
        countryModel.setStartValidity(LocalDateTime.now());
        countryModel.setEndValidity(LocalDateTime.now().minusDays(1));
        when(countryRepository.findByName(anyString())).thenReturn(Mono.just(countryModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }
    @Test
    void verifyCapAndCountryList1(){
        NormalizeResult item = new NormalizeResult();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("USA");
        item.setNormalizedAddress(analogAddress);
        CountryModel capModel = new CountryModel();
        capModel.setCountry("COUNTRY");
        capModel.setStartValidity(LocalDateTime.now());
        capModel.setEndValidity(LocalDateTime.now().minusDays(1));
        when(countryRepository.findByName(any())).thenReturn(Mono.just(capModel));
        StepVerifier.create(capAndCountryService.verifyCapAndCountry(item)).expectNext(item).verifyComplete();
    }
}

