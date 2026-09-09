package app;

import app.config.ApplicationConfig;

public class Main
{
    static void main()
    {
        IO.println(String.format("Hello and welcome!"));
        ApplicationConfig.start(7070);
    }
}
