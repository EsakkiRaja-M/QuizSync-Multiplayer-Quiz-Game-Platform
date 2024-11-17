import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.sql.*;
public class QuizClient {
    private static final String SERVER_ADDRESS = "192.168.0.1"; // Update this as per your server IP
    private static final int SERVER_PORT = 12346;
    private JFrame frame;
    private JTextArea questionArea;
    private JLabel imageLabel;
    private JButton submitButton;
    private JButton clearSelectionButton;
    private JRadioButton[] optionButtons;
    private JLabel timerLabel;
    private JProgressBar progressBar;
    private PrintWriter out;
    private BufferedReader in;
    private String clientName;
    private String clientID;
    private int currentTime;
    private Timer questionTimer;

    public static void main(String[] args) {
        new QuizClient().startClient();
    }
    private void startClient() {
        clientName = JOptionPane.showInputDialog("Enter your name:");
        clientID = JOptionPane.showInputDialog("Enter your ID:");
        if (clientName == null || clientID == null || clientName.isEmpty() || clientID.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Name and ID are required to start the quiz.");
            return;
        } else {
            clientName = clientName.toLowerCase();
            clientID = clientID.toLowerCase();
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/quiz", "root", "ur_password");
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("select * from game where name='" + clientName + "' and id='" + clientID + "'");
                if (rs.next()) {
                    setupGUI();
                    connectToServer();
                } else {
                    JOptionPane.showMessageDialog(null, "Name and Id are Incorrect!");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e);
            }
        }
    }
    private void setupGUI() {
        frame = new JFrame("Quiz Game - Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Welcome to the Quiz Game!", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setOpaque(true);
        titleLabel.setBackground(Color.CYAN);
        frame.add(titleLabel, BorderLayout.NORTH);
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        questionArea = new JTextArea(3, 40);
        questionArea.setFont(new Font("Arial", Font.PLAIN, 16));
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);
        questionArea.setBackground(Color.LIGHT_GRAY);
        mainContentPanel.add(new JScrollPane(questionArea), BorderLayout.NORTH);
        JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(250, 150));
        imagePanel.add(imageLabel);
        mainContentPanel.add(imagePanel, BorderLayout.CENTER);
        frame.add(mainContentPanel, BorderLayout.CENTER);

        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        ButtonGroup optionsGroup = new ButtonGroup();
        optionButtons = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 14));
            optionsGroup.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }
        mainContentPanel.add(optionsPanel, BorderLayout.SOUTH);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        timerLabel = new JLabel("Time Left: 30s", JLabel.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        timerLabel.setForeground(Color.RED);
        bottomPanel.add(timerLabel, BorderLayout.WEST);
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        submitButton = new JButton("Submit Answer");
        submitButton.addActionListener(e -> submitAnswer());
        bottomPanel.add(submitButton, BorderLayout.SOUTH);
        frame.add(bottomPanel, BorderLayout.PAGE_END);

        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(clientName);
            out.println(clientID);
            new Thread(this::listenForQuestions).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForQuestions() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("QUESTION:")) {
                    questionArea.setText(line.substring(9));
                    clearSelection(); // Reset radio buttons for new question
                } else if (line.startsWith("OPTIONS:")) {
                    String[] options = line.substring(8).split(",");
                    for (int i = 0; i < optionButtons.length; i++) {
                        optionButtons[i].setText(options[i]);
                    }
                } else if (line.startsWith("IMAGE:")) {
                    String imagePath = line.substring(6);
                    ImageIcon icon = new ImageIcon(imagePath);
                    imageLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(200, 150, Image.SCALE_SMOOTH)));
                } else if (line.startsWith("TIMER:")) {
                    currentTime = Integer.parseInt(line.substring(6));
                    startTimer();
                } else if (line.startsWith("QUIZ_END:")) {
                    String score = line.substring(9);
                    JOptionPane.showMessageDialog(frame, "Quiz Finished! Your score: " + score);
                    frame.dispose();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startTimer() {
        timerLabel.setText("Time Left: " + currentTime + "s");
        if (questionTimer != null) {
            questionTimer.stop();
        }
        questionTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentTime--;
                timerLabel.setText("Time Left: " + currentTime + "s");
                progressBar.setValue((30 - currentTime) * 4);

                if (currentTime <= 0) {
                    questionTimer.stop();
                    submitAnswer(); 
                     // Automatically submit if time runs out
                }
            }
        });
        questionTimer.start();
      
    }
    private void submitAnswer() {
        String selectedOption = null;
        for (JRadioButton button : optionButtons) {
            if (button.isSelected()) {
                selectedOption = button.getText();
                break;
            }
        }
        if (selectedOption != null) {
            out.println(selectedOption);
        } else if (currentTime <= 0) {
            out.println("No answer");  // Handles no answer on timer expiry
        } else {
            JOptionPane.showMessageDialog(frame, "Please select an option before submitting!");
        }
        clearSelection();
    }
    private void clearSelection() {
        // Clear all previous selections for each radio button
        for (JRadioButton button : optionButtons) {
            button.setSelected(false);
        }
    }
} 
