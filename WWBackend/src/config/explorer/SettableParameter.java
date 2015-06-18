package config.explorer;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// can use in method only.
public @interface SettableParameter
{

}
