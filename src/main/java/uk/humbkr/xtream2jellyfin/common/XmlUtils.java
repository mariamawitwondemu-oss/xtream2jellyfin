package uk.humbkr.xtream2jellyfin.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Getter;

public class XmlUtils {

    @Getter
    private static final XmlMapper xmlMapper = initializeXmlMapper();

    private static XmlMapper initializeXmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        xmlMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        return xmlMapper;
    }

}
