import java.util.HashSet;
import java.util.Set;

public class LetterHolder {
    
    Set<Character> invalidLetters = new HashSet<>(26);
    Set<Character> greenLetters = new HashSet<>(5);
    Set<Character> yellowLetters = new HashSet<>(5);

    public void addGreen(char c) {
        char cLower = Character.toLowerCase(c);
        greenLetters.add(cLower);
        if (invalidLetters.contains(cLower)) invalidLetters.remove(cLower);
    }

    public void addYellow(char c) {
        char cLower = Character.toLowerCase(c);
        yellowLetters.add(cLower);
        if (invalidLetters.contains(cLower)) invalidLetters.remove(cLower);
    }

    public void addInvalid(char c) {
        char cLower = Character.toLowerCase(c);
        invalidLetters.add(cLower);
    }

    public Set<Character> getInvalidLetters() {
        return invalidLetters;
    }

}
