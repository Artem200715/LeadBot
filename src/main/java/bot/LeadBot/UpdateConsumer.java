package bot.LeadBot;

import bot.db.User;
import bot.db.Worker;
import bot.func.LeadService;
import bot.func.SessionService;
import bot.func.StatusService;
import bot.func.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.metadata.HsqlTableMetaDataProvider;
import org.springframework.scheduling.annotation.Async;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UpdateConsumer implements LongPollingUpdateConsumer {
    Map<Long, String> checkPassword = new ConcurrentHashMap<>();
    Map<Long, String> keepLoginInMind = new ConcurrentHashMap<>();
    Map<Long, Boolean> isWorker = new ConcurrentHashMap<>();
    Map<Long, String> keepName = new ConcurrentHashMap<>();
    Map<Long, String> keepPhone = new ConcurrentHashMap<>();
    private final TelegramClient telegramClient;
    private final LeadService leadService;
    private final UserService userService;
    private final SessionService sessionService;
    private final StatusService statusService;
    PasswordEncoder passwordEncoder;

    public UpdateConsumer(@Value("${bot.token}") String botToken, LeadService leadService, UserService userService, SessionService sessionService, PasswordEncoder passwordEncoder, StatusService statusService) {
        this.telegramClient = new org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient(botToken);
        this.leadService = leadService;
        this.userService = userService;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
        this.statusService = statusService;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::processUpdateAsync);
    }

    public void processUpdateAsync(Update update) {
        try {
            Long chatId = null;
            if (update.hasMessage()) {
                chatId = update.getMessage().getChatId();
            } else if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
            }
            if (chatId == null) return;

            boolean workerFlag = isWorker.computeIfAbsent(chatId,
                    id -> {
                        Worker w = userService.findWorkerById(id);
                        return w != null && Boolean.TRUE.equals(w.getIsRegistered());
                    });

            Worker maybeWorker = workerFlag ? userService.findWorkerById(chatId) : null;

            if (maybeWorker != null && Boolean.TRUE.equals(maybeWorker.getIsRegistered())) {
                String currentSession = maybeWorker.getSession().getName();
                if (update.hasMessage()) {
                    String text = update.getMessage().getText();
                    if (text.equals("/help")) {
                        sendMessage(chatId, """
                                /start - начало работы бота
                                /restart - перезапуск работы бота
                                (Осторожно, бездумный перезапуск может сломать некоторые процессы, так что
                                перезапускайте бота только в случае полной поломки!!!)""");
                    } else if (text.equals("/restart")) {
                        maybeWorker.setWroteUsername(false);
                        maybeWorker.setWrotePassword(false);
                        keepPhone.remove(chatId);
                        keepLoginInMind.remove(chatId);
                        keepName.remove(chatId);
                        checkPassword.remove(chatId);
                        sessionService.setSession("Ничего", maybeWorker);
                        sendRegLogButton(chatId, "Выберите способ авторизации:");
                    }
                } else if (update.hasCallbackQuery()) {
                    String data = update.getCallbackQuery().getData();
                    Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
                    if (data.equals("logout")) {
                        sessionService.setSession("Ничего", maybeWorker);
                        userService.setIsRegistered(maybeWorker, false);
                        userService.deleteChatId(maybeWorker);
                        isWorker.put(chatId, false);
                        editMessage(chatId, messageId, "Вы успешно вышли из аккаунта!");
                        sendRegLogButton(chatId, "Выберите способ авторизации:");
                    }
                }
            } else {
                User currentUser = userService.findUserById(chatId);
                if (currentUser == null) {
                    isWorker.put(chatId, false);
                    return;
                }
                String currentSession = currentUser.getSession().getName();
                if (update.hasMessage()) {
                    String text = update.getMessage().getText();
                    if (text.equals("/help")) {
                        sendMessage(chatId, """
                        /start - начало работы бота
                        /restart - перезапуск работы бота
                        (Осторожно, бездумный перезапуск может сломать некоторые процессы, так что
                        перезапускайте бота только в случае полной поломки!!!)""");
                    } else if (text.equals("/restart")) {
                        currentUser.setWroteUsername(false);
                        currentUser.setWrotePassword(false);
                        keepPhone.remove(chatId);
                        keepLoginInMind.remove(chatId);
                        keepName.remove(chatId);
                        checkPassword.remove(chatId);
                        sessionService.setSession("Ничего", currentUser);
                        sendRegLogButton(chatId, "Выберите способ авторизации:");
                    } else {
                        if (!currentUser.getIsRegistered()) {
                            authorizationM(chatId, currentUser, currentSession, text);
                        } else {
                            if (currentSession.equals("Название услуги")) {
                                keepName.put(chatId, text);
                                sendAgainButton(chatId, "Готовы продолжить?");
                            } else if (currentSession.equals("Телефон")) {
                                keepPhone.put(chatId, text);
                                sendAgainButton(chatId, "Готовы продолжить?");
                            }
                        }
                    }
                } else if (update.hasCallbackQuery()) {
                    String data = update.getCallbackQuery().getData();
                    Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

                    if (data.equals("continue") && currentSession.equals("Телефон")) {
                        sessionService.setSession("Ничего", currentUser);

                        Worker freeWorker = leadService.assignAndCreateLead(
                                keepName.get(chatId),
                                keepPhone.get(chatId),
                                LocalDateTime.now(),
                                chatId,
                                statusService.getStatusFromTable("NEW")
                        );

                        if (freeWorker == null) {
                            editMessage(chatId, messageId, "На данный момент свободных операторов нет, ваша заявка будет рассмотрена первым освободившемся оператором");
                            sendUserMenuButton(chatId, "Приветствую, " + update.getCallbackQuery().getMessage().getChat().getUserName() + "!");
                        } else {
                            editMessage(chatId, messageId, "Ваша заявка была отправлена оператору");
                            sendMessage(freeWorker.getChatId(), "Вам пришла заявка от пользователя " + userService.findUserById(chatId).getUsername() + "\n" + "Название услуги: " + keepName.get(chatId) + "\n" + "Номер телефона: " + keepPhone.get(chatId) + "\n" + "Выберите действие с этой заявкой:");
                            sendUserMenuButton(chatId, "Приветствую, " + update.getCallbackQuery().getMessage().getChat().getUserName() + "!");
                        }
                        keepName.remove(chatId);
                        keepPhone.remove(chatId);
                        return;
                    }
                    if (!currentUser.getIsRegistered()) {
                        authorizationCQ(chatId, currentUser, currentSession, data, messageId);
                    } else {
                        if (data.equals("logout")) {
                            sessionService.setSession("Ничего", currentUser);
                            userService.setIsRegistered(currentUser, false);
                            userService.deleteChatId(currentUser);
                            isWorker.put(chatId, false);
                            editMessage(chatId, messageId, "Вы успешно вышли из аккаунта!");
                            sendRegLogButton(chatId, "Выберите способ авторизации:");
                        } else if (data.equals("create")) {
                            sessionService.setSession("Название услуги", currentUser);
                            editMessage(chatId, messageId, "Впишите название необходимой вам услуги:");
                        } else if (data.equals("again")) {
                            editMessage(chatId, messageId, "Повторите ввод:");
                        } else if (data.equals("continue")) {
                            if (currentSession.equals("Название услуги")) {
                                sessionService.setSession("Телефон", currentUser);
                                editMessage(chatId, messageId, "Введите свой номер телефона:");
                            }
                        } else if (data.equals("exit")) {
                            keepName.remove(chatId);
                            keepPhone.remove(chatId);
                            sendUserMenuButton(chatId, messageId, "Приветствую, " + update.getCallbackQuery().getMessage().getChat().getUserName() + "!");
                        }
                    }
                }
            }
        } catch (TelegramApiException ex) {
            throw new RuntimeException(ex);
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

    public void sendRegLogButton(Long chatId, Integer messageId, String answer) throws TelegramApiException {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                createBtn("Регистрация", "registration"),
                createBtn("Вход", "login")
        ));

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);

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
        } else {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Назад", "backRL")
            ));
        }

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }
//    public void sendLeadCreateButton(Long chatId, Integer messageId, String answer) throws TelegramApiException {
//        EditMessageText editMessage = EditMessageText.builder()
//                .chatId(chatId)
//                .messageId(messageId)
//                .text(answer)
//                .build();
//        List<InlineKeyboardRow> keyboard = new ArrayList<>();
//        keyboard.add(new InlineKeyboardRow(
//                createBtn("")
//        ));
//        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
//        telegramClient.execute(editMessage);
//    }
    public void sendRegButton(Long chatId, Integer messageId, String answer, Worker currentUser) throws TelegramApiException {
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
        } else {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Назад", "backRL")
            ));
        }

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }

    public void sendAgainButton(Long chatId, String answer) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn("Продолжить➡", "continue"),
                createBtn("Повторить🔄", "again")
        ));
        keyboard.add(new InlineKeyboardRow(
                createBtn("Вернуться", "exit")
        ));
        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
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
                createBtn((WroteU) ? "Логин [✅]" : "Логин [❌]", "loginR"),
                createBtn((WroteP) ? "Пароль [✅]" : "Пароль [❌]", "passwordR")
        ));
        if (WroteU && WroteP) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Зарегистрироваться", "completeR")
            ));
        } else {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Назад", "backRL")
            ));
        }

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
    }

    public void sendRegButton(Long chatId, String answer, Worker currentUser) throws TelegramApiException {
        boolean WroteP = currentUser.getWrotePassword();
        boolean WroteU = currentUser.getWroteUsername();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
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
        } else {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Назад", "backRL")
            ));
        }

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
    }

    public void sendLogButton(Long chatId, String answer, User currentUser) throws TelegramApiException {
        boolean WroteP = currentUser.getWrotePassword();
        boolean WroteU = currentUser.getWroteUsername();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn((WroteU) ? "Логин [✅]" : "Логин [❌]", "loginL"),
                createBtn((WroteP) ? "Пароль [✅]" : "Пароль [❌]", "passwordL")
        ));
        if (WroteU && WroteP) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Войти", "completeL")
            ));
        } else {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Назад", "backRL")
            ));
        }

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
    }

    public void sendLogButton(Long chatId, String answer, Worker currentUser) throws TelegramApiException {
        boolean WroteP = currentUser.getWrotePassword();
        boolean WroteU = currentUser.getWroteUsername();
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn((WroteU) ? "Логин [✅]" : "Логин [❌]", "loginL"),
                createBtn((WroteP) ? "Пароль [✅]" : "Пароль [❌]", "passwordL")
        ));
        if (WroteU && WroteP) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Войти", "completeL")
            ));
        } else {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Назад", "backRL")
            ));
        }

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);
    }

    public void sendLogButton(Long chatId, Integer messageId, String answer, User currentUser) throws TelegramApiException {
        boolean WroteP = currentUser.getWrotePassword();
        boolean WroteU = currentUser.getWroteUsername();
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn((WroteU) ? "Логин [✅]" : "Логин [❌]", "loginL"),
                createBtn((WroteP) ? "Пароль [✅]" : "Пароль [❌]", "passwordL")
        ));
        if (WroteU && WroteP) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Войти", "completeL")
            ));
        } else {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Назад", "backRL")
            ));
        }

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }

    public void sendLogButton(Long chatId, Integer messageId, String answer, Worker currentUser) throws TelegramApiException {
        boolean WroteP = currentUser.getWrotePassword();
        boolean WroteU = currentUser.getWroteUsername();
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(new InlineKeyboardRow(
                createBtn((WroteU) ? "Логин [✅]" : "Логин [❌]", "loginL"),
                createBtn((WroteP) ? "Пароль [✅]" : "Пароль [❌]", "passwordL")
        ));
        if (WroteU && WroteP) {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Войти", "completeL")
            ));
        } else {
            keyboard.add(new InlineKeyboardRow(
                    createBtn("Назад", "backRL")
            ));
        }

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);
    }

    public void sendUserMenuButton(Long chatId, Integer messageId, String answer) throws TelegramApiException {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                createBtn("Создать заявку", "create")
        ));
        keyboard.add(new InlineKeyboardRow(
                createBtn("Выйти из аккаунта", "logout")
        ));

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);

    }
    public void sendUserMenuButton(Long chatId, String answer) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                createBtn("Создать заявку", "create")
        ));
        keyboard.add(new InlineKeyboardRow(
                createBtn("Выйти из аккаунта", "logout")
        ));

        message.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(message);

    }

    public void sendWorkerMenuButton(Long chatId, Integer messageId, String answer) throws TelegramApiException {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(answer)
                .build();

        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(new InlineKeyboardRow(
                createBtn("Выйти из аккаунта", "logout")
        ));

        editMessage.setReplyMarkup(new InlineKeyboardMarkup(keyboard));
        telegramClient.execute(editMessage);

    }

    public void authorizationM(Long chatId, User currentUser, String currentSession, String text) throws TelegramApiException {
        if (text.equals("/start")) {
            sendRegLogButton(chatId, "Выберите способ авторизации:");
            sessionService.setSession("Ничего", currentUser);
        } else if (currentSession.equals("Ввод пароляР")) {
            sendMessage(chatId, "Повторите ввод пароля");
            setCheckPassword(chatId, passwordEncoder.encode(text));
            sessionService.setSession("Повтор пароля", currentUser);
        } else if (currentSession.equals("Повтор пароля")) {
            if (passwordEncoder.matches(text, getCheckPassword(chatId))) {
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
                keepLoginInMind.put(chatId, text);
                sendRegButton(chatId, "Для регистрации введите следующие данные:", currentUser);
            }
        } else if (currentSession.equals("Ввод логинаЛ")) {
            if (text.startsWith("&26_*-")) {
                if (userService.checkLoginWorker(text)) {
                    userService.setWroteUsername(currentUser, true);
                    keepLoginInMind.put(chatId, text);
                    sessionService.setSession("Логин", currentUser);
                    sendLogButton(chatId, "Введите следующие данные для входа в аккаунт:", currentUser);
                } else {
                    sendMessage(chatId, "Такого работника не существует!");
                    sendLogButton(chatId, "Введите следующие данные для входа в аккаунт:", currentUser);
                }
            } else {
                if (userService.checkLogin(text)) {
                    userService.setWroteUsername(currentUser, true);
                    keepLoginInMind.put(chatId, text);
                    sessionService.setSession("Логин", currentUser);
                    sendLogButton(chatId, "Введите следующие данные для входа в аккаунт:", currentUser);
                } else {
                    sendMessage(chatId, "Такого пользователя не существует!");
                    sendLogButton(chatId, "Введите следующие данные для входа в аккаунт:", currentUser);
                }
            }

        } else if (currentSession.equals("Ввод пароляЛ")) {
            String login = keepLoginInMind.get(chatId);
            if (login != null && login.startsWith("&26_*-")) {
                Worker found = userService.findWorkerByLogin(login);
                if (found != null && passwordEncoder.matches(text, found.getPassword())) {
                    userService.setWrotePassword(currentUser, true);
                    sessionService.setSession("Логин", currentUser);
                    sendLogButton(chatId, "Введите следующие данные для входа в аккаунт", currentUser);
                } else {
                    sendMessage(chatId, "Неверный пароль!!!");
                }
            } else {
                User found = userService.findUserByLogin(login);
                if (found != null && passwordEncoder.matches(text, found.getPassword())) {
                    userService.setWrotePassword(currentUser, true);
                    sessionService.setSession("Логин", currentUser);
                    sendLogButton(chatId, "Введите следующие данные для входа в аккаунт", currentUser);
                } else {
                    sendMessage(chatId, "Неверный пароль!!!");
                }
            }

        } else {
            sendMessage(chatId, "Вы не авторизованы! Введите /start для начала авторизации");

        }
    }

    public void authorizationM(Long chatId, Worker currentUser, String currentSession, String text) throws TelegramApiException {
        if (text.equals("/start")) {
            sendRegLogButton(chatId, "Выберите способ авторизации:");
            sessionService.setSession("Ничего", currentUser);
        } else if (currentSession.equals("Ввод пароляР")) {
            sendMessage(chatId, "Повторите ввод пароля");
            setCheckPassword(chatId, passwordEncoder.encode(text));
            sessionService.setSession("Повтор пароля", currentUser);
        } else if (currentSession.equals("Повтор пароля")) {
            if (passwordEncoder.matches(text, getCheckPassword(chatId))) {
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
                keepLoginInMind.put(chatId, text);
                sendRegButton(chatId, "Для регистрации введите следующие данные:", currentUser);
            }
        } else if (currentSession.equals("Ввод логинаЛ")) {
            if (userService.checkLogin(text)) {
                userService.setWroteUsername(currentUser, true);
                keepLoginInMind.put(chatId, text);
                sessionService.setSession("Логин", currentUser);
                sendLogButton(chatId, "Введите следующие данные для входа в аккаунт:", currentUser);
            } else {
                sendMessage(chatId, "Такого пользователя не существует!");
                sendLogButton(chatId, "Введите следующие данные для входа в аккаунт:", currentUser);
            }
        } else if (currentSession.equals("Ввод пароляЛ")) {
            String login = keepLoginInMind.get(chatId);
            Worker found = userService.findWorkerByLogin(login);
            if (found != null && passwordEncoder.matches(text, found.getPassword())) {
                userService.setWrotePassword(currentUser, true);
                sessionService.setSession("Логин", currentUser);
                sendLogButton(chatId, "Введите следующие данные для входа в аккаунт", currentUser);
            } else {
                sendMessage(chatId, "Неверный пароль!!!");
            }
        } else {
            sendMessage(chatId, "Вы не авторизованы! Введите /start для начала авторизации");

        }
    }

    public void authorizationCQ(Long chatId, User currentUser, String currentSession, String data, Integer messageId) throws TelegramApiException {
        if (data.equals("registration") && currentSession.equals("Ничего")) {
            sendRegButton(chatId, messageId, "Для регистрации введите следующие данные", currentUser);
            sessionService.setSession("Регистрация",  currentUser);
        } else if (data.equals("passwordR")) {
            editMessage(chatId, messageId, "Придумайте пароль:");
            sessionService.setSession("Ввод пароляР", currentUser);
        } else if(data.equals("loginR")) {
            editMessage(chatId, messageId, "Придумайте логин:");
            sessionService.setSession("Ввод логинаР", currentUser);
        } else if(data.equals("completeR")) {
            userService.setIsRegistered(currentUser, true);
            sessionService.setSession("Ничего", currentUser);
            userService.setUsername(keepLoginInMind.get(chatId), currentUser);
            userService.setPassword(checkPassword.get(chatId), currentUser);
            removeKeepLoginInMind(chatId);
            removeCheckPassword(chatId);
            userService.setWroteUsername(currentUser, false);
            userService.setWrotePassword(currentUser, false);
            userService.setCreatedAt(currentUser);
            sendUserMenuButton(chatId, messageId, "Вы успешно создали аккаунт!");
        } else if (data.equals("login") && currentSession.equals("Ничего")) {
            sendLogButton(chatId, messageId, "Введите следующие данные для входа в аккаунт:", currentUser);
            sessionService.setSession("Логин",  currentUser);
        } else if (data.equals("passwordL")) {
            if(currentUser.getWroteUsername()) {
                sendMessage(chatId, "Введите пароль:");
                sessionService.setSession("Ввод пароляЛ", currentUser);
            } else {
                sendMessage(chatId, "Сначала введите логин!");
            }
        } else if(data.equals("loginL")) {
            editMessage(chatId, messageId, "Введите логин:");
            sessionService.setSession("Ввод логинаЛ", currentUser);
        } else if(data.equals("completeL")) {
            String login = keepLoginInMind.get(chatId);
            userService.deleteCurrentUser(chatId);
            if (login != null && login.startsWith("&26_*-")) {
                Worker newWorker = userService.findWorkerByLogin(login);
                if (newWorker == null) {
                    sendMessage(chatId, "Воркер не найден!");
                    return;
                }
                userService.setIsRegistered(newWorker, true);
                sessionService.setSession("Ничего", newWorker);
                userService.setChatId(newWorker, chatId);
                removeKeepLoginInMind(chatId);
                isWorker.put(chatId, true);
                sendWorkerMenuButton(chatId, messageId, "Вы успешно вошли в аккаунт!");
            } else {
                User newUser = userService.findUserByLogin(login);
                userService.setIsRegistered(newUser, true);
                sessionService.setSession("Ничего", newUser);
                userService.setWroteUsername(newUser, false);
                userService.setWrotePassword(newUser, false);
                userService.setChatId(newUser, chatId);
                removeKeepLoginInMind(chatId);
                sendUserMenuButton(chatId, messageId, "Вы успешно вошли в аккаунт!");
            }


        } else if(data.equals("backRL")) {
            sessionService.setSession("Ничего", currentUser);
            userService.setWroteUsername(currentUser, false);
            userService.setWrotePassword(currentUser, false);
            sendRegLogButton(chatId, messageId, "Выберите способ авторизации");
        }
    }

    public void authorizationCQ(Long chatId, Worker currentUser, String currentSession, String data, Integer messageId) throws TelegramApiException {
        if (data.equals("registration") && currentSession.equals("Ничего")) {
            sendRegButton(chatId, messageId, "Для регистрации введите следующие данные", currentUser);
            sessionService.setSession("Регистрация",  currentUser);
        } else if (data.equals("passwordR")) {
            editMessage(chatId, messageId, "Придумайте пароль:");
            sessionService.setSession("Ввод пароляР", currentUser);
        } else if(data.equals("loginR")) {
            editMessage(chatId, messageId, "Придумайте логин:");
            sessionService.setSession("Ввод логинаР", currentUser);
        } else if(data.equals("completeR")) {
            userService.setIsRegistered(currentUser, true);
            sessionService.setSession("Ничего", currentUser);
            userService.setUsername(keepLoginInMind.get(chatId), currentUser);
            userService.setPassword(checkPassword.get(chatId), currentUser);
            removeKeepLoginInMind(chatId);
            removeCheckPassword(chatId);
            userService.setWroteUsername(currentUser, false);
            userService.setWrotePassword(currentUser, false);
            userService.setCreatedAt(currentUser);
            isWorker.put(chatId, true);
            sendWorkerMenuButton(chatId, messageId, "Вы успешно создали аккаунт!");
        } else if (data.equals("login") && currentSession.equals("Ничего")) {
            sendLogButton(chatId, messageId, "Введите следующие данные для входа в аккаунт:", currentUser);
            sessionService.setSession("Логин",  currentUser);
        } else if (data.equals("passwordL")) {
            if(currentUser.getWroteUsername()) {
                sendMessage(chatId, "Введите пароль:");
                sessionService.setSession("Ввод пароляЛ", currentUser);
            } else {
                sendMessage(chatId, "Сначала введите логин!");
            }
        } else if(data.equals("loginL")) {
            editMessage(chatId, messageId, "Введите логин:");
            sessionService.setSession("Ввод логинаЛ", currentUser);
        } else if(data.equals("completeL")) {
            String login = keepLoginInMind.get(chatId);
            Worker newWorker = userService.findWorkerByLogin(login);
            if (newWorker == null) {
                sendMessage(chatId, "Воркер не найден!");
                return;
            }
            userService.setIsRegistered(newWorker, true);
            sessionService.setSession("Ничего", newWorker);
            userService.setWroteUsername(newWorker, false);
            userService.setWrotePassword(newWorker, false);
            userService.deleteCurrentWorker(chatId);
            userService.setChatId(newWorker, chatId);
            removeKeepLoginInMind(chatId);
            isWorker.put(chatId, true);
            sendWorkerMenuButton(chatId, messageId, "Вы успешно вошли в аккаунт!");
        } else if(data.equals("backRL")) {
            sessionService.setSession("Ничего", currentUser);
            userService.setWroteUsername(currentUser, false);
            userService.setWrotePassword(currentUser, false);
            sendRegLogButton(chatId, messageId, "Выберите способ авторизации");
        }
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
    public void removeKeepLoginInMind(Long chatId) {
        keepLoginInMind.remove(chatId);
    }
    @Async
    public void sendLeadNotificationsAsync(Long chatId, Integer messageId, Worker freeWorker, String userName, String keepNameText, String keepPhoneText) {
        try {
            if (freeWorker == null) {
                editMessage(chatId, messageId, "На данный момент свободных операторов нет, ваша заявка будет рассмотрена первым освободившемся оператором");
            } else {
                editMessage(chatId, messageId, "Ваша заявка была отправлена оператору");

                String workerMessage = "Вам пришла заявка от пользователя " + userName + "\n" +
                        "Название услуги: " + keepNameText + "\n" +
                        "Номер телефона: " + keepPhoneText + "\n" +
                        "Выберите действие с этой заявкой:";
                sendMessage(freeWorker.getChatId(), workerMessage);
            }
            sendUserMenuButton(chatId, "Приветствую, " + userName + "!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}