package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import com.plexpt.chatgpt.ChatGPT;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "tinder_ai_full_bot"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "7864589028:AAF1Ty-WQofxgSbax06FWNTMcxKpNvXyE7Q"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "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"; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGpt = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currMode = null;
    private ArrayList<String> list = new ArrayList<>();

    private UserInfo me;

    private UserInfo she;

    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();


        if(message.equals("/start")){
            currMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");
            sendTextMessage(text);

            showMainMenu("Главное меню бота", "/start",
                    "Генерация Tinder-профля \uD83D\uDE0E", "/profile ",
                    "Сообщение для знакомства \uD83E\uDD70", "/opener",
                    "Переписка от вашего имени \uD83D\uDE08", "/message",
                    "Переписка со звездами \uD83D\uDD25", "/date",
                    "Задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }

        if (message.equals("/gpt")){

            currMode = DialogMode.GPT;
            String text = loadMessage("gpt");
            sendPhotoMessage("gpt");
            sendTextMessage(text);
            return;
        }

        // command Date
        if(message.equals("/date")){
            currMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Aрлана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райн Гослинг", "date_gosling",
                    "Том харди", "date_hardy");

            return;
        }


        if (currMode == DialogMode.DATE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")){
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор! \nТвоя задача пригласить девушку на свидание ❤\uFE0F за 5 сообщений");
                String prompt = loadPrompt(query);
                chatGpt.setPrompt(prompt);
                return;
            }


            Message msg = sendTextMessage("Подождите, девушка набирает текст ...");
            String answer = chatGpt.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }

        if (currMode == DialogMode.GPT && !isMessageCommand()){
            String prompt = loadPrompt("gpt");

            Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
            String answer = chatGpt.sendMessage(prompt, message);
            updateTextMessage(msg, answer);
            return;
        }

        if (message.equals("/message")){
            currMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите в чат ваше переписку",
                    "Следующее сообшение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;

        }

        if(currMode == DialogMode.MESSAGE && !isMessageCommand()){
            String query = getCallbackQueryButtonKey();
            if(query.startsWith("message_")){
                String prompt = loadPrompt(query);
                String userCharHistory = String.join("\n\n", list);
                Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                String answer = chatGpt.sendMessage(prompt, userCharHistory); //10 sec

                updateTextMessage(msg, answer);
            }
            list.add(message);
            return;
        }
        // command PROFILE
        if (message.equals("/profile")){
            currMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");
            me = new UserInfo();
            questionCount = 1;
            sendTextMessage("Сообщите вам лет?");
            return;
        }

        if (currMode == DialogMode.PROFILE && !isMessageCommand()){

            switch (questionCount){
                case 1:
                    me.age = message;

                    questionCount = 2;
                    sendTextMessage("Кем вы работаете?");

                    return;
                case 2:
                    me.occupation = message;

                    questionCount = 3;
                    sendTextMessage("У вас есть хобби?");
                    return;
                case 3:
                    me.hobby = message;
                    questionCount = 4;
                    sendTextMessage("Что вам НЕ нравится в людях?");
                    return;
                case 4:
                    me.annoys = message;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    me.goals = message;

                    String aboutMySelf = me.toString();
                    String prompt = loadPrompt("profile");

                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT \uD83E\uDDE0 думает...");
                    String answer = chatGpt.sendMessage(prompt, aboutMySelf);
                    updateTextMessage(msg, answer);
                    return;


            }

            return;


        }

        //command OPENER

        if(message.equals("/opener")){
            currMode = DialogMode.OPENER;
            sendPhotoMessage("opener");

            she = new UserInfo();
            questionCount = 1;
            sendTextMessage("Пришли информацию о человеке для знакомства:");
            return;
        }

        if(currMode == DialogMode.OPENER && !isMessageCommand()){

            switch (questionCount){
                case 1:
                    she.name = message;

                    questionCount = 2;
                    sendTextMessage("Сколько ей лет?");
                    return;
                case 2:
                    she.name = message;

                    questionCount = 3;
                    sendTextMessage("Eсть ли у нее хобби и какие?");
                    return;
                case 3:
                    she.hobby = message;

                    questionCount = 4;
                    sendTextMessage("Кем она работает?");
                    return;
                case 4:
                    she.occupation = message;

                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    she.goals = message;

                    String aboutFriend = message;
                    String prompt = loadPrompt("opener");

                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT \uD83E\uDDE0 думает...");
                    String answer = chatGpt.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg, answer);
                    return;
            }

            return;
        }


    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
