package bot.LeadBot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

@Component
public class LeadBot implements SpringLongPollingBot {
    private final UpdateConsumer updateConsumer;
    @Value("${bot.token}")
    private String token;

    public LeadBot(UpdateConsumer updateConsumer) {
        this.updateConsumer = updateConsumer;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumer;
    }
}