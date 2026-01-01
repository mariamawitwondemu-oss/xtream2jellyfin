package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@Data
public class MovieDetails {

    @JsonDeserialize(using = MovieInfoDeserializer.class)
    private MovieInfo info;

    @JsonProperty("movie_data")
    private MovieData movieData;

}
