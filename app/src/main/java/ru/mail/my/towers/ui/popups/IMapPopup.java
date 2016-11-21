package ru.mail.my.towers.ui.popups;

public interface IMapPopup {
    void show();

    void close();

    interface IMapActivity {
        void onPopupResult(IMapPopup popup);
    }
}
