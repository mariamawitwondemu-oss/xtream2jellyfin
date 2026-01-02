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
@JacksonXmlRootElement(localName = "episodedetails")
public class EpisodeNfo {

    @JacksonXmlProperty(localName = "title")
    private String title;

    @JacksonXmlProperty(localName = "season")
    private Integer season;

    @JacksonXmlProperty(localName = "episode")
    private Integer episode;

    @JacksonXmlProperty(localName = "aired")
    private String aired;

    @JacksonXmlProperty(localName = "plot")
    private String plot;

    @JacksonXmlProperty(localName = "userrating")
    private Double userRating;

    @JacksonXmlProperty(localName = "director")
    private String director;

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
}
