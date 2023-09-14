
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class WordleWrangler {

    private int wordLength;
    private LetterSlot[] letterSlots;
    private List<Character> invalidCharacters = new ArrayList<>(21);
    private List<String> invalidWords = new ArrayList<>(6);
    private List<String> words;
    private int attemptCount;
    private String[] ordinals = {" ", "first", "second", "third", "fourth", "fifth", "sixth"};
    private String dictionaryFilename = "eng_words_alphabetic.txt";

    public WordleWrangler() {
        this(5);
    }

    public WordleWrangler(int length) {
        wordLength = length;
        
        // Initiate the LetterSlot objects
        letterSlots = new LetterSlot[wordLength];
        for (int i = 0; i < wordLength; i++) {
            letterSlots[i] = new LetterSlot();
        }

        // Load up the words
        try {
            words = getFileData(dictionaryFilename);
        } catch (IOException e) {
            throw new Error("Error loading up dictionary file " + dictionaryFilename + 
                ". Make sure this file is in the same directory as the program.");
        }

        attemptCount = 1;
    }

    public static void main(String[] args) {

        WordleWrangler ww = new WordleWrangler();
        ConsoleColors cc = new ConsoleColors();

        Scanner scanner = new Scanner(System.in);

        System.out.println("Thanks for trying the Wordle Wrangler!\n");
        System.out.println("Just type the word you tried, then a space, and then the result.");
        System.out.println("Type '?' in place of a " + cc.WHITE_UNDERLINED + "gray letter" + cc.RESET + 
            ", type in lowercase for a " + cc.YELLOW + "yellow letter" + cc.RESET + 
            ", and type in UPPERCASE for a " + cc.GREEN + "green letter" + cc.RESET);
        System.out.println("For example: if you entered 'ocean' into Wordle and got o" + cc.YELLOW + "c" + cc.RESET + cc.GREEN + "ea" + cc.RESET + 
            "n, then you would type: ocean ?cEA?\n");
        System.out.println("Good luck!\n");

        while (ww.getAttemptCount() <= 6) {
            System.out.print(cc.RED_BOLD + "============ Attempt number " + ww.getAttemptCount() + " ============\n\n" + cc.RESET);
            System.out.print("Type your " + ww.getAttemptOrdinal() + " word and result here: ");
            String tried = scanner.next();
            String result = scanner.next();

            List<String> resultWords = ww.attempt(tried, result);
            int numResults = resultWords.size();
            LetterSlot[] slot = ww.getLetterSlots();

            System.out.println(" ");
            System.out.print("You tried '" + tried + "' and got ");

            for (int i = 0; i < ww.wordLength; i++) {
                System.out.print(cc.colorSlot(slot[i]));
                if (i == ww.wordLength - 1) System.out.print("\n\n");
            }

            System.out.println(cc.WHITE_BOLD_BRIGHT + "Try one of these " + numResults + " words next:\n" + cc.RESET);
            
            Iterator<String> resultsIterator = resultWords.iterator();
            for (int i = 0; i < numResults; i++) {
                System.out.print(resultsIterator.next());
                if (i != numResults - 1) System.out.print(", ");
                if (i % 10 == 9) System.out.print("\n");
            }

            System.out.print("\n\n");
        }

        scanner.close();

    }

    public List<String> attempt(String tried, String result) {

        if (tried.length() != wordLength)
            throw new InvalidParameterException(
                "The input '" + tried + "'' does not match the size of the word length " + wordLength + ". Try typing it again."
            );

        if (result.length() != wordLength)
            throw new InvalidParameterException(
                "The input '" + result + "'' does not match the size of the word length " + wordLength + ". Try typing it again."
            );

        String triedLower = tried.toLowerCase();

        // Add the attempt to the list of invalid words, so that it won't be suggested to the user again
        invalidWords.add(triedLower);

        for (int i = 0; i < wordLength; i++) {

            char cR = result.charAt(i);
            char cT = triedLower.charAt(i);
            letterSlots[i].clear();

            if (cR == '?') {  // If the letter was gray
                invalidCharacters.add(tried.charAt(i));
                letterSlots[i].setLetter(cT);
            } else if (Character.isLowerCase(cR)) {  // If the letter was yellow
                checkCharForEquality(cR, cT);
                letterSlots[i].setLetter(cR);
                letterSlots[i].setYellow();
            } else if (Character.isUpperCase(cR)) {  // If the letter was green
                checkCharForEquality(cR, cT);
                letterSlots[i].setLetter(cR);
                letterSlots[i].setGreen();
            } else {  // Illegal character passed
                throw new Error("Only pass in lowercase characters, uppercase characters, and the question mark (?).");
            }
        }

        return filterWords(tried, result);
    }

    private void checkCharForEquality(char a, char b) {
        if (Character.toLowerCase(a) != Character.toLowerCase(b)) {
            throw new Error("The letters in the parameters do not correspond to one another. Try typing it again.");
        }
    }

    public List<String> filterWords(String tried, String result) {
        List<String> output = new ArrayList<>();

        Iterator<String> wordsIterable = words.iterator();

        wordLoop:
        while (wordsIterable.hasNext()) {

            String word = wordsIterable.next();
            String wordCopy = word;

            // Check green words conditions
            // Since this is the most strict condition, it will eliminate most of the words first
            for (int i = 0; i < wordLength; i++) {

                LetterSlot slot = letterSlots[i];

                if (slot.isGreen()) {
                    if (word.charAt(i) != slot.getLetter()) continue wordLoop;

                    // Remove the letter from the word, so that we don't catch it again later
                    wordCopy = wordCopy.replaceFirst(slot.getString(), "_");
                } 
            }

            // Check yellow words conditions
            for (int i = 0; i < wordLength; i++) {

                LetterSlot slot = letterSlots[i];

                if (slot.isYellow()) {
                    if (!wordCopy.contains(slot.getString())) continue wordLoop;
                    if (word.charAt(i) == slot.getLetter()) continue wordLoop; // If the letter is yellow, it can't be at its current position

                    // Remove the letter from the word, so that we don't catch it again later.
                    // This is important in case there are two yellow letters that are the same.
                    wordCopy = wordCopy.replaceFirst(slot.getString(), "_");
                }

                // Also check that the word doesn't contain a letter we've already tried before.
                if (invalidCharacters.contains(word.charAt(i))) continue wordLoop;
            }

            // Finally, add the word to the output list, if it hasn't been tried yet.
            if (!invalidWords.contains(word)) output.add(word);
        }

        incrementAttemptCount();

        return output;
    }

    public List<String> getFileData(String fname) throws IOException {

        List<String> data = Files.lines(Paths.get(fname))
            .filter(x -> x.length() == wordLength)
            .collect(Collectors.toList());

        return data;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttemptCount() {
        attemptCount++;
    }

    public String getAttemptOrdinal() {
        return ordinals[attemptCount];
    }

    public LetterSlot[] getLetterSlots() {
        return letterSlots;
    }
}