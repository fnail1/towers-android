package ru.mail.my.towers.toolkit.events;

import rx.Observable;
import rx.subjects.PublishSubject;

public class EventBus {

    private PublishSubject<Event> subject = PublishSubject.create();

    public void send(Event object) {
        subject.onNext(object);
    }

    public Observable<Event> getEvents() {
        return subject;
    }

    public interface Event {}
}
