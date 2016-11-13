package ru.mail.my.towers.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)

public @interface DbForeignKey {
    String table();

    String column() default DbUtils.COLUMN_ID;

    ForeignKeyAction onDelete() default ForeignKeyAction.CASCADE;

    ForeignKeyAction onUpdate() default ForeignKeyAction.CASCADE;

}
