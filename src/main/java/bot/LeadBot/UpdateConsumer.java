package bot.LeadBot;

import bot.db.User;
import bot.func.LeadService;
import bot.func.SessionService;
import bot.func.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//Если честно, так как это тестовый шаблон, и технологии тут будут тестовые, так что может и не
//работать, ну короче тут будет основная работа бота, и тут будет впервые применена именно вариация с
//многопоточностью

@Component
public class UpdateConsumer implements LongPollingUpdateConsumer {
    Map<Long, String> checkPassword = new ConcurrentHashMap<>();
    private final TelegramClient telegramClient;
    private final LeadService leadService;
    private final UserService userService;
    private final SessionService sessionService;
    PasswordEncoder passwordEncoder;
    public UpdateConsumer(@Value("${bot.token}") String botToken, LeadService leadService, UserService userService, SessionService sessionService, PasswordEncoder passwordEncoder) {
        this.telegramClient = new org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient(botToken);
        this.leadService = leadService;
        this.userService = userService;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
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
                        } else if(currentSession.equals("Ввод пароляР")) {
                            sendMessage(chatId, "Повторите ввод пароля");
                            setCheckPassword(chatId, passwordEncoder.encode(text));
                            sessionService.setSession("Повтор пароля", currentUser);
                        } else if(currentSession.equals("Повтор пароля")) {
                            if (passwordEncoder.matches(text, getCheckPassword(chatId))) {
                                userService.setPassword(getCheckPassword(chatId), currentUser);
                                removeCheckPassword(chatId);
                                userService.setWrotePassword(currentUser, true);
                                sessionService.setSession("Регистрация", currentUser);
                                sendRegButton(chatId, "Для регистрации введите следующие данные", currentUser);
                            } else {
                                sendMessage(chatId, "Пароли не совпадают!");
                            }
                        } else if (currentSession.equals("Ввод логинаР")) {
                            if (userService.checkLogin(text)) {
                                sendMessage(chatId, "Такой пользователь уже существует!!!");
                            } else {
                                sessionService.setSession("Регистрация", currentUser);
                                userService.setWroteUsername(currentUser, true);
                                userService.setUsername(text, currentUser);
                                sendRegButton(chatId, "Для регистрации введите следующие данные:", currentUser);
                            }
                        } else {
                            sendMessage(chatId, "Вы не авторизованы! Введите /start для начала работы с ботом");

                        }
                    }
                }

            } else if(update.hasCallbackQuery()) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                User currentUser = userService.findUserById(chatId);
                String currentSession = currentUser.getSession().getName();
                String data = update.getCallbackQuery().getData();
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
                if (!currentUser.getIsRegistered()) {
                    if (data.equals("registration") && currentSession.equals("Ничего")) {
                        sendRegButton(chatId, messageId, "Для регистрации введите следующие данные", currentUser);
                        sessionService.setSession("Регистрация",  currentUser);
                    } else if (data.equals("passwordR")) {
                        editMessage(chatId, messageId, "Придумайте пароль:");
                        sessionService.setSession("Ввод пароляР", currentUser);
                    } else if(data.equals("loginR")) {
                        editMessage(chatId, messageId, "Придумайте логин");
                        sessionService.setSession("Ввод логинаР", currentUser);
                    } else if(data.equals("completeR")) {
                        userService.setIsRegistered(currentUser, true);
                        sessionService.setSession("Ничего", currentUser);
                        userService.setWroteUsername(currentUser, false);
                        userService.setWrotePassword(currentUser, false);
                        editMessage(chatId, messageId, "Вы успешно создали аккаунт!");
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке обновления: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
                createBtn("Регистрация", "registration"),
                createBtn("Вход", "login")
        ));

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);

    }

    public void sendRegButton(Long chatId, Integer messageId, String answer, User currentUser) throws TelegramApiException {
        boolean WroteP = currentUser.getWrotePassword();
        boolean WroteU = currentUser.getWroteUsername();
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn((WroteU) ? "Логин [✅]" : "Логин [❌]", "loginR"),
                createBtn((WroteP) ? "Пароль [✅]" : "Пароль [❌]", "passwordR")
        ));
        if (WroteU && WroteP) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Зарегистрироваться", "completeR")
            ));
        }
        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }
    public void sendRegButton(Long chatId, String answer, User currentUser) throws TelegramApiException {
        boolean WroteP = currentUser.getWrotePassword();
        boolean WroteU = currentUser.getWroteUsername();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn((WroteP) ? "Пароль [✅]" : "Пароль [❌]", "passwordR"),
                createBtn((WroteU) ? "Логин [✅]" : "Логин [❌]", "loginR")
        ));
        if (WroteU && WroteP) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Зарегистрироваться", "completeR")
            ));
        }
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
    }
    public void setCheckPassword(Long chatId, String password) {
        checkPassword.put(chatId, password);
    }
    public String getCheckPassword(Long chatId) {
        return checkPassword.get(chatId);
    }
    public void removeCheckPassword(Long chatId) {
        checkPassword.remove(chatId);
    }
}
