import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.LabeledScoredTreeNode;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;


public class NLP {

    public static final HashMap<String, String> synonyms = new HashMap<>();
    public static final HashSet<String> fillerWords = new HashSet<>();

    public static String getSynonym(String s){
        if (synonyms.containsKey(s.toLowerCase())) {
            return synonyms.get(s.toLowerCase());
        } else {
            return s.toLowerCase();
        }
    }

    public static Sentence processSentence(String text, StanfordCoreNLP coreNLP) {

        Annotation document = new Annotation(text);
        coreNLP.annotate(document);
        // run all Annotators on this text

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);

        Sentence sent = new Sentence();
        for (CoreMap sentence : sentences) {
            LabeledScoredTreeNode tree = (LabeledScoredTreeNode) sentence.get(TreeAnnotation.class).getChild(0);
//            System.out.println();
//            System.out.println(tree);

            String fileContents = "";

            for (Tree tr : tree.getChildrenAsList()) {
                switch (tr.label().toString()) {
                    case "VP":
                        for (Tree tre : tr.getChildrenAsList()) {
                            if (tre.label().toString().equals("NP")) {
                                if (sent.object != null) {
                                    break;
                                }
                                sent.object = tre.getLeaves().stream().map(
                                        (item) -> item.toString()).collect(Collectors.joining(" "));
                            }
                        }
                        if (sent.predicate != null) {
                            break;
                        }
                        sent.predicate = tr.getLeaves().stream().map(
                                (item) -> item.toString()).collect(Collectors.joining(" "));
                        break;
                    case "NP":
                        if (sent.subject != null) {
                            break;
                        }
                        sent.subject = tr.getLeaves().stream().map(
                                (item) -> item.toString()).collect(Collectors.joining(" "));
                        sent.subject = getSynonym(sent.subject);
                        break;

                    default:
                        System.out.println("Do not understand label: " + tr.label().toString());
                }

            }
        }
        return sent;
    }

    public static String answerQuestion(StanfordCoreNLP nlp, Map<String, List<Sentence>> corpus, String question) {
        Sentence qSentence = processSentence(question, nlp);
        int score = 0;

        // get predicate words
        String predicate[] = qSentence.predicate.split("(?<=[a-zA-Z0-9][,;:]? )");
        predicate = Arrays.stream(predicate)
                .map(
                        (String s) ->
                                s.replaceAll("[,;:]? ", ""))
                .filter(
                        (String s) -> !fillerWords.contains(s.toLowerCase()))
                .toArray(String[]::new);

//        for (String s: predicate) {
//            System.out.println(s);
//        }

        int neededScore = predicate.length/2;

        List<Sentence> statements = corpus.get(getSynonym(qSentence.subject));

        if (statements == null) {
            return "I don't know";
        }

        // look through sentences with the same subject
        for (Sentence stmt: statements) {
            if (stmt.predicate == null) {
                continue;
            }

            String stmtPreds[] = stmt.predicate.split("[,;:]* ");

            // count the number of words in the predicates
            for (String s1: predicate) {
                for (String s2: stmtPreds) {
                    s1 = getSynonym(s1);
                    s2 = getSynonym(s2);

                    if (s1.equalsIgnoreCase(s2)) {
                        if (++score >= neededScore) {
                            return "yes";
                        }
                    }
                }

            }

        }
        return "no";
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        // set up some synonyms
        synonyms.put("abraham lincoln", "lincoln");
        synonyms.put("abraham", "lincoln");
        synonyms.put("he", "lincoln");

        // words that do not contribute to the meaning of the sentence
        fillerWords.add("the");
        fillerWords.add("is");
        fillerWords.add("was");
        fillerWords.add("a");
        fillerWords.add("in");
        fillerWords.add("are");
        fillerWords.add("it's");


        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        StanfordCoreNLP coreNLP = new StanfordCoreNLP(props);

        HashMap<String, List<Sentence>> sentenceMap = new HashMap<>();

        File f = new File("lincoln.txt2");

        // split the file up into sentences
        String fileContents = new String(Files.readAllBytes(f.toPath())).replaceAll("\n", "");
        String sentences[] = fileContents.split("(?<=([a-z0-9][\\.\\?]))");

        int failedProcess = 0;
        // process the sentences
        for (String s: sentences) {
            Sentence sentence = processSentence(s, coreNLP);

            if (sentence.object == null &&
                    sentence.subject == null &&
                    sentence.predicate == null) {
                failedProcess++;
            }

            if (sentenceMap.containsKey(sentence.subject)) {
                sentenceMap.get(sentence.subject).add(sentence);
            } else {
                ArrayList<Sentence> newList = new ArrayList<>();
                newList.add(sentence);
                sentenceMap.put(sentence.subject, newList);
            }
        }

//        // print out the results
//        for (List<Sentence> entry : sentenceMap.values()) {
//            for (Sentence s: entry) {
//                System.out.println(s);
//            }
//        }
        int correctProcess = (sentences.length-failedProcess);
        System.out.println("Successfully processed " +
                correctProcess +
                " sentences out of " +
                sentences.length +
                " (" +  ( ((double) correctProcess * 100.0/sentences.length) ) + "%)" );

        Scanner scan = new Scanner(System.in);

        for (;;) {
            String command = scan.nextLine();
            if (command.equals('q')) {
                break;
            }

            if (command == null) {
                continue;
            }

            System.out.println(answerQuestion(coreNLP, sentenceMap, command));
        }


    }

    // Processes: {This, that} one?
    static public void processDeterminer(SemanticGraph dependencies, IndexedWord root){
        List<Pair<GrammaticalRelation,IndexedWord>> s = dependencies.childPairs(root);

        System.out.println("Identity of object: " + root.originalText().toLowerCase());
    }

    //Processes: {That, this, the} {block, sphere}
    static public void processNounPhrase(SemanticGraph dependencies, IndexedWord root){
        List<Pair<GrammaticalRelation,IndexedWord>> s = dependencies.childPairs(root);

        System.out.println("Subject: " + root.originalText().toLowerCase());
//        System.out.println("Type of object: " + s.get(0).second.originalText().toLowerCase());
    }

    // Processes: {Pick up, put down} {that, this} {block, sphere}
    static public void processVerbPhrase(SemanticGraph dependencies, IndexedWord root){
        List<Pair<GrammaticalRelation,IndexedWord>> s = dependencies.childPairs(root);
        Pair<GrammaticalRelation,IndexedWord> prt = s.get(0);
        Pair<GrammaticalRelation,IndexedWord> dobj = s.get(1);

        List<Pair<GrammaticalRelation,IndexedWord>> newS = dependencies.childPairs(dobj.second);

        System.out.println("Predicate" + root.originalText().toLowerCase() + prt.second.originalText().toLowerCase());
        System.out.println("Action: " + root.originalText().toLowerCase() + prt.second.originalText().toLowerCase());
        System.out.println("Type of object: " + dobj.second.originalText().toLowerCase());
        System.out.println("Identity of object: " + newS.get(0).second.originalText().toLowerCase());
    }

}