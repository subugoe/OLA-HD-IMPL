package ola.hd.longtermstorage.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchHit {

    private String id;

    private String type;

    private String name;

    private float score;

    private SearchHitDetail detail;

    public SearchHit() {
        // A default constructor is necessary for JSON deserialization
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public SearchHitDetail getDetail() {
        return detail;
    }

    public void setDetail(SearchHitDetail detail) {
        this.detail = detail;
    }
}
