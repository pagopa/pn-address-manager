package it.pagopa.pn.template.utils;

import it.pagopa.pn.template.rest.v1.dto.AnalogAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class AddressUtilsTest {


    @Test
    void compareAddress(){
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        AddressUtils addressUtils = new AddressUtils(true);
        assertTrue(addressUtils.compareAddress(base,base));
    }

    @Test
    void compareAddress1(){
        AnalogAddress base = new AnalogAddress();
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        AddressUtils addressUtils = new AddressUtils(true);
        assertTrue(addressUtils.compareAddress(base,base));
    }

    @Test
    void compareAddress2(){
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        AnalogAddress target = new AnalogAddress();
        AddressUtils addressUtils = new AddressUtils(true);
        assertFalse(addressUtils.compareAddress(base,target));
    }

    @Test
    void normalizeAddress(){
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCap("00010");
        AddressUtils addressUtils = new AddressUtils(true);
        assertNotNull(addressUtils.normalizeAddress(base));
    }

    @Test
    void normalizeAddress2(){
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCap("ARUBA");
        base.setCountry("ARUBA");
        AddressUtils addressUtils = new AddressUtils(true);
        assertNotNull(addressUtils.normalizeAddress(base));
    }


    @Test
    void normalizeAddress3(){
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCap("ARUBAA");
        base.setCountry("ARUBAA");
        AddressUtils addressUtils = new AddressUtils(true);
        assertNull(addressUtils.normalizeAddress(base));
    }
}

