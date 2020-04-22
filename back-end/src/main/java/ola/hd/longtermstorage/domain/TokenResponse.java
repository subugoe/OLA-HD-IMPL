package ola.hd.longtermstorage.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import ola.hd.longtermstorage.utils.SecurityConstants;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiredTime;

    public TokenResponse(String accessToken) {
        this.accessToken = accessToken;
        this.expiredTime = new Date().getTime() + SecurityConstants.EXPIRATION_TIME;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiredTime() {
        return expiredTime;
    }
}
