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
            System.out.println();
            System.out.println(tree);

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
                        break;

                    default:
                        System.out.println("Do not understand label: " + tr.label().toString());
                }

            }
        }
        return sent;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        StanfordCoreNLP coreNLP = new StanfordCoreNLP(props);

        HashMap<String, List<Sentence>> sentenceMap = new HashMap<>();

//        String text = "John picked up the red block";
//        String text = "Pick up that block";
//        String text = "In 1921, Einstein received the Nobel Prize for his original work on the photoelectric effect.";
//        String text = "Did Einstein receive the Nobel Prize?";
//        String text = "Mary saw a ring through the window and asked John for it.";

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

        // print out the results
        for (List<Sentence> entry : sentenceMap.values()) {
            for (Sentence s: entry) {
                System.out.println(s);
            }
        }
        int correctProcess = (sentences.length-failedProcess);
        System.out.println("Successfully processed " +
                correctProcess +
                " sentences out of " +
                sentences.length +
                " (" +  ( ((double) correctProcess * 100.0/sentences.length) ) + "%)" );

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