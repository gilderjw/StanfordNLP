public class Sentence {
    public String subject;
    public String object;
    public String predicate;

    @Override
    public String toString() {
        return "Subject: " + this.subject + "\n" +
                "Predicate: " + this.predicate + "\n" +
                "Object: " + this.object + "\n";
    }
}
