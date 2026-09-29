package bot.LeadBot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

//Если честно, так как это тестовый шаблон, и технологии тут будут тестовые, так что может и не
//работать, ну короче тут будет основная работа бота, и тут будет впервые применена именно вариация с
//многопоточностью

@Component
public class UpdateConsumer implements LongPollingUpdateConsumer {
    private final TelegramClient telegramClient;

    //токен бота нужно вписать в application.properties
    public UpdateConsumer(@Value("${bot.token}") String botToken) {
        this.telegramClient = new org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient(botToken);
        // this.yourService = yourService;
    }


    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::processUpdateAsync);
    }
    @Async
    public void processUpdateAsync(Update update) {
        Long  chatId = update.getMessage().getChatId();
        try {
            if(update.hasMessage() && update.getMessage().hasText()) {
                sendMessage(chatId, update.getMessage().getText());
            }/* else if () {} и так далее, короче тут основная обработка входящих обновлений*/
        } catch (Exception e) {
            System.err.println("Ошибка при обработке обновления: " + e.getMessage());
            e.printStackTrace();
        }
    }
    //Дальше идут функции, впишу только основные

    //Отсылание ответного сообщения
    public void sendMessage(Long chatId, String answer) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    //Изменение сообщения (только при нажатии кнопки!)
    public void editMessage(Long chatId, Integer messageId, String newText) {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(newText)
                .build();
        try {
            telegramClient.execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //Кнопка. Да, это не сообщение с кнопкой, господи, как сделать сообщение с ней тоже покажу
    InlineKeyboardButton createBtn(String name, String data) {
        return InlineKeyboardButton.builder()
                .text(name)
                .callbackData(data)
                .build();
    }

    //Вот сообщение с кнопкой, ну и дальше логика простая, накрайняк можно загуглить
    public void sendPrimerButton(Long chatId, String answer) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                createBtn("Пример", "primer"),
                createBtn("Пример1", "primer1")
        ));

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
    }
}
