import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
class QuizQuestion {
    private String question;
    private String[] options;
    private String correctAnswer;
    private String imagePath;
    public QuizQuestion(String question, String[] options, String     correctAnswer, String imagePath) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.imagePath = imagePath;
    }
    public String getQuestion() {
        return question;
    }
    public String[] getOptions() {
        return options;
    }
    public String getCorrectAnswer() {
        return correctAnswer;
    }
    public String getImagePath() {
        return imagePath;
    }
}
public class QuizServer {
    private static final int PORT = 12346;
    private static List<QuizQuestion> questions = new ArrayList<>();
    private static List<ClientHandler> clients = new ArrayList<>();
    private static JFrame frame;
    private static JTextArea clientScores;
    public static void main(String[] args) {
        loadQuestions();
        setupServerGUI();
        startServer();
    }
    private static void loadQuestions() {
        questions.add(new QuizQuestion("What is the main function of the OSI model?", 
        new String[] { "To provide a standard for network communication", "To encrypt data", "To control hardware", "To define IP addressing" }, 
        "To provide a standard for network communication", "images/osi.jpeg"));

        questions.add(new QuizQuestion("Which protocol is used to send emails?", 
        new String[] { "SMTP", "FTP", "HTTP", "TCP" }, 
        "SMTP", "images/email.jpeg"));

        questions.add(new QuizQuestion("What does DNS stand for?", 
        new String[] { "Domain Name System", "Data Network Service", "Direct Network Service", "Dynamic Name Service" }, 
        "Domain Name System", "images/dns.jpg"));

        questions.add(new QuizQuestion("What is the purpose of a router?", 
        new String[] { "To route traffic between networks", "To encrypt data", "To monitor traffic", "To create networks" }, 
        "To route traffic between networks", "images/router.jpeg"));

        questions.add(new QuizQuestion("What layer does IP operate at?", 
        new String[] { "Transport", "Network", "Data Link", "Application" }, 
        "Network", "images/ip.jpeg"));
    }

    private static void setupServerGUI() {
        frame = new JFrame("Multiplayer Quiz Game Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Multiplayer Quiz Game Server", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setOpaque(true);
        titleLabel.setBackground(Color.CYAN);
        frame.add(titleLabel, BorderLayout.NORTH);

        clientScores = new JTextArea();
        clientScores.setFont(new Font("Arial", Font.PLAIN, 14));
        clientScores.setEditable(false);
        clientScores.setBackground(Color.LIGHT_GRAY);
        frame.add(new JScrollPane(clientScores), BorderLayout.CENTER);
        JButton printButton = new JButton("Print Results");
        printButton.addActionListener(e -> {
            try {
                clientScores.print();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        frame.add(printButton, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Quiz Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        private String clientID;
        private int score = 0;
        private int currentQuestionIndex = 0;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        @Override
        public void run() {
            try {
  in=newBufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                clientName = in.readLine();
                clientID = in.readLine();
                System.out.println("Client details received - Name: " + clientName + ", ID: " + clientID);

                clientScores.append("Participant connected: " + clientName + " (ID: " + clientID + ")\n");
                sendQuestion();
                String answer;
                while ((answer = in.readLine()) != null) {
                    System.out.println("Received answer from " + clientName + ": " + answer);

                    checkAnswer(answer);
                    sendQuestion();
                }
            } catch (IOException e) {
                System.err.println("Connection lost with client: " + clientName);
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    System.out.println("Socket closed for client: " + clientName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendQuestion() {
            if (currentQuestionIndex < questions.size()) {
                QuizQuestion question = questions.get(currentQuestionIndex);
                out.println("QUESTION:" + question.getQuestion());
                out.println("OPTIONS:" + String.join(",", question.getOptions()));
                out.println("IMAGE:" + question.getImagePath());
                out.println("TIMER:30");
                System.out.println("Sent question to " + clientName + ": " + question.getQuestion());
                currentQuestionIndex++;
            } else {
                out.println("QUIZ_END:" + score + " out of " + questions.size());
                System.out.println("Quiz ended for " + clientName + ". Score: " + score);
                clientScores.append("Participant Finished & Left: " + clientName + " (ID: " + clientID + ") Score: " + score + " out of " + questions.size() + "\n");
            }
        }

        private void checkAnswer(String answer) {
            if (currentQuestionIndex > 0) {
                QuizQuestion lastQuestion = questions.get(currentQuestionIndex - 1);
                if (answer.equalsIgnoreCase(lastQuestion.getCorrectAnswer())) {
                    score++;
                    System.out.println(clientName + " answered correctly. Current score: " + score);
                } else {
                    System.out.println(clientName + " answered incorrectly.");
                }

            }
            clientScores.append("Name: " + clientName + "  Id: " + clientID + "  Scored: " + score + " out of " + questions.size() + "\n");
        }
    }
}
