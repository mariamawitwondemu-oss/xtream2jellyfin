package uk.humbkr.xtream2jellyfin.metadata.nfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "season")
public class SeasonNfo {

    @JacksonXmlProperty(localName = "title")
    private String title;

    @JacksonXmlProperty(localName = "plot")
    private String plot;

    @JacksonXmlProperty(localName = "seasonnumber")
    private Integer seasonNumber;

    @JacksonXmlProperty(localName = "premiered")
    private String premiered;

    @JacksonXmlProperty(localName = "year")
    private Integer year;

    @JacksonXmlProperty(localName = "userrating")
    private Double userRating;

    @JacksonXmlProperty(localName = "uniqueid")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<UniqueId> uniqueIds;

    @JacksonXmlProperty(localName = "thumb")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Thumb> thumbs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Thumb {

        @JacksonXmlProperty(isAttribute = true, localName = "aspect")
        private String aspect;

        @JacksonXmlText
        private String url;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniqueId {

        @JacksonXmlProperty(isAttribute = true, localName = "type")
        private String type;

        @JacksonXmlProperty(isAttribute = true, localName = "default")
        private Boolean isDefault;

        @JacksonXmlText
        private String value;

    }
}
