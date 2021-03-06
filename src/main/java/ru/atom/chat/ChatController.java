package ru.atom.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Controller
@RequestMapping("chat")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final String HISTORY_FILE_NAME = "messages_history.txt";

    private Queue<String> messages;
    private Map<String, String> usersOnline;

    public ChatController() {
        messages = readFromFileToHistory();
        usersOnline = new ConcurrentHashMap<>();
    }

    /**
     * curl -X POST -i localhost:8080/chat/login -d "name=I_AM_STUPID"
     */
    @CrossOrigin(origins = "http://34.210.203.44:8080")
    @RequestMapping(
            path = "login",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> login(@RequestParam("name") String name) {
        if (name.length() < 1) {
            return ResponseEntity.badRequest().body("Too short name, sorry :(");
        }
        if (name.length() > 20) {
            return ResponseEntity.badRequest().body("Too long name, sorry :(");
        }
        if (usersOnline.containsKey(name)) {
            return ResponseEntity.badRequest().body("Already logged in:(");
        }
        usersOnline.put(name, name);

        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM',' HH:mm:ss");

        String message = "[" + dateFormat.format(now) + "] [" + name + "] logged in";
        messages.add(message);
        writeToFile(message);
        return ResponseEntity.ok().build();
    }

    /**
     * curl -i localhost:8080/chat/chat
     */
    @CrossOrigin(origins = "http://34.210.203.44:8080")
    @RequestMapping(
            path = "chat",
            method = RequestMethod.GET,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> chat() {
        return new ResponseEntity<>(messages.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n")),
                HttpStatus.OK);
    }

    /**
     * curl -i localhost:8080/chat/online
     */
    @CrossOrigin(origins = "http://34.210.203.44:8080")
    @RequestMapping(
            path = "online",
            method = RequestMethod.GET,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity online() {
        Collection<String> users = usersOnline.values();
        StringBuilder answer = new StringBuilder();
        for (String user: users) {
            answer.append(user + "\n");
        }

        return new ResponseEntity<>(answer, HttpStatus.OK);
    }

    /**
     * curl -X POST -i localhost:8080/chat/logout -d "name=I_AM_STUPID"
     */
    @CrossOrigin(origins = "http://34.210.203.44:8080")
    @RequestMapping(
            path = "logout",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity logout(@RequestParam("name") String name) {
        if (!usersOnline.containsKey(name)) {
            return ResponseEntity.badRequest().body("There is no such user logged in");
        }
        usersOnline.remove(name);

        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM',' HH:mm:ss");

        String message = "[" + dateFormat.format(now) + "] [" + name + "] logged out";
        messages.add(message);
        writeToFile(message);
        return ResponseEntity.ok().build();
    }


    /**
     * curl -X POST -i localhost:8080/chat/say -d "name=I_AM_STUPID&msg=Hello everyone in this chat"
     */
    @CrossOrigin(origins = "http://34.210.203.44:8080")
    @RequestMapping(
            path = "say",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity say(@RequestParam("name") String name, @RequestParam("msg") String msg) {

        if (!usersOnline.containsKey(name)) {
            return ResponseEntity.badRequest().body("Login first please =)");
        }

        if (msg.length() == 0) {
            return ResponseEntity.badRequest().body("Enter yor message please =)");
        }

        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM',' HH:mm:ss");

        String message = "[" + dateFormat.format(now) + "] [" + name + "] " + msg;
        messages.add(message);
        writeToFile(message);
        return ResponseEntity.ok().build();
    }

    private void writeToFile(String msg) {
        try (FileWriter fw = new FileWriter(HISTORY_FILE_NAME, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)
            ) {
            out.println(msg);
        } catch (IOException e) {
            System.out.println("Error while writing message to file");
        }
    }

    private ConcurrentLinkedQueue readFromFileToHistory() {
        ConcurrentLinkedQueue<String> result = new ConcurrentLinkedQueue<>();

        try (FileReader fr = new FileReader(HISTORY_FILE_NAME);
            BufferedReader br = new BufferedReader(fr)
            ) {
            String line = br.readLine();
            while (line != null && !line.equals("")) {
                result.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println("Error while loading messages history");
        }

        return result;
    }
}
