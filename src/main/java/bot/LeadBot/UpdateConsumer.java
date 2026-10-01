package bot.LeadBot;

import bot.db.User;
import bot.func.LeadService;
import bot.func.SessionService;
import bot.func.UserService;
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
    private final LeadService leadService;
    private final UserService userService;
    private final SessionService sessionService;
    //токен бота нужно вписать в application.properties
    public UpdateConsumer(@Value("${bot.token}") String botToken, LeadService leadService, UserService userService, SessionService sessionService) {
        this.telegramClient = new org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient(botToken);
        this.leadService = leadService;
        this.userService = userService;
        this.sessionService = sessionService;
    }


    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::processUpdateAsync);
    }
    @Async
    public void processUpdateAsync(Update update) {
        try {
            if(update.hasMessage()) {
                Long chatId = update.getMessage().getChatId();
                User currentUser = userService.findUserById(chatId);
                String currentSession = currentUser.getSession().getName();
                String text = update.getMessage().getText();
                if (text.equals("/help")) {
                    sendMessage(chatId, """
                            /start - начало работы бота
                            /restart - перезапуск работы бота
                            (Осторожно, бездумный перезапуск может сломать некоторые процессы, так что
                            перезапускайте бота только в случае полной поломки!!!)""");
                } else if(text.equals("/restart")) {
                    currentUser.setWroteUsername(false);
                    currentUser.setWrotePassword(false);
                    currentUser.setIsRegistered(false);
                    sessionService.setSession("Ничего", currentUser);
                } else {
                    if(!currentUser.getIsRegistered()) {
                        if (text.equals("/start")) {
                            sendRegLogButton(chatId, "Выберите способ авторизации:");
                        } else {
                            sendMessage(chatId, "Вы не авторизованы! Введите /start для начала работы с ботом");
                        }
                    }
                }

            }
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

    InlineKeyboardButton createBtn(String name, String data) {
        return InlineKeyboardButton.builder()
                .text(name)
                .callbackData(data)
                .build();
    }

    public void sendRegLogButton(Long chatId, String answer) throws TelegramApiException {
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
