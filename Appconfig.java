package app.javamail.config;

public record AppConfig(
        MailConfig mail
) {

    public static AppConfig defaults() {
        return new AppConfig(
                new MailConfig(
                        "localhost",
                        587,
                        true,
                        "localhost",
                        143,
                        true,
                        ""
                )
        );
    }
}
