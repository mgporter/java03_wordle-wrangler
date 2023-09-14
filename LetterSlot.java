public class LetterSlot {
    
    private char letter;
    private boolean green;
    private boolean yellow;
    private boolean gray;

    public LetterSlot() {
        clear();
    }

    public void setLetter(char c) {
        letter = Character.toLowerCase(c);
    }

    public char getLetter() {
        return letter;
    }

    public String getString() {
        return Character.toString(letter);
    }

    public void setGreen() {
        green = true;
        yellow = false;
        gray = false;
    }

    public void setYellow() {
        green = false;
        yellow = true;
        gray = false;
    }

    public void setGray() {
        green = false;
        yellow = false;
        gray = true;
    }

    public boolean isGreen() {
        return green;
    }

    public boolean isYellow() {
        return yellow;
    }

    public boolean isGray() {
        return gray;
    }

    public void clear() {
        letter = ' ';
        green = false;
        yellow = false;
        gray = true;
    }
}
