package ru.mail.my.towers.data;

public interface IDbSerializationHandlers {
    void onBeforeSerialization();
    void onAfterDeserialization();
}
