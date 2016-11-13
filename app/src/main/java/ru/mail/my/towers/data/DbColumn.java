package ru.mail.my.towers.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbColumn {
    String name() default "";

    int length() default -1;

    boolean notNull() default false;

    ConflictAction onNullConflict() default ConflictAction.FAIL;

    boolean unique() default false;

    ConflictAction onUniqueConflict() default ConflictAction.FAIL;

    boolean primaryKey() default false;
}
