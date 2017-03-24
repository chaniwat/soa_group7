package com.shenzhentagram.model;

public class AuthenticateDetail {

    private String access_token;
    private User user;

    public AuthenticateDetail() { }

    public AuthenticateDetail(String access_token, User user) {
        this.access_token = access_token;
        this.user = user;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
