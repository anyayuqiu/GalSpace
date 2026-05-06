package com.galspace.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VndbResponse {
    private List<VndbVisualNovel> results;
    private boolean more;
    private Integer count;
}
