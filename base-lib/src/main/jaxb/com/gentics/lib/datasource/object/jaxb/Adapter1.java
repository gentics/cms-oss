
package com.gentics.lib.datasource.object.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class Adapter1
    extends XmlAdapter<String, Integer>
{


    public Integer unmarshal(String value) {
        return Integer.valueOf(value);
    }

    public String marshal(Integer value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

}
