
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class WordleWrangler {

    private int wordLength;
    private LetterSlot[] letterSlots;
    private int maxWordsToDisplay = 100;

    // The LetterHolder remembers which letters have been invalidated, and it handles cases
    // for when there are two of the same letters, but only one is valid.
    private LetterHolder letterHolder = new LetterHolder();
    private Set<String> invalidWords = new HashSet<>(12);
    private Set<String> possibleWords = new HashSet<>(maxWordsToDisplay * 2);
    private List<String> words;
    private int attemptCount;
    private String[] ordinals = {" ", "first", "second", "third", "fourth", "fifth", "sixth"};
    private String dictionaryFilename = "eng_words_69k.txt";
    

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

        int length = ww.wordLength;

        Scanner scanner = new Scanner(System.in);

        System.out.println("\nThanks for trying the Wordle Wrangler!\n");
        System.out.println("First, I will ask you to type the word you tried,\nthen I will ask you to type the result.\nYou'll see word suggestions after that!\n");
        System.out.println("For typing in the result:\n  use '?' in place of a " + cc.WHITE_UNDERLINED + "gray letter" + cc.RESET + 
            "\n  type in lowercase for a " + cc.YELLOW_BRIGHT + "yellow letter" + cc.RESET + 
            "\n  type in UPPERCASE for a " + cc.GREEN_BRIGHT + "green letter" + cc.RESET);
        System.out.println("For example: o" + cc.YELLOW_BRIGHT + "c" + cc.RESET + cc.GREEN_BRIGHT + "ea" + cc.RESET + 
            "n, becomes \"?cEA?\".\n");
        System.out.println("Let's try it!\n");

        String tried = "";
        String result = "";

        while (ww.getAttemptCount() <= 6) {
            System.out.print(cc.CYAN_BOLD_BRIGHT + "============ Attempt number " + ww.getAttemptCount() + " ============\n\n" + cc.RESET);

            while (true) {
                System.out.print("First, type the word exactly as you entered it in wordle, in all lowercase: ");
                tried = scanner.next().toLowerCase();

                if (tried.length() != length) {
                    cc.printColorWarning("It looks like the input you typed doesn't match the number of letters required. You need to have " + length + " letters. Try again.\n");
                } else {
                    break;
                }
            }

            while (true) {
                System.out.print("Now, type the result, using a '?' for " + cc.WHITE_UNDERLINED + 
                    "gray letters" + cc.RESET + ", lowercase for " + cc.YELLOW_BRIGHT + "yellow" + cc.RESET + ", and UPPERCASE for " + 
                    cc.GREEN_BRIGHT + "green: " + cc.RESET);
                result = scanner.next();

                if (result.length() != length) {
                    cc.printColorWarning("It looks like the input you typed doesn't match the number of letters required. You need to have " + length + " letters. Try again.\n");
                } else {
                    break;
                }
            }

            // Make sure both inputs are valid. If not, repeat the main loop.
            if (!WordleWrangler.checkForEquality(tried, result)) {
                cc.printColorWarning("It looks like your attempt and the result don't match. " +
                    "They need to have all of the same letters, except that the result contains " +
                    "question marks ('?') in place of the gray letters.\n");
                continue;
            }

            if (!WordleWrangler.checkForIllegalCharacters(tried, result)) {
                cc.printColorWarning("Only pass in lowercase characters, uppercase characters, " +
                    "and the question mark ('?')");
                continue;
            }

            Set<String> resultWords = ww.attempt(tried, result);
            int numResults = resultWords.size();
            LetterSlot[] slot = ww.getLetterSlots();

            System.out.println(" ");
            System.out.print("You tried '" + tried + "' and got ");

            // Print out the colorized version of the results input
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
                if (i >= ww.maxWordsToDisplay - 1) {
                    System.out.println("Trimming output to just the first 100 words...");
                    break;
                }
            }

            System.out.print("\n\n");
        }

        scanner.close();

    }

    public Set<String> attempt(String tried, String result) {

        String triedLower = tried.toLowerCase();

        // Add the attempt to the list of invalid words, so that it won't be suggested to the user again
        invalidWords.add(triedLower);

        for (int i = 0; i < wordLength; i++) {

            char cR = result.charAt(i);
            char cT = triedLower.charAt(i);
            letterSlots[i].clear();

            if (cR == '?') {  // If the letter was gray
                letterSlots[i].setLetter(cT);
                letterHolder.addInvalid(cT);
            } else if (Character.isLowerCase(cR)) {  // If the letter was yellow
                letterSlots[i].setLetter(cR);
                letterSlots[i].setYellow();
                letterHolder.addYellow(cR);
            } else if (Character.isUpperCase(cR)) {  // If the letter was green
                letterSlots[i].setLetter(cR);
                letterSlots[i].setGreen();
                letterHolder.addGreen(cR);
            }
        }

        return filterWords(triedLower);
    }

    public static boolean checkForEquality(String a, String b) {

        if (a.length() != b.length()) return false;

        for (int i = 0; i < a.length(); i++) {
            if (b.charAt(i) == '?') continue;
            if (a.charAt(i) != Character.toLowerCase(b.charAt(i))) return false;
        }

        return true;
    }

    public static boolean checkForIllegalCharacters(String a, String b) {

        if (a.matches("[^a-zA-Z]")) return false;
        if (b.matches("[^a-zA-Z\\?]")) return false;

        return true;

    }

    public Set<String> filterWords(String tried) {
        Set<String> output = new HashSet<>();

        // Use the whole dictionary on the first pass. After that, only use the possible correct words.
        Iterator<String> wordsIterable = possibleWords.isEmpty() ? words.iterator() : possibleWords.iterator();

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
                if (letterHolder.getInvalidLetters().contains(word.charAt(i))) continue wordLoop;
            }

            // Finally, add the word to the output list, if it hasn't been tried yet.
            if (!word.equals(tried)) output.add(word);
        }

        possibleWords = new HashSet<>(output);

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