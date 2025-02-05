
package com.gentics.contentnode.dbcopy.jaxb;

import com.gentics.contentnode.dbcopy.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für tables element declaration.</p>
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.</p>
 * 
 * <pre>{@code
 * <element name="tables">
 *   <complexType>
 *     <complexContent>
 *       <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *         <sequence>
 *           <element name="table" type="{}tableType" maxOccurs="unbounded" minOccurs="0"/>
 *         </sequence>
 *         <attribute name="roottable" use="required" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       </restriction>
 *     </complexContent>
 *   </complexType>
 * </element>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "table"
})
@XmlRootElement(name = "tables")
public class Tables {

    @XmlElement(type = Table.class)
    protected JAXBTableType[] table;
    @XmlAttribute(name = "roottable", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String roottable;

    /**
     * 
     * @return
     *     array of
     *     {@link JAXBTableType }
     *     
     */
    public JAXBTableType[] getTable() {
        if (this.table == null) {
            return new JAXBTableType[ 0 ] ;
        }
        JAXBTableType[] retVal = new Table[this.table.length] ;
        System.arraycopy(this.table, 0, retVal, 0, this.table.length);
        return (retVal);
    }

    /**
     * 
     * 
     * @return
     *     one of
     *     {@link JAXBTableType }
     *     
     */
    public JAXBTableType getTable(int idx) {
        if (this.table == null) {
            throw new IndexOutOfBoundsException();
        }
        return this.table[idx];
    }

    public int getTableLength() {
        if (this.table == null) {
            return  0;
        }
        return this.table.length;
    }

    /**
     * 
     * 
     * @param values
     *     allowed objects are
     *     {@link JAXBTableType }
     *     
     */
    public void setTable(JAXBTableType[] values) {
        if (values == null) {
            this.table = null;
            return ;
        }
        int len = values.length;
        this.table = ((Table[]) new Table[len] );
        for (int i = 0; (i<len); i ++) {
            this.table[i] = ((Table) values[i]);
        }
    }

    /**
     * 
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBTableType }
     *     
     */
    public JAXBTableType setTable(int idx, JAXBTableType value) {
        return this.table[idx] = ((Table) value);
    }

    public boolean isSetTable() {
        return ((this.table!= null)&&(this.table.length > 0));
    }

    public void unsetTable() {
        this.table = null;
    }

    /**
     * Ruft den Wert der roottable-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRoottable() {
        return roottable;
    }

    /**
     * Legt den Wert der roottable-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRoottable(String value) {
        this.roottable = value;
    }

    public boolean isSetRoottable() {
        return (this.roottable!= null);
    }

}
