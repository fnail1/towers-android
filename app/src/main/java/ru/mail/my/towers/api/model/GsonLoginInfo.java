package ru.mail.my.towers.api.model;

public class GsonLoginInfo {
    public String token;

    /**
     * новый ли пользователь (может требоваться оформление профиля)
     */
    public boolean isNewUser;
}
