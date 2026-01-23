package club.tesseract.minestom.utils.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandMetadata {
    CommandCategory[] categories(); // Changed to array
    String description() default "";
    boolean enabled() default true;
    int priority() default 0; // Lower numbers load first
}