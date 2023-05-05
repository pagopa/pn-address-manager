package it.pagopa.pn.address.manager.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressModel {

    @CsvBindByName(column = "pn-address-manager-cx-id")
    @CsvBindByPosition(position = 0)
    private String cxid;

    @CsvBindByName(column = "correlationId")
    @CsvBindByPosition(position = 1)
    private String correlationId;

    @CsvBindByName(column = "addressId")
    @CsvBindByPosition(position = 2)
    private String addressId;

    @CsvBindByName(column = "addressRow")
    @CsvBindByPosition(position = 3)
    private String addressRow;

    @CsvBindByName(column = "addressRow2")
    @CsvBindByPosition(position = 4)
    private String addressRow2;

    @CsvBindByName(column = "cap")
    @CsvBindByPosition(position = 5)
    private String cap;

    @CsvBindByName(column = "city")
    @CsvBindByPosition(position = 6)
    private String city;

    @CsvBindByName(column = "city2")
    @CsvBindByPosition(position = 7)
    private String city2;

    @CsvBindByName(column = "pr")
    @CsvBindByPosition(position = 8)
    private String pr;

    @CsvBindByName(column = "country")
    @CsvBindByPosition(position = 9)
    private String country;
}
